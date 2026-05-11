<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$invoice_id     = intval($_POST["invoice_id"] ?? 0);
$status         = trim($_POST["status"] ?? "");        // "paid" | "unpaid"
$user_id        = trim($_POST["user_id"] ?? "");
$payment_method = trim($_POST["payment_method"] ?? ""); // "cash" | "wallet"
$txn_id         = trim($_POST["txn_id"] ?? "");

if (!$invoice_id || !in_array($status, ["paid", "unpaid"])) {
    echo json_encode(["status" => "error", "message" => "Tham số không hợp lệ"]);
    exit;
}

if ($status === "paid") {
    $paid_at = date("Y-m-d H:i:s");
    $stmt = $conn->prepare(
        "UPDATE invoices SET status=?, payment_method=?, txn_id=?, paid_at=?
         WHERE id=? AND (landlord_id=? OR tenant_id=?)"
    );
    $stmt->bind_param("ssssiss", $status, $payment_method, $txn_id, $paid_at, $invoice_id, $user_id, $user_id);
    $stmt->execute();

    if ($stmt->affected_rows > 0) {
        $stmt->close();

        // Lấy thông tin hóa đơn để gửi thông báo cho chủ trọ
        $q = $conn->prepare(
            "SELECT i.landlord_id, i.tenant_id, i.month, i.total, c.room_name,
                    u.fullName AS tenant_name
             FROM invoices i
             JOIN contracts c ON c.id = i.contract_id
             LEFT JOIN users u ON u.uid = i.tenant_id
             WHERE i.id = ?"
        );
        $q->bind_param("i", $invoice_id);
        $q->execute();
        $row = $q->get_result()->fetch_assoc();
        $q->close();

        if ($row) {
            $method_label = ($payment_method === "cash") ? "Tiền mặt" : "Ví điện tử";
            $room         = $row["room_name"] ?? "Phòng trọ";
            $tenant_name  = $row["tenant_name"] ?? "Người thuê";
            $month_fmt    = str_replace("-", "/", $row["month"]);
            $total_fmt    = number_format($row["total"], 0, ".", ",");
            $paid_fmt     = date("d/m/Y H:i", strtotime($paid_at));

            $is_tenant_paying = ($user_id === $row["tenant_id"]);

            if ($is_tenant_paying) {
                // Khách thanh toán → thông báo cho chủ trọ
                $title   = "💰 Thanh toán hóa đơn tháng $month_fmt";
                $message = "$tenant_name đã thanh toán hóa đơn tháng $month_fmt\n"
                         . "Phòng: $room\n"
                         . "Số tiền: {$total_fmt} VND\n"
                         . "Hình thức: $method_label\n"
                         . "Mã GD: $txn_id\n"
                         . "Thời gian: $paid_fmt";
                $notify_user = $row["landlord_id"];
            } else {
                // Chủ trọ xác nhận → thông báo cho khách thuê
                $title   = "✅ Hóa đơn tháng $month_fmt đã được xác nhận";
                $message = "Hóa đơn tháng $month_fmt của bạn đã được chủ trọ xác nhận thanh toán.\n"
                         . "Phòng: $room\n"
                         . "Số tiền: {$total_fmt} VND\n"
                         . "Hình thức: $method_label\n"
                         . "Thời gian: $paid_fmt";
                $notify_user = $row["tenant_id"];
            }

            $ins = $conn->prepare(
                "INSERT INTO notifications (user_id, title, message, type) VALUES (?, ?, ?, 'payment')"
            );
            $ins->bind_param("sss", $notify_user, $title, $message);
            $ins->execute();
            $ins->close();
        }

        echo json_encode(["status" => "success"]);
    } else {
        $stmt->close();
        echo json_encode(["status" => "error", "message" => "Không tìm thấy hóa đơn"]);
    }
} else {
    // Đánh dấu lại chưa thanh toán → xóa thông tin giao dịch
    $stmt = $conn->prepare(
        "UPDATE invoices SET status=?, payment_method=NULL, txn_id=NULL, paid_at=NULL
         WHERE id=? AND (landlord_id=? OR tenant_id=?)"
    );
    $stmt->bind_param("siss", $status, $invoice_id, $user_id, $user_id);
    $stmt->execute();

    if ($stmt->affected_rows > 0) {
        echo json_encode(["status" => "success"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Không tìm thấy hóa đơn"]);
    }
    $stmt->close();
}
