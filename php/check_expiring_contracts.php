<?php
/**
 * check_expiring_contracts.php
 * Kiểm tra hợp đồng sắp hết hạn và tạo thông báo tự động.
 * Gọi bằng cron job hoặc từ app khi mở màn hình thông báo.
 *
 * Thông báo được tạo khi còn 30, 15, 7, 3, 1 ngày trước khi hết hạn.
 * Tránh tạo trùng bằng cách kiểm tra reference_id + type + ngày.
 */
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

// Lấy tất cả hợp đồng đang active (agreed)
$stmt = $conn->prepare(
    "SELECT id, landlord_id, tenant_id, tenant_name, room_name, start_date, duration_months
     FROM contracts
     WHERE status = 'agreed'"
);
$stmt->execute();
$contracts = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

$today     = new DateTime('today');
$created   = 0;
$skipped   = 0;

foreach ($contracts as $c) {
    // Tính ngày kết thúc
    $start   = new DateTime($c['start_date']);
    $endDate = clone $start;
    $endDate->modify("+{$c['duration_months']} months");

    if ($endDate <= $today) continue;

    $daysLeft = (int)$today->diff($endDate)->days;
    // Chỉ thông báo khi còn <= 30 ngày
    if ($daysLeft > 30) continue;

    $endStr    = $endDate->format('d/m/Y');
    $roomLabel = $c['room_name'] ?: "Phòng trọ";

    // Nội dung thông báo
    if ($daysLeft == 1) {
        $title   = "⚠️ Hợp đồng hết hạn ngày mai!";
        $msgTenant   = "Hợp đồng thuê {$roomLabel} sẽ hết hạn vào ngày mai ({$endStr}). Vui lòng liên hệ chủ trọ để gia hạn hoặc chuẩn bị trả phòng.";
        $msgLandlord = "Hợp đồng với {$c['tenant_name']} ({$roomLabel}) sẽ hết hạn vào ngày mai ({$endStr}).";
    } else {
        $title   = "🔔 Hợp đồng sắp hết hạn ({$daysLeft} ngày)";
        $msgTenant   = "Hợp đồng thuê {$roomLabel} sẽ hết hạn sau {$daysLeft} ngày (ngày {$endStr}). Vui lòng liên hệ chủ trọ để gia hạn nếu cần.";
        $msgLandlord = "Hợp đồng với {$c['tenant_name']} ({$roomLabel}) sẽ hết hạn sau {$daysLeft} ngày (ngày {$endStr}).";
    }

    $type         = "contract_expiring";
    $contractId   = (int)$c['id'];

    // Gửi cho người thuê — chỉ 1 lần duy nhất
    if (!notifExists($conn, $c['tenant_id'], $type, $contractId)) {
        insertNotif($conn, $c['tenant_id'], $title, $msgTenant, $type, $contractId);
        $created++;
    } else {
        $skipped++;
    }

    // Gửi cho chủ trọ — chỉ 1 lần duy nhất
    if (!notifExists($conn, $c['landlord_id'], $type, $contractId)) {
        insertNotif($conn, $c['landlord_id'], $title, $msgLandlord, $type, $contractId);
        $created++;
    } else {
        $skipped++;
    }
}

echo json_encode([
    "status"  => "success",
    "created" => $created,
    "skipped" => $skipped
]);

$conn->close();

// ── Helpers ───────────────────────────────────────────────────────────────

function notifExists(mysqli $conn, string $userId, string $type, int $refId): bool {
    $stmt = $conn->prepare(
        "SELECT id FROM notifications WHERE user_id = ? AND type = ? AND reference_id = ? LIMIT 1"
    );
    $stmt->bind_param("ssi", $userId, $type, $refId);
    $stmt->execute();
    $exists = $stmt->get_result()->num_rows > 0;
    $stmt->close();
    return $exists;
}

function insertNotif(mysqli $conn, string $userId, string $title, string $message, string $type, int $refId): void {
    $stmt = $conn->prepare(
        "INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at)
         VALUES (?, ?, ?, ?, ?, 0, NOW())"
    );
    $stmt->bind_param("ssssi", $userId, $title, $message, $type, $refId);
    $stmt->execute();
    $stmt->close();
}
