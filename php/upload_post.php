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
    $lat             = isset($_POST['lat'])  && $_POST['lat']  !== '' ? floatval($_POST['lat'])  : 0.0;
    $lng             = isset($_POST['lng'])  && $_POST['lng']  !== '' ? floatval($_POST['lng'])  : 0.0;
    $available_rooms = $total_rooms;
    $available       = $total_rooms > 0 ? 1 : 0;

    if (empty($title) || empty($location) || empty($price) || empty($contact_phone)) {
        echo "Vui lòng nhập đầy đủ thông tin bắt buộc";
        exit;
    }

    try {
        $conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS total_rooms INT DEFAULT 1");
        $conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS lat DOUBLE DEFAULT NULL");
        $conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS lng DOUBLE DEFAULT NULL");

        $sql = "INSERT INTO posts (user_id, title, location, price, area, description, amenities, map_url, contact_name, contact_phone, images, available, available_rooms, total_rooms, lat, lng)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("sssddssssssiiidd",
            $user_id, $title, $location, $price, $area,
            $description, $amenities, $map_url,
            $contact_name, $contact_phone, $images,
            $available, $available_rooms, $total_rooms,
            $lat, $lng
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
