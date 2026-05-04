<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối thất bại"]);
    exit;
}

$landlord_id = $_POST["landlord_id"] ?? "";
$tenant_id   = $_POST["tenant_id"]   ?? "";

if (empty($landlord_id) || empty($tenant_id)) {
    echo json_encode(["status" => "error", "message" => "Thiếu dữ liệu"]);
    exit;
}

// chat_id cố định: uid nhỏ hơn đứng trước
$chat_id = ($landlord_id < $tenant_id)
    ? $landlord_id . "_" . $tenant_id
    : $tenant_id . "_" . $landlord_id;

// Kiểm tra đã có tin nhắn nào chưa
$check = $conn->prepare("SELECT COUNT(*) as cnt FROM messages WHERE chat_id = ?");
$check->bind_param("s", $chat_id);
$check->execute();
$result = $check->get_result()->fetch_assoc();
$check->close();

if ($result["cnt"] > 0) {
    echo json_encode(["status" => "skipped", "message" => "Đã có tin nhắn"]);
    $conn->close();
    exit;
}

// Lấy tên chủ trọ
$nameStmt = $conn->prepare("SELECT fullName FROM users WHERE uid = ?");
$nameStmt->bind_param("s", $landlord_id);
$nameStmt->execute();
$nameRow = $nameStmt->get_result()->fetch_assoc();
$nameStmt->close();
$landlordName = $nameRow["fullName"] ?? "Chủ trọ";

$welcome = "Xin chào! Tôi là $landlordName. Cảm ơn bạn đã quan tâm đến phòng trọ. Bạn cần tôi tư vấn thêm thông tin gì không? 😊";

$stmt = $conn->prepare(
    "INSERT INTO messages (chat_id, sender_id, receiver_id, message) VALUES (?, ?, ?, ?)"
);
$stmt->bind_param("ssss", $chat_id, $landlord_id, $tenant_id, $welcome);

if ($stmt->execute()) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error", "message" => $stmt->error]);
}

$stmt->close();
$conn->close();
