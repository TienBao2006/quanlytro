<?php
// Script tạm để reset available_rooms = total_rooms cho tất cả bài chưa có booking confirmed
header("Content-Type: application/json");
$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
$conn->set_charset("utf8mb4");

// Đảm bảo cột tồn tại
$conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS total_rooms INT DEFAULT 1");
$conn->query("UPDATE posts SET total_rooms = available_rooms WHERE total_rooms IS NULL OR total_rooms = 0");

// Tính lại available_rooms cho từng bài dựa trên booking confirmed thực tế
$posts = $conn->query("SELECT id, total_rooms FROM posts");
$updated = 0;
while ($post = $posts->fetch_assoc()) {
    $pid = $post['id'];
    $total = $post['total_rooms'];
    $res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $pid AND status = 'confirmed'");
    $confirmed = (int)$res->fetch_assoc()['cnt'];
    $available_rooms = max(0, $total - $confirmed);
    $available = $available_rooms > 0 ? 1 : 0;
    $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $pid");
    $updated++;
}

echo json_encode(["status" => "ok", "posts_updated" => $updated]);
$conn->close();
