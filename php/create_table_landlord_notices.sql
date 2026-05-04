CREATE TABLE IF NOT EXISTS `landlord_notices` (
  `id`          INT AUTO_INCREMENT PRIMARY KEY,
  `landlord_id` VARCHAR(100) NOT NULL,
  `post_id`     INT NOT NULL,
  `title`       VARCHAR(255) NOT NULL,
  `message`     TEXT NOT NULL,
  `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_landlord (`landlord_id`),
  INDEX idx_post (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
