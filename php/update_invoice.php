<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$invoice_id      = intval($_POST["invoice_id"] ?? 0);
$landlord_id     = trim($_POST["landlord_id"] ?? "");
$month           = trim($_POST["month"] ?? "");
$electric_old    = floatval($_POST["electric_old"] ?? 0);
$electric_new    = floatval($_POST["electric_new"] ?? 0);
$electric_price  = floatval($_POST["electric_price"] ?? 0);
$water_old       = floatval($_POST["water_old"] ?? 0);
$water_new       = floatval($_POST["water_new"] ?? 0);
$water_price     = floatval($_POST["water_price"] ?? 0);
$other_fee       = floatval($_POST["other_fee"] ?? 0);
$other_fee_note  = trim($_POST["other_fee_note"] ?? "");

if (!$invoice_id || !$landlord_id || !$month) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin bắt buộc"]);
    exit;
}

// Lấy rent_price từ hóa đơn gốc
$stmt = $conn->prepare("SELECT rent_price FROM invoices WHERE id = ? AND landlord_id = ?");
$stmt->bind_param("is", $invoice_id, $landlord_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$row) {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy hóa đơn"]);
    exit;
}

$electric_used = $electric_new - $electric_old;
$water_used    = $water_new - $water_old;
$electric_cost = $electric_used * $electric_price;
$water_cost    = $water_used    * $water_price;
$total         = $row["rent_price"] + $electric_cost + $water_cost + $other_fee;

$upd = $conn->prepare("UPDATE invoices SET
    month = ?,
    electric_old = ?, electric_new = ?, electric_used = ?, electric_price = ?, electric_cost = ?,
    water_old = ?, water_new = ?, water_used = ?, water_price = ?, water_cost = ?,
    other_fee = ?, other_fee_note = ?, total = ?
    WHERE id = ? AND landlord_id = ?");

$upd->bind_param(
    "sdddddddddddsdis",
    $month,
    $electric_old, $electric_new, $electric_used, $electric_price, $electric_cost,
    $water_old, $water_new, $water_used, $water_price, $water_cost,
    $other_fee, $other_fee_note, $total,
    $invoice_id, $landlord_id
);

if ($upd->execute() && $upd->affected_rows >= 0) {
    echo json_encode(["status" => "success", "total" => $total]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}
$upd->close();
