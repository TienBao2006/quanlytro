<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}

$uid         = $_POST['uid']          ?? '';
$oldPassword = $_POST['old_password'] ?? '';
$newPassword = $_POST['new_password'] ?? '';

if (empty($uid) || empty($oldPassword) || empty($newPassword)) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

if (strlen($newPassword) < 6) {
    echo json_encode(["status" => "error", "message" => "Mật khẩu mới phải ít nhất 6 ký tự"]);
    exit;
}

$stmt = $conn->prepare("SELECT password FROM users WHERE uid = ?");
$stmt->bind_param("s", $uid);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Người dùng không tồn tại"]);
    $stmt->close(); $conn->close(); exit;
}

$row = $result->fetch_assoc();
$stored = $row['password'];
$stmt->close();

$isValid = password_verify($oldPassword, $stored) || ($oldPassword === $stored);
if (!$isValid) {
    echo json_encode(["status" => "error", "message" => "Mật khẩu hiện tại không đúng"]);
    $conn->close(); exit;
}

$hashed = password_hash($newPassword, PASSWORD_DEFAULT);
$upd = $conn->prepare("UPDATE users SET password = ? WHERE uid = ?");
$upd->bind_param("ss", $hashed, $uid);
$upd->execute();
$upd->close();
$conn->close();

echo json_encode(["status" => "success", "message" => "Đổi mật khẩu thành công"]);
