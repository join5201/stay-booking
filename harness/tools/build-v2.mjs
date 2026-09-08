import fs from 'node:fs';
import path from 'node:path';

const root = 'C:/Dev/potenup/99_projects/o2o';
const source = fs.readFileSync(path.join(root, '11-o2o-api-spec.md'), 'utf8').replace(/\r\n/g, '\n');
if (!source.includes('작성: 2026-09-07 v1')) throw new Error('Expected v1 source');
const section = (a, b) => source.slice(source.indexOf(a), b ? source.indexOf(b, source.indexOf(a)) : undefined);
const now = '2026-10-01T03:00:00.000Z';
const date1 = '2026-10-10', date2 = '2026-10-11', end = '2026-10-12';
const clone = v => structuredClone(v);
const field = (name, type, description, required = true) => ({name, type, description, required});
const id = name => field(name, 'string', '비어 있지 않은 ID, 최대 64자');
const version = field('version', 'integer', '0 이상. 마지막 조회 버전과 일치해야 함');
const stamp = name => field(name, 'timestamp', 'UTC 시각');
const pageFields = [field('page', 'integer', '0 이상, 기본 0', false), field('size', 'integer', '1~100, 기본 20', false)];
const stayFields = [field('checkIn', 'date', '서버의 오늘 이상, 숙박 시작일 포함'), field('checkOut', 'date', 'checkIn보다 뒤, 최대 30박, 끝 날짜 제외'), field('guestCount', 'integer', '1~100. 객실 수용 여부는 API별 처리 규칙에 따라 판단')];
const regionField = field('regionCode', 'string', '1~32자. 등록된 지역 코드');
const amountField = field('amount', 'integer', '1~1,000,000,000원');
const currencyField = field('currency', 'string', 'KRW 고정');
const rangeFields = [field('from', 'date', '범위 시작일 포함'), field('to', 'date', 'from보다 뒤, 최대 366일, 끝 날짜 제외')];
const countField = field('totalCount', 'integer', '0~100,000. 증감량이 아닌 총 수량');
const dayField = field('date', 'date', '등록일은 서버의 오늘 이상');
const propFields = [field('name', 'string', '공백 제거 후 1~100자'), regionField, field('address', 'string', '1~300자'), field('description', 'string', '최대 2,000자, 생략하면 빈 문자열', false)];
const roomFields = [field('name', 'string', '공백 제거 후 1~100자'), field('maxOccupancy', 'integer', '1~100'), field('description', 'string', '최대 2,000자, 생략하면 빈 문자열', false)];
const promoFields = [field('name', 'string', '1~100자'), field('discountRate', 'integer', '1~99, 백분율'), field('campaignStartDate', 'date', '캠페인 적용 판단일 시작, 포함'), field('campaignEndDate', 'date', '시작보다 뒤, 끝 날짜 제외'), field('stayStartDate', 'date/null', '할인 대상 숙박일 시작. stayEndDate와 함께 설정 또는 둘 다 null', false), field('stayEndDate', 'date/null', '시작보다 뒤, 끝 날짜 제외. 두 필드 생략 시 둘 다 null', false), field('minNights', 'integer', '1~30'), field('regionCodes', 'string[]', '최대 100개, 중복 금지. 등록된 지역 코드. 빈 배열은 전체 지역'), field('enabled', 'boolean', '기본 true', false)];
const patchFields = fields => [version, ...fields.map(f => ({...f, required:false, description:f.description.replace(/, 생략하면 빈 문자열|기본 true|두 필드 생략 시 둘 다 null/g, '').trim() + '. 생략하면 기존 값 유지'}))];

const property = {id:'prop_001',hostId:'host_001',name:'서울 스테이',regionCode:'SEOUL',address:'서울특별시 종로구 예시로 10',description:'숙박용 예시 숙소',version:0,createdAt:now,updatedAt:now};
const room = {id:'room_001',propertyId:'prop_001',name:'스탠다드 더블',maxOccupancy:2,description:'2인 객실',version:0,createdAt:now,updatedAt:now};
const inv = {roomTypeId:'room_001',date:date1,totalCount:5,heldCount:0,soldCount:0,availableCount:5,version:0};
const rate = {roomTypeId:'room_001',date:date1,amount:100000,currency:'KRW',version:0};
const promo = {id:'promo_001',name:'가을 할인',discountRate:10,campaignStartDate:'2026-10-01',campaignEndDate:'2026-11-01',stayStartDate:'2026-10-01',stayEndDate:'2026-12-01',minNights:2,regionCodes:['SEOUL'],enabled:true,version:0,createdAt:now,updatedAt:now};
const price = {currency:'KRW',baseTotalAmount:200000,discountTotalAmount:20000,totalAmount:180000,appliedPromotion:{id:'promo_001',name:'가을 할인',discountRate:10},days:[{date:date1,baseAmount:100000,discountAmount:10000,finalAmount:90000},{date:date2,baseAmount:100000,discountAmount:10000,finalAmount:90000}]};
const attempt = {id:'attempt_001',bookingId:'booking_001',attemptNumber:1,status:'REQUESTED',amount:180000,currency:'KRW',pgTransactionId:'mock_tx_001',mockMode:'APPROVE',requestedAt:now,completedAt:null,failureCode:null};
const booking = {id:'booking_001',guestId:'guest_001',propertyId:'prop_001',roomTypeId:'room_001',checkIn:date1,checkOut:end,guestCount:2,status:'HELD',expiresAt:'2026-10-01T03:10:00.000Z',expirationReason:null,priceSnapshot:price,payment:{attemptCount:0,approvedAttemptId:null,attempts:[],refund:null},cancellationReason:null,createdAt:now,updatedAt:now,confirmedAt:null,canceledAt:null,expiredAt:null,serverNow:now,version:0};
const canceled = clone(booking);
Object.assign(canceled,{status:'CANCELED',cancellationReason:'일정 변경',confirmedAt:'2026-10-01T03:00:01.000Z',canceledAt:'2026-10-01T03:05:00.000Z',updatedAt:'2026-10-01T03:05:00.000Z',serverNow:'2026-10-01T03:05:00.000Z',version:2});
canceled.payment={attemptCount:1,approvedAttemptId:'attempt_001',attempts:[{...attempt,status:'APPROVED',completedAt:'2026-10-01T03:00:01.000Z'}],refund:{id:'refund_001',paymentAttemptId:'attempt_001',amount:180000,currency:'KRW',status:'REFUNDED',reason:'BOOKING_CANCELED',refundedAt:'2026-10-01T03:05:00.000Z'}};
const page = value => ({items:[value],page:0,size:20,totalElements:1,totalPages:1});
const changed = (value, changes) => ({...value,...changes,version:value.version+1,...('updatedAt' in value ? {updatedAt:'2026-10-01T03:01:00.000Z'} : {})});
const schemas = {};
const schema = (name, fields) => {schemas[name]=fields;};
const responseFields = fields => fields.map(f=>({...f,required:true}));
schema('Property',[id('id'),id('hostId'),...responseFields(propFields),field('version','integer','변경 버전'),stamp('createdAt'),stamp('updatedAt')]);
schema('RoomType',[id('id'),id('propertyId'),...responseFields(roomFields),field('version','integer','변경 버전'),stamp('createdAt'),stamp('updatedAt')]);
schema('DailyInventory',[id('roomTypeId'),field('date','date','해당 숙박 날짜'),countField,field('heldCount','integer','선점 수, 0 이상'),field('soldCount','integer','판매 수, 0 이상'),field('availableCount','integer','totalCount - heldCount - soldCount, 0 이상'),field('version','integer','재고 변경 버전')]);
schema('DailyRate',[id('roomTypeId'),field('date','date','해당 숙박 날짜'),amountField,currencyField,field('version','integer','요금 변경 버전')]);
for (const [name, item] of [['InventoryRange','DailyInventory'],['RateRange','DailyRate']]) schema(name,[id('roomTypeId'),...rangeFields,field('items',item+'[]','date 오름차순. 존재하는 날짜만 포함'),field('missingDates','date[]','레코드가 없는 날짜, 없으면 빈 배열')]);
schema('Promotion',[id('id'),...responseFields(promoFields),field('version','integer','변경 버전'),stamp('createdAt'),stamp('updatedAt')]);
schema('AppliedPromotion',[id('id'),field('name','string','적용 시점에 복사한 이름'),field('discountRate','integer','적용한 백분율')]);
schema('PriceDay',[field('date','date','숙박 날짜'),field('baseAmount','integer','할인 전 1박 금액'),field('discountAmount','integer','날짜별 버림한 할인액'),field('finalAmount','integer','baseAmount - discountAmount')]);
schema('PriceSnapshot',[currencyField,field('baseTotalAmount','integer','days의 baseAmount 합계'),field('discountTotalAmount','integer','days의 discountAmount 합계'),field('totalAmount','integer','days의 finalAmount 합계'),field('appliedPromotion','AppliedPromotion/null','선택한 프로모션. 없으면 null'),field('days','PriceDay[]','숙박 전 날짜를 date 오름차순으로 포함')]);
schema('ApplicablePromotion',[id('id'),field('name','string','프로모션 이름'),field('discountRate','integer','할인 백분율'),field('discountAmount','integer','전체 숙박의 할인액'),field('selected','boolean','최종 적용 대상으로 선택됐는지')]);
schema('ApplicablePromotions',[id('roomTypeId'),...stayFields,stamp('evaluatedAt'),field('items','ApplicablePromotion[]','할인액 내림차순, 동률이면 ID 오름차순. 없으면 빈 배열'),field('selectedPromotionId','string/null','선택한 프로모션 ID, 없으면 null')]);
schema('RoomSearchResult',[id('roomTypeId'),field('name','string','객실 타입 이름'),field('maxOccupancy','integer','최대 인원'),field('availableCount','integer','숙박 전 날짜 최소 가용 수'),field('totalAmount','integer','할인 후 전체 숙박 금액')]);
schema('PropertySearchResult',[field('property','Property','숙소 정보'),field('lowestTotalAmount','integer','포함된 객실의 최저 할인 후 총액'),currencyField,field('availableRoomTypes','RoomSearchResult[]','모든 예약 가능 조건을 충족한 객실 타입')]);
schema('AvailabilityDay',[field('date','date','숙박 날짜'),field('availableCount','integer/null','재고가 있으면 가용 수, 레코드가 없으면 null')]);
schema('Availability',[id('roomTypeId'),...stayFields,field('available','boolean','인원, 전 날짜 재고와 요금을 모두 만족하는지'),field('availableCount','integer','재고가 모두 있으면 최소 가용 수, 누락이면 0'),field('days','AvailabilityDay[]','전 숙박 날짜, date 오름차순'),field('missingInventoryDates','date[]','재고 레코드 누락 날짜'),field('missingRateDates','date[]','요금 누락 날짜'),field('reasons','string[]','OCCUPANCY_EXCEEDED / INVENTORY_NOT_CONFIGURED / INVENTORY_UNAVAILABLE / RATE_NOT_CONFIGURED. 해당 항목만 중복 없이 반환')]);
schema('PriceQuote',[id('roomTypeId'),...stayFields,field('nights','integer','checkOut - checkIn, 1~30'),stamp('estimatedAt'),field('price','PriceSnapshot','조회 시점의 계산 결과. 예약 확정 금액은 아님')]);
schema('PaymentAttempt',[id('id'),id('bookingId'),field('attemptNumber','integer','1~3'),field('status','string','REQUESTED / APPROVED / FAILED'),field('amount','integer','예약 스냅샷 총액, 최대 30,000,000,000원'),currencyField,id('pgTransactionId'),field('mockMode','string','APPROVE / DECLINE / DEFER'),stamp('requestedAt'),field('completedAt','timestamp/null','미완료이면 null'),field('failureCode','string/null','FAILED이면 MOCK_DECLINED, 그 외 null')]);
schema('Refund',[id('id'),id('paymentAttemptId'),field('amount','integer','승인 금액 전액'),currencyField,field('status','string','REFUNDED'),field('reason','string','BOOKING_CANCELED / LATE_APPROVAL'),stamp('refundedAt')]);
schema('PaymentSummary',[field('attemptCount','integer','0~3'),field('approvedAttemptId','string/null','승인 시도 ID, 승인 전 null. 환불해도 유지'),field('attempts','PaymentAttempt[]','attemptNumber 오름차순, 최대 3개'),field('refund','Refund/null','환불 전 null')]);
schema('Booking',[id('id'),id('guestId'),id('propertyId'),id('roomTypeId'),...stayFields,field('status','string','HELD / CONFIRMED / CANCELED / EXPIRED'),field('expiresAt','timestamp','생성 시 저장한 Hold 만료 시각, 확정과 취소 후에도 유지'),field('expirationReason','string/null','EXPIRED이면 TTL_EXPIRED / PAYMENT_FAILED, 그 외 null'),field('priceSnapshot','PriceSnapshot','생성 시점에 고정한 금액'),field('payment','PaymentSummary','결제 시도와 환불 내역'),field('cancellationReason','string/null','취소 전 null, 이유 없이 취소하면 빈 문자열'),stamp('createdAt'),stamp('updatedAt'),field('confirmedAt','timestamp/null','확정 전 null, 취소 후에도 유지'),field('canceledAt','timestamp/null','취소 전 null'),field('expiredAt','timestamp/null','만료 전 null'),stamp('serverNow'),field('version','integer','예약 상태 변경 버전')]);
schema('PaymentAttemptList',[id('bookingId'),field('attemptCount','integer','0~3'),field('items','PaymentAttempt[]','attemptNumber 오름차순, 최대 3개')]);
schema('MockEventResult',[field('eventId','string','입력 이벤트 ID'),id('paymentAttemptId'),field('result','string','PROCESSED / DUPLICATE'),field('processedAt','timestamp','최초 업무 처리 완료 시각')]);
const pageSchema = name => {
  const key='Page<'+name+'>';
  if (!schemas[key]) schema(key,[field('items',name+'[]','현재 페이지 목록, 없으면 빈 배열'),field('page','integer','요청 페이지, 0 이상'),field('size','integer','요청 크기, 1~100'),field('totalElements','integer','전체 항목 수, 0 이상'),field('totalPages','integer','전체 페이지 수, 결과가 없으면 0')]);
  return key;
};

const apis=[];
function api(id,group,method,url,title,actor,responseSchema,response,config={}) {
  apis.push({id,group,method,url:(url.startsWith('/internal')?'':'/api/v1')+url,title,actor,responseSchema,response,status:method==='POST'?201:200,...config});
}
const ownHost='HOST, 대상 숙소의 소유자';
const ownGuest='GUEST, 대상 예약의 소유자';
const fieldsObject = (value, fields) => Object.fromEntries(fields.filter(f=>f.name in value).map(f=>[f.name,value[f.name]]));
const propCreate=fieldsObject(property,propFields), roomCreate=fieldsObject(room,roomFields), promoCreate=fieldsObject(promo,promoFields);
api('CAT-01','판매 준비: 숙소','POST','/properties','숙소 등록','HOST','Property',property,{bodyFields:propFields,body:propCreate,location:'/api/v1/properties/prop_001',rules:['hostId는 개발 행위자 정보에서 채운다. body로 소유자를 받지 않는다.','등록된 지역 코드를 확인하고 저장한다. 지역 fixture와 프론트 선택 목록은 초기 세팅에서 준비한다.'],tests:['T01']});
api('CAT-02','판매 준비: 숙소','PATCH','/properties/{propertyId}','숙소 수정',ownHost,'Property',changed(property,{name:'서울 스테이 본관'}),{bodyFields:patchFields(propFields),body:{version:0,name:'서울 스테이 본관'},errors:['VERSION_CONFLICT'],rules:['version 외 변경 필드가 한 개 이상 필요하다.','ID, hostId와 createdAt을 변경할 수 없다.'],tests:['T01','T02']});
api('CAT-03','판매 준비: 숙소','GET','/properties/{propertyId}','숙소 상세 조회','공개','Property',property,{tests:['T01']});
api('CAT-04','판매 준비: 숙소','GET','/properties','숙소 목록 조회','공개',pageSchema('Property'),page(property),{query:[{...regionField,required:false},...pageFields],tests:['T01']});
api('CAT-05','판매 준비: 숙소','GET','/host/properties','본인 숙소 목록 조회','HOST',pageSchema('Property'),page(property),{query:pageFields,rules:['행위자의 hostId로 범위를 제한한다. hostId 쿼리는 받지 않는다.'],tests:['T01','T02']});
api('CAT-06','판매 준비: 객실 타입','POST','/properties/{propertyId}/room-types','객실 타입 등록',ownHost,'RoomType',room,{bodyFields:roomFields,body:roomCreate,location:'/api/v1/room-types/room_001',rules:['부모 숙소의 소유자를 검사한다. propertyId는 경로에서 가져온다.'],tests:['T01','T02']});
api('CAT-07','판매 준비: 객실 타입','PATCH','/room-types/{roomTypeId}','객실 타입 수정',ownHost,'RoomType',changed(room,{name:'스탠다드 더블 A'}),{bodyFields:patchFields(roomFields),body:{version:0,name:'스탠다드 더블 A'},errors:['VERSION_CONFLICT'],rules:['propertyId를 변경하지 않는다.','최대 인원 수정은 신규 예약에 적용한다. 기존 예약의 guestCount는 바꾸지 않는다.'],tests:['T01','T02']});
api('CAT-08','판매 준비: 객실 타입','GET','/room-types/{roomTypeId}','객실 타입 상세 조회','공개','RoomType',room,{tests:['T01']});
api('CAT-09','판매 준비: 객실 타입','GET','/properties/{propertyId}/room-types','숙소의 객실 타입 목록 조회','공개',pageSchema('RoomType'),page(room),{query:pageFields,tests:['T01']});
api('INV-01','판매 준비: 재고','POST','/room-types/{roomTypeId}/inventories','날짜별 재고 등록',ownHost,'DailyInventory',inv,{bodyFields:[dayField,countField],body:{date:date1,totalCount:5},location:'/api/v1/room-types/room_001/inventories/'+date1,errors:['RESOURCE_ALREADY_EXISTS'],rules:['heldCount와 soldCount는 0으로 시작하며 입력받지 않는다.','객실 타입과 날짜의 조합이 이미 있으면 덮어쓰지 않는다.'],tests:['T01']});
const rangeResponse={roomTypeId:'room_001',from:date1,to:end,items:[inv,{...inv,date:date2}],missingDates:[]};
api('INV-02','판매 준비: 재고','POST','/room-types/{roomTypeId}/inventories/bulk','기간 재고 일괄 등록',ownHost,'InventoryRange',rangeResponse,{bodyFields:[...rangeFields,countField],body:{from:date1,to:end,totalCount:5},location:'/api/v1/room-types/room_001/inventories?from='+date1+'&to='+end,errors:['RESOURCE_ALREADY_EXISTS'],rules:['from은 서버의 오늘 이상이고, to는 제외한다. 최대 366일이다.','한 날짜라도 이미 존재하면 전부 실패한다. 새 날짜만 부분 등록하지 않는다.','성공 시 missingDates는 빈 배열이다. totalCount=0 등록도 허용한다.'],tests:['T03']});
api('INV-03','판매 준비: 재고','PATCH','/room-types/{roomTypeId}/inventories/{date}','날짜별 재고 수정',ownHost,'DailyInventory',changed(inv,{totalCount:8,availableCount:8}),{bodyFields:[version,countField],body:{version:0,totalCount:8},errors:['VERSION_CONFLICT','INVENTORY_BELOW_COMMITTED'],rules:['수정 날짜는 서버의 오늘 이상이다.','totalCount >= heldCount + soldCount를 동시성 제어 안에서 검사한다.','heldCount와 soldCount를 직접 수정하지 않는다.'],tests:['T04','T05']});
api('INV-04','판매 준비: 재고','GET','/room-types/{roomTypeId}/inventories','기간 재고 조회',ownHost,'InventoryRange',rangeResponse,{query:rangeFields,rules:['과거 날짜 조회를 허용한다. 없는 날짜는 items에 만들지 않고 missingDates에 적는다.'],tests:['T01','T07']});
api('INV-05','판매 준비: 재고','GET','/room-types/{roomTypeId}/inventories/{date}','날짜별 재고 조회',ownHost,'DailyInventory',inv,{rules:['과거 날짜 조회를 허용한다. 해당 날짜 레코드가 없으면 404다.'],tests:['T01']});
api('RATE-01','판매 준비: 요금','POST','/room-types/{roomTypeId}/rates','날짜별 요금 등록',ownHost,'DailyRate',rate,{bodyFields:[dayField,amountField,currencyField],body:{date:date1,amount:100000,currency:'KRW'},location:'/api/v1/room-types/room_001/rates/'+date1,errors:['RESOURCE_ALREADY_EXISTS'],tests:['T01']});
api('RATE-02','판매 준비: 요금','PATCH','/room-types/{roomTypeId}/rates/{date}','날짜별 요금 수정',ownHost,'DailyRate',changed(rate,{amount:110000}),{bodyFields:[version,amountField],body:{version:0,amount:110000},errors:['VERSION_CONFLICT'],rules:['수정 날짜는 서버의 오늘 이상이다. 통화는 바꾸지 않는다.','이미 생성된 예약의 가격 스냅샷은 변경하지 않는다.'],tests:['T05','T13']});
api('RATE-03','판매 준비: 요금','GET','/room-types/{roomTypeId}/rates','기간 요금 조회',ownHost,'RateRange',{roomTypeId:'room_001',from:date1,to:end,items:[rate,{...rate,date:date2}],missingDates:[]},{query:rangeFields,rules:['과거 조회를 허용한다. 최대 366일이며 누락 날짜는 missingDates에 적는다.'],tests:['T01','T07']});
api('RATE-04','판매 준비: 요금','GET','/room-types/{roomTypeId}/rates/{date}','날짜별 요금 조회',ownHost,'DailyRate',rate,{rules:['과거 날짜 조회를 허용한다. 해당 날짜 레코드가 없으면 404다.'],tests:['T01']});

api('PROMO-01','프로모션','POST','/promotions','프로모션 등록','OPERATOR','Promotion',promo,{bodyFields:promoFields,body:promoCreate,location:'/api/v1/promotions/promo_001',rules:['정률 할인과 자동 적용형만 등록한다. 쿠폰과 수량 제한은 없다.','캠페인 기간은 적용 판단일이고, 숙박 기간은 할인 대상 날짜다. 두 기간을 구분한다.'],tests:['T28']});
api('PROMO-02','프로모션','PATCH','/promotions/{promotionId}','프로모션 수정','OPERATOR','Promotion',changed(promo,{enabled:false}),{bodyFields:patchFields(promoFields),body:{version:0,enabled:false},errors:['VERSION_CONFLICT'],rules:['숙박 기간을 변경하거나 해제할 때 두 날짜를 함께 보낸다.','변경하지 않은 값과 합친 최종 상태에서도 날짜 순서와 제약을 검사한다.','enabled=false는 수동 종료다. 기존 예약의 할인 스냅샷은 바뀌지 않는다.'],tests:['T13','T28']});
api('PROMO-03','프로모션','GET','/promotions/{promotionId}','프로모션 상세 조회','OPERATOR','Promotion',promo,{tests:['T28']});
api('PROMO-04','프로모션','GET','/promotions','프로모션 관리 목록 조회','OPERATOR',pageSchema('Promotion'),page(promo),{query:[field('enabled','boolean','true 또는 false. 생략하면 전체',false),...pageFields],tests:['T28']});
api('PROMO-05','프로모션','GET','/room-types/{roomTypeId}/applicable-promotions','적용 가능 프로모션 조회','공개','ApplicablePromotions',{roomTypeId:'room_001',checkIn:date1,checkOut:end,guestCount:2,evaluatedAt:now,items:[{id:'promo_001',name:'가을 할인',discountRate:10,discountAmount:20000,selected:true}],selectedPromotionId:'promo_001'},{query:stayFields,errors:['RATE_NOT_CONFIGURED','OCCUPANCY_EXCEEDED'],rules:['서버의 오늘이 캠페인 기간에 속하고 지역, 박수, 숙박 기간 조건을 모두 만족하는 후보를 조회한다.','할인액이 가장 큰 하나를 선택한다. 동률이면 ID 오름차순이다.','재고를 선점하지 않는다. 재고 소진 여부와 할인 조건 충족 여부는 별개다.','후보가 없으면 items는 빈 배열, selectedPromotionId는 null이다.'],tests:['T28']});
api('SEARCH-01','검색','GET','/search/properties','숙소 검색','공개',pageSchema('PropertySearchResult'),page({property,lowestTotalAmount:180000,currency:'KRW',availableRoomTypes:[{roomTypeId:'room_001',name:'스탠다드 더블',maxOccupancy:2,availableCount:5,totalAmount:180000}]}),{query:[regionField,...stayFields,...pageFields],rules:['인원 수용, 전 날짜 재고 존재, 전 날짜 가용 수 1 이상, 전 날짜 요금 존재를 모두 만족하는 객실만 포함한다.','해당 객실이 없는 숙소는 제외한다. 결과가 없으면 200과 빈 items를 반환한다.','검색은 Hold를 생성하지 않는다. 결과 이후 재고와 요금은 바뀔 수 있다.'],tests:['T06','T07','T27']});
api('SEARCH-02','검색','GET','/room-types/{roomTypeId}/availability','객실별 연박 가용성 조회','공개','Availability',{roomTypeId:'room_001',checkIn:date1,checkOut:end,guestCount:2,available:true,availableCount:5,days:[{date:date1,availableCount:5},{date:date2,availableCount:5}],missingInventoryDates:[],missingRateDates:[],reasons:[]},{query:stayFields,rules:['가용성 부족은 200과 available=false로 반환한다. 날짜나 기간 형식 오류는 400이다.','days는 전 숙박 날짜를 포함한다. 재고 레코드가 없으면 해당 날짜 availableCount=null이며 missingInventoryDates에도 넣는다.','요금 누락과 인원 초과도 reasons에 넣는다. 최상위 availableCount는 재고 기준 최소 수이며 인원이나 요금 조건까지 뜻하지 않는다.'],tests:['T06','T07','T27']});
api('SEARCH-03','검색','GET','/room-types/{roomTypeId}/price-quote','예상 숙박 금액 조회','공개','PriceQuote',{roomTypeId:'room_001',checkIn:date1,checkOut:end,guestCount:2,nights:2,estimatedAt:now,price},{query:stayFields,errors:['RATE_NOT_CONFIGURED','OCCUPANCY_EXCEEDED'],rules:['요금과 인원 조건을 확인하고 날짜별 단가, 할인액과 총액을 반환한다.','재고를 보장하거나 선점하지 않는다. 예약 생성 시 현재 가격으로 다시 계산한다.'],tests:['T12','T13','T28']});
const bookingFields=[id('roomTypeId'),...stayFields,field('expectedTotalAmount','integer','1~30,000,000,000원. 사용자가 확인한 예상 총액'),currencyField];
api('BOOK-01','예약과 결제','POST','/bookings','예약 요청','GUEST','Booking',booking,{bodyFields:bookingFields,body:{roomTypeId:'room_001',checkIn:date1,checkOut:end,guestCount:2,expectedTotalAmount:180000,currency:'KRW'},idempotent:true,location:'/api/v1/bookings/booking_001',errors:['INVENTORY_UNAVAILABLE','INVENTORY_NOT_CONFIGURED','RATE_NOT_CONFIGURED','OCCUPANCY_EXCEEDED','PRICE_CHANGED'],rules:['guestId는 행위자에서 채운다. 객실 수는 1이며 프로모션 ID, 재고 수량과 예약 상태는 입력받지 않는다.','전 날짜 재고와 요금을 확인하고 가격을 재계산한다. 예상 총액이 다르면 PRICE_CHANGED이며 예약과 Hold는 만들지 않는다.','전 날짜 heldCount를 각각 1 올리고 HELD 예약, 가격 스냅샷과 멱등 결과를 함께 저장한다. 하나라도 실패하면 전부 롤백한다.','검색 결과만 믿고 선점하지 않는다. 동시 요청에서도 초과 예약이 없어야 한다.'],tests:['T06','T07','T08','T09','T10','T11','T12','T13','T29']});
api('BOOK-02','예약과 결제','GET','/bookings','본인 예약 목록 조회','GUEST',pageSchema('Booking'),page(booking),{query:[field('status','string','HELD / CONFIRMED / CANCELED / EXPIRED. 생략하면 전체',false),...pageFields],rules:['본인 예약만 반환한다. guestId와 hostId 쿼리는 받지 않는다.','createdAt 내림차순, 동률이면 ID 내림차순이다.'],tests:['T02']});
api('BOOK-03','예약과 결제','GET','/bookings/{bookingId}','예약 상세 조회',ownGuest,'Booking',booking,{rules:['결제 시도와 환불 정보를 함께 반환한다.','프론트는 serverNow와 expiresAt으로 남은 시간을 표시한다. 실제 결제 가능 여부는 서버 처리 시 다시 검사한다.'],tests:['T02','T13','T29']});
api('PAY-01','예약과 결제','POST','/bookings/{bookingId}/payment-attempts','Mock 결제 요청',ownGuest,'PaymentAttempt',attempt,{status:202,bodyFields:[field('mockMode','string','APPROVE / DECLINE / DEFER. 생략하면 APPROVE',false)],body:{mockMode:'APPROVE'},idempotent:true,location:'/api/v1/bookings/booking_001/payment-attempts',errors:['BOOKING_EXPIRED','BOOKING_STATE_CONFLICT','PAYMENT_IN_PROGRESS','PAYMENT_ATTEMPTS_EXHAUSTED'],rules:['접수 시도는 REQUESTED이며 202를 반환한다. 결제 결과는 예약 상세나 시도 목록으로 조회한다.','청구액은 예약의 고정된 스냅샷에서 가져온다. 클라이언트의 금액이나 카드정보는 받지 않는다.','유효한 HELD이고 진행 중 시도가 없으며 누적 시도 3회 미만일 때 새 시도를 받는다.','같은 키의 재전송은 시도를 늘리지 않는다. 실패 후 명시적인 다음 시도는 새 키를 사용한다.'],tests:['T14','T15','T16','T17','T18','T26']});
api('PAY-02','예약과 결제','GET','/bookings/{bookingId}/payment-attempts','결제 시도 목록 조회',ownGuest,'PaymentAttemptList',{bookingId:'booking_001',attemptCount:1,items:[attempt]},{rules:['최대 3개이므로 페이지를 나누지 않는다. attemptNumber 오름차순이다.','환불 여부는 예약 상세의 payment.refund로 확인한다. 환불해도 승인 시도의 status는 APPROVED다.'],tests:['T14','T16']});
api('BOOK-04','예약과 결제','POST','/bookings/{bookingId}/cancellations','예약 취소',ownGuest,'Booking',canceled,{status:200,bodyFields:[field('reason','string','최대 300자. 생략하면 빈 문자열',false)],body:{reason:'일정 변경'},idempotent:true,errors:['BOOKING_STATE_CONFLICT','CANCELLATION_NOT_ALLOWED'],rules:['본인의 CONFIRMED 예약만 체크인 날짜 전에 전체 취소할 수 있다. 현재 초안은 취소 수수료 0과 전액 Mock 환불을 적용한다.','취소 상태, 모든 숙박 날짜의 soldCount 반환, 환불 기록이 함께 완료돼야 200을 반환한다.','같은 성공 키로 재전송하면 최초 응답을 반환한다. 새 키로 이미 취소한 예약을 다시 취소하면 409다.','HELD 상태 이탈은 취소 API가 아닌 TTL 만료로 처리한다.'],tests:['T22','T24','T25']});
const internalRules=section('### 7.3 로컬 Mock 결제 결과 전달','## 8. 기능 목록과 명세 연결');
api('INTERNAL-01','내부 처리','POST','/internal/mock-payments/events','Mock 결제 결과 전달','MOCK_SYSTEM','MockEventResult',{eventId:'mock_event_0001',paymentAttemptId:'attempt_001',result:'PROCESSED',processedAt:'2026-10-01T03:00:01.000Z'},{status:200,bodyFields:[field('eventId','string','1~128자. 이벤트 중복 식별자'),id('paymentAttemptId'),id('pgTransactionId'),field('outcome','string','APPROVED / FAILED'),field('amount','integer','1~30,000,000,000원. 대상 시도 금액과 일치'),currencyField,field('failureCode','string','outcome=FAILED에서 필수, MOCK_DECLINED. APPROVED에서는 보내지 않음',false)],body:{eventId:'mock_event_0001',paymentAttemptId:'attempt_001',pgTransactionId:'mock_tx_001',outcome:'APPROVED',amount:180000,currency:'KRW'},errors:['MOCK_EVENT_CONFLICT','PAYMENT_AMOUNT_MISMATCH'],rules:['개발 프로파일의 테스트 러너와 Mock 어댑터에서 사용한다. 프론트 사용자 기능에서 직접 호출하지 않는다.','Idempotency-Key 대신 eventId와 거래 결과로 중복을 판단한다.','이벤트, 시도 결과, 예약 정책, Mock 환불과 재고 변경이 함께 저장된 뒤 200을 반환한다.'],extra:internalRules.slice(internalRules.indexOf('처리 계약:')).replace('처리 계약:','**중복과 결과 처리**').replace('이 경로에는 Idempotency-Key 대신 eventId와 거래 결과 중복 검사를 사용한다. 실제 PG 연동과 웹훅 인증은 이번 범위가 아니다.',''),tests:['T18','T19','T20','T21','T22','T23','T30']});

const escape = value => String(value).replace(/\|/g,'\\|').replace(/\n/g,' ');
const table = (heads, rows) => '| '+heads.join(' | ')+' |\n|'+heads.map(()=> '---').join('|')+'|\n'+rows.map(row=>'| '+row.map(escape).join(' | ')+' |').join('\n')+'\n';
const json = value => '```json\n'+JSON.stringify(value,null,2)+'\n```\n';
const slug = value => value.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/-$/,'');
const modelLink = name => '['+name.replace(/</g,'&lt;').replace(/>/g,'&gt;')+'](#model-'+slug(name)+')';
const fieldTable = fields => table(['필드','타입','필수','제약'],fields.map(f=>['`'+f.name+'`',f.type,f.required?'O':(f.name==='failureCode'?'조건부':'X'),f.description]));
const modelTable = fields => table(['필드','타입','설명'],fields.map(f=>{
  const base=f.type.replace(/\[\]|\/null/g,'');
  const type=schemas[base]?modelLink(base)+f.type.slice(base.length):f.type;
  let description=f.description.replace(/, 생략하면 빈 문자열|\. 생략하면 빈 문자열|, 기본 20|, 기본 0/g,'').replace('기본 true','사용 여부').replace('두 필드 생략 시 둘 다 null','숙박 기간 제한이 없으면 두 필드 모두 null');
  if(f.name==='checkIn')description='숙박 시작일, 포함';
  if(f.name==='checkOut')description='숙박 종료일, 제외';
  if(f.name==='guestCount')description='요청 또는 예약의 인원, 1~100';
  return ['`'+f.name+'`',type,description];
}));
const errorRows = [...section('### 2.5 오류','## 3. 판매 준비 API').matchAll(/^\| (\d{3}) \| ([A-Z_]+) \| (.+) \|$/gm)];
const errors = Object.fromEntries(errorRows.map(m=>[m[2],[m[1],m[2],m[3]]]));
const testIds = new Set([...source.matchAll(/^\| (T\d+) \|/gm)].map(m=>m[1]));
const idempotentErrors=['IDEMPOTENCY_KEY_REQUIRED','IDEMPOTENCY_KEY_REUSED','REQUEST_IN_PROGRESS'];

function endpoint(a) {
  let out='<a id="'+a.id.toLowerCase()+'"></a>\n\n### '+a.method+' '+a.url+' : '+a.title+'\n\n';
  out+='API ID: `'+a.id+'`  \n인증: '+(a.actor==='공개'?'불필요':a.actor)+'\n\n';
  if(a.idempotent) out+='**Request Headers**\n\n'+table(['헤더','필수','설명'],[['Idempotency-Key','O','8~128자. 새 업무 요청은 새 키, 재전송은 같은 키']])+'\n';
  const params=[...a.url.matchAll(/\{([^}]+)\}/g)].map(m=>m[1]);
  if(params.length) out+='**Path Variables**\n\n'+table(['변수','타입','설명'],params.map(name=>[name,name==='date'?'date':'string',name==='date'?(a.method==='GET'?'YYYY-MM-DD. 과거 조회 허용':'YYYY-MM-DD. 서버의 오늘 이상'):'대상 ID, 최대 64자']))+'\n';
  if(a.query?.length) out+='**Query Parameters**\n\n'+fieldTable(a.query)+'\n';
  if(a.bodyFields) out+='**Request Body**\n\n'+json(a.body)+'\n'+fieldTable(a.bodyFields)+'\n'+(a.method==='PATCH'?'version 외 변경 필드가 한 개 이상 필요하다. 생략한 필드는 유지하며 null 허용 필드만 null로 해제할 수 있다.\n\n':'');
  out+='**Response `'+a.status+' '+({200:'OK',201:'Created',202:'Accepted'}[a.status])+'`**\n\n';
  if(a.location) out+='```http\nLocation: '+a.location+'\n```\n\n';
  out+='응답 모델: '+modelLink(a.responseSchema)+'\n\n'+json(a.response)+'\n';
  const apiErrors=[...(a.idempotent?idempotentErrors:[]),...(a.errors||[])];
  if(apiErrors.length) out+='**Error Responses**\n\n'+table(['상태','code','조건'],[...new Set(apiErrors)].map(code=>{if(!errors[code])throw new Error(code);return errors[code];}))+'\n';
  if(a.rules?.length) out+='**처리 규칙**\n\n'+a.rules.map(r=>'- '+r).join('\n')+'\n\n';
  if(a.extra)out+=a.extra.trim()+'\n\n';
  out+='검증 항목: '+a.tests.map(t=>'['+t+'](#'+t.toLowerCase()+')').join(', ')+'\n\n';
  return out;
}

const groups=[['판매 준비: 숙소','숙소','properties'],['판매 준비: 객실 타입','객실 타입','room-types'],['판매 준비: 재고','재고','inventories'],['판매 준비: 요금','요금','rates'],['프로모션','프로모션','promotions'],['검색','검색','search'],['예약과 결제','예약과 결제','bookings']];
let common=section('## 2. 공통 계약','## 3. 판매 준비 API');
common=common.replace('## 2. 공통 계약','<a id="common"></a>\n\n## 공통').replace('### 2.1 경로, 타입과 응답','### 요청과 응답 규칙').replace('아래 서비스 API 표는 이 접두사를 생략한다.','모든 엔드포인트 제목에 전체 경로를 표시한다.').replace('### 2.2 행위자와 접근 범위','### 인증과 접근 제어').replace('### 2.3 목록과 날짜 범위','### 목록과 날짜 범위').replace('### 2.4 멱등 처리','### 멱등 처리').replace('### 2.5 오류','### 에러 응답');
common=common.replace('이 fixture 구성은 초기 데이터로 준비할 항목이며 현재 생성된 계정이 아니다.','예시는 해당 fixture와 SEOUL 지역 코드가 준비된 환경을 전제로 한다.');
common=common.replace('Page<T>', '`Page<T>`').replace('- 날짜는 YYYY-MM-DD, 시각은 UTC의 YYYY-MM-DDTHH:mm:ss.SSSZ 형식이다.','- 날짜는 YYYY-MM-DD이고 숙박과 캠페인 날짜는 Asia/Seoul을 기준으로 한다. 시각은 UTC의 YYYY-MM-DDTHH:mm:ss.SSSZ 형식이다.');
common=common.replace('아래 API별 오류 표기는 해당 기능의 주요 409다.','API별 Error Responses에는 기능별 오류를 적는다. 공통 오류 형식과 상태 코드는 모든 API에 적용한다.');
common=common.replace('로컬 개발에서는 X-Dev-Actor-Id가 필요하다.','인증이 필요한 로컬 API는 X-Dev-Actor-Id로 개발 행위자를 전달한다.');
common=common.replace('이 헤더는 누구나 fixture ID를 입력할 수 있는 로컬 테스트 장치다. 소유권 분기 테스트용이며 신원 인증을 보장하지 않는다. 개발 프로파일 밖에서는 이 인증 어댑터와 /internal 경로를 활성화하지 않는다.','X-Dev-Actor-Id는 로컬 역할과 소유권 테스트용이며 신원 인증을 보장하지 않는다. 개발 프로파일 밖에서는 이 어댑터와 /internal 경로를 비활성화한다. AccessToken, RefreshToken, 회원가입과 로그인 API는 이번 범위에 없다.');
common=common.replace('### 목록과 날짜 범위','### 공통 헤더\n\n'+table(['구분','헤더','필수','용도'],[['요청','Content-Type','Body가 있으면 O','application/json'],['요청','Accept','X','application/json'],['요청','X-Dev-Actor-Id','인증 필요 API에서 O','서버 fixture에 등록한 행위자 ID'],['요청','Idempotency-Key','예약 생성, 결제 요청, 취소에서 O','재전송 중복 방지'],['응답','Content-Type','O','application/json'],['응답','Location','201과 202에서 O','생성 자원 또는 결과 조회 경로'],['응답','Idempotency-Replayed','성공 재전송에서 O','true'],['응답','Retry-After','REQUEST_IN_PROGRESS에서 O','1초']])+'\nJSON Body가 모두 선택 필드인 POST도 {}를 보낸다. GET은 Body를 받지 않는다. API에 표시하지 않은 Path, Query, Body 필드는 지원하지 않는다. 모든 응답 필드는 반환하며, /null 타입만 null을 허용한다.\n\n### 목록과 날짜 범위');
common=common.replace('재전송 body는 최초 시점의 스냅샷이다.','만료된 HELD에 대한 결제 요청은 만료와 재고 반환을 저장한 뒤 409를 반환할 수 있다. 이 경우 결제 시도를 만들지 않으며 오류 발생을 이유로 이미 완료한 만료 처리를 롤백하지 않는다.\n\n재전송 body는 최초 시점의 스냅샷이다.');

let out='# O2O 숙박 예약 API 명세서\n\n버전: v2  \n기준일: 2026-09-07  \n상태: 구현용 초안\n\nSpring 기반 Java 백엔드, Next.js 프론트, MySQL을 사용하는 로컬 개발용 HTTP 계약이다. 판매 준비, 프로모션, 검색, 예약과 Hold 관리를 포함한다. 배포와 운영 준비는 제외한다.\n\n서비스 API 32개와 로컬 Mock API 1개를 정의한다. 모든 요청과 응답 모델, 처리 규칙과 검증 기준은 이 문서 안에 있다. TTL과 할인 및 취소 정책의 제안값은 마지막 절에 모았다.\n\n## 목차\n\n';
out+='1. [공통](#common)\n'+groups.map((g,i)=>(i+2)+'. ['+g[1]+'](#'+g[2]+')').join('\n')+'\n9. [내부 처리와 Mock 이벤트](#internal)\n10. [응답 모델](#models)\n11. [검증 기준](#verification)\n12. [검토할 정책](#proposals)\n\n';
out+=common;
for(const [group,title,anchor]of groups){out+='<a id="'+anchor+'"></a>\n\n## '+title+'\n\n';for(const a of apis.filter(a=>a.group===group))out+=endpoint(a);}
out+='<a id="internal"></a>\n\n## 내부 처리와 Mock 이벤트\n\n';
out+='### Hold와 재고\n\n예약 하나는 객실 타입 하나의 객실 한 개를 checkIn 이상, checkOut 미만으로 점유한다. Hold는 별도 자원이나 ID가 아닌 Booking의 HELD 상태다. 재고 수량은 재고 컨텍스트의 커맨드로만 변경한다.\n\n';
let policies=section('### 7.1 외부 HTTP API로 만들지 않는 기능','### 7.3 로컬 Mock 결제 결과 전달');
policies=policies.replace('### 7.1 외부 HTTP API로 만들지 않는 기능\n\n','').replace('### 7.2 상태 전이와 시간 경계','### 상태 전이와 시간 경계');
out+=policies;
out+='### 가격과 프로모션\n\n';
out+='- 서버의 오늘이 campaignStartDate 이상, campaignEndDate 미만이고 enabled=true인 프로모션을 검사한다.\n- 지역과 최소 박수 조건을 만족하고, 설정한 숙박 기간 안에 전체 숙박 구간이 들어와야 한다.\n- 실제 할인액이 가장 큰 하나만 적용한다. 동률이면 ID 오름차순이다.\n- 날짜별 할인액은 floor(baseAmount * discountRate / 100), 최종액은 baseAmount - discountAmount다. 전체 금액은 날짜별 값의 합이다.\n- 할인 조건을 만족하는 후보가 없으면 appliedPromotion=null이고 모든 할인액은 0이다.\n- 검색, 예상 금액과 예약은 같은 계산 규칙을 사용한다. 예약 생성 시 계산한 스냅샷 하나를 비교하고 그대로 저장한다. 비교 후 다시 계산해 다른 금액을 저장하지 않는다.\n- 예약 생성 시 날짜별 단가, 할인액, 프로모션 ID와 이름을 고정한다. 이후 요금이나 프로모션 수정은 기존 예약에 반영하지 않는다.\n- 예상 총액과 현재 금액이 다르면 PRICE_CHANGED다. 화면에서 다시 조회하고 확인받은 금액으로 요청한다. 같은 총액에서 할인 구성만 달라지면 최신 구성을 저장한다.\n- 캠페인 기간이 지나면 조회와 계산에서 제외한다. 기간 만료로 enabled를 바꾸는 자동 작업은 없다.\n\n';
out+='### 결제 접수와 환불\n\n';
out+='- 게스트의 결제 요청은 예약이 받아 고정된 총액을 결제에 전달한다. 결제는 예약 Repository를 읽어 금액을 구하지 않는다.\n- 새 시도 처리 순서는 멱등 재전송 확인, TTL 만료 여부, 예약 상태, 진행 중 시도, 시도 한도 검사다.\n- EXPIRED 또는 만료 시각을 지난 HELD이면 BOOKING_EXPIRED다. HELD는 먼저 만료와 재고 반환을 저장한다.\n- CONFIRMED와 CANCELED는 BOOKING_STATE_CONFLICT다. 유효한 HELD에 REQUESTED가 있으면 PAYMENT_IN_PROGRESS, 이미 3회면 PAYMENT_ATTEMPTS_EXHAUSTED다.\n- 시도 저장과 횟수 증가는 한 번만 수행한다. 202는 접수 결과이며 승인 결과가 아니다.\n- APPROVE는 승인, DECLINE은 실패 결과를 자동 전달한다. DEFER는 테스트용 수동 이벤트를 기다린다.\n- 자동 결과는 시도 저장 후 전달하고, 저장된 REQUESTED와 mockMode로 재시작 후에도 재개한다. 중복 전달은 같은 업무를 반복하지 않는다.\n- 실패는 접수 API의 500이 아닌 시도의 FAILED 상태다. 유효한 TTL 내 1회와 2회 실패는 HELD를 유지하고, 3회 실패는 만료와 재고 반환으로 이어진다.\n- 취소는 CONFIRMED에서만 가능하다. HELD의 화면 이탈은 TTL로 처리한다. 호스트나 운영자의 취소는 제공하지 않는다.\n- 취소는 전체 금액의 동기 Mock 환불을 사용한다. 취소 상태, 환불 기록과 재고 반환이 모두 저장돼야 성공이다. 실패하면 부분 결과를 남기지 않는다.\n- 지연 승인 환불은 이미 반환한 재고를 다시 바꾸지 않는다. 같은 승인 시도에 환불은 한 개만 존재한다.\n- 환불 후에도 시도는 APPROVED, approvedAttemptId는 기존 값을 유지한다. Refund로 반환 사실을 구분한다.\n- GET에 잠시 HELD가 보이더라도 처리 시각이 expiresAt 이상이면 승인할 수 없다. 응답 상태나 화면 카운트다운이 승인 가능성을 보장하지 않는다.\n\n';
out+=endpoint(apis.find(a=>a.id==='INTERNAL-01'));

out+='<a id="models"></a>\n\n## 응답 모델\n\n모든 표의 필드는 항상 응답한다. 타입의 /null은 null 허용을 뜻한다. 배열은 값이 없으면 []다. 예시의 날짜와 시각은 2026-10-01을 기준으로 하며, 각 API 예시는 독립적인 상태를 보여준다.\n\n';
for(const [name,fields]of Object.entries(schemas)){out+='<a id="model-'+slug(name)+'"></a>\n\n### '+name.replace(/</g,'&lt;').replace(/>/g,'&gt;')+'\n\n'+modelTable(fields)+'\n';}
let tests=section('## 9. 구현 시 확인할 계약 테스트','## 10. 하네스 구현에 전달할 내용').replace('## 9. 구현 시 확인할 계약 테스트','<a id="verification"></a>\n\n## 검증 기준');
tests=tests.replace(/^\| (T\d+) \|/gm,(_,id)=>'| <a id="'+id.toLowerCase()+'"></a>'+id+' |');
out+=tests;
out+='<a id="proposals"></a>\n\n## 검토할 정책\n\n다음 값은 구현용 초안의 제안이다. 정책이 바뀌면 해당 요청과 응답, 상태 처리와 검증 기준을 함께 변경한다.\n\n';
let proposals=section('| ID | 항목 | 초안에서 사용하는 제안 |','## 2. 공통 계약');
proposals=proposals.slice(0,proposals.indexOf('\n\nP05'));
proposals=proposals.replace('기존 문서의 수동 종료는 PATCH enabled=false로 표현한다.','수동 종료는 PATCH enabled=false로 표현한다.');
proposals=proposals.replace('| P08 | 원자성 구현 후보 | 단일 MySQL 트랜잭션으로 예약과 재고 변경을 묶고 같은 날짜 순서로 동시성을 제어한다. 구체 락 방식과 Payment 애그리거트는 구현 설계에서 정한다 |','| P08 | 원자성 구현 후보 | 단일 MySQL 트랜잭션으로 예약과 재고 변경을 묶는다. 구체 락 방식과 Payment 애그리거트 구조는 미정이지만, 외부 계약의 전체 성공 또는 전체 실패는 지켜야 한다 |');
out+=proposals+'\n\n회원 기능, 쿠폰, 알림, 정산, 리뷰, 호실 배정과 실제 PG 연동은 이번 범위에서 제외한다. 부분 취소, 예약 변경과 별도 판매 제약 필드는 위 제안에 따라 보류한다.\n';

// Structural verification before writing a review candidate.
function validateValue(value,type,label){
  if(value===null){if(!type.endsWith('/null'))throw new Error('Unexpected null '+label);return;}
  const plain=type.replace(/\/null$/,'');
  if(plain.endsWith('[]')){
    if(!Array.isArray(value))throw new Error('Expected array '+label);
    value.forEach((v,i)=>validateValue(v,plain.slice(0,-2),label+'['+i+']'));return;
  }
  if(schemas[plain]){
    if(typeof value!=='object'||Array.isArray(value))throw new Error('Expected object '+label);
    const fields=schemas[plain];
    if(Object.keys(value).sort().join(',')!==fields.map(f=>f.name).sort().join(','))throw new Error('Schema keys '+label);
    for(const f of fields)validateValue(value[f.name],f.type,label+'.'+f.name);
    return;
  }
  if(plain==='integer'){if(!Number.isSafeInteger(value))throw new Error('Expected integer '+label);return;}
  if(plain==='boolean'){if(typeof value!=='boolean')throw new Error('Expected boolean '+label);return;}
  if(typeof value!=='string')throw new Error('Expected string '+label);
  if(plain==='date'&&!/^\d{4}-\d{2}-\d{2}$/.test(value))throw new Error('Expected date '+label);
  if(plain==='timestamp'&&!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/.test(value))throw new Error('Expected timestamp '+label);
}
if(apis.length!==33)throw new Error('Expected 33 endpoints');
if(new Set(apis.map(a=>a.id)).size!==33||new Set(apis.map(a=>a.method+' '+a.url)).size!==33)throw new Error('Duplicate endpoint');
for(const a of apis){
  validateValue(a.response,a.responseSchema,a.id);
  for(const t of a.tests)if(!testIds.has(t))throw new Error('Unknown test '+t);
  if(!schemas[a.responseSchema])throw new Error('Unknown schema '+a.responseSchema);
  const fields=schemas[a.responseSchema];
  const expected=fields.map(f=>f.name).sort().join(',');
  if(Object.keys(a.response).sort().join(',')!==expected)throw new Error('Response fields mismatch '+a.id);
  if(a.bodyFields){for(const f of a.bodyFields)if(f.required&&!(f.name in a.body))throw new Error('Missing required body '+a.id+' '+f.name);}
}
const oldEndpoints=[...source.matchAll(/^\| ((?:CAT|INV|RATE|PROMO|SEARCH|BOOK|PAY)-\d+) \| (GET|POST|PATCH) ([^|]+)\|/gm)].map(m=>m[1]+' '+m[2]+' /api/v1'+m[3].trim());
const newEndpoints=apis.filter(a=>a.id!=='INTERNAL-01').map(a=>a.id+' '+a.method+' '+a.url);
if(oldEndpoints.sort().join('\n')!==newEndpoints.sort().join('\n'))throw new Error('Service routes changed');
const jsonBlocks=[...out.matchAll(/```json\n([\s\S]*?)\n```/g)];
jsonBlocks.forEach(m=>JSON.parse(m[1]));
const anchors=[...out.matchAll(/<a id="([^"]+)"><\/a>/g)].map(m=>m[1]);
if(new Set(anchors).size!==anchors.length)throw new Error('Duplicate anchor');
for(const m of out.matchAll(/\]\(#([^)]*)\)/g))if(!anchors.includes(m[1]))throw new Error('Broken anchor '+m[1]);
if(/[\u2014\u00b7]/u.test(out))throw new Error('Forbidden punctuation');
if(/C:\/|01-o2o|10-4|10-6|이전 문서|기존 문서|참고 문서|ReAct 적용 기록/.test(out))throw new Error('External document dependency');
const dir=path.join(root,'tmp/api-spec-v2');
fs.mkdirSync(dir,{recursive:true});
fs.writeFileSync(path.join(dir,'11-o2o-api-spec.candidate.md'),out,'utf8');
fs.writeFileSync(path.join(dir,'endpoint-manifest.json'),JSON.stringify(apis,null,2),'utf8');
console.log(JSON.stringify({endpoints:apis.length,jsonExamples:jsonBlocks.length,models:Object.keys(schemas).length,anchors:anchors.length,lines:out.split('\n').length},null,2));
