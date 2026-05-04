<?php
header("Content-Type: application/json; charset=UTF-8");
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
include 'db_config.php';

// Tạo bảng nếu chưa có
$conn->query("CREATE TABLE IF NOT EXISTS contracts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    post_id INT NOT NULL,
    landlord_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    tenant_name VARCHAR(255) NOT NULL,
    tenant_phone VARCHAR(20) NOT NULL,
    tenant_id_card VARCHAR(50) NOT NULL,
    tenant_address TEXT,
    landlord_name VARCHAR(255) NOT NULL,
    landlord_phone VARCHAR(20) NOT NULL,
    landlord_address TEXT,
    room_name VARCHAR(255),
    room_address TEXT,
    room_area FLOAT DEFAULT 0,
    amenities TEXT,
    rent_price DOUBLE NOT NULL,
    deposit DOUBLE NOT NULL,
    electric_price DOUBLE DEFAULT 0,
    water_price DOUBLE DEFAULT 0,
    other_fee DOUBLE DEFAULT 0,
    other_fee_note TEXT,
    start_date DATE NOT NULL,
    duration_months INT NOT NULL,
    payment_day INT DEFAULT 5,
    payment_method VARCHAR(50) DEFAULT 'cash',
    late_payment_rule TEXT,
    rules TEXT,
    termination_notice_days INT DEFAULT 30,
    deposit_return_condition TEXT,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

$booking_id       = intval($_POST['booking_id'] ?? 0);
$post_id          = intval($_POST['post_id'] ?? 0);
$landlord_id      = trim($_POST['landlord_id'] ?? '');
$tenant_id        = trim($_POST['tenant_id'] ?? '');
$tenant_name      = trim($_POST['tenant_name'] ?? '');
$tenant_phone     = trim($_POST['tenant_phone'] ?? '');
$tenant_id_card   = trim($_POST['tenant_id_card'] ?? '');
$tenant_address   = trim($_POST['tenant_address'] ?? '');
$landlord_name    = trim($_POST['landlord_name'] ?? '');
$landlord_phone   = trim($_POST['landlord_phone'] ?? '');
$landlord_address = trim($_POST['landlord_address'] ?? '');
$room_name        = trim($_POST['room_name'] ?? '');
$room_address     = trim($_POST['room_address'] ?? '');
$room_area        = floatval($_POST['room_area'] ?? 0);
$amenities        = trim($_POST['amenities'] ?? '');
$rent_price       = floatval($_POST['rent_price'] ?? 0);
$deposit          = floatval($_POST['deposit'] ?? 0);
$electric_price   = floatval($_POST['electric_price'] ?? 0);
$water_price      = floatval($_POST['water_price'] ?? 0);
$other_fee        = floatval($_POST['other_fee'] ?? 0);
$other_fee_note   = trim($_POST['other_fee_note'] ?? '');
$start_date       = trim($_POST['start_date'] ?? '');
$duration_months  = intval($_POST['duration_months'] ?? 12);
$payment_day      = intval($_POST['payment_day'] ?? 5);
$payment_method   = trim($_POST['payment_method'] ?? 'cash');
$late_payment_rule= trim($_POST['late_payment_rule'] ?? '');
$rules            = trim($_POST['rules'] ?? '');
$termination_days = intval($_POST['termination_notice_days'] ?? 30);
$deposit_return   = trim($_POST['deposit_return_condition'] ?? '');

if (!$booking_id || !$landlord_id || !$tenant_id
    || !$tenant_name || !$tenant_phone || !$tenant_id_card
    || !$landlord_name || !$landlord_phone
    || !$room_address || !$rent_price || !$deposit || !$start_date) {
    $missing = [];
    if (!$booking_id)     $missing[] = "booking_id";
    if (!$landlord_id)    $missing[] = "landlord_id";
    if (!$tenant_id)      $missing[] = "tenant_id";
    if (!$tenant_name)    $missing[] = "tenant_name";
    if (!$tenant_phone)   $missing[] = "tenant_phone";
    if (!$tenant_id_card) $missing[] = "tenant_id_card";
    if (!$landlord_name)  $missing[] = "landlord_name";
    if (!$landlord_phone) $missing[] = "landlord_phone";
    if (!$room_address)   $missing[] = "room_address";
    if (!$rent_price)     $missing[] = "rent_price";
    if (!$deposit && $deposit !== 0.0) $missing[] = "deposit";
    if (!$start_date)     $missing[] = "start_date";
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin: " . implode(", ", $missing)]);
    exit;
}

$stmt = $conn->prepare("INSERT INTO contracts 
    (booking_id, post_id, landlord_id, tenant_id, tenant_name, tenant_phone, tenant_id_card, tenant_address,
     landlord_name, landlord_phone, landlord_address, room_name, room_address, room_area, amenities,
     rent_price, deposit, electric_price, water_price, other_fee, other_fee_note,
     start_date, duration_months, payment_day, payment_method, late_payment_rule,
     rules, termination_notice_days, deposit_return_condition)
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

// i=int, d=double, s=string — 29 params
// ii  = booking_id, post_id
// ss  = landlord_id, tenant_id
// sss = tenant_name, tenant_phone, tenant_id_card
// s   = tenant_address
// sss = landlord_name, landlord_phone, landlord_address
// sss = room_name, room_address
// d   = room_area
// s   = amenities
// dd  = rent_price, deposit
// ddd = electric_price, water_price, other_fee
// s   = other_fee_note
// s   = start_date
// ii  = duration_months, payment_day
// s   = payment_method
// s   = late_payment_rule
// s   = rules
// i   = termination_notice_days
// s   = deposit_return_condition
$stmt->bind_param(
    "iisssssssssssdsdddddssississs",
    $booking_id, $post_id,
    $landlord_id, $tenant_id,
    $tenant_name, $tenant_phone, $tenant_id_card, $tenant_address,
    $landlord_name, $landlord_phone, $landlord_address,
    $room_name, $room_address,
    $room_area,
    $amenities,
    $rent_price, $deposit,
    $electric_price, $water_price, $other_fee,
    $other_fee_note,
    $start_date,
    $duration_months, $payment_day,
    $payment_method,
    $late_payment_rule,
    $rules,
    $termination_days,
    $deposit_return
);

try {
    $stmt->execute();
    echo json_encode(["status" => "success", "contract_id" => $conn->insert_id]);
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
$stmt->close();
$conn->close();
