package com.o2o.inventory.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.o2o.inventory.domain.DailyInventory;
import com.o2o.inventory.domain.DailyInventoryRepository;
import com.o2o.inventory.domain.InventoryAllocationService;
import com.o2o.inventory.domain.InventoryShortageException;
import com.o2o.shared.RoomTypeId;
import com.o2o.shared.SeoulDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 마지막 객실 한 칸의 동시 선점 측정 장치. 설계 근거: 06-4 0절의 비관적 락 선언, 06-4 1-2 hold(n)의
 * Pre(잠금 뒤 가용 확인), 이슈 252.
 *
 * 요구사항 검증이 아니라 측정이다. 재고 1칸에 요청 여럿이 같은 순간 선점을 시도하는 판을 방식마다
 * 돌리고, 판마다 성공 수와 재고 부족 거절 수와 남은 선점 수를 센다.
 *
 * 기준선. 선점 경로에서 잠금 한 줄만 뺀 것이다. 잠금 없는 범위 조회(findRange)로 읽고 hold를
 * 적용해 저장한다. 운영 코드에는 없고 이 테스트에만 둔다. 재고의 version은 hold가 손으로 올리는
 * 값이라 저장할 때 대조하지 않는다. 그래서 같은 가용 1을 읽은 요청이 모두 통과하고, 저장은 각자
 * 계산한 held_count = 1을 덮어쓴다. 성공이 둘 이상인데 선점 수는 1로 남는 판이 초과 판매다.
 * 몇 판에서 나는지는 스레드 출발 시차에 따라 달라서 기록만 하고 상한을 걸지 않는다.
 *
 * 현재. 선점 경로가 부르는 InventoryAllocationService.hold 그대로다. 잠금 조회가 요청을 줄
 * 세우므로 판마다 성공 1, 나머지는 재고 부족이어야 하고 선점 수는 1이다.
 *
 * 테스트 트랜잭션을 쓰지 않는다. 요청마다 TransactionTemplate이 자기 트랜잭션을 연다. 격리 수준은
 * 풀 기본값(READ COMMITTED)을 그대로 쓴다. 판마다 새 객실 타입을 쓰고, 이 테스트의 머리가 붙은
 * 행만 앞뒤로 지운다.
 *
 * 실행 예시. backend 폴더에서, MySQL 컨테이너가 떠 있어야 한다
 *   ./gradlew test --tests com.o2o.inventory.infrastructure.LastRoomHoldRaceMeasurementTest
 *   결과 표는 build/measurements/last-room-hold-race.md와 테스트 리포트의 표준 출력에 남는다
 *
 * 실패 시 출력 예시
 *   Communications link failure
 *     컨테이너가 안 떠 있다. docker compose -f backend/docker-compose.yml up -d
 *   현재 경로인데 한 판에서 성공이 1이 아니다 ==> expected: <1> but was: <2>
 *     잠금 조회가 요청을 줄 세우지 못했다. findRangeForUpdate의 잠금을 먼저 본다
 */
@SpringBootTest
class LastRoomHoldRaceMeasurementTest {

    /** 방식과 동시 요청 수마다 이만큼 판을 돈다 */
    private static final int ROUNDS = 30;

    /** 마지막 객실 하나 */
    private static final int TOTAL = 1;

    /** 이 테스트가 만든 행만 지우려고 붙이는 객실 타입 ID 머리. LIKE의 와일드카드인 밑줄을 피했다 */
    private static final String PREFIX = "race-room-";

    private static final Path REPORT = Path.of("build", "measurements", "last-room-hold-race.md");

    /** 테스트 메서드마다 인스턴스가 새로 생기므로 결과는 정적 맵에 모아 마지막에 한 번 쓴다 */
    private static final Map<String, String> SECTIONS = new ConcurrentHashMap<>();

    @Autowired
    private DailyInventoryRepository inventoryRepository;

    @Autowired
    private InventoryAllocationService allocationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbcTemplate.update("delete from daily_inventory where room_type_id like ?", PREFIX + "%");
    }

    /** 기준선, 두 손님. 집계가 맞는지만 건다. 초과 판매 판 수는 기록만 한다 */
    @Test
    void 기준선_잠금_없이_읽으면_동시_2건의_초과_판매_판_수를_기록한다() throws Exception {
        measureBaseline(2);
    }

    /** 기준선, 열 손님 */
    @Test
    void 기준선_잠금_없이_읽으면_동시_10건의_초과_판매_판_수를_기록한다() throws Exception {
        measureBaseline(10);
    }

    /** 현재, 두 손님. 판마다 성공 1, 재고 부족 1, 선점 1 */
    @Test
    void 현재_잠금_조회로_읽으면_동시_2건에서_판마다_한_요청만_선점한다() throws Exception {
        measureCurrent(2);
    }

    /** 현재, 열 손님. 판마다 성공 1, 재고 부족 9, 선점 1 */
    @Test
    void 현재_잠금_조회로_읽으면_동시_10건에서_판마다_한_요청만_선점한다() throws Exception {
        measureCurrent(10);
    }

    private void measureBaseline(int requests) throws Exception {
        Run run = measure(this::holdWithoutLock, requests);
        record("baseline-" + requests, run.row("기준선: 잠금 없이 읽고 선점"));

        assertEquals(0, run.failed(), "재고 부족 말고 다른 실패가 나면 측정이 성립하지 않는다: "
                + run.firstFailure());
        for (Round round : run.rounds()) {
            assertEquals(requests, round.succeeded() + round.shortage());
            // 덮어쓰기라 선점 수는 성공 수와 관계없이 1이다. 1이 아니면 실패 모양이 예상과 다르다
            assertEquals(TOTAL, round.held(), "예상한 덮어쓰기가 아니다: " + round);
        }
    }

    private void measureCurrent(int requests) throws Exception {
        Run run = measure(this::holdWithLock, requests);
        record("current-" + requests, run.row("현재: 잠금 조회로 읽고 선점"));

        assertEquals(0, run.failed(), "실패가 났다: " + run.firstFailure());
        for (Round round : run.rounds()) {
            assertEquals(1, round.succeeded(), "현재 경로인데 한 판에서 성공이 1이 아니다");
            assertEquals(requests - 1, round.shortage());
            assertEquals(TOTAL, round.held());
        }
    }

    /** 표 하나에 네 칸을 모은다. 한 메서드만 돌렸으면 그 결과만 쓴다 */
    @AfterAll
    static void writeReport() throws IOException {
        if (SECTIONS.isEmpty()) {
            return;
        }
        StringBuilder md = new StringBuilder()
                .append("# 마지막 객실 동시 선점 측정\n\n")
                .append(SECTIONS.getOrDefault("env", "")).append("\n\n")
                .append("| 방식 | 동시 요청 | 판 | 초과 판매 판 | 판당 최대 성공 | 성공 합 | 재고 부족 합 | "
                        + "그 밖의 실패 | 남은 선점(판마다) | 요청 최대 ms |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (String key : List.of("baseline-2", "current-2", "baseline-10", "current-10")) {
            if (SECTIONS.containsKey(key)) {
                md.append(SECTIONS.get(key)).append('\n');
            }
        }
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, md.toString());
    }

    /** 한 요청이 1박을 1실 선점하는 방법. 요청마다 새 트랜잭션 안에서 불린다 */
    @FunctionalInterface
    private interface OneNightHold {
        void hold(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut);
    }

    /**
     * 기준선. InventoryAllocationService.lock에서 잠금 조회만 잠금 없는 조회로 바꾼 것이다. 운영
     * 코드에는 없다. 잠금이 없을 때 무엇이 깨지는지 보려고 이 테스트에만 둔다.
     */
    private void holdWithoutLock(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        List<DailyInventory> unlocked = inventoryRepository.findRange(roomTypeId, checkIn, checkOut);
        allocationService.hold(unlocked, 1, Instant.now());
    }

    /** 현재. 선점 경로가 부르는 그대로다 */
    private void holdWithLock(RoomTypeId roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        allocationService.hold(roomTypeId, checkIn, checkOut, 1, Instant.now());
    }

    /** 판마다 새 객실 타입에 1박 재고 1칸을 열고, 요청 requests개를 같은 순간 출발시킨다 */
    private Run measure(OneNightHold method, int requests) throws Exception {
        LocalDate checkIn = firstNight();
        LocalDate checkOut = checkIn.plusDays(1);
        Run run = new Run(requests);
        ExecutorService threads = Executors.newFixedThreadPool(requests);
        try {
            for (int r = 0; r < ROUNDS; r++) {
                RoomTypeId roomTypeId = newRoomTypeId();
                inTransaction(() -> inventoryRepository.saveAll(
                        List.of(DailyInventory.open(roomTypeId, checkIn, TOTAL, Instant.now()))));

                CountDownLatch ready = new CountDownLatch(requests);
                CountDownLatch go = new CountDownLatch(1);
                List<Future<Outcome>> sent = new ArrayList<>();
                for (int i = 0; i < requests; i++) {
                    sent.add(threads.submit(request(method, roomTypeId, checkIn, checkOut, ready, go)));
                }
                // 모두 출발선에 선 뒤에 같이 놓는다. 먼저 뜬 쪽이 혼자 끝내면 경합이 생기지 않는다
                ready.await();
                go.countDown();
                Round round = new Round();
                for (Future<Outcome> f : sent) {
                    run.add(round, f.get(60, TimeUnit.SECONDS));
                }
                run.addRound(round.withHeld(heldOf(roomTypeId, checkIn)));
            }
        } finally {
            threads.shutdownNow();
        }
        return run;
    }

    private Callable<Outcome> request(OneNightHold method, RoomTypeId roomTypeId, LocalDate checkIn,
                                      LocalDate checkOut, CountDownLatch ready, CountDownLatch go) {
        return () -> {
            ready.countDown();
            go.await();
            long began = System.nanoTime();
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(
                        status -> method.hold(roomTypeId, checkIn, checkOut));
                return new Outcome(Result.SUCCEEDED, System.nanoTime() - began, null);
            } catch (InventoryShortageException e) {
                return new Outcome(Result.SHORTAGE, System.nanoTime() - began, null);
            } catch (RuntimeException e) {
                return new Outcome(Result.FAILED, System.nanoTime() - began, e);
            }
        };
    }

    /** T4. 날짜를 박지 않고 오늘(서울)에서 센다 */
    private static LocalDate firstNight() {
        return LocalDate.now(SeoulDate.ZONE).plusDays(30);
    }

    private static RoomTypeId newRoomTypeId() {
        return RoomTypeId.of(PREFIX + UUID.randomUUID().toString().replace("-", ""));
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    private int heldOf(RoomTypeId roomTypeId, LocalDate night) {
        Integer held = jdbcTemplate.queryForObject(
                "select held_count from daily_inventory where room_type_id = ? and stay_date = ?",
                Integer.class, roomTypeId.value(), Date.valueOf(night));
        return Objects.requireNonNull(held);
    }

    private void record(String key, String value) {
        SECTIONS.putIfAbsent("env", environment());
        SECTIONS.put(key, value);
        System.out.println("[측정 " + key + "] " + value);
    }

    /** 결과를 읽을 때 조건이 같이 보이게 한다 */
    private String environment() {
        String version = jdbcTemplate.queryForObject("select version()", String.class);
        String isolation = jdbcTemplate.queryForObject("select @@transaction_isolation", String.class);
        return "측정 " + Instant.now() + ". MySQL " + version + ", 격리 수준 " + isolation
                + ". 방식과 동시 요청 수마다 " + ROUNDS + "판, 판마다 새 객실 타입의 1박 재고 " + TOTAL
                + "칸을 요청 여럿이 같은 순간 선점한다. 기준선은 운영 코드에 없는 테스트 전용 방식이고, "
                + "초과 판매 판 수는 스레드 출발 시차에 따라 달라진다.";
    }

    private enum Result {
        SUCCEEDED,
        SHORTAGE,
        FAILED
    }

    private record Outcome(Result result, long nanos, RuntimeException error) {
    }

    /** 한 판의 집계 */
    private static final class Round {

        private int succeeded;
        private int shortage;
        private int held = -1;

        Round withHeld(int value) {
            this.held = value;
            return this;
        }

        int succeeded() {
            return succeeded;
        }

        int shortage() {
            return shortage;
        }

        int held() {
            return held;
        }

        @Override
        public String toString() {
            return "성공 " + succeeded + ", 재고 부족 " + shortage + ", 남은 선점 " + held;
        }
    }

    /** 한 방식의 집계. 판은 테스트 스레드가 차례로 돌며 합치므로 동기화가 필요 없다 */
    private static final class Run {

        private final int requests;
        private final List<Round> rounds = new ArrayList<>();
        private int failed;
        private long maxRequestNanos;
        private RuntimeException firstFailure;

        Run(int requests) {
            this.requests = requests;
        }

        void add(Round round, Outcome outcome) {
            switch (outcome.result()) {
                case SUCCEEDED -> round.succeeded++;
                case SHORTAGE -> round.shortage++;
                case FAILED -> {
                    failed++;
                    if (firstFailure == null) {
                        firstFailure = outcome.error();
                    }
                }
            }
            maxRequestNanos = Math.max(maxRequestNanos, outcome.nanos());
        }

        void addRound(Round round) {
            rounds.add(round);
        }

        String row(String label) {
            int oversold = (int) rounds.stream().filter(r -> r.succeeded() > TOTAL).count();
            int maxSucceeded = rounds.stream().mapToInt(Round::succeeded).max().orElse(0);
            int succeeded = rounds.stream().mapToInt(Round::succeeded).sum();
            int shortage = rounds.stream().mapToInt(Round::shortage).sum();
            String held = rounds.stream().mapToInt(Round::held).distinct().sorted()
                    .mapToObj(String::valueOf).reduce((a, b) -> a + ", " + b).orElse("");
            return "| " + label + " | " + requests + " | " + rounds.size() + " | " + oversold + " | "
                    + maxSucceeded + " | " + succeeded + " | " + shortage + " | " + failed + " | "
                    + held + " | " + maxRequestNanos / 1_000_000 + " |";
        }

        List<Round> rounds() {
            return rounds;
        }

        int failed() {
            return failed;
        }

        RuntimeException firstFailure() {
            return firstFailure;
        }
    }
}
