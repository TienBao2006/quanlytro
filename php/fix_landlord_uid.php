<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

// Lấy uid thật của chủ trọ từ bảng users
$email = $_GET['email'] ?? '';
if (empty($email)) {
    // Hiện tất cả users để chọn
    $result = $conn->query("SELECT uid, fullName, email, role FROM users");
    $users = [];
    while ($row = $result->fetch_assoc()) $users[] = $row;
    echo json_encode(["users" => $users], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

// Lấy uid thật
$stmt = $conn->prepare("SELECT uid FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
if (!$row) { echo json_encode(["error" => "Không tìm thấy user"]); exit; }

$real_uid = $row['uid'];

// Update contracts có landlord_id = "0" thành uid thật
$update = $conn->prepare("UPDATE contracts SET landlord_id = ? WHERE landlord_id = '0'");
$update->bind_param("s", $real_uid);
$update->execute();

echo json_encode([
    "status" => "success",
    "real_uid" => $real_uid,
    "updated_rows" => $update->affected_rows
], JSON_PRETTY_PRINT);
$conn->close();
