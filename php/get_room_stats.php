<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
include 'db_config.php';

$landlord_id = trim($_GET['landlord_id'] ?? '');
$month       = trim($_GET['month'] ?? '');  // format: YYYY-MM

if ($landlord_id === '' || $month === '') {
    echo json_encode(["status" => "error", "message" => "Thiếu tham số"]);
    exit;
}

// Resolve numeric id
$numeric_id = null;
$stmt = $conn->prepare("SELECT id FROM users WHERE uid = ? LIMIT 1");
$stmt->bind_param("s", $landlord_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
if ($row) $numeric_id = (string)$row['id'];
$stmt->close();

$ids = $numeric_id
    ? "(i.landlord_id = '$landlord_id' OR i.landlord_id = '$numeric_id')"
    : "i.landlord_id = '$landlord_id'";

$sql = "
    SELECT
        c.room_name,
        c.room_address,
        c.tenant_name,
        c.rent_price,
        i.total,
        i.status,
        i.id AS invoice_id
    FROM invoices i
    JOIN contracts c ON c.id = i.contract_id
    WHERE $ids AND i.month = ?
    ORDER BY c.room_name ASC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $month);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

$rooms = [];
foreach ($rows as $r) {
    $rooms[] = [
        "room_name"    => $r['room_name'] ?? 'Phòng trọ',
        "room_address" => $r['room_address'] ?? '',
        "tenant_name"  => $r['tenant_name'],
        "rent_price"   => (float)$r['rent_price'],
        "total"        => (float)$r['total'],
        "paid"         => $r['status'] === 'paid' ? (float)$r['total'] : 0.0,
        "debt"         => $r['status'] !== 'paid' ? (float)$r['total'] : 0.0,
        "status"       => $r['status'],
        "invoice_id"   => (int)$r['invoice_id'],
    ];
}

$total_paid = array_sum(array_column($rooms, 'paid'));
$total_debt = array_sum(array_column($rooms, 'debt'));

echo json_encode([
    "status"     => "success",
    "month"      => $month,
    "rooms"      => $rooms,
    "total_paid" => $total_paid,
    "total_debt" => $total_debt,
], JSON_UNESCAPED_UNICODE);
$conn->close();
