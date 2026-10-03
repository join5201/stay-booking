-- 검색이 읽는 두 표의 인덱스 (이슈 242)
--
-- 왜 이 파일이 필요한가: 검색은 지역의 숙소를 region_code로, 그 숙소들의 객실 타입을 property_id로 읽는다.
-- 두 표에는 기본 키 말고 인덱스가 없어 매번 표 전체를 훑고, 객실 타입은 훑은 뒤 다시 정렬한다.
-- 2026-10-02 측정 DB(숙소 1,000, 객실 타입 3,000)의 제주 검색에서 객실 타입 훑기와 정렬이 9.26ms였다.
--
-- InnoDB 보조 인덱스는 끝에 기본 키 id를 품는다. 그래서 (region_code)는 order by id를,
-- (property_id)는 order by property_id, id를 그대로 따른다.

CREATE INDEX `idx_property_region_code` ON `property` (`region_code`);
CREATE INDEX `idx_room_type_property_id` ON `room_type` (`property_id`);
