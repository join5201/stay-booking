-- 처음 표 12개 (이슈 233)
--
-- 왜 이 파일이 필요한가: 지금까지는 앱이 켜질 때 Hibernate가 엔티티를 보고 표를 만들었다(ddl-auto=update).
-- 이제 표는 이 폴더의 SQL 파일이 만들고 앱은 엔티티와 표가 맞는지만 확인한다(validate).
-- Flyway가 앱이 켜질 때 아직 실행하지 않은 파일을 판 번호 순으로 실행하고 flyway_schema_history에 적는다.
--
-- 내용은 2026-10-01 main 3ba0ed0의 엔티티가 MySQL 8.4.11에 만든 표를 mysqldump --no-data로 뽑은 것이다.
-- 문자셋과 정렬 규칙을 표마다 적어 둔다. 서버 기본값이 달라도 같은 표가 만들어진다.
--
-- 한 번 실행된 뒤에는 이 파일을 고치지 않는다. 주석 한 줄도 안 된다. Flyway가 파일 체크섬을
-- 이력 표의 값과 견줘 다르면 앱을 띄우지 않는다. 표를 바꿀 때는 V2__설명.sql을 새로 만든다.
--
-- 실패 시 출력 예시
--   Found non-empty schema(s) `o2o_catalog_test` but no schema history table
--     Flyway를 붙이기 전에 Hibernate가 만든 표가 남아 있다. 그 DB를 DROP 뒤 CREATE로 비우고 다시 켠다
--   Migration checksum mismatch for migration version 1
--     이 파일이 실행된 뒤에 고쳐졌다. 되돌리고 새 V 파일로 바꾼다
--   Schema validation: missing column [...] in table [...]
--     엔티티만 고치고 SQL 파일을 안 만들었다. 새 V 파일에 그 변경을 적는다

CREATE TABLE `booking` (
  `id` varchar(64) NOT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `cancellation_reason` varchar(300) DEFAULT NULL,
  `confirmed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expiration_reason` enum('PAYMENT_FAILED','TTL_EXPIRED') DEFAULT NULL,
  `expired_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `check_in` date NOT NULL,
  `check_out` date NOT NULL,
  `base_total_amount` bigint NOT NULL,
  `currency` varchar(3) NOT NULL,
  `discount_total_amount` bigint NOT NULL,
  `promotion_discount_rate` int DEFAULT NULL,
  `promotion_id` varchar(64) DEFAULT NULL,
  `promotion_name` varchar(200) DEFAULT NULL,
  `total_amount` bigint NOT NULL,
  `property_id` varchar(64) NOT NULL,
  `room_type_id` varchar(64) NOT NULL,
  `status` enum('CANCELED','CONFIRMED','EXPIRED','HELD') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_count` int NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_booking_user_idempotency_key` (`user_id`,`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_price_day` (
  `booking_id` varchar(64) NOT NULL,
  `stay_date` date NOT NULL,
  `base_amount` bigint NOT NULL,
  `discount_amount` bigint NOT NULL,
  KEY `FKexix0vip6f9ll9cefxjb4r9tj` (`booking_id`),
  CONSTRAINT `FKexix0vip6f9ll9cefxjb4r9tj` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `daily_inventory` (
  `id` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `held_count` int NOT NULL,
  `room_type_id` varchar(64) NOT NULL,
  `sold_count` int NOT NULL,
  `stay_date` date NOT NULL,
  `total_count` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_inventory_room_date` (`room_type_id`,`stay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `daily_rate` (
  `id` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `amount` bigint NOT NULL,
  `currency` varchar(3) NOT NULL,
  `room_type_id` varchar(64) NOT NULL,
  `stay_date` date NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_daily_rate_room_date` (`room_type_id`,`stay_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `idempotency_record` (
  `id` varchar(64) NOT NULL,
  `actor_id` varchar(64) NOT NULL,
  `body_hash` varchar(64) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `http_method` varchar(16) NOT NULL,
  `request_path` varchar(200) NOT NULL,
  `response_body` text,
  `response_location` varchar(300) DEFAULT NULL,
  `response_status` int DEFAULT NULL,
  `state` enum('COMPLETED','IN_PROGRESS') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotency_scope` (`actor_id`,`http_method`,`request_path`,`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `mock_payment_event` (
  `event_id` varchar(128) NOT NULL,
  `body_hash` varchar(64) NOT NULL,
  `payment_attempt_id` varchar(64) NOT NULL,
  `processed_at` datetime(6) NOT NULL,
  `result` enum('DUPLICATE','PROCESSED') NOT NULL,
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment` (
  `id` varchar(64) NOT NULL,
  `amount` bigint NOT NULL,
  `currency` varchar(3) NOT NULL,
  `booking_id` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_booking` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `payment_attempt` (
  `id` varchar(64) NOT NULL,
  `amount` bigint NOT NULL,
  `currency` varchar(3) NOT NULL,
  `attempt_number` int NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `failure_code` varchar(32) DEFAULT NULL,
  `kind` enum('NORMAL','ORPHAN') NOT NULL,
  `mock_mode` enum('APPROVE','DECLINE','DEFER') NOT NULL,
  `pg_transaction_id` varchar(64) NOT NULL,
  `refund_reason` enum('BOOKING_CANCELED','LATE_APPROVAL') DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `status` enum('APPROVED','FAILED','REFUNDED','REQUESTED') NOT NULL,
  `payment_id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_attempt_pg_transaction` (`pg_transaction_id`),
  KEY `FKj0y3ph0528vdvfqqaiqvsicmo` (`payment_id`),
  CONSTRAINT `FKj0y3ph0528vdvfqqaiqvsicmo` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promotion` (
  `id` varchar(64) NOT NULL,
  `campaign_end_date` date NOT NULL,
  `campaign_start_date` date NOT NULL,
  `min_nights` int NOT NULL,
  `stay_end_date` date DEFAULT NULL,
  `stay_start_date` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `discount_rate` int NOT NULL,
  `enabled` bit(1) NOT NULL,
  `name` varchar(100) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `promotion_region_code` (
  `promotion_id` varchar(64) NOT NULL,
  `region_code` varchar(32) NOT NULL,
  `position` int NOT NULL,
  PRIMARY KEY (`promotion_id`,`position`),
  CONSTRAINT `FKofucbrqi059qu1nj9h9pljedt` FOREIGN KEY (`promotion_id`) REFERENCES `promotion` (`id`),
  CONSTRAINT `promotion_region_code_chk_1` CHECK ((`position` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `property` (
  `id` varchar(64) NOT NULL,
  `address` varchar(300) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) NOT NULL,
  `host_id` varchar(64) NOT NULL,
  `name` varchar(100) NOT NULL,
  `region_code` varchar(32) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `room_type` (
  `id` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) NOT NULL,
  `max_occupancy` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `property_id` varchar(64) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `version` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
