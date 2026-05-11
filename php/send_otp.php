<?php
error_reporting(0);
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=utf-8");

$conn = new mysqli("localhost", "root", "", "quan_ly_tro");
if ($conn->connect_error) {
    echo json_encode(["status" => "error", "message" => "Kết nối DB thất bại"]);
    exit;
}
$conn->set_charset("utf8mb4");

$email = trim($_POST['email'] ?? '');
if (empty($email) || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode(["status" => "error", "message" => "Email không hợp lệ"]);
    exit;
}

// Tạo OTP 6 số
$otp = str_pad(rand(0, 999999), 6, '0', STR_PAD_LEFT);
$expires_at = date('Y-m-d H:i:s', time() + 300); // 5 phút, giờ local

// Tạo bảng nếu chưa có
$conn->query("CREATE TABLE IF NOT EXISTS otp_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(100) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at DATETIME NOT NULL,
    used TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

// Xóa OTP cũ
$conn->query("DELETE FROM otp_codes WHERE phone = '" . $conn->real_escape_string($email) . "'");

// Lưu OTP mới
$stmt = $conn->prepare("INSERT INTO otp_codes (phone, otp_code, expires_at) VALUES (?, ?, ?)");
$stmt->bind_param("sss", $email, $otp, $expires_at);
$insertOk = $stmt->execute();
$insertId = $conn->insert_id;
$stmt->close();

if (!$insertOk) {
    echo json_encode(["status" => "error", "message" => "Lưu OTP thất bại: " . $conn->error]);
    $conn->close();
    exit;
}

// Gửi email qua Gmail SMTP (không cần thư viện, dùng mail() hoặc SMTP thủ công)
$to = $email;
$subject = "=?UTF-8?B?" . base64_encode("Mã xác thực TroHub") . "?=";
$message = "Mã OTP của bạn là: $otp\nMã có hiệu lực trong 5 phút.\n\nTroHub - Ứng dụng quản lý trọ";
$headers  = "From: TroHub <trohub.otp@gmail.com>\r\n";
$headers .= "Content-Type: text/plain; charset=UTF-8\r\n";

// Dùng SMTP thủ công qua socket (không cần PHPMailer)
function sendGmail($to, $subject, $body)
{
    $smtp_host = "ssl://smtp.gmail.com";
    $smtp_port = 465;
    $smtp_user = "tranthienqnm@gmail.com"; // Email Gmail của bạn
    $smtp_pass = "vcdvshtbzehbxzrt";  // App Password 16 ký tự

    $socket = fsockopen($smtp_host, $smtp_port, $errno, $errstr, 10);
    if (!$socket) return false;

    $read = fgets($socket, 1024);

    fputs($socket, "EHLO localhost\r\n");
    $read = "";
    while (strpos($read, "250 ") === false) $read = fgets($socket, 1024);

    fputs($socket, "AUTH LOGIN\r\n");
    fgets($socket, 1024);

    fputs($socket, base64_encode($smtp_user) . "\r\n");
    fgets($socket, 1024);

    fputs($socket, base64_encode($smtp_pass) . "\r\n");
    $auth = fgets($socket, 1024);
    if (strpos($auth, "235") === false) {
        fclose($socket);
        return false;
    }

    fputs($socket, "MAIL FROM: <$smtp_user>\r\n");
    fgets($socket, 1024);

    fputs($socket, "RCPT TO: <$to>\r\n");
    fgets($socket, 1024);

    fputs($socket, "DATA\r\n");
    fgets($socket, 1024);

    $subjectEncoded = "=?UTF-8?B?" . base64_encode("Mã xác thực TroHub") . "?=";
    fputs($socket, "Subject: $subjectEncoded\r\n");
    fputs($socket, "From: TroHub <$smtp_user>\r\n");
    fputs($socket, "To: $to\r\n");
    fputs($socket, "Content-Type: text/plain; charset=UTF-8\r\n");
    fputs($socket, "\r\n");
    fputs($socket, $body . "\r\n");
    fputs($socket, ".\r\n");
    $sent = fgets($socket, 1024);

    fputs($socket, "QUIT\r\n");
    fclose($socket);

    return strpos($sent, "250") !== false;
}

$body = "Mã OTP của bạn là: $otp\nMã có hiệu lực trong 5 phút.\n\nTroHub - Ứng dụng quản lý trọ";
$sent = sendGmail($email, "Mã xác thực TroHub", $body);

if ($sent) {
    echo json_encode([
        "status" => "success",
        "message" => "OTP đã gửi đến $email"
    ], JSON_UNESCAPED_UNICODE);
} else {
    echo json_encode([
        "status" => "error",
        "message" => "Không gửi được email, kiểm tra cấu hình SMTP"
    ], JSON_UNESCAPED_UNICODE);
}

$conn->close();
