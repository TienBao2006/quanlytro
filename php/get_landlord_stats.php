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

// Số hợp đồng đang hoạt động (tenant đã agreed/confirmed, chưa bị hủy)
if ($numeric_id) {
    $stmt = $conn->prepare("
        SELECT COUNT(*) AS active_contracts
        FROM contracts
        WHERE (landlord_id = ? OR landlord_id = ?)
          AND status IN ('agreed', 'confirmed')
    ");
    $stmt->bind_param("ss", $landlord_id, $numeric_id);
} else {
    $stmt = $conn->prepare("
        SELECT COUNT(*) AS active_contracts
        FROM contracts
        WHERE landlord_id = ?
          AND status IN ('agreed', 'confirmed')
    ");
    $stmt->bind_param("s", $landlord_id);
}
$stmt->execute();
$active_contracts = intval($stmt->get_result()->fetch_assoc()['active_contracts']);
$stmt->close();

$empty_rooms = max(0, $total_rooms - $active_contracts);

echo json_encode([
    "status"           => "success",
    "total_rooms"      => $total_rooms,
    "active_contracts" => $active_contracts,
    "empty_rooms"      => $empty_rooms
]);
$conn->close();
