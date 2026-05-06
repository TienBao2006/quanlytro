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
    // Gửi notification cho bên còn lại
    if ($role === 'tenant') {
        // Khách yêu cầu hủy → thông báo cho chủ trọ
        $info2 = $conn->query("SELECT c.landlord_id, c.tenant_name, c.room_name FROM contracts c WHERE c.id = $contract_id");
        if ($info2 && $row2 = $info2->fetch_assoc()) {
            $landlord_id  = $row2['landlord_id'];
            $tenant_name  = $conn->real_escape_string($row2['tenant_name']);
            $room_label   = $conn->real_escape_string($row2['room_name'] ?: 'Phòng trọ');
            $title_notif  = "Yêu cầu hủy hợp đồng";
            $msg_notif    = "Người thuê $tenant_name đã yêu cầu hủy hợp đồng phòng $room_label. Vui lòng vào mục Hợp đồng để phản hồi.";
            $conn->query("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES ('$landlord_id', '$title_notif', '$msg_notif', 'cancel_contract', $contract_id, 0, NOW())");
        }
    } else {
        // Chủ yêu cầu hủy → thông báo cho khách
        $info2 = $conn->query("SELECT c.tenant_id, c.room_name FROM contracts c WHERE c.id = $contract_id");
        if ($info2 && $row2 = $info2->fetch_assoc()) {
            $tenant_id2  = $row2['tenant_id'];
            $room_label  = $conn->real_escape_string($row2['room_name'] ?: 'Phòng trọ');
            $title_notif = "Yêu cầu hủy hợp đồng";
            $msg_notif   = "Chủ trọ đã gửi yêu cầu hủy hợp đồng phòng $room_label. Vui lòng vào mục Hợp đồng để phản hồi.";
            $conn->query("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES ('$tenant_id2', '$title_notif', '$msg_notif', 'cancel_contract', $contract_id, 0, NOW())");
        }
    }
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => "Không thể gửi yêu cầu hủy"]);
}
$stmt->close();
$conn->close();
