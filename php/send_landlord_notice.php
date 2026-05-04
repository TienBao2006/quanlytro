<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$landlord_id = trim($_POST["landlord_id"] ?? "");
$post_id     = intval($_POST["post_id"] ?? 0);
$title       = trim($_POST["title"] ?? "");
$message     = trim($_POST["message"] ?? "");

if (!$landlord_id || !$post_id || !$title || !$message) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Kiểm tra chủ trọ có sở hữu post này không
// Join với users để so sánh uid đúng (giống get_posts.php)
$check = $conn->prepare(
    "SELECT p.id FROM posts p
     LEFT JOIN users u ON u.phone = p.contact_phone
     WHERE p.id = ? AND (COALESCE(u.uid, p.user_id) = ? OR p.user_id = ?)"
);
$check->bind_param("iss", $post_id, $landlord_id, $landlord_id);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Không có quyền"]);
    exit;
}
$check->close();

// Lấy tất cả tenant đang có hợp đồng active với post này
$stmt = $conn->prepare(
    "SELECT DISTINCT tenant_id FROM contracts
     WHERE post_id=? AND landlord_id=? AND status IN ('agreed','active','cancel_requested','cancel_requested_by_tenant')"
);
$stmt->bind_param("is", $post_id, $landlord_id);
$stmt->execute();
$tenants = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

if (empty($tenants)) {
    echo json_encode(["status" => "error", "message" => "Không có người thuê nào"]);
    exit;
}

// Lưu thông báo vào bảng landlord_notices
$ins = $conn->prepare(
    "INSERT INTO landlord_notices (landlord_id, post_id, title, message, created_at)
     VALUES (?, ?, ?, ?, NOW())"
);
$ins->bind_param("siss", $landlord_id, $post_id, $title, $message);
$ins->execute();
$notice_id = $ins->insert_id;
$ins->close();

// Gửi thông báo đến từng tenant
$notif = $conn->prepare(
    "INSERT INTO notifications (user_id, title, message, type, is_read, created_at)
     VALUES (?, ?, ?, 'landlord_notice', 0, NOW())"
);
$count = 0;
foreach ($tenants as $t) {
    $notif->bind_param("sss", $t["tenant_id"], $title, $message);
    $notif->execute();
    $count++;
}
$notif->close();

echo json_encode([
    "status"       => "success",
    "notice_id"    => $notice_id,
    "sent_to"      => $count,
    "message"      => "Đã gửi thông báo đến $count người thuê"
]);
