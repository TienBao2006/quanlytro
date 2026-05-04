<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$contract_id = intval($_POST['contract_id'] ?? 0);
$landlord_id = trim($_POST['landlord_id'] ?? '');
$action      = trim($_POST['action'] ?? ''); // "accept" | "reject"

if (!$contract_id || !$landlord_id || !in_array($action, ['accept', 'reject'])) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Lấy thông tin hợp đồng
$chk = $conn->prepare("SELECT id, tenant_id, tenant_name, room_name, duration_months, renew_requested_months FROM contracts WHERE id=? AND landlord_id=? AND status='renew_requested'");
$chk->bind_param("is", $contract_id, $landlord_id);
$chk->execute();
$contract = $chk->get_result()->fetch_assoc();
$chk->close();

if (!$contract) {
    echo json_encode(["status" => "error", "message" => "Hợp đồng không hợp lệ"]);
    exit;
}

$tenant_id     = $contract['tenant_id'];
$roomLabel     = $contract['room_name'] ?: "Phòng trọ";
$extendMonths  = (int)$contract['renew_requested_months'];
$newDuration   = (int)$contract['duration_months'] + $extendMonths;

if ($action === 'accept') {
    // Gia hạn: cộng thêm số tháng, reset status về agreed
    $stmt = $conn->prepare("UPDATE contracts SET duration_months=?, status='agreed', renew_requested_months=0 WHERE id=?");
    $stmt->bind_param("ii", $newDuration, $contract_id);
    $stmt->execute();
    $stmt->close();

    $title = "✅ Yêu cầu gia hạn được chấp nhận";
    $msg   = "Chủ trọ đã chấp nhận gia hạn hợp đồng ({$roomLabel}) thêm {$extendMonths} tháng. Thời hạn mới: {$newDuration} tháng.";
} else {
    // Từ chối: trả về agreed
    $stmt = $conn->prepare("UPDATE contracts SET status='agreed', renew_requested_months=0 WHERE id=?");
    $stmt->bind_param("i", $contract_id);
    $stmt->execute();
    $stmt->close();

    $title = "❌ Yêu cầu gia hạn bị từ chối";
    $msg   = "Chủ trọ đã từ chối yêu cầu gia hạn hợp đồng ({$roomLabel}).";
}

// Thông báo cho người thuê
$ins = $conn->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES (?,?,?,'renew_responded',?,0,NOW())");
$ins->bind_param("sssi", $tenant_id, $title, $msg, $contract_id);
$ins->execute();
$ins->close();

echo json_encode(["status" => "success"]);
$conn->close();
