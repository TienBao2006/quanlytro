-- Thêm cột is_read vào bảng messages
ALTER TABLE messages ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0;

-- Index để query nhanh hơn
CREATE INDEX idx_messages_receiver_read ON messages (receiver_id, is_read);
