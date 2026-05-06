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
    // Lấy post_id và user_id từ booking
    $post_res = $conn->query("SELECT post_id, user_id, full_name FROM bookings WHERE id = $booking_id");
    if ($post_res && $row = $post_res->fetch_assoc()) {
        $post_id    = (int)$row['post_id'];
        $tenant_uid = $row['user_id'];
        $full_name  = $conn->real_escape_string($row['full_name'] ?? '');

        // Cập nhật available_rooms
        $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
        $confirmed_count = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

        $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
        $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

        $available_rooms = max(0, $total_rooms - $confirmed_count);
        $available = $available_rooms > 0 ? 1 : 0;
        $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");

        // Lấy title bài đăng để thông báo
        $post_info = $conn->query("SELECT title FROM posts WHERE id = $post_id");
        $post_title = $post_info ? $conn->real_escape_string($post_info->fetch_assoc()['title'] ?? 'Phòng trọ') : 'Phòng trọ';

        // Thông báo cho khách thuê
        if ($new_status === 'confirmed') {
            $ntitle = "✅ Đặt phòng được xác nhận";
            $nmsg   = "Yêu cầu đặt phòng \"$post_title\" của bạn đã được chủ trọ xác nhận.";
        } else {
            $ntitle = "❌ Đặt phòng bị từ chối";
            $nmsg   = "Yêu cầu đặt phòng \"$post_title\" của bạn đã bị chủ trọ từ chối.";
        }
        if ($tenant_uid) {
            $conn->query("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES ('$tenant_uid', '$ntitle', '$nmsg', 'booking_response', $booking_id, 0, NOW())");
        }
    }
    echo json_encode(["status" => "success", "message" => "Cập nhật thành công"], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["status" => "error", "message" => "Lỗi cập nhật"]);
}

$stmt->close();
$conn->close();
