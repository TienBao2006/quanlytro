<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$landlord_id = trim($_GET['landlord_id'] ?? '');
$tenant_id   = trim($_GET['tenant_id'] ?? '');
$contract_id = intval($_GET['id'] ?? 0);

if ($contract_id > 0) {
    $stmt = $conn->prepare("SELECT * FROM contracts WHERE id = ?");
    $stmt->bind_param("i", $contract_id);
} elseif ($landlord_id !== '') {
    // So sánh cả string lẫn số để tương thích uid cũ
    $stmt = $conn->prepare("SELECT * FROM contracts WHERE landlord_id = ? OR landlord_id = CAST(? AS CHAR) ORDER BY created_at DESC");
    $stmt->bind_param("ss", $landlord_id, $landlord_id);
} elseif ($tenant_id !== '') {
    $stmt = $conn->prepare("SELECT * FROM contracts WHERE tenant_id = ? ORDER BY created_at DESC");
    $stmt->bind_param("s", $tenant_id);
} else {
    echo json_encode(["status" => "error", "message" => "Thiếu tham số"]);
    exit;
}

$stmt->execute();
$result = $stmt->get_result();
$contracts = [];
while ($row = $result->fetch_assoc()) {
    // Ép kiểu số để Gson parse đúng
    $row['id']                     = intval($row['id']);
    $row['booking_id']             = intval($row['booking_id']);
    $row['post_id']                = intval($row['post_id']);
    $row['room_area']              = floatval($row['room_area']);
    $row['rent_price']             = floatval($row['rent_price']);
    $row['deposit']                = floatval($row['deposit']);
    $row['electric_price']         = floatval($row['electric_price']);
    $row['water_price']            = floatval($row['water_price']);
    $row['other_fee']              = floatval($row['other_fee']);
    $row['duration_months']        = intval($row['duration_months']);
    $row['payment_day']            = intval($row['payment_day']);
    $row['termination_notice_days']= intval($row['termination_notice_days']);
    $contracts[] = $row;
}
echo json_encode(["status" => "success", "contracts" => $contracts]);
$stmt->close();
$conn->close();
