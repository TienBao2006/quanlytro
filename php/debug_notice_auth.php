<?php
header("Content-Type: application/json; charset=utf-8");
require_once "db_config.php";

$landlord_id = trim($_GET["landlord_id"] ?? "");
$post_id     = intval($_GET["post_id"] ?? 0);

// Xem raw data của post
$raw = $conn->prepare("SELECT id, user_id, contact_phone FROM posts WHERE id=?");
$raw->bind_param("i", $post_id);
$raw->execute();
$post_row = $raw->get_result()->fetch_assoc();
$raw->close();

// Xem uid từ users join
$join = $conn->prepare(
    "SELECT p.id, p.user_id AS post_user_id, u.uid AS users_uid, COALESCE(u.uid, p.user_id) AS resolved_uid
     FROM posts p LEFT JOIN users u ON u.phone = p.contact_phone WHERE p.id=?"
);
$join->bind_param("i", $post_id);
$join->execute();
$join_row = $join->get_result()->fetch_assoc();
$join->close();

echo json_encode([
    "landlord_id_sent" => $landlord_id,
    "post_raw"         => $post_row,
    "post_join"        => $join_row,
    "match"            => ($join_row["resolved_uid"] ?? "") === $landlord_id
]);
