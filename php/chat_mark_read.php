<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error"]);
    exit;
}

$receiver_id = $_POST["receiver_id"] ?? "";
$sender_id   = $_POST["sender_id"]   ?? "";

if (empty($receiver_id) || empty($sender_id)) {
    echo json_encode(["status" => "error", "message" => "Thiếu dữ liệu"]);
    exit;
}

// Đánh dấu tất cả tin nhắn từ sender gửi cho receiver là đã đọc
$stmt = $conn->prepare(
    "UPDATE messages SET is_read = 1 WHERE sender_id = ? AND receiver_id = ? AND is_read = 0"
);
$stmt->bind_param("ss", $sender_id, $receiver_id);
$stmt->execute();

echo json_encode(["status" => "success", "updated" => $stmt->affected_rows]);

$stmt->close();
$conn->close();
