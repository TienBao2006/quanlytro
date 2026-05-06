<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$member_id   = intval($_POST['member_id']   ?? 0);
$landlord_id = trim($_POST['landlord_id']   ?? '');

if (!$member_id || !$landlord_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin"]);
    exit;
}

$stmt = $conn->prepare("DELETE FROM room_members WHERE id = ? AND landlord_id = ?");
$stmt->bind_param("is", $member_id, $landlord_id);
$stmt->execute();

echo json_encode(["status" => "success"]);
$stmt->close();
$conn->close();
