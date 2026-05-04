<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$landlord_id = trim($_GET["landlord_id"] ?? "");
$tenant_id   = trim($_GET["tenant_id"] ?? "");
$contract_id = intval($_GET["contract_id"] ?? 0);

if (!$landlord_id && !$tenant_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu tham số"]);
    exit;
}

if ($landlord_id) {
    $where = "i.landlord_id = ?";
    $param = $landlord_id;
} else {
    $where = "i.tenant_id = ?";
    $param = $tenant_id;
}

$extra = $contract_id ? " AND i.contract_id = $contract_id" : "";

$sql = "SELECT i.*, c.room_name, c.room_address
        FROM invoices i
        JOIN contracts c ON c.id = i.contract_id
        WHERE $where $extra
        ORDER BY i.month DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $param);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

echo json_encode(["status" => "success", "invoices" => $rows]);
