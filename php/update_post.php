<?php
header('Content-Type: text/html; charset=utf-8');
include 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $id              = isset($_POST['id'])              ? $_POST['id']              : '';
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

    if (empty($id) || empty($title) || empty($location) || empty($price)) {
        echo "Thiếu thông tin cập nhật";
        exit;
    }

    try {
        // Thêm cột total_rooms nếu chưa có
        $conn->query("ALTER TABLE posts ADD COLUMN IF NOT EXISTS total_rooms INT DEFAULT 1");

        // Tính lại available_rooms = total_rooms - số booking confirmed
        $res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = '$id' AND status = 'confirmed'");
        $confirmed_count = $res ? (int)$res->fetch_assoc()['cnt'] : 0;
        $available_rooms = max(0, $total_rooms - $confirmed_count);
        $available = $available_rooms > 0 ? 1 : 0;

        $sql = "UPDATE posts SET
                title = ?, location = ?, price = ?, area = ?,
                description = ?, amenities = ?, map_url = ?,
                contact_name = ?, contact_phone = ?, images = ?,
                available = ?, available_rooms = ?, total_rooms = ?
                WHERE id = ?";

        $stmt = $conn->prepare($sql);
        $stmt->bind_param("ssddssssssiiii",
            $title, $location, $price, $area,
            $description, $amenities, $map_url,
            $contact_name, $contact_phone, $images,
            $available, $available_rooms, $total_rooms, $id
        );

        if ($stmt->execute()) {
            echo "OK";
        } else {
            echo "Lỗi thực thi: " . $stmt->error;
        }
        $stmt->close();
    } catch (mysqli_sql_exception $e) {
        echo "Lỗi Database: " . $e->getMessage();
    }
} else {
    echo "Phương thức không hợp lệ";
}
$conn->close();
