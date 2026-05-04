<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$uid      = trim($_POST['uid']      ?? '');
$fullName = trim($_POST['fullName'] ?? '');
$email    = trim($_POST['email']    ?? '');
$phone    = trim($_POST['phone']    ?? '');
$address  = trim($_POST['address']  ?? '');
$dob      = trim($_POST['dob']      ?? '');
$id_card  = trim($_POST['id_card']  ?? '');
$avatar   = trim($_POST['avatar']   ?? '');  // base64

if (!$uid) {
    echo json_encode(["status" => "error", "message" => "Thiếu uid"]);
    exit;
}

if ($avatar !== '') {
    $stmt = $conn->prepare(
        "UPDATE users SET fullName=?, email=?, phone=?, address=?, dob=?, id_card=?, avatar=? WHERE uid=?"
    );
    $stmt->bind_param("ssssssss", $fullName, $email, $phone, $address, $dob, $id_card, $avatar, $uid);
} else {
    $stmt = $conn->prepare(
        "UPDATE users SET fullName=?, email=?, phone=?, address=?, dob=?, id_card=? WHERE uid=?"
    );
    $stmt->bind_param("sssssss", $fullName, $email, $phone, $address, $dob, $id_card, $uid);
}

if ($stmt->execute()) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}
$stmt->close();
$conn->close();
