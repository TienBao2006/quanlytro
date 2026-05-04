<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "none"]);
    exit;
}
$conn->set_charset("utf8mb4");

$post_id = intval($_GET['post_id'] ?? 0);
$user_id = trim($_GET['user_id'] ?? '');

if ($post_id === 0 || empty($user_id)) {
    echo json_encode(["status" => "none"]);
    exit;
}

$stmt = $conn->prepare("SELECT status FROM bookings WHERE post_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 1");
$stmt->bind_param("is", $post_id, $user_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(["status" => "none"]);
} else {
    $row = $result->fetch_assoc();
    echo json_encode(["status" => $row['status']]);
}

$stmt->close();
$conn->close();
