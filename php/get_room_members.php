<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$contract_id = intval($_GET['contract_id'] ?? 0);
if (!$contract_id) {
    echo json_encode(["status" => "error", "members" => []]);
    exit;
}

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

$stmt = $conn->prepare("SELECT id, full_name, phone, id_card, dob, note, created_at FROM room_members WHERE contract_id = ? ORDER BY created_at ASC");
$stmt->bind_param("i", $contract_id);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

echo json_encode(["status" => "success", "members" => $rows], JSON_UNESCAPED_UNICODE);
$conn->close();
