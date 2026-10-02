package com.o2o.shared;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 표를 Flyway가 만들었는지 본다(이슈 233).
 *
 * 컨텍스트가 뜬 것만으로 V1이 만든 표와 엔티티가 맞는다는 것은 확인된다(ddl-auto=validate).
 * 이 테스트는 그 표를 만든 것이 Hibernate가 아니라 Flyway라는 것을 이력 표로 본다.
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void historyTableRecordsEveryMigrationAsSuccess() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select version, success from flyway_schema_history order by installed_rank");

        // 판이 늘면 이 줄이 아니라 아래 success 검사가 새 판까지 본다. 첫 판이 1인 것만 고정한다
        assertEquals("1", rows.get(0).get("version"));
        for (Map<String, Object> row : rows) {
            assertEquals(Boolean.TRUE, row.get("success"), "실패한 판: " + row.get("version"));
        }
    }

    // V2. 검색이 읽는 두 칸의 인덱스(이슈 242). 이름과 칸까지 본다
    @Test
    void searchIndexesExist() {
        List<String> indexes = jdbcTemplate.queryForList("""
                select concat(table_name, '.', index_name, '.', column_name)
                from information_schema.statistics
                where table_schema = database() and index_name in ('idx_property_region_code', 'idx_room_type_property_id')
                order by 1""", String.class);

        assertEquals(List.of(
                "property.idx_property_region_code.region_code",
                "room_type.idx_room_type_property_id.property_id"), indexes);
    }
}
