<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
include 'db_config.php';

$landlord_id = trim($_GET['landlord_id'] ?? '');
if ($landlord_id === '') {
    echo json_encode(["status" => "error", "message" => "Thiếu landlord_id"]);
    exit;
}

// Lấy phone của chủ trọ để match posts (vì posts.user_id có thể là Firebase UID khác)
$phone = null;
$numeric_id = null;
$stmt = $conn->prepare("SELECT id, phone FROM users WHERE uid = ? LIMIT 1");
$stmt->bind_param("s", $landlord_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
if ($row) {
    $numeric_id = (string)$row['id'];
    $phone = $row['phone'];
}
$stmt->close();

// Tổng số phòng: match theo uid, numeric id, HOẶC contact_phone (để bắt cả bài đăng dùng Firebase UID)
if ($phone) {
    $stmt = $conn->prepare("
        SELECT COALESCE(SUM(COALESCE(total_rooms, available_rooms, 1)), 0) AS total_rooms
        FROM posts
        WHERE user_id = ? OR user_id = ? OR contact_phone = ?
    ");
    $stmt->bind_param("sss", $landlord_id, $numeric_id, $phone);
} else {
    $stmt = $conn->prepare("
        SELECT COALESCE(SUM(COALESCE(total_rooms, available_rooms, 1)), 0) AS total_rooms
        FROM posts
        WHERE user_id = ?
    ");
    $stmt->bind_param("s", $landlord_id);
}
$stmt->execute();
$total_rooms = intval($stmt->get_result()->fetch_assoc()['total_rooms']);
$stmt->close();

// Số hợp đồng đang hoạt động (bao gồm cả đang chờ xác nhận hủy/gia hạn)
if ($numeric_id) {
    $stmt = $conn->prepare("
        SELECT COUNT(*) AS active_contracts
        FROM contracts
        WHERE (landlord_id = ? OR landlord_id = ?)
          AND status IN ('agreed', 'confirmed', 'active', 'cancel_requested', 'cancel_requested_by_tenant', 'renew_requested')
    ");
    $stmt->bind_param("ss", $landlord_id, $numeric_id);
} else {
    $stmt = $conn->prepare("
        SELECT COUNT(*) AS active_contracts
        FROM contracts
        WHERE landlord_id = ?
          AND status IN ('agreed', 'confirmed', 'active', 'cancel_requested', 'cancel_requested_by_tenant', 'renew_requested')
    ");
    $stmt->bind_param("s", $landlord_id);
}
$stmt->execute();
$active_contracts = intval($stmt->get_result()->fetch_assoc()['active_contracts']);
$stmt->close();

// Tổng người ở = số hợp đồng active + số người ở cùng (room_members)
if ($numeric_id) {
    $stmt = $conn->prepare("
        SELECT COUNT(*) AS member_count
        FROM room_members rm
        JOIN contracts c ON c.id = rm.contract_id
        WHERE (c.landlord_id = ? OR c.landlord_id = ?)
          AND c.status IN ('agreed', 'confirmed', 'active', 'cancel_requested', 'cancel_requested_by_tenant', 'renew_requested')
    ");
    $stmt->bind_param("ss", $landlord_id, $numeric_id);
} else {
    $stmt = $conn->prepare("
        SELECT COUNT(*) AS member_count
        FROM room_members rm
        JOIN contracts c ON c.id = rm.contract_id
        WHERE c.landlord_id = ?
          AND c.status IN ('agreed', 'confirmed', 'active', 'cancel_requested', 'cancel_requested_by_tenant', 'renew_requested')
    ");
    $stmt->bind_param("s", $landlord_id);
}
$stmt->execute();
$member_count = intval($stmt->get_result()->fetch_assoc()['member_count']);
$stmt->close();

$total_people = $active_contracts + $member_count;
$empty_rooms = max(0, $total_rooms - $active_contracts);

// Tính lại empty_rooms dựa trên contracts thực tế theo post_id của chủ
// để tránh lỗi landlord_id không khớp
if ($phone) {
    $cnt_res = $conn->query("
        SELECT COUNT(*) as cnt FROM contracts c
        JOIN posts p ON p.id = c.post_id
        WHERE (p.user_id = '$landlord_id' OR p.user_id = '$numeric_id' OR p.contact_phone = '$phone')
          AND c.status IN ('agreed','confirmed','active','cancel_requested','cancel_requested_by_tenant','renew_requested')
    ");
} else {
    $cnt_res = $conn->query("
        SELECT COUNT(*) as cnt FROM contracts c
        JOIN posts p ON p.id = c.post_id
        WHERE (p.user_id = '$landlord_id')
          AND c.status IN ('agreed','confirmed','active','cancel_requested','cancel_requested_by_tenant','renew_requested')
    ");
}
$occupied_via_post = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : $active_contracts;
$empty_rooms = max(0, $total_rooms - $occupied_via_post);

echo json_encode([
    "status"           => "success",
    "total_rooms"      => $total_rooms,
    "active_contracts" => $total_people,
    "empty_rooms"      => $empty_rooms
]);
$conn->close();
