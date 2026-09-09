package com.o2o;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 근거: 작업 계약 task-S9-catalog 8절 3단계와 결정 D-5.
 *
 * 11 API 명세에 헬스 체크 경로가 없다. 명세에 없는 공개 경로를 만들면 코드 평가 기준의
 * API 계약 준수 축에 걸리므로, 살아 있음 확인을 경로 대신 이 테스트가 맡는다.
 *
 * 대상이 실제 MySQL인 이유는 06-4 1-4 검증 책임 위치 표가 유일성과 무결성을 DB에 두기
 * 때문이다. 메모리 저장소로 바꾸면 그 책임을 확인할 수 없다.
 */
@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void springContextStartsAndConnectsToMysql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            // 제품 이름까지 보는 이유: 붙기만 하면 통과시키면 나중에 내장 DB로 바뀌어도 초록이 뜬다
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
            assertTrue(connection.isValid(2));
        }
    }
}
