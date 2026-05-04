<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$user_id = trim($_GET["user_id"] ?? "");
if (!$user_id) {
    echo json_encode(["status" => "error", "count" => 0]);
    exit;
}

// Tạo thông báo sắp hết hạn trước khi đếm
checkExpiringContracts($conn, $user_id);

$stmt = $conn->prepare("SELECT COUNT(*) AS cnt FROM notifications WHERE user_id=? AND is_read=0");
$stmt->bind_param("s", $user_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
$stmt->close();

echo json_encode(["status" => "success", "count" => (int)($row["cnt"] ?? 0)]);

function checkExpiringContracts(mysqli $conn, string $userId): void {
    $stmt = $conn->prepare(
        "SELECT id, landlord_id, tenant_id, tenant_name, room_name, start_date, duration_months
         FROM contracts WHERE status = 'agreed' AND (landlord_id = ? OR tenant_id = ?)"
    );
    $stmt->bind_param("ss", $userId, $userId);
    $stmt->execute();
    $contracts = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
    $stmt->close();

    $today = new DateTime('today');

    foreach ($contracts as $c) {
        $endDate = (new DateTime($c['start_date']))->modify("+{$c['duration_months']} months");
        if ($endDate <= $today) continue;

        $daysLeft   = (int)$today->diff($endDate)->days;
        if ($daysLeft > 30) continue;

        $contractId = (int)$c['id'];

        // Chỉ tạo 1 lần duy nhất
        $chk = $conn->prepare("SELECT id FROM notifications WHERE user_id=? AND type='contract_expiring' AND reference_id=? LIMIT 1");
        $chk->bind_param("si", $userId, $contractId);
        $chk->execute();
        $exists = $chk->get_result()->num_rows > 0;
        $chk->close();
        if ($exists) continue;

        $endStr     = $endDate->format('d/m/Y');
        $roomLabel  = $c['room_name'] ?: "Phòng trọ";
        $isLandlord = ($c['landlord_id'] === $userId);

        $title = $daysLeft <= 1
            ? "⚠️ Hợp đồng hết hạn ngày mai!"
            : "🔔 Hợp đồng sắp hết hạn ({$daysLeft} ngày)";
        $msg = $daysLeft <= 1
            ? ($isLandlord
                ? "Hợp đồng với {$c['tenant_name']} ({$roomLabel}) sẽ hết hạn vào ngày mai ({$endStr})."
                : "Hợp đồng thuê {$roomLabel} sẽ hết hạn vào ngày mai ({$endStr}). Vui lòng liên hệ chủ trọ để gia hạn hoặc chuẩn bị trả phòng.")
            : ($isLandlord
                ? "Hợp đồng với {$c['tenant_name']} ({$roomLabel}) sẽ hết hạn sau {$daysLeft} ngày (ngày {$endStr})."
                : "Hợp đồng thuê {$roomLabel} sẽ hết hạn sau {$daysLeft} ngày (ngày {$endStr}). Vui lòng liên hệ chủ trọ để gia hạn nếu cần.");

        $ins = $conn->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id, is_read, created_at) VALUES (?,?,?,'contract_expiring',?,0,NOW())");
        $ins->bind_param("sssi", $userId, $title, $msg, $contractId);
        $ins->execute();
        $ins->close();
    }
}
