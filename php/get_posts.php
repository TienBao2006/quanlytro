<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode([]);
    exit;
}

// Đảm bảo cột total_rooms tồn tại
$conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS total_rooms INT DEFAULT 1");
// Đồng bộ total_rooms cho các bài cũ chưa có giá trị
$conn->query("UPDATE posts SET total_rooms = available_rooms WHERE total_rooms IS NULL OR total_rooms = 0");

// Join với users qua contact_phone để luôn lấy MySQL uid đúng
$post_id_filter = intval($_GET['id'] ?? 0);
$where = $post_id_filter > 0 ? "WHERE p.id = $post_id_filter" : "";

$result = $conn->query("
    SELECT p.id,
           COALESCE(u.uid, p.user_id) AS user_id,
           p.title, p.location, p.price, p.area,
           p.description, p.amenities, p.map_url,
           p.contact_name, p.contact_phone, p.images, p.created_at,
           COALESCE(p.total_rooms, p.available_rooms, 1) AS total_rooms,
           (
               COALESCE(p.total_rooms, p.available_rooms, 1) - (
                   SELECT COUNT(*) FROM contracts c
                   WHERE c.post_id = p.id
                     AND c.status IN ('active','agreed','confirmed','cancel_requested','cancel_requested_by_tenant','renew_requested')
               )
           ) AS available_rooms,
           CASE WHEN (
               COALESCE(p.total_rooms, p.available_rooms, 1) - (
                   SELECT COUNT(*) FROM contracts c
                   WHERE c.post_id = p.id
                     AND c.status IN ('active','agreed','confirmed','cancel_requested','cancel_requested_by_tenant','renew_requested')
               )
           ) > 0 THEN 1 ELSE 0 END AS available
    FROM posts p
    LEFT JOIN users u ON u.phone = p.contact_phone
    $where
    ORDER BY p.created_at DESC
");

if (!$result) {
    echo json_encode([]);
    $conn->close();
    exit;
}

$posts = [];
while ($row = $result->fetch_assoc()) {
    $imgs = $row['images'] ?? '[]';
    if (is_string($imgs)) {
        $decoded = json_decode($imgs, true);
        $row['images'] = is_array($decoded) ? $decoded : [];
    }
    $posts[] = $row;
}

echo json_encode($posts);
$conn->close();
