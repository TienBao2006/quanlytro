<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["error" => "DB connect failed: " . $conn->connect_error]);
    exit;
}

// Kiểm tra bảng posts tồn tại không
$tables = [];
$r = $conn->query("SHOW TABLES");
while ($row = $r->fetch_row()) $tables[] = $row[0];

// Lấy cấu trúc bảng posts
$columns = [];
$r2 = $conn->query("SHOW COLUMNS FROM posts");
if ($r2) {
    while ($row = $r2->fetch_assoc()) $columns[] = $row['Field'];
}

// Đếm số bài
$count = 0;
$r3 = $conn->query("SELECT COUNT(*) as cnt FROM posts");
if ($r3) $count = $r3->fetch_assoc()['cnt'];

// Lấy 1 bài mẫu (raw)
$sample = null;
$r4 = $conn->query("SELECT * FROM posts LIMIT 1");
if ($r4 && $r4->num_rows > 0) {
    $sample = $r4->fetch_assoc();
    // Không decode images, giữ nguyên để xem format
}

echo json_encode([
    "tables"  => $tables,
    "columns" => $columns,
    "count"   => $count,
    "sample"  => $sample
], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);

$conn->close();
