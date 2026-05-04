<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["error" => "DB connect failed"]);
    exit;
}

$results = [];

// Lấy tất cả distinct IDs trong messages không có trong users
$r = $conn->query("
    SELECT DISTINCT id_col FROM (
        SELECT sender_id AS id_col FROM messages
        UNION
        SELECT receiver_id AS id_col FROM messages
    ) all_ids
    WHERE id_col NOT IN (SELECT uid FROM users)
");

$unknownIds = [];
while ($row = $r->fetch_assoc()) {
    $unknownIds[] = $row['id_col'];
}

// Với mỗi unknown ID, tìm user tương ứng qua bảng posts
foreach ($unknownIds as $firebaseUid) {
    // Tìm trong posts xem firebase uid này thuộc về ai
    $postStmt = $conn->prepare("SELECT contact_phone FROM posts WHERE user_id = ? LIMIT 1");
    $postStmt->bind_param("s", $firebaseUid);
    $postStmt->execute();
    $postRow = $postStmt->get_result()->fetch_assoc();
    $postStmt->close();

    if ($postRow) {
        // Tìm mysql uid từ phone
        $userStmt = $conn->prepare("SELECT uid, fullName FROM users WHERE phone = ?");
        $userStmt->bind_param("s", $postRow['contact_phone']);
        $userStmt->execute();
        $userRow = $userStmt->get_result()->fetch_assoc();
        $userStmt->close();

        if ($userRow) {
            $mysqlUid = $userRow['uid'];

            // Update sender_id
            $upd1 = $conn->prepare("UPDATE messages SET sender_id = ?, chat_id = REPLACE(chat_id, ?, ?) WHERE sender_id = ?");
            $upd1->bind_param("ssss", $mysqlUid, $firebaseUid, $mysqlUid, $firebaseUid);
            $upd1->execute();
            $affected1 = $upd1->affected_rows;
            $upd1->close();

            // Update receiver_id
            $upd2 = $conn->prepare("UPDATE messages SET receiver_id = ?, chat_id = REPLACE(chat_id, ?, ?) WHERE receiver_id = ?");
            $upd2->bind_param("ssss", $mysqlUid, $firebaseUid, $mysqlUid, $firebaseUid);
            $upd2->execute();
            $affected2 = $upd2->affected_rows;
            $upd2->close();

            $results[] = [
                "firebase_uid" => $firebaseUid,
                "mapped_to" => $mysqlUid,
                "name" => $userRow['fullName'],
                "sender_rows_updated" => $affected1,
                "receiver_rows_updated" => $affected2
            ];
        } else {
            $results[] = ["firebase_uid" => $firebaseUid, "error" => "Không tìm thấy user với phone: " . $postRow['contact_phone']];
        }
    } else {
        $results[] = ["firebase_uid" => $firebaseUid, "error" => "Không tìm thấy post với user_id này"];
    }
}

if (empty($unknownIds)) {
    $results[] = ["info" => "Không có Firebase UID nào cần fix"];
}

echo json_encode(["fixed" => $results], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
$conn->close();
