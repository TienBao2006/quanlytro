<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối thất bại"]);
    exit;
}

$sender_id   = $_POST["sender_id"]   ?? "";
$receiver_id = $_POST["receiver_id"] ?? "";
$message     = $_POST["message"]     ?? "";

if (empty($sender_id) || empty($receiver_id) || empty($message)) {
    echo json_encode(["status" => "error", "message" => "Thiếu dữ liệu"]);
    exit;
}

// chat_id cố định: uid nhỏ hơn đứng trước
$chat_id = ($sender_id < $receiver_id)
    ? $sender_id . "_" . $receiver_id
    : $receiver_id . "_" . $sender_id;

$stmt = $conn->prepare(
    "INSERT INTO messages (chat_id, sender_id, receiver_id, message) VALUES (?, ?, ?, ?)"
);
$stmt->bind_param("ssss", $chat_id, $sender_id, $receiver_id, $message);

if ($stmt->execute()) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}

$stmt->close();
$conn->close();
