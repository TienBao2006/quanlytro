<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$invoice_id  = intval($_POST["invoice_id"] ?? 0);
$landlord_id = trim($_POST["landlord_id"] ?? "");

if (!$invoice_id || !$landlord_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu tham số"]);
    exit;
}

$stmt = $conn->prepare("DELETE FROM invoices WHERE id = ? AND landlord_id = ?");
$stmt->bind_param("is", $invoice_id, $landlord_id);
if ($stmt->execute() && $stmt->affected_rows > 0) {
    echo json_encode(["status" => "success", "message" => "Đã xóa hóa đơn"]);
} else {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy hoặc không có quyền xóa"]);
}
$stmt->close();
