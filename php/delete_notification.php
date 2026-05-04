<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$notification_id = intval($_POST['notification_id'] ?? 0);
$user_id         = trim($_POST['user_id'] ?? '');

if (!$notification_id || !$user_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

$stmt = $conn->prepare("DELETE FROM notifications WHERE id = ? AND user_id = ?");
$stmt->bind_param("is", $notification_id, $user_id);
$stmt->execute();
$affected = $stmt->affected_rows;
$stmt->close();

echo json_encode([
    "status" => $affected > 0 ? "success" : "error",
    "message" => $affected > 0 ? "Đã xóa" : "Không tìm thấy thông báo"
]);
$conn->close();
