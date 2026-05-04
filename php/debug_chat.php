<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["error" => "DB connect failed"]);
    exit;
}

$result = [];

// Xem tất cả messages
$r = $conn->query("SELECT * FROM messages ORDER BY id DESC LIMIT 20");
$result["messages"] = [];
while ($row = $r->fetch_assoc()) {
    $result["messages"][] = $row;
}

// Xem tất cả users
$r2 = $conn->query("SELECT uid, fullName, phone, role FROM users");
$result["users"] = [];
while ($row = $r2->fetch_assoc()) {
    $result["users"][] = $row;
}

echo json_encode($result, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
$conn->close();
