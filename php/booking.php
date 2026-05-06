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
    $booking_id = $conn->insert_id;

    // Thông báo cho chủ trọ khi có khách đặt phòng
    $post_res = $conn->query("SELECT title, user_id, contact_phone FROM posts WHERE id = $post_id");
    if ($post_res && $prow = $post_res->fetch_assoc()) {
        $post_title   = $conn->real_escape_string($prow['title']);
        $landlord_uid = $prow['user_id'];
        // Nếu user_id là numeric, tìm uid thật
        if (is_numeric($landlord_uid)) {
            $ur = $conn->query("SELECT uid FROM users WHERE id = $landlord_uid LIMIT 1");
            if ($ur && $urow = $ur->fetch_assoc()) $landlord_uid = $urow['uid'];
        }
        // Fallback: tìm qua contact_phone
        if (!$landlord_uid && $prow['contact_phone']) {
            $cp = $conn->real_escape_string($prow['contact_phone']);
            $ur = $conn->query("SELECT uid FROM users WHERE phone = '$cp' LIMIT 1");
            if ($ur && $urow = $ur->fetch_assoc()) $landlord_uid = $urow['uid'];
        }
        $fname = $conn->real_escape_string($full_name);
        $ntitle = "🏠 Yêu cầu đặt phòng mới";
        $nmsg   = "$fname đã gửi yêu cầu đặt phòng \"$post_title\". Vui lòng vào mục Đặt phòng để xác nhận.";
        if ($landlord_uid) {
            $conn->query("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES ('$landlord_uid', '$ntitle', '$nmsg', 'booking_new', $booking_id, 0, NOW())");
        }
    }

    echo json_encode(["status" => "success", "message" => "Đặt phòng thành công", "booking_id" => $booking_id], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode(["status" => "error", "message" => "Lỗi lưu dữ liệu"]);
}

$stmt->close();
$conn->close();
