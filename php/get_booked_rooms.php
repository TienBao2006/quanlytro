<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "booked_rooms" => []]);
    exit;
}
$conn->set_charset("utf8mb4");

$post_id = intval($_GET['post_id'] ?? 0);
if ($post_id === 0) {
    echo json_encode(["status" => "error", "booked_rooms" => []]);
    exit;
}

// Lấy các phòng đã có booking pending hoặc confirmed
$stmt = $conn->prepare(
    "SELECT DISTINCT room_number FROM bookings
     WHERE post_id = ? AND status IN ('pending', 'confirmed') AND room_number != ''"
);
$stmt->bind_param("i", $post_id);
$stmt->execute();
$result = $stmt->get_result();

$booked = [];
while ($row = $result->fetch_assoc()) {
    $booked[] = $row['room_number'];
}

echo json_encode(["status" => "success", "booked_rooms" => $booked], JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
