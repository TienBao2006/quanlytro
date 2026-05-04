<?php
// File debug tạm - XÓA sau khi kiểm tra xong
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
$conn->set_charset("utf8mb4");

$result = $conn->query("SELECT id, post_id, user_id, full_name, status, created_at FROM bookings ORDER BY created_at DESC LIMIT 10");
$rows = [];
while ($r = $result->fetch_assoc()) $rows[] = $r;

echo json_encode($rows, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
$conn->close();
