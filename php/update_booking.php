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

$booking_id  = intval($_POST['booking_id'] ?? 0);
$new_status  = trim($_POST['status'] ?? '');       // "confirmed" | "rejected" | "pending"
$landlord_id = trim($_POST['landlord_id'] ?? '');

if ($booking_id === 0 || !in_array($new_status, ['confirmed', 'rejected', 'pending']) || empty($landlord_id)) {
    echo json_encode(["status" => "error", "message" => "Tham số không hợp lệ"]);
    exit;
}

// Chỉ cho phép chủ trọ sở hữu bài đăng mới được cập nhật
$stmt = $conn->prepare("
    UPDATE bookings b
    JOIN posts p ON p.id = b.post_id
    LEFT JOIN users u ON u.phone = p.contact_phone
    SET b.status = ?
    WHERE b.id = ? AND COALESCE(u.uid, p.user_id) = ?
");
$stmt->bind_param("sis", $new_status, $booking_id, $landlord_id);
$stmt->execute();

if ($stmt->affected_rows > 0) {
    // Tính lại available_rooms sau khi cập nhật status
    $post_res = $conn->query("SELECT post_id, user_id FROM bookings WHERE id = $booking_id");
    if ($post_res && $row = $post_res->fetch_assoc()) {
        $post_id    = (int)$row['post_id'];
        $tenant_uid = $row['user_id'];

        $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
        $confirmed_count = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

        $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
        $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

        $available_rooms = max(0, $total_rooms - $confirmed_count);
        $available = $available_rooms > 0 ? 1 : 0;

        $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");

        // Gửi thông báo cho khách thuê
        $post_info = $conn->query("SELECT title FROM posts WHERE id = $post_id");
        $post_title = $post_info ? $conn->real_escape_string($post_info->fetch_assoc()['title'] ?? 'Phòng trọ') : 'Phòng trọ';

        if ($new_status === 'confirmed') {
            $ntitle = "✅ Đặt phòng được xác nhận";
            $nmsg   = "Yêu cầu đặt phòng \"$post_title\" của bạn đã được chủ trọ xác nhận.";
        } elseif ($new_status === 'pending') {
            $ntitle = "🔄 Xác nhận đặt phòng bị hủy";
            $nmsg   = "Chủ trọ đã hủy xác nhận yêu cầu đặt phòng \"$post_title\". Yêu cầu của bạn đang chờ xử lý lại.";
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
    echo json_encode(["status" => "error", "message" => "Không tìm thấy hoặc không có quyền"], JSON_UNESCAPED_UNICODE);
}

$stmt->close();
$conn->close();
