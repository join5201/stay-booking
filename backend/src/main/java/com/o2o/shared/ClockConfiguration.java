package com.o2o.shared;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 설계 근거: 11 공통 요청과 응답 규칙. 시각은 UTC의 YYYY-MM-DDTHH:mm:ss.SSSZ 형식이다.
 * 그래서 systemUTC를 쓴다. 숙박 날짜의 오늘 판정도 같은 UTC 기준으로 한다.
 *
 * 앱 서비스가 Clock을 주입받는 이유는 eval-criteria-code.md의 테스트 격리와 재현성 축이
 * 시간 제어를 요구하기 때문이다. 테스트는 이 빈을 고정 Clock으로 바꿔 끼운다.
 *
 * 2026-09-09에 catalog.infrastructure에서 여기로 올렸다. 옮긴 근거는 옮기기 전 파일이 스스로
 * 적어 둔 조건이다. 두 번째 컨텍스트가 시각을 쓰는 순간 shared로 올린다고 적었고, 재고와 요금
 * 앱 서비스가 그 두 번째다. 카탈로그 아래 두면 재고가 카탈로그의 빈에 기대게 되고 그것은
 * 평가 축의 경계 위반이다.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
