<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode([]);
    exit;
}

$sender_id   = $_GET["sender_id"]   ?? "";
$receiver_id = $_GET["receiver_id"] ?? "";

if (empty($sender_id) || empty($receiver_id)) {
    echo json_encode([]);
    exit;
}

$chat_id = ($sender_id < $receiver_id)
    ? $sender_id . "_" . $receiver_id
    : $receiver_id . "_" . $sender_id;

$stmt = $conn->prepare(
    "SELECT id, sender_id, receiver_id, message, created_at
     FROM messages
     WHERE chat_id = ?
     ORDER BY created_at ASC"
);
$stmt->bind_param("s", $chat_id);
$stmt->execute();
$result = $stmt->get_result();

$messages = [];
while ($row = $result->fetch_assoc()) {
    $messages[] = $row;
}

echo json_encode($messages);

$stmt->close();
$conn->close();