<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}
$conn->set_charset("utf8mb4");

$booking_id = intval($_POST['booking_id'] ?? 0);
$new_status = $_POST['status'] ?? ''; // confirmed | rejected

if ($booking_id === 0 || !in_array($new_status, ['confirmed', 'rejected'])) {
    echo json_encode(["status" => "error", "message" => "Dữ liệu không hợp lệ"]);
    exit;
}

$stmt = $conn->prepare("UPDATE bookings SET status = ? WHERE id = ?");
$stmt->bind_param("si", $new_status, $booking_id);

if ($stmt->execute()) {
    // Lấy post_id từ booking
    $post_res = $conn->query("SELECT post_id FROM bookings WHERE id = $booking_id");
    if ($post_res && $row = $post_res->fetch_assoc()) {
        $post_id = (int)$row['post_id'];

        // Đếm số booking confirmed thực tế (tách riêng để tránh lỗi subquery MySQL)
        $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
        $confirmed_count = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

        // Lấy total_rooms
        $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
        $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

        $available_rooms = max(0, $total_rooms - $confirmed_count);
        $available = $available_rooms > 0 ? 1 : 0;

        $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");
    }
    echo json_encode(["status" => "success", "message" => "Cập nhật thành công"], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["status" => "error", "message" => "Lỗi cập nhật"]);
}

$stmt->close();
$conn->close();
