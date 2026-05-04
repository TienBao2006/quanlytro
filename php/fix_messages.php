<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["error" => "DB connect failed"]);
    exit;
}

$fixed = 0;
$errors = [];

// Lấy tất cả messages có sender_id hoặc receiver_id không tồn tại trong bảng users
$result = $conn->query("SELECT DISTINCT sender_id FROM messages");
$allIds = [];
while ($row = $result->fetch_assoc()) $allIds[] = $row['sender_id'];

$result2 = $conn->query("SELECT DISTINCT receiver_id FROM messages");
while ($row = $result2->fetch_assoc()) $allIds[] = $row['receiver_id'];

$allIds = array_unique($allIds);

// Với mỗi id, kiểm tra có trong users không
foreach ($allIds as $id) {
    $check = $conn->prepare("SELECT uid FROM users WHERE uid = ?");
    $check->bind_param("s", $id);
    $check->execute();
    $check->store_result();
    if ($check->num_rows > 0) {
        $check->close();
        continue; // uid hợp lệ, bỏ qua
    }
    $check->close();

    // uid không hợp lệ (Firebase UID) - tìm user tương ứng qua bảng posts
    // Tìm user_id MySQL từ bảng posts có user_id = id này
    $postCheck = $conn->prepare("SELECT p.user_id, u.uid as mysql_uid FROM posts p JOIN users u ON u.uid = p.user_id WHERE p.user_id != ? LIMIT 1");
    // Thay vào đó: tìm trong users bảng xem có uid nào match không
    // Vì Firebase UID không có trong users, ta cần map thủ công
    // Lấy tất cả users để hiển thị
}

// Hiển thị tất cả messages và users để debug
$msgs = [];
$r = $conn->query("SELECT id, sender_id, receiver_id, message FROM messages ORDER BY id DESC LIMIT 20");
while ($row = $r->fetch_assoc()) $msgs[] = $row;

$users = [];
$r2 = $conn->query("SELECT uid, fullName, phone FROM users");
while ($row = $r2->fetch_assoc()) $users[] = $row;

// Tìm Firebase UIDs không có trong users
$unknownIds = [];
foreach ($msgs as $msg) {
    foreach ([$msg['sender_id'], $msg['receiver_id']] as $id) {
        $found = false;
        foreach ($users as $u) {
            if ($u['uid'] === $id) { $found = true; break; }
        }
        if (!$found) $unknownIds[$id] = true;
    }
}

echo json_encode([
    "messages" => $msgs,
    "users" => $users,
    "unknown_ids" => array_keys($unknownIds),
    "instruction" => "Copy unknown_ids và map thủ công với users bên dưới"
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

$conn->close();
