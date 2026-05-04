<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$landlord_id = trim($_GET["landlord_id"] ?? "");
$post_id     = intval($_GET["post_id"] ?? 0);

if (!$landlord_id) {
    echo json_encode(["status" => "error", "message" => "Thiếu landlord_id"]);
    exit;
}

$where = "WHERE n.landlord_id=?";
$params = [$landlord_id];
$types  = "s";

if ($post_id > 0) {
    $where .= " AND n.post_id=?";
    $params[] = $post_id;
    $types   .= "i";
}

$sql = "SELECT n.id, n.post_id, n.title, n.message, n.created_at,
               p.title AS post_title,
               (SELECT COUNT(DISTINCT tenant_id) FROM contracts
                WHERE post_id=n.post_id AND landlord_id=n.landlord_id
                  AND status IN ('agreed','active','cancel_requested','cancel_requested_by_tenant')) AS tenant_count
        FROM landlord_notices n
        LEFT JOIN posts p ON p.id = n.post_id
        $where
        ORDER BY n.created_at DESC LIMIT 50";

$stmt = $conn->prepare($sql);
$stmt->bind_param($types, ...$params);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);
$stmt->close();

echo json_encode(["status" => "success", "notices" => $rows]);
