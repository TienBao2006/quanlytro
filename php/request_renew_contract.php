<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$contract_id    = intval($_POST['contract_id'] ?? 0);
$tenant_id      = trim($_POST['tenant_id'] ?? '');
$extend_months  = intval($_POST['extend_months'] ?? 0);

if (!$contract_id || !$tenant_id || $extend_months <= 0) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

// Kiểm tra hợp đồng thuộc về tenant này và đang agreed
$chk = $conn->prepare("SELECT id, landlord_id, tenant_name, room_name FROM contracts WHERE id=? AND tenant_id=? AND status='agreed'");
$chk->bind_param("is", $contract_id, $tenant_id);
$chk->execute();
$contract = $chk->get_result()->fetch_assoc();
$chk->close();

if (!$contract) {
    echo json_encode(["status" => "error", "message" => "Hợp đồng không hợp lệ"]);
    exit;
}

// Cập nhật trạng thái yêu cầu gia hạn
$stmt = $conn->prepare("UPDATE contracts SET status='renew_requested', renew_requested_months=? WHERE id=?");
$stmt->bind_param("ii", $extend_months, $contract_id);
$stmt->execute();
$stmt->close();

// Thông báo cho chủ trọ
$landlord_id = $contract['landlord_id'];
$roomLabel   = $contract['room_name'] ?: "Phòng trọ";
$tenantName  = $contract['tenant_name'];
$title       = "🔄 Yêu cầu gia hạn hợp đồng";
$msg         = "{$tenantName} ({$roomLabel}) yêu cầu gia hạn hợp đồng thêm {$extend_months} tháng.";

$ins = $conn->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES (?,?,?,'renew_requested',?,0,NOW())");
$ins->bind_param("sssi", $landlord_id, $title, $msg, $contract_id);
$ins->execute();
$ins->close();

echo json_encode(["status" => "success", "message" => "Đã gửi yêu cầu gia hạn"]);
$conn->close();
