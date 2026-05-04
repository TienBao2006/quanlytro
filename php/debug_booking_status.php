<?php
error_reporting(E_ALL);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
$conn->set_charset("utf8mb4");

// Lấy tất cả bookings confirmed để kiểm tra
$bookings = $conn->query("SELECT b.id, b.post_id, b.status, p.available_rooms, p.total_rooms 
    FROM bookings b 
    JOIN posts p ON p.id = b.post_id 
    WHERE b.status = 'confirmed'");

$result = [];
while ($row = $bookings->fetch_assoc()) {
    $result[] = $row;
}

// Kiểm tra update_booking_status logic thủ công với booking đầu tiên
if (!empty($result)) {
    $test = $result[0];
    $post_id = $test['post_id'];
    
    $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
    $confirmed_count = (int)$cnt_res->fetch_assoc()['cnt'];
    
    $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
    $total_rooms = (int)$tr_res->fetch_assoc()['tr'];
    
    $available_rooms = max(0, $total_rooms - $confirmed_count);
    
    echo json_encode([
        "confirmed_bookings" => $result,
        "test_post_id" => $post_id,
        "confirmed_count" => $confirmed_count,
        "total_rooms" => $total_rooms,
        "should_be_available_rooms" => $available_rooms
    ], JSON_PRETTY_PRINT);
} else {
    echo json_encode(["message" => "Không có booking confirmed nào", "all_bookings" => 
        $conn->query("SELECT id, post_id, status FROM bookings")->fetch_all(MYSQLI_ASSOC)
    ]);
}
$conn->close();
