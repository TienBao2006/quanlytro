<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$landlord_id = trim($_GET['landlord_id'] ?? '');
if (!$landlord_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu landlord_id"]);
    exit;
}

// Lấy danh sách khách đang thuê (hợp đồng active/agreed) kèm thông tin phòng
$sql = "
    SELECT
        c.id            AS contract_id,
        c.tenant_id,
        c.tenant_name,
        c.tenant_phone,
        c.tenant_id_card,
        c.tenant_address,
        c.room_name,
        c.room_address,
        c.start_date,
        c.duration_months,
        c.rent_price,
        c.status        AS contract_status,
        u.avatar,
        u.email,
        u.dob,
        (SELECT COUNT(*) FROM invoices i WHERE i.contract_id = c.id AND i.status = 'unpaid') AS unpaid_count,
        (SELECT COUNT(*) FROM invoices i WHERE i.contract_id = c.id AND i.status = 'paid')   AS paid_count,
        (SELECT COUNT(*) FROM room_members rm WHERE rm.contract_id = c.id)                   AS member_count
    FROM contracts c
    LEFT JOIN users u ON u.uid = c.tenant_id
    WHERE c.landlord_id = ?
      AND c.status IN ('active','agreed')
    ORDER BY c.created_at DESC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $landlord_id);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

// Ép kiểu số
foreach ($rows as &$r) {
    $r['contract_id']    = intval($r['contract_id']);
    $r['duration_months']= intval($r['duration_months']);
    $r['rent_price']     = floatval($r['rent_price']);
    $r['unpaid_count']   = intval($r['unpaid_count']);
    $r['paid_count']     = intval($r['paid_count']);
    $r['member_count']   = intval($r['member_count']);
}

echo json_encode(["status" => "success", "tenants" => $rows], JSON_UNESCAPED_UNICODE);
$conn->close();
