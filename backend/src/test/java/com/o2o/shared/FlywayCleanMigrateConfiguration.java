package com.o2o.shared;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 테스트 컨텍스트가 뜰 때마다 DB를 비우고 표를 다시 만든다(이슈 233).
 *
 * 2026-10-01 전에는 ddl-auto=create-drop이 그 일을 했다. 표를 Flyway가 만들게 되면서 Hibernate는
 * validate만 하므로 비우는 일을 여기로 옮겼다. 안 비우면 앞 컨텍스트와 앞 실행의 데이터가 남는다.
 *
 * 테스트 소스에만 있다. 운영 설정은 clean이 막혀 있고(spring.flyway.clean-disabled 기본값 true)
 * 이 클래스도 없어서 migrate만 한다. @SpringBootTest의 컴포넌트 스캔이 이 클래스를 찾는다.
 */
@Configuration
class FlywayCleanMigrateConfiguration {

    @Bean
    FlywayMigrationStrategy cleanThenMigrate() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
