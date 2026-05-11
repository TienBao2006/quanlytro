<?php
header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
include 'db_config.php';

// Test insert đơn giản không có ảnh
$user_id      = trim($_GET['uid'] ?? 'test_uid');
$title        = 'Test bài đăng';
$location     = 'Test địa chỉ, Quận 1, TP.HCM';
$price        = 3000000.0;
$area         = 25.0;
$description  = 'Mô tả test';
$amenities    = '';
$map_url      = '10.762622,106.660172';
$contact_name = 'Test User';
$contact_phone= '0901234567';
$images       = '[]';
$total_rooms  = 1;
$available_rooms = 1;
$available    = 1;
$lat          = 10.762622;
$lng          = 106.660172;

$sql = "INSERT INTO posts (user_id, title, location, price, area, description, amenities, map_url, contact_name, contact_phone, images, available, available_rooms, total_rooms, lat, lng)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    echo json_encode(["status" => "error", "step" => "prepare", "error" => $conn->error]);
    exit;
}

$bind = $stmt->bind_param("sssddssssssiiidd",
    $user_id, $title, $location, $price, $area,
    $description, $amenities, $map_url,
    $contact_name, $contact_phone, $images,
    $available, $available_rooms, $total_rooms,
    $lat, $lng
);

if (!$bind) {
    echo json_encode(["status" => "error", "step" => "bind_param", "error" => $stmt->error]);
    exit;
}

if ($stmt->execute()) {
    $new_id = $conn->insert_id;
    // Xóa bài test vừa tạo
    $conn->query("DELETE FROM posts WHERE id = $new_id");
    echo json_encode(["status" => "success", "message" => "Insert OK, đã xóa bài test", "insert_id" => $new_id]);
} else {
    echo json_encode(["status" => "error", "step" => "execute", "error" => $stmt->error]);
}

$stmt->close();
$conn->close();
