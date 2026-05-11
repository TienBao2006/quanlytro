<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) { echo json_encode([]); exit; }

// Đảm bảo cột lat/lng tồn tại
$conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS lat DOUBLE DEFAULT NULL");
$conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS lng DOUBLE DEFAULT NULL");

$lat    = floatval($_GET['lat']    ?? 0);
$lng    = floatval($_GET['lng']    ?? 0);
$radius = floatval($_GET['radius'] ?? 10);
$limit  = intval($_GET['limit']    ?? 20);

if ($lat == 0 && $lng == 0) { echo json_encode([]); exit; }

// Kiểm tra có bài nào có tọa độ không
$check = $conn->query("SELECT COUNT(*) AS cnt FROM posts WHERE lat IS NOT NULL AND lng IS NOT NULL");
$cnt = $check ? $check->fetch_assoc()['cnt'] : 0;

if ($cnt == 0) {
    // Chưa có bài nào có tọa độ — trả về tất cả bài kèm distance_km = null
    $result = $conn->query("
        SELECT p.id,
               COALESCE(u.uid, p.user_id) AS user_id,
               p.title, p.location, p.price, p.area,
               p.description, p.amenities, p.map_url,
               p.contact_name, p.contact_phone, p.images, p.created_at,
               p.lat, p.lng,
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
               ) > 0 THEN 1 ELSE 0 END AS available,
               NULL AS distance_km
        FROM posts p
        LEFT JOIN users u ON u.phone = p.contact_phone
        ORDER BY p.created_at DESC
        LIMIT $limit
    ");
} else {
    // Có tọa độ — dùng Haversine
    $result = $conn->query("
        SELECT p.id,
               COALESCE(u.uid, p.user_id) AS user_id,
               p.title, p.location, p.price, p.area,
               p.description, p.amenities, p.map_url,
               p.contact_name, p.contact_phone, p.images, p.created_at,
               p.lat, p.lng,
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
               ) > 0 THEN 1 ELSE 0 END AS available,
               (6371 * ACOS(
                   COS(RADIANS($lat)) * COS(RADIANS(p.lat)) *
                   COS(RADIANS(p.lng) - RADIANS($lng)) +
                   SIN(RADIANS($lat)) * SIN(RADIANS(p.lat))
               )) AS distance_km
        FROM posts p
        LEFT JOIN users u ON u.phone = p.contact_phone
        WHERE p.lat IS NOT NULL AND p.lng IS NOT NULL
        HAVING distance_km <= $radius
        ORDER BY distance_km ASC
        LIMIT $limit
    ");
}

if (!$result) { echo json_encode([]); $conn->close(); exit; }

$posts = [];
while ($row = $result->fetch_assoc()) {
    $imgs = $row['images'] ?? '[]';
    if (is_string($imgs)) {
        $decoded = json_decode($imgs, true);
        $row['images'] = is_array($decoded) ? $decoded : [];
    }
    if ($row['distance_km'] !== null) {
        $row['distance_km'] = round((float)$row['distance_km'], 1);
    }
    $posts[] = $row;
}

echo json_encode($posts);
$conn->close();
