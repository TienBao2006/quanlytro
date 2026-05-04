<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

// Kiểm tra bảng contracts có tồn tại không
$tableCheck = $conn->query("SHOW TABLES LIKE 'contracts'");
$tableExists = $tableCheck->num_rows > 0;

// Đếm số hợp đồng
$count = 0;
$rows = [];
if ($tableExists) {
    $result = $conn->query("SELECT id, landlord_id, tenant_id, status, created_at FROM contracts ORDER BY created_at DESC LIMIT 10");
    $count = $conn->query("SELECT COUNT(*) as c FROM contracts")->fetch_assoc()['c'];
    while ($row = $result->fetch_assoc()) {
        $rows[] = $row;
    }
}

echo json_encode([
    "table_exists" => $tableExists,
    "total_contracts" => $count,
    "latest_10" => $rows
], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
$conn->close();
