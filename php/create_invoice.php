<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$contract_id    = intval($_POST["contract_id"] ?? 0);
$landlord_id    = trim($_POST["landlord_id"] ?? "");
$month          = trim($_POST["month"] ?? "");          // "2025-04"
$electric_old   = floatval($_POST["electric_old"] ?? 0);
$electric_new   = floatval($_POST["electric_new"] ?? 0);
$water_old      = floatval($_POST["water_old"] ?? 0);
$water_new      = floatval($_POST["water_new"] ?? 0);
$other_fee      = floatval($_POST["other_fee"] ?? 0);
$other_fee_note = trim($_POST["other_fee_note"] ?? "");
// Giá điện/nước từ client (nếu không gửi thì lấy từ hợp đồng)
$electric_price_override = isset($_POST["electric_price"]) && $_POST["electric_price"] !== "" ? floatval($_POST["electric_price"]) : null;
$water_price_override    = isset($_POST["water_price"])    && $_POST["water_price"]    !== "" ? floatval($_POST["water_price"])    : null;

if (!$contract_id || !$landlord_id || !$month) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin bắt buộc"]);
    exit;
}

// Lấy thông tin hợp đồng (giá điện, nước, tiền phòng)
$stmt = $conn->prepare("SELECT rent_price, electric_price, water_price, tenant_id FROM contracts WHERE id=? AND landlord_id=?");
$stmt->bind_param("is", $contract_id, $landlord_id);
$stmt->execute();
$res = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$res) {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy hợp đồng"]);
    exit;
}

$electric_used  = $electric_new - $electric_old;
$water_used     = $water_new - $water_old;
$electric_price = $electric_price_override ?? $res["electric_price"];
$water_price    = $water_price_override    ?? $res["water_price"];
$electric_cost  = $electric_used * $electric_price;
$water_cost     = $water_used    * $water_price;
$total          = $res["rent_price"] + $electric_cost + $water_cost + $other_fee;

// Kiểm tra đã có hóa đơn tháng này chưa
$chk = $conn->prepare("SELECT id FROM invoices WHERE contract_id=? AND month=?");
$chk->bind_param("is", $contract_id, $month);
$chk->execute();
$chk->store_result();
if ($chk->num_rows > 0) {
    $chk->close();
    echo json_encode(["status" => "error", "message" => "Hóa đơn tháng này đã tồn tại"]);
    exit;
}
$chk->close();

$ins = $conn->prepare("INSERT INTO invoices
    (contract_id, landlord_id, tenant_id, month,
     electric_old, electric_new, electric_used, electric_price, electric_cost,
     water_old, water_new, water_used, water_price, water_cost,
     rent_price, other_fee, other_fee_note, total, status)
    VALUES (?,?,?,?, ?,?,?,?,?, ?,?,?,?,?, ?,?,?,?, 'unpaid')");

$ins->bind_param(
    "isssddddddddddddds",
    $contract_id, $landlord_id, $res["tenant_id"], $month,
    $electric_old, $electric_new, $electric_used, $electric_price, $electric_cost,
    $water_old, $water_new, $water_used, $water_price, $water_cost,
    $res["rent_price"], $other_fee, $other_fee_note, $total
);

if ($ins->execute()) {
    $invoice_id = $ins->insert_id;
    $ins->close();

    // Tạo thông báo cho người thuê
    $month_label = date("m/Y", strtotime($month . "-01"));
    $msg = "Bạn có hóa đơn tháng $month_label. Tổng tiền: " . number_format($total, 0, ',', '.') . " VND";
    $notif = $conn->prepare("INSERT INTO notifications (user_id, title, message, type) VALUES (?, 'Hóa đơn mới', ?, 'invoice')");
    $notif->bind_param("ss", $res["tenant_id"], $msg);
    $notif->execute();
    $notif->close();

    echo json_encode(["status" => "success", "invoice_id" => $invoice_id, "total" => $total]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}
