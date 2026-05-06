<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$contract_id = intval($_POST['contract_id'] ?? 0);
$landlord_id = trim($_POST['landlord_id'] ?? '');
$tenant_id   = trim($_POST['tenant_id'] ?? '');
$message     = trim($_POST['message'] ?? '');

if (!$contract_id || !$landlord_id || !$tenant_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu tham số"]);
    exit;
}

if (!$message) {
    $message = "Bạn có hóa đơn chưa thanh toán. Vui lòng thanh toán đúng hạn.";
}

$stmt = $conn->prepare(
    "INSERT INTO notifications (user_id, title, message, type, is_read, created_at)
     VALUES (?, 'Nhắc thanh toán', ?, 'payment', 0, NOW())"
);
$stmt->bind_param("ss", $tenant_id, $message);
$ok = $stmt->execute();
$stmt->close();

echo json_encode([
    "status"  => $ok ? "success" : "error",
    "message" => $ok ? "Đã gửi nhắc nhở" : "Lỗi gửi thông báo"
]);
$conn->close();
