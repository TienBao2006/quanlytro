<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$user_id = trim($_GET["user_id"] ?? "");
if (!$user_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu user_id"]);
    exit;
}

// Tự động kiểm tra và tạo thông báo hợp đồng sắp hết hạn
checkExpiringContracts($conn, $user_id);

$stmt = $conn->prepare(
    "SELECT id, title, message, type, is_read, created_at
     FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50"
);
$stmt->bind_param("s", $user_id);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

// Đánh dấu tất cả là đã đọc
$upd = $conn->prepare("UPDATE notifications SET is_read=1 WHERE user_id=?");
$upd->bind_param("s", $user_id);
$upd->execute();
$upd->close();

echo json_encode(["status" => "success", "notifications" => $rows]);

// ── Helper: kiểm tra hợp đồng sắp hết hạn ────────────────────────────────
function checkExpiringContracts(mysqli $conn, string $userId): void {
    // Lấy hợp đồng agreed của user này (cả chủ lẫn khách)
    $stmt = $conn->prepare(
        "SELECT id, landlord_id, tenant_id, tenant_name, room_name, start_date, duration_months
         FROM contracts
         WHERE status = 'agreed' AND (landlord_id = ? OR tenant_id = ?)"
    );
    $stmt->bind_param("ss", $userId, $userId);
    $stmt->execute();
    $contracts = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    $today      = new DateTime('today');

    foreach ($contracts as $c) {
        $start   = new DateTime($c['start_date']);
        $endDate = clone $start;
        $endDate->modify("+{$c['duration_months']} months");

        if ($endDate <= $today) continue;

        $daysLeft   = (int)$today->diff($endDate)->days;
        // Chỉ thông báo khi còn <= 30 ngày
        if ($daysLeft > 30) continue;

        $endStr     = $endDate->format('d/m/Y');
        $roomLabel  = $c['room_name'] ?: "Phòng trọ";
        $contractId = (int)$c['id'];
        $isLandlord = ($c['landlord_id'] === $userId);

        if ($daysLeft <= 1) {
            $title = "⚠️ Hợp đồng hết hạn ngày mai!";
            $msg   = $isLandlord
                ? "Hợp đồng với {$c['tenant_name']} ({$roomLabel}) sẽ hết hạn vào ngày mai ({$endStr})."
                : "Hợp đồng thuê {$roomLabel} sẽ hết hạn vào ngày mai ({$endStr}). Vui lòng liên hệ chủ trọ để gia hạn hoặc chuẩn bị trả phòng.";
        } else {
            $title = "🔔 Hợp đồng sắp hết hạn ({$daysLeft} ngày)";
            $msg   = $isLandlord
                ? "Hợp đồng với {$c['tenant_name']} ({$roomLabel}) sẽ hết hạn sau {$daysLeft} ngày (ngày {$endStr})."
                : "Hợp đồng thuê {$roomLabel} sẽ hết hạn sau {$daysLeft} ngày (ngày {$endStr}). Vui lòng liên hệ chủ trọ để gia hạn nếu cần.";
        }

        // Chỉ tạo 1 lần duy nhất cho mỗi hợp đồng
        $chk = $conn->prepare(
            "SELECT id FROM notifications WHERE user_id=? AND type='contract_expiring' AND reference_id=? LIMIT 1"
        );
        $chk->bind_param("si", $userId, $contractId);
        $chk->execute();
        $exists = $chk->get_result()->num_rows > 0;
        $chk->close();

        if (!$exists) {
            $ins = $conn->prepare(
                "INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES (?,?,?,'contract_expiring',?,0,NOW())"
            );
            $ins->bind_param("sssi", $userId, $title, $msg, $contractId);
            $ins->execute();
            $ins->close();
        }
    }
}
