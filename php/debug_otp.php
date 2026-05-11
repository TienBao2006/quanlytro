<?php
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
$conn->set_charset("utf8mb4");

// Tạo bảng nếu chưa có
$conn->query("CREATE TABLE IF NOT EXISTS otp_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(100) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

// Xem tất cả OTP trong DB
$rows = [];
$r = $conn->query("SELECT * FROM otp_codes ORDER BY id DESC LIMIT 10");
if ($r) while ($row = $r->fetch_assoc()) $rows[] = $row;

// Thử insert thủ công
$testEmail = "test@test.com";
$testOtp = "999999";
$expires = date('Y-m-d H:i:s', time() + 300);
$conn->query("DELETE FROM otp_codes WHERE phone='$testEmail'");
$ok = $conn->query("INSERT INTO otp_codes (phone, otp_code, expires_at) VALUES ('$testEmail', '$testOtp', '$expires')");

echo json_encode([
    "table_exists" => true,
    "insert_test" => $ok ? "OK" : $conn->error,
    "last_10_otps" => $rows,
    "server_time" => date('Y-m-d H:i:s'),
    "mysql_error" => $conn->error
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

$conn->close();
