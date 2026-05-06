<?php
// Script chạy 1 lần để đồng bộ lại available_rooms cho tất cả posts
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$posts = $conn->query("SELECT id, COALESCE(total_rooms, available_rooms, 1) as total_rooms FROM posts");
$updated = 0;

while ($post = $posts->fetch_assoc()) {
    $pid = (int)$post['id'];
    $total = (int)$post['total_rooms'];

    $cnt = $conn->query("
        SELECT COUNT(*) as cnt FROM contracts
        WHERE post_id = $pid AND status IN ('active','agreed','confirmed')
    ");
    $occupied = $cnt ? (int)$cnt->fetch_assoc()['cnt'] : 0;

    $available_rooms = max(0, $total - $occupied);
    $available = $available_rooms > 0 ? 1 : 0;

    $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $pid");
    $updated++;
}

echo json_encode(["status" => "success", "updated_posts" => $updated]);
$conn->close();
