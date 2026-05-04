<?php
$servername = "localhost";
$username = "root"; // Mặc định của XAMPP là root
$password = "";     // Mặc định của XAMPP là để trống
$dbname = "quan_ly_tro"; // THAY ĐỔI: Tên database của bạn là gì thì điền vào đây

// Tạo kết nối
$conn = new mysqli($servername, $username, $password, $dbname);

// Kiểm tra kết nối
if ($conn->connect_error) {
    die("Kết nối thất bại: " . $conn->connect_error);
}

// Thiết lập font tiếng Việt
$conn->set_charset("utf8mb4");
