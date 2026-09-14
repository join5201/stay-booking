package com.o2o.shared;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 등록된 지역 코드 fixture. 설계 근거: 11 CAT-01 처리 규칙(등록된 지역 코드를 확인하고 저장한다.
 * 지역 fixture와 프론트 선택 목록은 초기 세팅에서 준비한다), CAT-01과 CAT-02와 CAT-04 필드표의
 * 등록된 지역 코드. R1 평가 A-02와 B-02.
 *
 * shared에 두는 이유는 검색 SEARCH-01과 프로모션의 regionCodes도 같은 문구를 쓰기 때문이다.
 * 그 둘이 이 목록을 확인할지는 프로모션과 검색 쌍의 결정표가 가른다. 이번 바퀴에 확인하는 것은
 * 카탈로그의 등록과 수정과 목록 셋이다.
 *
 * 값을 코드에 두는 이유는 ActorRegistry와 같다. 비밀이 아니고 초기 세팅 값이다. 목록은 광역
 * 자치단체 17곳의 영문 코드로 제안한 것이고 사용자가 정한다(결정표 A-02 행. 명세 예시는 SEOUL
 * 하나뿐이다). 바꿀 때는 이 목록 한 곳만 고친다.
 *
 * register는 초기 세팅과 테스트 fixture용이다. 요청 경로에서 부르지 않는다. 테스트가 서로의
 * 목록에 섞이지 않게 고유한 코드를 쓰는데 그 코드가 여기 있어야 등록이 통과하기 때문이다.
 */
@Component
public class RegionRegistry {

    public static final int CODE_MAX_LENGTH = 32;

    private static final List<String> FIXTURE = List.of(
            "SEOUL", "BUSAN", "DAEGU", "INCHEON", "GWANGJU", "DAEJEON", "ULSAN", "SEJONG",
            "GYEONGGI", "GANGWON", "CHUNGBUK", "CHUNGNAM", "JEONBUK", "JEONNAM",
            "GYEONGBUK", "GYEONGNAM", "JEJU");

    private final Set<String> codes = ConcurrentHashMap.newKeySet();

    public RegionRegistry() {
        codes.addAll(FIXTURE);
    }

    public boolean isRegistered(String code) {
        return code != null && codes.contains(code);
    }

    /** 등록된 코드가 아니면 거절한다. 400 INVALID_REQUEST로 나간다 */
    public void require(String code) {
        if (!isRegistered(code)) {
            throw new UnregisteredRegionException(code);
        }
    }

    /** 초기 세팅과 테스트 fixture용. 요청 경로에서 부르지 않는다 */
    public void register(String code) {
        if (code == null || code.isBlank() || code.length() > CODE_MAX_LENGTH) {
            throw new IllegalArgumentException("지역 코드는 1자 이상 " + CODE_MAX_LENGTH
                    + "자 이하여야 한다: " + code);
        }
        codes.add(code);
    }
}
