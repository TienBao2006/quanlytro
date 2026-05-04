<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["error" => "DB connect failed"]);
    exit;
}

// Map tất cả ID không hợp lệ -> MySQL uid đúng
// Thêm vào đây nếu có thêm Firebase UID mới
$mappings = [
    "q5An9uJXTbOcFWoDgvJPD9Dsila2" => "u_19",
    "landlord_39"                   => "u_19",
];

$results = [];
foreach ($mappings as $oldId => $newId) {
    $conn->query("UPDATE messages SET sender_id='$newId'   WHERE sender_id='$oldId'");
    $conn->query("UPDATE messages SET receiver_id='$newId' WHERE receiver_id='$oldId'");
    $conn->query("UPDATE messages SET chat_id=REPLACE(chat_id,'$oldId','$newId') WHERE chat_id LIKE '%$oldId%'");
    $results[] = "Fixed: $oldId -> $newId";
}

// Kiểm tra kết quả
$msgs = [];
$r = $conn->query("SELECT id, chat_id, sender_id, receiver_id, message FROM messages ORDER BY id DESC LIMIT 10");
while ($row = $r->fetch_assoc()) $msgs[] = $row;

echo json_encode([
    "results" => $results,
    "messages" => $msgs
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

$conn->close();
