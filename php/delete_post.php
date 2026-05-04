<?php
include 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $id = $_POST['id'] ?? 0;

    if ($id > 0) {
        $sql = "DELETE FROM posts WHERE id = ?";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $id);

        if ($stmt->execute()) {
            echo "Thành công";
        } else {
            echo "Lỗi: " . $conn->error;
        }
    } else {
        echo "ID không hợp lệ";
    }
}
$conn->close();
