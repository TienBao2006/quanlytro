<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}

$phone = $_POST['phone'] ?? '';
if (empty($phone)) {
    echo json_encode(["status" => "error", "message" => "Thiếu số điện thoại"]);
    exit;
}

// Tạo OTP 6 số
$otp = str_pad(rand(0, 999999), 6, '0', STR_PAD_LEFT);
// Dùng UTC để tránh lệch múi giờ với MySQL
$expires_at = gmdate('Y-m-d H:i:s', time() + 300); // +5 phút

// Lưu OTP vào DB (tạo bảng nếu chưa có)
$conn->query("CREATE TABLE IF NOT EXISTS otp_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

// Xóa OTP cũ của số này
$conn->query("DELETE FROM otp_codes WHERE phone = '$phone'");

// Lưu OTP mới
$stmt = $conn->prepare("INSERT INTO otp_codes (phone, otp_code, expires_at) VALUES (?, ?, ?)");
$stmt->bind_param("sss", $phone, $otp, $expires_at);
$stmt->execute();
$stmt->close();

// TODO: Tích hợp SMS gateway thực tế (Twilio, ESMS, SpeedSMS...)
// Hiện tại trả OTP về để test (production: chỉ gửi SMS, không trả về)
echo json_encode([
    "status" => "success",
    "message" => "OTP đã gửi đến $phone",
    "otp_debug" => $otp  // Xóa dòng này khi production
], JSON_UNESCAPED_UNICODE);

$conn->close();
