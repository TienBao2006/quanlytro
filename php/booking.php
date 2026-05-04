<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}

// Tạo bảng bookings nếu chưa có
$conn->query("CREATE TABLE IF NOT EXISTS bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id VARCHAR(100),
    room_number VARCHAR(50),
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    id_card VARCHAR(20) NOT NULL,
    dob VARCHAR(20) NOT NULL,
    id_issue_date VARCHAR(20) NOT NULL,
    start_date VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");
$conn->query("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS room_number VARCHAR(50) AFTER user_id");
$conn->query("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS start_date VARCHAR(20) AFTER id_issue_date");
$conn->query("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS duration VARCHAR(20) AFTER start_date");

$post_id       = $_POST['post_id']       ?? '';
$user_id       = $_POST['user_id']       ?? '';
$room_number   = $_POST['room_number']   ?? '';
$full_name     = $_POST['full_name']     ?? '';
$phone         = $_POST['phone']         ?? '';
$id_card       = $_POST['id_card']       ?? '';
$dob           = $_POST['dob']           ?? '';
$id_issue_date = $_POST['id_issue_date'] ?? '';
$start_date    = $_POST['start_date']    ?? '';
$duration      = $_POST['duration']      ?? '';
$email         = $_POST['email']         ?? '';
$address       = $_POST['address']       ?? '';

if (empty($post_id) || empty($full_name) || empty($phone) || empty($id_card) || empty($dob) || empty($id_issue_date)) {
    echo json_encode(["status" => "error", "message" => "Vui lòng điền đầy đủ thông tin bắt buộc"]);
    exit;
}

$stmt = $conn->prepare("INSERT INTO bookings (post_id, user_id, room_number, full_name, phone, id_card, dob, id_issue_date, start_date, duration, email, address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("isssssssssss", $post_id, $user_id, $room_number, $full_name, $phone, $id_card, $dob, $id_issue_date, $start_date, $duration, $email, $address);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Đặt phòng thành công", "booking_id" => $conn->insert_id], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["status" => "error", "message" => "Lỗi lưu dữ liệu"]);
}

$stmt->close();
$conn->close();
