<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode([]);
    exit;
}
$conn->set_charset("utf8mb4");

$landlord_id = $_GET['landlord_id'] ?? '';
if (empty($landlord_id)) {
    echo json_encode([]);
    exit;
}

// Lấy tất cả booking của các bài đăng thuộc chủ trọ này
$sql = "SELECT b.id, b.post_id, b.user_id, b.full_name, b.phone, b.id_card, 
               b.dob, b.id_issue_date, b.email, b.address, b.status, b.created_at,
               p.title as post_title, p.price as post_price
        FROM bookings b
        JOIN posts p ON b.post_id = p.id
        WHERE p.user_id = ?
        ORDER BY 
            CASE b.status 
                WHEN 'pending' THEN 1 
                WHEN 'confirmed' THEN 2 
                ELSE 3 
            END,
            b.created_at DESC";

$stmt = $conn->prepare($sql);
$stmt->bind_param("s", $landlord_id);
$stmt->execute();
$result = $stmt->get_result();

$bookings = [];
while ($row = $result->fetch_assoc()) {
    $bookings[] = $row;
}

echo json_encode($bookings, JSON_UNESCAPED_UNICODE);
$stmt->close();
$conn->close();
