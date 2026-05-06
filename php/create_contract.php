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

// Validate và normalize start_date — chấp nhận yyyy-mm-dd hoặc dd/mm/yyyy
if (!empty($start_date)) {
    if (preg_match('/^(\d{2})\/(\d{2})\/(\d{4})$/', $start_date, $m)) {
        $start_date = "{$m[3]}-{$m[2]}-{$m[1]}";
    }
    // Nếu vẫn không đúng format yyyy-mm-dd thì reset về rỗng
    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $start_date) || $start_date === '0000-00-00') {
        $start_date = '';
    }
}
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
    $contract_id = $conn->insert_id;

    // Thông báo cho khách thuê khi chủ tạo hợp đồng
    $room_label = $conn->real_escape_string($room_name ?: $room_address);
    $ntitle = "📄 Hợp đồng mới chờ xác nhận";
    $nmsg   = "Chủ trọ đã tạo hợp đồng cho phòng \"$room_label\". Vui lòng vào mục Hợp đồng để xem và xác nhận.";
    $conn->query("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES ('$tenant_id', '$ntitle', '$nmsg', 'contract_new', $contract_id, 0, NOW())");

    // Cập nhật available_rooms của post nếu có post_id hợp lệ
    if ($post_id > 0) {
        $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
        $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

        $cnt_res = $conn->query("
            SELECT COUNT(*) as cnt FROM contracts
            WHERE post_id = $post_id AND status IN ('active','agreed','confirmed')
        ");
        $occupied = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

        $available_rooms = max(0, $total_rooms - $occupied);
        $available = $available_rooms > 0 ? 1 : 0;
        $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");
    }

    echo json_encode(["status" => "success", "contract_id" => $contract_id]);
} catch (Exception $e) {
    echo json_encode(["status" => "error", "message" => $e->getMessage()]);
}
$stmt->close();
$conn->close();
