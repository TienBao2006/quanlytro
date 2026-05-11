<?php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
include 'db_config.php';

$landlord_id = trim($_GET['landlord_id'] ?? '');
if ($landlord_id === '') {
    echo json_encode(["status" => "error", "message" => "Thiếu landlord_id"]);
    exit;
}

// Lấy numeric id để match cả 2 dạng landlord_id
$numeric_id = null;
$stmt = $conn->prepare("SELECT id FROM users WHERE uid = ? LIMIT 1");
$stmt->bind_param("s", $landlord_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
if ($row) $numeric_id = (string)$row['id'];
$stmt->close();

$ids = $numeric_id
    ? "landlord_id = '$landlord_id' OR landlord_id = '$numeric_id'"
    : "landlord_id = '$landlord_id'";

// Doanh thu từng tháng: paid + unpaid (nợ)
$sql = "
    SELECT
        month,
        SUM(CASE WHEN status = 'paid' THEN total ELSE 0 END)  AS paid,
        SUM(CASE WHEN status != 'paid' THEN total ELSE 0 END) AS debt,
        SUM(total) AS total_billed
    FROM invoices
    WHERE ($ids)
    GROUP BY month
    ORDER BY month DESC
    LIMIT 24
";
$rows = $conn->query($sql)->fetch_all(MYSQLI_ASSOC);

$current_month = date('Y-m');
$prev_month    = date('Y-m', strtotime('first day of last month'));

$current_revenue = 0;
$prev_revenue    = 0;
$monthly = [];

foreach ($rows as $r) {
    $monthly[] = [
        "month"        => $r['month'],
        "revenue"      => (float)$r['paid'],
        "paid"         => (float)$r['paid'],
        "debt"         => (float)$r['debt'],
        "total_billed" => (float)$r['total_billed'],
    ];
    if ($r['month'] === $current_month) $current_revenue = (float)$r['paid'];
    if ($r['month'] === $prev_month)    $prev_revenue    = (float)$r['paid'];
}

// Tổng theo năm
$yearly = [];
foreach ($monthly as $m) {
    $year = substr($m['month'], 0, 4);
    if (!isset($yearly[$year])) {
        $yearly[$year] = ['year' => $year, 'paid' => 0, 'debt' => 0, 'total_billed' => 0];
    }
    $yearly[$year]['paid']         += $m['paid'];
    $yearly[$year]['debt']         += $m['debt'];
    $yearly[$year]['total_billed'] += $m['total_billed'];
}
$yearly = array_values(array_reverse($yearly));

$percent_change = 0;
if ($prev_revenue > 0) {
    $percent_change = round((($current_revenue - $prev_revenue) / $prev_revenue) * 100, 1);
}

echo json_encode([
    "status"          => "success",
    "current_month"   => $current_month,
    "current_revenue" => $current_revenue,
    "prev_revenue"    => $prev_revenue,
    "percent_change"  => $percent_change,
    "monthly"         => $monthly,
    "yearly"          => $yearly,
]);
$conn->close();
