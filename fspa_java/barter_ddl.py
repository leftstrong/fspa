import pymysql

HOST = "192.168.0.186"
PORT = 3306
USER = "root"
PASS = "123456"
DB = "crmeb"

conn = pymysql.connect(host=HOST, port=PORT, user=USER, password=PASS, db=DB, charset="utf8mb4")
cur = conn.cursor()

def col_exists(table, col):
    cur.execute(
        "SELECT COUNT(1) FROM information_schema.columns "
        "WHERE table_schema=%s AND table_name=%s AND column_name=%s", (DB, table, col))
    return cur.fetchone()[0] > 0

# 1) eb_barter_item.delivery_type
if not col_exists("eb_barter_item", "delivery_type"):
    cur.execute(
        "ALTER TABLE eb_barter_item ADD COLUMN delivery_type TINYINT NOT NULL DEFAULT 2 "
        "COMMENT '交付方式 1自提 2快递'")
    print("added eb_barter_item.delivery_type")
else:
    print("skip eb_barter_item.delivery_type (exists)")

# 2) eb_barter_order.apply_message / applicant_address_id
if not col_exists("eb_barter_order", "apply_message"):
    cur.execute(
        "ALTER TABLE eb_barter_order ADD COLUMN apply_message VARCHAR(500) NULL "
        "COMMENT '申请人附言'")
    print("added eb_barter_order.apply_message")
else:
    print("skip eb_barter_order.apply_message (exists)")

if not col_exists("eb_barter_order", "applicant_address_id"):
    cur.execute(
        "ALTER TABLE eb_barter_order ADD COLUMN applicant_address_id INT NULL "
        "COMMENT '申请人收货地址ID'")
    print("added eb_barter_order.applicant_address_id")
else:
    print("skip eb_barter_order.applicant_address_id (exists)")

# 3) new table eb_barter_order_message
cur.execute("""
CREATE TABLE IF NOT EXISTS eb_barter_order_message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  from_uid INT NOT NULL,
  to_uid INT NOT NULL,
  type TINYINT NOT NULL DEFAULT 1 COMMENT '1文本 2图片 3定位',
  content VARCHAR(1000) NULL,
  lat VARCHAR(32) NULL,
  lng VARCHAR(32) NULL,
  address VARCHAR(255) NULL,
  create_time DATETIME NULL,
  INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物单内聊天消息'
""")
print("ensured eb_barter_order_message")

# 4) clear legacy apply / order data
cur.execute("DELETE FROM eb_barter_apply")
cur.execute("DELETE FROM eb_barter_order")
print("cleared legacy apply/order data")

conn.commit()
cur.close()
conn.close()
print("DDL DONE")
