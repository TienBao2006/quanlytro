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

$booking_id = intval($_POST['booking_id'] ?? 0);
if ($booking_id === 0) {
    echo json_encode(["status" => "error", "message" => "Thiếu booking_id"]);
    exit;
}

// Lấy post_id và status trước khi xóa
$res = $conn->query("SELECT post_id, status FROM bookings WHERE id = $booking_id");
if (!$res || $res->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy booking"]);
    exit;
}
$booking = $res->fetch_assoc();
$post_id = (int)$booking['post_id'];
$old_status = $booking['status'];

// Xóa booking
$conn->query("DELETE FROM bookings WHERE id = $booking_id");

// Nếu booking đã confirmed thì tính lại available_rooms
if ($old_status === 'confirmed') {
    $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
    $confirmed_count = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

    $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
    $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

    $available_rooms = max(0, $total_rooms - $confirmed_count);
    $available = $available_rooms > 0 ? 1 : 0;

    $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");
}

echo json_encode(["status" => "success", "message" => "Đã xóa booking"], JSON_UNESCAPED_UNICODE);
$conn->close();
