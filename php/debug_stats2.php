<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
include 'db_config.php';

// Hiện toàn bộ users
$users = [];
$r = $conn->query("SELECT id, uid, fullName, role FROM users");
while ($row = $r->fetch_assoc()) $users[] = $row;

// Hiện toàn bộ posts (user_id)
$posts = [];
$r = $conn->query("SELECT id, user_id, title, total_rooms, available_rooms FROM posts ORDER BY id DESC LIMIT 10");
while ($row = $r->fetch_assoc()) $posts[] = $row;

// Hiện toàn bộ contracts
$contracts = [];
$r = $conn->query("SELECT id, landlord_id, tenant_id, status FROM contracts ORDER BY id DESC LIMIT 10");
while ($row = $r->fetch_assoc()) $contracts[] = $row;

echo json_encode([
    "users"     => $users,
    "posts"     => $posts,
    "contracts" => $contracts
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
$conn->close();
