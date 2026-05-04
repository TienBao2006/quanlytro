<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode([]);
    exit;
}

$user_id = $_GET["user_id"] ?? "";
if (empty($user_id)) {
    echo json_encode([]);
    exit;
}

// Lấy tin nhắn cuối của mỗi chat_id liên quan đến user
$stmt = $conn->prepare("
    SELECT m.chat_id,
           m.sender_id,
           m.receiver_id,
           m.message    AS last_message,
           m.created_at AS last_time
    FROM messages m
    JOIN (
        SELECT chat_id, MAX(id) AS max_id
        FROM messages
        WHERE sender_id = ? OR receiver_id = ?
        GROUP BY chat_id
    ) latest ON m.id = latest.max_id
    ORDER BY m.created_at DESC
");
$stmt->bind_param("ss", $user_id, $user_id);
$stmt->execute();
$result = $stmt->get_result();

$chats = [];
while ($row = $result->fetch_assoc()) {
    $other_id = ($row["sender_id"] === $user_id) ? $row["receiver_id"] : $row["sender_id"];

    // Lookup tên và avatar từ bảng users
    $uStmt = $conn->prepare("SELECT fullName, avatar FROM users WHERE uid = ?");
    $uStmt->bind_param("s", $other_id);
    $uStmt->execute();
    $uResult = $uStmt->get_result();
    $uRow = $uResult->fetch_assoc();
    $other_name   = $uRow ? $uRow["fullName"] : $other_id;
    $other_avatar = $uRow ? ($uRow["avatar"] ?? "") : "";
    $uStmt->close();

    $chats[] = [
        "chat_id"      => $row["chat_id"],
        "other_id"     => $other_id,
        "other_name"   => $other_name,
        "other_avatar" => $other_avatar,
        "last_message" => $row["last_message"],
        "last_time"    => $row["last_time"]
    ];
}

echo json_encode($chats, JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
