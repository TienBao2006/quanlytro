<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}

$email    = $_POST['email']    ?? '';
$password = $_POST['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

$stmt = $conn->prepare("SELECT uid, fullName, phone, password, role FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Sai tài khoản hoặc mật khẩu"]);
    $stmt->close();
    $conn->close();
    exit;
}

$row = $result->fetch_assoc();
$storedPassword = $row['password'];

// Hỗ trợ cả password hash lẫn plain text
$isValid = password_verify($password, $storedPassword) || ($password === $storedPassword);

if (!$isValid) {
    echo json_encode(["status" => "error", "message" => "Sai tài khoản hoặc mật khẩu"]);
    $stmt->close();
    $conn->close();
    exit;
}

echo json_encode([
    "status"   => "success",
    "uid"      => $row['uid'],
    "fullName" => $row['fullName'],
    "phone"    => $row['phone'],
    "role"     => $row['role']
], JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
