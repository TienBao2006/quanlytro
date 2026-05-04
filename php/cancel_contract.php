<?php
ob_start();
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';
ob_clean();

$contract_id = intval($_POST['contract_id'] ?? 0);
$user_id     = trim($_POST['user_id'] ?? '');
$role        = trim($_POST['role'] ?? ''); // "landlord" | "tenant"

if (!$contract_id || !$user_id || !in_array($role, ['landlord', 'tenant'])) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Chỉ gửi yêu cầu hủy, không hủy thẳng
// Chủ yêu cầu → cancel_requested (khách phản hồi)
// Khách yêu cầu → cancel_requested_by_tenant (chủ phản hồi)
$new_status = ($role === 'landlord') ? 'cancel_requested' : 'cancel_requested_by_tenant';

if ($role === 'landlord') {
    $stmt = $conn->prepare("UPDATE contracts SET status=? WHERE id=? AND landlord_id=? AND status NOT IN ('cancelled','cancel_requested','cancel_requested_by_tenant')");
    $stmt->bind_param("sis", $new_status, $contract_id, $user_id);
} else {
    $stmt = $conn->prepare("UPDATE contracts SET status=? WHERE id=? AND tenant_id=? AND status NOT IN ('cancelled','cancel_requested','cancel_requested_by_tenant')");
    $stmt->bind_param("sis", $new_status, $contract_id, $user_id);
}

$stmt->execute();

if ($stmt->affected_rows > 0) {
    // Hủy booking và trả lại phòng trống ngay khi gửi yêu cầu hủy
    // (để đảm bảo phòng luôn được cập nhật dù flow có hoàn tất hay không)
    $info = $conn->query("SELECT booking_id, post_id FROM contracts WHERE id = $contract_id");
    if ($info && $row = $info->fetch_assoc()) {
        $booking_id = (int)$row['booking_id'];
        $post_id    = (int)$row['post_id'];

        $conn->query("UPDATE bookings SET status = 'cancelled' WHERE id = $booking_id");

        $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
        $confirmed_count = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

        $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
        $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

        $available_rooms = max(0, $total_rooms - $confirmed_count);
        $available = $available_rooms > 0 ? 1 : 0;

        $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");
    }
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => "Không thể gửi yêu cầu hủy"]);
}
$stmt->close();
$conn->close();
