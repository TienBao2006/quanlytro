<?php
header('Content-Type: text/html; charset=utf-8');
include 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $user_id         = isset($_POST['user_id'])         ? $_POST['user_id']         : '';
    $title           = isset($_POST['title'])           ? $_POST['title']           : '';
    $location        = isset($_POST['location'])        ? $_POST['location']        : '';
    $price           = isset($_POST['price'])           ? $_POST['price']           : 0;
    $area            = isset($_POST['area'])            ? $_POST['area']            : 0;
    $description     = isset($_POST['description'])     ? $_POST['description']     : '';
    $amenities       = isset($_POST['amenities'])       ? $_POST['amenities']       : '';
    $map_url         = isset($_POST['map_url'])         ? $_POST['map_url']         : '';
    $contact_name    = isset($_POST['contact_name'])    ? $_POST['contact_name']    : '';
    $contact_phone   = isset($_POST['contact_phone'])   ? $_POST['contact_phone']   : '';
    $images          = isset($_POST['images'])          ? $_POST['images']          : '[]';
    $total_rooms     = isset($_POST['total_rooms'])     ? (int)$_POST['total_rooms']     : 1;
    $available_rooms = $total_rooms; // khi mới đăng, chưa có ai thuê
    $available       = $total_rooms > 0 ? 1 : 0;

    if (empty($title) || empty($location) || empty($price) || empty($contact_phone)) {
        echo "Vui lòng nhập đầy đủ thông tin bắt buộc";
        exit;
    }

    try {
        // Thêm cột total_rooms nếu chưa có
        $conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS total_rooms INT DEFAULT 1");

        $sql = "INSERT INTO posts (user_id, title, location, price, area, description, amenities, map_url, contact_name, contact_phone, images, available, available_rooms, total_rooms)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("sssddssssssiii",
            $user_id, $title, $location, $price, $area,
            $description, $amenities, $map_url,
            $contact_name, $contact_phone, $images,
            $available, $available_rooms, $total_rooms
        );

        if ($stmt->execute()) {
            echo "OK";
        } else {
            echo "Lỗi: " . $stmt->error;
        }
        $stmt->close();
    } catch (mysqli_sql_exception $e) {
        echo "Lỗi Database: " . $e->getMessage();
    }
} else {
    echo "Không hợp lệ";
}
$conn->close();
