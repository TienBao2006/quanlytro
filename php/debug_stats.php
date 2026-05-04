<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
include 'db_config.php';

$landlord_id = trim($_GET['landlord_id'] ?? '');

// Tìm user
$user = null;
if ($landlord_id !== '') {
    $stmt = $conn->prepare("SELECT id, uid, phone FROM users WHERE uid = ? LIMIT 1");
    $stmt->bind_param("s", $landlord_id);
    $stmt->execute();
    $user = $stmt->get_result()->fetch_assoc();
    $stmt->close();
}

// Lấy tất cả posts của user (thử cả uid lẫn numeric id)
$posts_rows = [];
if ($user) {
    $r = $conn->query("SELECT id, user_id, title, total_rooms, available_rooms FROM posts WHERE user_id = '{$user['uid']}' OR user_id = '{$user['id']}'");
    while ($row = $r->fetch_assoc()) $posts_rows[] = $row;
}

// Lấy contracts
$contracts_rows = [];
if ($user) {
    $r = $conn->query("SELECT id, landlord_id, status FROM contracts WHERE landlord_id = '{$user['uid']}' OR landlord_id = '{$user['id']}'");
    while ($row = $r->fetch_assoc()) $contracts_rows[] = $row;
}

echo json_encode([
    "input_landlord_id" => $landlord_id,
    "user_found"        => $user,
    "posts"             => $posts_rows,
    "contracts"         => $contracts_rows
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
$conn->close();
