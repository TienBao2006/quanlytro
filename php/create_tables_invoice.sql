-- Chạy SQL này trong phpMyAdmin để tạo bảng hóa đơn

CREATE TABLE IF NOT EXISTS `invoices` (
  `id`             INT AUTO_INCREMENT PRIMARY KEY,
  `contract_id`    INT NOT NULL,
  `landlord_id`    VARCHAR(100) NOT NULL,
  `tenant_id`      VARCHAR(100) NOT NULL,
  `month`          VARCHAR(7) NOT NULL,        -- "2025-04"
  `electric_old`   DECIMAL(10,2) DEFAULT 0,
  `electric_new`   DECIMAL(10,2) DEFAULT 0,
  `electric_used`  DECIMAL(10,2) DEFAULT 0,
  `electric_price` DECIMAL(15,2) DEFAULT 0,
  `electric_cost`  DECIMAL(15,2) DEFAULT 0,
  `water_old`      DECIMAL(10,2) DEFAULT 0,
  `water_new`      DECIMAL(10,2) DEFAULT 0,
  `water_used`     DECIMAL(10,2) DEFAULT 0,
  `water_price`    DECIMAL(15,2) DEFAULT 0,
  `water_cost`     DECIMAL(15,2) DEFAULT 0,
  `rent_price`     DECIMAL(15,2) DEFAULT 0,
  `other_fee`      DECIMAL(15,2) DEFAULT 0,
  `other_fee_note` TEXT,
  `total`          DECIMAL(15,2) DEFAULT 0,
  `status`           ENUM('unpaid','paid') DEFAULT 'unpaid',
  `payment_method`   VARCHAR(50) DEFAULT NULL,   -- 'cash' | 'wallet'
  `txn_id`           VARCHAR(50) DEFAULT NULL,   -- mã giao dịch
  `paid_at`          DATETIME DEFAULT NULL,      -- thời điểm thanh toán
  `created_at`       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uq_contract_month` (`contract_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bảng thông báo (nếu chưa có)
CREATE TABLE IF NOT EXISTS `notifications` (
  `id`         INT AUTO_INCREMENT PRIMARY KEY,
  `user_id`    VARCHAR(100) NOT NULL,
  `title`      VARCHAR(255) NOT NULL,
  `message`    TEXT NOT NULL,
  `type`       VARCHAR(50) DEFAULT 'general',
  `is_read`    TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Migration: thêm cột đơn giá điện/nước (chạy nếu bảng đã tồn tại)
ALTER TABLE `invoices`
  ADD COLUMN IF NOT EXISTS `electric_price` DECIMAL(15,2) DEFAULT 0 AFTER `electric_used`,
  ADD COLUMN IF NOT EXISTS `water_price`    DECIMAL(15,2) DEFAULT 0 AFTER `water_used`;

-- Migration: thêm cột thanh toán (chạy nếu bảng đã tồn tại)
ALTER TABLE `invoices`
  ADD COLUMN IF NOT EXISTS `payment_method` VARCHAR(50) DEFAULT NULL AFTER `status`,
  ADD COLUMN IF NOT EXISTS `txn_id`         VARCHAR(50) DEFAULT NULL AFTER `payment_method`,
  ADD COLUMN IF NOT EXISTS `paid_at`        DATETIME   DEFAULT NULL AFTER `txn_id`;

-- Migration: thêm cột reference_id cho notifications (liên kết đến hợp đồng/hóa đơn)
ALTER TABLE `notifications`
  ADD COLUMN IF NOT EXISTS `reference_id` INT DEFAULT NULL AFTER `type`;

-- Migration: thêm cột gia hạn hợp đồng
ALTER TABLE `contracts`
  ADD COLUMN IF NOT EXISTS `renew_requested_months` INT DEFAULT 0 AFTER `status`;

-- Migration: thêm thông tin mở rộng cho users
ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `address`  VARCHAR(255) DEFAULT NULL AFTER `phone`,
  ADD COLUMN IF NOT EXISTS `dob`      VARCHAR(20)  DEFAULT NULL AFTER `address`,
  ADD COLUMN IF NOT EXISTS `id_card`  VARCHAR(30)  DEFAULT NULL AFTER `dob`;

-- Migration: thêm cột avatar cho users (base64)
ALTER TABLE `users`
  ADD COLUMN IF NOT EXISTS `avatar` MEDIUMTEXT DEFAULT NULL AFTER `id_card`;
