<?php
// Script sửa lại available_rooms cho tất cả bài đăng
// Chạy 1 lần để fix dữ liệu bị sai
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$posts_res = $conn->query("SELECT id, COALESCE(total_rooms, available_rooms, 1) as total_rooms FROM posts");
$results = [];

while ($post = $posts_res->fetch_assoc()) {
    $post_id     = (int)$post['id'];
    $total_rooms = (int)$post['total_rooms'];

    // Đếm hợp đồng đang chiếm phòng (tất cả status chưa kết thúc)
    $cnt_res = $conn->query("
        SELECT COUNT(*) as cnt FROM contracts
        WHERE post_id = $post_id
          AND status IN ('active','agreed','confirmed','cancel_requested','cancel_requested_by_tenant','renew_requested')
    ");
    $occupied = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

    $available_rooms = max(0, $total_rooms - $occupied);
    $available = $available_rooms > 0 ? 1 : 0;

    $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");

    $results[] = [
        "post_id"         => $post_id,
        "total_rooms"     => $total_rooms,
        "occupied"        => $occupied,
        "available_rooms" => $available_rooms
    ];
}

echo json_encode(["status" => "success", "fixed" => count($results), "details" => $results]);
$conn->close();
