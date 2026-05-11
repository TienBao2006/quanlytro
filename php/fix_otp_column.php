<?php
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
$conn->set_charset("utf8mb4");

// Mở rộng cột phone để chứa email
$ok = $conn->query("ALTER TABLE otp_codes MODIFY COLUMN phone VARCHAR(150) NOT NULL");

echo json_encode([
    "status" => $ok ? "success" : "error",
    "message" => $ok ? "Đã sửa cột phone thành VARCHAR(150)" : $conn->error
]);

$conn->close();
