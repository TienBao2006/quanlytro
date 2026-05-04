<?php
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';

$contract_id = intval($_GET['contract_id'] ?? 0);
if (!$contract_id) {
    echo json_encode(["error" => "Cần truyền ?contract_id=X"]);
    exit;
}

$result = [];

// 1. Thông tin contract
$c = $conn->query("SELECT id, status, booking_id, post_id FROM contracts WHERE id = $contract_id");
$contract = $c ? $c->fetch_assoc() : null;
$result['contract'] = $contract;

if ($contract) {
    $booking_id = (int)$contract['booking_id'];
    $post_id    = (int)$contract['post_id'];

    // 2. Thông tin booking
    $b = $conn->query("SELECT id, status, post_id FROM bookings WHERE id = $booking_id");
    $result['booking'] = $b ? $b->fetch_assoc() : null;

    // 3. Thông tin post
    $p = $conn->query("SELECT id, available, available_rooms, total_rooms FROM posts WHERE id = $post_id");
    $result['post'] = $p ? $p->fetch_assoc() : null;

    // 4. Tất cả booking confirmed của post này
    $bc = $conn->query("SELECT id, status FROM bookings WHERE post_id = $post_id");
    $result['all_bookings_of_post'] = [];
    while ($row = $bc->fetch_assoc()) {
        $result['all_bookings_of_post'][] = $row;
    }
}

echo json_encode($result, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
$conn->close();
