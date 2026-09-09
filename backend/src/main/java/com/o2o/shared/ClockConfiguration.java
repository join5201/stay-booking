package com.o2o.catalog.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 설계 근거: 11 공통 요청과 응답 규칙. 시각은 UTC의 YYYY-MM-DDTHH:mm:ss.SSSZ 형식이다.
 * 그래서 systemUTC를 쓴다. 숙박과 캠페인 날짜의 Asia/Seoul은 재고와 요금 계열이라 범위 밖이다.
 *
 * 앱 서비스가 Clock을 주입받는 이유는 eval-criteria-code.md의 테스트 격리와 재현성 축이
 * 시간 제어를 요구하기 때문이다. 테스트는 이 빈을 고정 Clock으로 바꿔 끼운다.
 *
 * 이 빈이 catalog 아래 있는 것은 이번 바퀴에 catalog만 있기 때문이다. 두 번째 컨텍스트가
 * 시각을 쓰는 순간 shared로 올린다. 한 컨텍스트만 쓰는 것을 공유 커널에 두지 않는다는
 * com.o2o.shared 패키지 주석의 규칙을 따른다.
 */
@Configuration
public class CatalogClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
