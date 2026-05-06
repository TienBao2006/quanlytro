<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$conn->query("CREATE TABLE IF NOT EXISTS room_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contract_id INT NOT NULL,
    landlord_id VARCHAR(100) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) DEFAULT '',
    id_card VARCHAR(50) DEFAULT '',
    dob VARCHAR(20) DEFAULT '',
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

$contract_id = intval($_POST['contract_id'] ?? 0);
$landlord_id = trim($_POST['landlord_id'] ?? '');
$full_name   = trim($_POST['full_name']   ?? '');
$phone       = trim($_POST['phone']       ?? '');
$id_card     = trim($_POST['id_card']     ?? '');
$dob         = trim($_POST['dob']         ?? '');
$note        = trim($_POST['note']        ?? '');

if (!$contract_id || !$landlord_id || !$full_name) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin bắt buộc"]);
    exit;
}

$stmt = $conn->prepare("INSERT INTO room_members (contract_id, landlord_id, full_name, phone, id_card, dob, note) VALUES (?,?,?,?,?,?,?)");
$stmt->bind_param("issssss", $contract_id, $landlord_id, $full_name, $phone, $id_card, $dob, $note);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "member_id" => $conn->insert_id], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}
$stmt->close();
$conn->close();
