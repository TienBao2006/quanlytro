<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}
$conn->set_charset("utf8mb4");

// Nhận email hoặc phone
$identifier = trim($_POST['email'] ?? $_POST['phone'] ?? '');
$otp        = trim($_POST['otp'] ?? '');

if (empty($identifier) || empty($otp)) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Tìm OTP mới nhất chưa dùng, không kiểm tra hết hạn (tránh lỗi múi giờ)
$stmt = $conn->prepare("SELECT id, otp_code FROM otp_codes WHERE phone = ? AND used = 0 ORDER BY id DESC LIMIT 1");
$stmt->bind_param("s", $identifier);
$stmt->execute();
$result = $stmt->get_result();
$stmt->close();

if ($result->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy OTP, vui lòng gửi lại"]);
    $conn->close();
    exit;
}

$row = $result->fetch_assoc();

if ($row['otp_code'] !== $otp) {
    echo json_encode(["status" => "error", "message" => "Mã OTP không đúng"]);
    $conn->close();
    exit;
}

// Đánh dấu đã dùng
$conn->query("UPDATE otp_codes SET used = 1 WHERE id = " . (int)$row['id']);

echo json_encode(["status" => "success", "message" => "Xác thực thành công"]);
$conn->close();
