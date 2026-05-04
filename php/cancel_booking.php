<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}
$conn->set_charset("utf8mb4");

$post_id = intval($_POST['post_id'] ?? 0);
$user_id = trim($_POST['user_id'] ?? '');

if ($post_id === 0 || empty($user_id)) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Lấy booking pending của tenant với bài này
$res = $conn->query("SELECT id, status FROM bookings WHERE post_id = $post_id AND user_id = '$user_id' AND status = 'pending' ORDER BY created_at DESC LIMIT 1");
if (!$res || $res->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy booking pending"]);
    exit;
}
$booking = $res->fetch_assoc();
$booking_id = (int)$booking['id'];

// Xóa booking (chỉ pending mới được hủy)
$conn->query("DELETE FROM bookings WHERE id = $booking_id AND status = 'pending'");

echo json_encode(["status" => "success", "message" => "Đã hủy đặt phòng"], JSON_UNESCAPED_UNICODE);
$conn->close();
