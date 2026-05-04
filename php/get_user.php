<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error"]);
    exit;
}

$phone = $_GET["phone"] ?? "";
$uid   = $_GET["uid"]   ?? "";

if (!empty($uid)) {
    $stmt = $conn->prepare("SELECT uid, fullName, email, phone, address, dob, id_card, role, avatar FROM users WHERE uid = ?");
    $stmt->bind_param("s", $uid);
} elseif (!empty($phone)) {
    $stmt = $conn->prepare("SELECT uid, fullName, email, phone, address, dob, id_card, role, avatar FROM users WHERE phone = ?");
    $stmt->bind_param("s", $phone);
} else {
    echo json_encode(["status" => "error", "message" => "Thiếu tham số"]);
    exit;
}

$stmt->execute();
$result = $stmt->get_result();
$row = $result->fetch_assoc();

if ($row) {
    echo json_encode([
        "status"   => "success",
        "uid"      => $row["uid"],
        "fullName" => $row["fullName"],
        "email"    => $row["email"] ?? "",
        "phone"    => $row["phone"],
        "address"  => $row["address"] ?? "",
        "dob"      => $row["dob"] ?? "",
        "id_card"  => $row["id_card"] ?? "",
        "role"     => $row["role"] ?? "",
        "avatar"   => $row["avatar"] ?? ""
    ], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["status" => "error", "message" => "Không tìm thấy"]);
}

$stmt->close();
$conn->close();
