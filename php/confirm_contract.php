<?php
ob_start();
header("Content-Type: application/json; charset=UTF-8");
include 'db_config.php';
ob_clean();

$contract_id = intval($_POST['contract_id'] ?? 0);
$tenant_id   = trim($_POST['tenant_id'] ?? '');
$action      = trim($_POST['action'] ?? ''); // "agreed" | "rejected"

if (!$contract_id || !$tenant_id || !in_array($action, ['agreed', 'rejected'])) {
    echo json_encode(["status" => "error", "message" => "Thiếu thông tin", "debug" => compact('contract_id','tenant_id','action')]);
    exit;
}

$stmt = $conn->prepare("UPDATE contracts SET status = ? WHERE id = ? AND tenant_id = ?");
$stmt->bind_param("sis", $action, $contract_id, $tenant_id);
$stmt->execute();
$affected = $stmt->affected_rows;
$stmt->close();

// Nếu không affected, kiểm tra xem contract có tồn tại không
if ($affected === 0) {
    $check = $conn->prepare("SELECT id, status, tenant_id FROM contracts WHERE id = ?");
    $check->bind_param("i", $contract_id);
    $check->execute();
    $check->bind_result($cid, $cstatus, $ctenant);
    $check->fetch();
    $check->close();
    // Nếu status đã đúng rồi thì coi như thành công
    if ($cstatus === $action) {
        echo json_encode(["status" => "success"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Không cập nhật được", "debug" => ["contract_tenant" => $ctenant, "sent_tenant" => $tenant_id, "current_status" => $cstatus]]);
    }
    $conn->close();
    exit;
}

// Update thành công
// Nếu từ chối → hủy booking và trả lại phòng trống
if ($action === 'rejected') {
    $info = $conn->query("SELECT booking_id, post_id FROM contracts WHERE id = $contract_id");
    if ($info && $row = $info->fetch_assoc()) {
        $booking_id = (int)$row['booking_id'];
        $post_id    = (int)$row['post_id'];

        $conn->query("UPDATE bookings SET status = 'cancelled' WHERE id = $booking_id");

        $cnt_res = $conn->query("SELECT COUNT(*) as cnt FROM bookings WHERE post_id = $post_id AND status = 'confirmed'");
        $confirmed_count = $cnt_res ? (int)$cnt_res->fetch_assoc()['cnt'] : 0;

        $tr_res = $conn->query("SELECT COALESCE(total_rooms, available_rooms, 1) as tr FROM posts WHERE id = $post_id");
        $total_rooms = $tr_res ? (int)$tr_res->fetch_assoc()['tr'] : 1;

        $available_rooms = max(0, $total_rooms - $confirmed_count);
        $available = $available_rooms > 0 ? 1 : 0;

        $conn->query("UPDATE posts SET available_rooms = $available_rooms, available = $available WHERE id = $post_id");
    }
}
echo json_encode(["status" => "success"]);
$conn->close();
