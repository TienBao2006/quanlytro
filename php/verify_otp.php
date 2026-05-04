<?php
error_reporting(E_ALL);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}
$conn->set_charset("utf8mb4");

$phone = trim($_POST['phone'] ?? '');
$otp   = trim($_POST['otp']   ?? '');

if (empty($phone) || empty($otp)) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin", "phone" => $phone, "otp" => $otp]);
    exit;
}

// Lấy OTP mới nhất của số điện thoại này (chưa dùng)
$stmt = $conn->prepare("SELECT id, otp_code, expires_at, used FROM otp_codes WHERE phone = ? AND used = 0 ORDER BY created_at DESC LIMIT 1");
$stmt->bind_param("s", $phone);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy OTP cho số này"]);
    $stmt->close();
    $conn->close();
    exit;
}

$row = $result->fetch_assoc();
$stmt->close();

// Kiểm tra OTP có đúng không
if ($row['otp_code'] !== $otp) {
    echo json_encode(["status" => "error", "message" => "Mã OTP không đúng"]);
    $conn->close();
    exit;
}

// Kiểm tra hết hạn bằng PHP (tránh lỗi múi giờ MySQL)
$expiresAt = strtotime($row['expires_at']);
if (time() > $expiresAt) {
    echo json_encode(["status" => "error", "message" => "Mã OTP đã hết hạn, vui lòng gửi lại"]);
    $conn->close();
    exit;
}

// Đánh dấu OTP đã dùng
$upd = $conn->prepare("UPDATE otp_codes SET used = 1 WHERE id = ?");
$upd->bind_param("i", $row['id']);
$upd->execute();
$upd->close();

echo json_encode(["status" => "success", "message" => "Xác thực thành công"]);
$conn->close();
