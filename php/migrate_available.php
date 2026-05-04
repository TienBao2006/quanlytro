<?php
// Chạy file này 1 lần để thêm cột available và available_rooms vào bảng posts
include 'db_config.php';

$queries = [
    "ALTER TABLE posts ADD COLUMN IF NOT EXISTS available TINYINT(1) NOT NULL DEFAULT 1",
    "ALTER TABLE posts ADD COLUMN IF NOT EXISTS available_rooms INT NOT NULL DEFAULT 1"
];

foreach ($queries as $sql) {
    if ($conn->query($sql)) {
        echo "OK: $sql<br>";
    } else {
        echo "Lỗi: " . $conn->error . "<br>";
    }
}

$conn->close();
echo "Hoàn tất migration!";
