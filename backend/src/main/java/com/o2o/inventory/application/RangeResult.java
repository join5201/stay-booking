package com.o2o.inventory.application;

import java.time.LocalDate;
import java.util.List;

/**
 * 기간 조회의 결과. 설계 근거: 11 응답 모델 InventoryRange와 RateRange가 items와
 * missingDates 두 열을 갖는다.
 *
 * 왜 앱 서비스가 missingDates까지 만드나. 없는 날짜를 알려면 요청한 기간 전체를 알아야
 * 하는데 그것을 아는 것은 유스케이스다. 리포지토리는 있는 것만 돌려주고 없는 것을 모른다.
 * api 층에서 만들면 그 층이 기간을 펼치는 계산을 하게 되고 그것은 형식 변환이 아니다.
 *
 * 재고와 요금이 같은 모양이라 하나를 타입 매개변수로 둔다. 둘을 따로 만들면 같은 코드가
 * 두 벌이 되고 한쪽만 고치는 사고가 난다. 명세도 두 모델을 같은 다섯 열로 적는다.
 */
public record RangeResult<T>(List<T> items, List<LocalDate> missingDates) {
}
