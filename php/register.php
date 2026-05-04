<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}

$fullName = $_POST["fullName"] ?? "";
$email    = $_POST["email"]    ?? "";
$phone    = $_POST["phone"]    ?? "";
$password = $_POST["password"] ?? "";
$role     = $_POST["role"]     ?? "Người thuê";

if (empty($fullName) || empty($email) || empty($phone) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Kiểm tra email đã tồn tại
$check = $conn->prepare("SELECT id FROM users WHERE email = ?");
$check->bind_param("s", $email);
$check->execute();
$check->store_result();
if ($check->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "Email đã tồn tại"]);
    $check->close();
    $conn->close();
    exit;
}
$check->close();

$uid          = uniqid("u_", true);
$passwordHash = password_hash($password, PASSWORD_DEFAULT);

$stmt = $conn->prepare("INSERT INTO users (uid, fullName, email, phone, password, role) VALUES (?, ?, ?, ?, ?, ?)");
$stmt->bind_param("ssssss", $uid, $fullName, $email, $phone, $passwordHash, $role);

if ($stmt->execute()) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => "Lỗi lưu dữ liệu"]);
}

$stmt->close();
$conn->close();
