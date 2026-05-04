<?php
ob_start();
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';
ob_clean();

$contract_id      = intval($_POST['contract_id'] ?? 0);
$landlord_id      = trim($_POST['landlord_id'] ?? '');

if (!$contract_id || !$landlord_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu contract_id hoặc landlord_id"]);
    exit;
}

// Lấy từng field
$p1  = floatval($_POST['rent_price'] ?? 0);        // d
$p2  = floatval($_POST['deposit'] ?? 0);            // d
$p3  = floatval($_POST['electric_price'] ?? 0);     // d
$p4  = floatval($_POST['water_price'] ?? 0);        // d
$p5  = floatval($_POST['other_fee'] ?? 0);          // d
$p6  = trim($_POST['other_fee_note'] ?? '');        // s
$p7  = trim($_POST['start_date'] ?? '');            // s
$p8  = intval($_POST['duration_months'] ?? 12);     // i
$p9  = intval($_POST['payment_day'] ?? 5);          // i
$p10 = trim($_POST['payment_method'] ?? 'cash');    // s
$p11 = trim($_POST['late_payment_rule'] ?? '');     // s
$p12 = trim($_POST['rules'] ?? '');                 // s
$p13 = intval($_POST['termination_notice_days'] ?? 30); // i
$p14 = trim($_POST['deposit_return_condition'] ?? '');  // s
$p15 = trim($_POST['room_name'] ?? '');             // s
$p16 = trim($_POST['room_address'] ?? '');          // s
$p17 = floatval($_POST['room_area'] ?? 0);          // d
$p18 = trim($_POST['amenities'] ?? '');             // s
// WHERE: $contract_id(i), $landlord_id(s)

// format: d d d d d s s i i s s s i s s s d s i s  = 20
$stmt = $conn->prepare("UPDATE contracts SET
    rent_price=?, deposit=?, electric_price=?, water_price=?, other_fee=?,
    other_fee_note=?, start_date=?, duration_months=?, payment_day=?,
    payment_method=?, late_payment_rule=?, rules=?, termination_notice_days=?,
    deposit_return_condition=?, room_name=?, room_address=?, room_area=?,
    amenities=?, status='active'
    WHERE id=? AND landlord_id=?");

$stmt->bind_param(
    "dddddssiiisssissdsis",
    $p1, $p2, $p3, $p4, $p5,
    $p6, $p7,
    $p8, $p9,
    $p10, $p11, $p12,
    $p13,
    $p14, $p15, $p16,
    $p17,
    $p18,
    $contract_id, $landlord_id
);

try {
    $stmt->execute();
    echo json_encode(["status" => "success"]);
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
$stmt->close();
$conn->close();
