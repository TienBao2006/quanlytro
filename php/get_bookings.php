<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}
$conn->set_charset("utf8mb4");

// Lấy danh sách booking theo landlord_id (chủ trọ)
$landlord_id = trim($_GET['landlord_id'] ?? '');
if (empty($landlord_id)) {
    echo json_encode(["status" => "error", "message" => "Thiếu landlord_id"]);
    exit;
}

// Join bookings với posts để lọc theo chủ trọ
$stmt = $conn->prepare("
    SELECT b.id, b.post_id, b.user_id, b.room_number, b.full_name, b.phone,
           b.id_card, b.dob, b.id_issue_date, b.start_date, b.duration, b.email, b.address,
           b.status, b.created_at,
           p.title AS post_title,
           IF(c.id IS NOT NULL, 1, 0) AS has_contract
    FROM bookings b
    JOIN posts p ON p.id = b.post_id
    LEFT JOIN users u ON u.phone = p.contact_phone
    LEFT JOIN contracts c ON c.booking_id = b.id
    WHERE COALESCE(u.uid, p.user_id) = ?
    ORDER BY b.created_at DESC
");
$stmt->bind_param("s", $landlord_id);
$stmt->execute();
$result = $stmt->get_result();

$bookings = [];
while ($row = $result->fetch_assoc()) {
    $bookings[] = $row;
}

echo json_encode(["status" => "success", "bookings" => $bookings], JSON_UNESCAPED_UNICODE);

$stmt->close();
$conn->close();
