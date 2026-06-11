-- =============================================
-- 电商业务场景 - 表结构定义
-- 数据库：PostgreSQL
-- 说明：涵盖商品、分类、品牌、供应商、客户、订单、订单明细、支付、库存、物流等表
--       设计为多表关联查询场景，用于AI表模型业务功能配置测试
-- =============================================

-- =============================================
-- 表名：ecom_category
-- 说明：商品分类表（支持树形结构）
-- =============================================
CREATE TABLE ecom_category
(
    id          SERIAL PRIMARY KEY,
    parent_id   INT                   DEFAULT NULL,
    name        VARCHAR(100) NOT NULL,
    level       INT2         NOT NULL DEFAULT 1,
    sort_order  INT          NOT NULL DEFAULT 0,
    icon        VARCHAR(200)          DEFAULT NULL,
    is_visible  INT2         NOT NULL DEFAULT 1,
    status      INT2         NOT NULL DEFAULT 1,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE ecom_category IS '商品分类表';
COMMENT ON COLUMN ecom_category.id IS '分类ID';
COMMENT ON COLUMN ecom_category.parent_id IS '父分类ID，NULL表示顶级分类';
COMMENT ON COLUMN ecom_category.name IS '分类名称';
COMMENT ON COLUMN ecom_category.level IS '分类层级：1-一级 2-二级 3-三级';
COMMENT ON COLUMN ecom_category.sort_order IS '排序号';
COMMENT ON COLUMN ecom_category.icon IS '分类图标URL';
COMMENT ON COLUMN ecom_category.is_visible IS '是否可见：0-隐藏 1-可见';
COMMENT ON COLUMN ecom_category.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN ecom_category.create_time IS '创建时间';
COMMENT ON COLUMN ecom_category.modify_time IS '修改时间';

CREATE INDEX idx_ecom_category_parent_id ON ecom_category (parent_id);
CREATE INDEX idx_ecom_category_level ON ecom_category (level);

-- =============================================
-- 表名：ecom_brand
-- 说明：品牌表
-- =============================================
CREATE TABLE ecom_brand
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    logo        VARCHAR(300)          DEFAULT NULL,
    description VARCHAR(500)          DEFAULT NULL,
    country     VARCHAR(50)           DEFAULT NULL,
    status      INT2         NOT NULL DEFAULT 1,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE ecom_brand IS '品牌表';
COMMENT ON COLUMN ecom_brand.id IS '品牌ID';
COMMENT ON COLUMN ecom_brand.name IS '品牌名称';
COMMENT ON COLUMN ecom_brand.logo IS '品牌Logo URL';
COMMENT ON COLUMN ecom_brand.description IS '品牌描述';
COMMENT ON COLUMN ecom_brand.country IS '品牌所属国家';
COMMENT ON COLUMN ecom_brand.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN ecom_brand.create_time IS '创建时间';
COMMENT ON COLUMN ecom_brand.modify_time IS '修改时间';

CREATE INDEX idx_ecom_brand_country ON ecom_brand (country);

-- =============================================
-- 表名：ecom_supplier
-- 说明：供应商表
-- =============================================
CREATE TABLE ecom_supplier
(
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    contact_name      VARCHAR(50)           DEFAULT NULL,
    contact_phone     VARCHAR(20)           DEFAULT NULL,
    province          VARCHAR(50)           DEFAULT NULL,
    city              VARCHAR(50)           DEFAULT NULL,
    address           VARCHAR(300)          DEFAULT NULL,
    cooperation_start DATE                  DEFAULT NULL,
    status            INT2         NOT NULL DEFAULT 1,
    create_time       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_time       TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE ecom_supplier IS '供应商表';
COMMENT ON COLUMN ecom_supplier.id IS '供应商ID';
COMMENT ON COLUMN ecom_supplier.name IS '供应商名称';
COMMENT ON COLUMN ecom_supplier.contact_name IS '联系人姓名';
COMMENT ON COLUMN ecom_supplier.contact_phone IS '联系电话';
COMMENT ON COLUMN ecom_supplier.province IS '省份';
COMMENT ON COLUMN ecom_supplier.city IS '城市';
COMMENT ON COLUMN ecom_supplier.address IS '详细地址';
COMMENT ON COLUMN ecom_supplier.cooperation_start IS '合作开始日期';
COMMENT ON COLUMN ecom_supplier.status IS '状态：0-停用 1-合作中';
COMMENT ON COLUMN ecom_supplier.create_time IS '创建时间';
COMMENT ON COLUMN ecom_supplier.modify_time IS '修改时间';

CREATE INDEX idx_ecom_supplier_province ON ecom_supplier (province);
CREATE INDEX idx_ecom_supplier_city ON ecom_supplier (city);

-- =============================================
-- 表名：ecom_product
-- 说明：商品表
-- =============================================
CREATE TABLE ecom_product
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    category_id INT            NOT NULL,
    brand_id    INT                     DEFAULT NULL,
    supplier_id INT                     DEFAULT NULL,
    sku         VARCHAR(50)             DEFAULT NULL,
    price       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    cost_price  DECIMAL(12, 2)          DEFAULT 0,
    unit        VARCHAR(20)             DEFAULT '件',
    weight      DECIMAL(8, 2)           DEFAULT NULL,
    image_url   VARCHAR(500)            DEFAULT NULL,
    description TEXT                    DEFAULT NULL,
    status      INT2           NOT NULL DEFAULT 1,
    create_time TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    modify_time TIMESTAMP               DEFAULT NULL
);

COMMENT ON TABLE ecom_product IS '商品表';
COMMENT ON COLUMN ecom_product.id IS '商品ID';
COMMENT ON COLUMN ecom_product.name IS '商品名称';
COMMENT ON COLUMN ecom_product.category_id IS '分类ID，关联ecom_category.id';
COMMENT ON COLUMN ecom_product.brand_id IS '品牌ID，关联ecom_brand.id';
COMMENT ON COLUMN ecom_product.supplier_id IS '供应商ID，关联ecom_supplier.id';
COMMENT ON COLUMN ecom_product.sku IS 'SKU编码';
COMMENT ON COLUMN ecom_product.price IS '销售价格';
COMMENT ON COLUMN ecom_product.cost_price IS '成本价格';
COMMENT ON COLUMN ecom_product.unit IS '计量单位';
COMMENT ON COLUMN ecom_product.weight IS '重量(kg)';
COMMENT ON COLUMN ecom_product.image_url IS '商品主图URL';
COMMENT ON COLUMN ecom_product.description IS '商品描述';
COMMENT ON COLUMN ecom_product.status IS '状态：0-下架 1-上架 2-预售';
COMMENT ON COLUMN ecom_product.create_time IS '创建时间';
COMMENT ON COLUMN ecom_product.modify_time IS '修改时间';

CREATE INDEX idx_ecom_product_category_id ON ecom_product (category_id);
CREATE INDEX idx_ecom_product_brand_id ON ecom_product (brand_id);
CREATE INDEX idx_ecom_product_supplier_id ON ecom_product (supplier_id);
CREATE INDEX idx_ecom_product_sku ON ecom_product (sku);
CREATE INDEX idx_ecom_product_status ON ecom_product (status);

ALTER TABLE ecom_product
    ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES ecom_category (id);
ALTER TABLE ecom_product
    ADD CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES ecom_brand (id);
ALTER TABLE ecom_product
    ADD CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES ecom_supplier (id);

-- =============================================
-- 表名：ecom_customer
-- 说明：客户表
-- =============================================
CREATE TABLE ecom_customer
(
    id            SERIAL PRIMARY KEY,
    name          VARCHAR(50)   DEFAULT NULL,
    phone         VARCHAR(20)   DEFAULT NULL,
    email         VARCHAR(100)  DEFAULT NULL,
    gender        INT2          DEFAULT NULL,
    birthday      DATE          DEFAULT NULL,
    province      VARCHAR(50)   DEFAULT NULL,
    city          VARCHAR(50)   DEFAULT NULL,
    address       VARCHAR(300)  DEFAULT NULL,
    member_level  INT2 NOT NULL DEFAULT 0,
    register_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP     DEFAULT NULL,
    status        INT2 NOT NULL DEFAULT 1
);

COMMENT ON TABLE ecom_customer IS '客户表';
COMMENT ON COLUMN ecom_customer.id IS '客户ID';
COMMENT ON COLUMN ecom_customer.name IS '客户姓名';
COMMENT ON COLUMN ecom_customer.phone IS '手机号';
COMMENT ON COLUMN ecom_customer.email IS '邮箱';
COMMENT ON COLUMN ecom_customer.gender IS '性别：0-未知 1-男 2-女';
COMMENT ON COLUMN ecom_customer.birthday IS '出生日期';
COMMENT ON COLUMN ecom_customer.province IS '省份';
COMMENT ON COLUMN ecom_customer.city IS '城市';
COMMENT ON COLUMN ecom_customer.address IS '详细地址';
COMMENT ON COLUMN ecom_customer.member_level IS '会员等级：0-普通 1-银卡 2-金卡 3-钻石';
COMMENT ON COLUMN ecom_customer.register_time IS '注册时间';
COMMENT ON COLUMN ecom_customer.last_login IS '最后登录时间';
COMMENT ON COLUMN ecom_customer.status IS '状态：0-禁用 1-正常';

CREATE INDEX idx_ecom_customer_phone ON ecom_customer (phone);
CREATE INDEX idx_ecom_customer_member_level ON ecom_customer (member_level);
CREATE INDEX idx_ecom_customer_province ON ecom_customer (province);
CREATE INDEX idx_ecom_customer_city ON ecom_customer (city);

-- =============================================
-- 表名：ecom_order
-- 说明：订单表
-- =============================================
CREATE TABLE ecom_order
(
    id              SERIAL PRIMARY KEY,
    order_no        VARCHAR(32)    NOT NULL,
    customer_id     INT            NOT NULL,
    total_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    pay_amount      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    freight_amount  DECIMAL(10, 2)          DEFAULT 0,
    order_status    INT2           NOT NULL DEFAULT 0,
    pay_status      INT2           NOT NULL DEFAULT 0,
    pay_time        TIMESTAMP               DEFAULT NULL,
    order_source    INT2                    DEFAULT 0,
    remark          VARCHAR(500)            DEFAULT NULL,
    province        VARCHAR(50)             DEFAULT NULL,
    city            VARCHAR(50)             DEFAULT NULL,
    receiver_name   VARCHAR(50)             DEFAULT NULL,
    receiver_phone  VARCHAR(20)             DEFAULT NULL,
    receiver_addr   VARCHAR(300)            DEFAULT NULL,
    create_time     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    modify_time     TIMESTAMP               DEFAULT NULL
);

COMMENT ON TABLE ecom_order IS '订单表';
COMMENT ON COLUMN ecom_order.id IS '订单ID';
COMMENT ON COLUMN ecom_order.order_no IS '订单编号';
COMMENT ON COLUMN ecom_order.customer_id IS '客户ID，关联ecom_customer.id';
COMMENT ON COLUMN ecom_order.total_amount IS '商品总金额';
COMMENT ON COLUMN ecom_order.discount_amount IS '优惠金额';
COMMENT ON COLUMN ecom_order.pay_amount IS '实付金额';
COMMENT ON COLUMN ecom_order.freight_amount IS '运费';
COMMENT ON COLUMN ecom_order.order_status IS '订单状态：0-待付款 1-待发货 2-已发货 3-已完成 4-已取消 5-退货中 6-已退货';
COMMENT ON COLUMN ecom_order.pay_status IS '支付状态：0-未支付 1-已支付 2-已退款';
COMMENT ON COLUMN ecom_order.pay_time IS '支付时间';
COMMENT ON COLUMN ecom_order.order_source IS '订单来源：0-PC 1-H5 2-小程序 3-APP';
COMMENT ON COLUMN ecom_order.remark IS '订单备注';
COMMENT ON COLUMN ecom_order.province IS '收货省份';
COMMENT ON COLUMN ecom_order.city IS '收货城市';
COMMENT ON COLUMN ecom_order.receiver_name IS '收货人姓名';
COMMENT ON COLUMN ecom_order.receiver_phone IS '收货人电话';
COMMENT ON COLUMN ecom_order.receiver_addr IS '收货地址';
COMMENT ON COLUMN ecom_order.create_time IS '创建时间';
COMMENT ON COLUMN ecom_order.modify_time IS '修改时间';

CREATE UNIQUE INDEX uk_ecom_order_order_no ON ecom_order (order_no);
CREATE INDEX idx_ecom_order_customer_id ON ecom_order (customer_id);
CREATE INDEX idx_ecom_order_order_status ON ecom_order (order_status);
CREATE INDEX idx_ecom_order_pay_status ON ecom_order (pay_status);
CREATE INDEX idx_ecom_order_create_time ON ecom_order (create_time);
CREATE INDEX idx_ecom_order_city ON ecom_order (city);

ALTER TABLE ecom_order
    ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES ecom_customer (id);

-- =============================================
-- 表名：ecom_order_item
-- 说明：订单明细表
-- =============================================
CREATE TABLE ecom_order_item
(
    id           SERIAL PRIMARY KEY,
    order_id     INT            NOT NULL,
    product_id   INT            NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    sku          VARCHAR(50)             DEFAULT NULL,
    price        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    quantity     INT            NOT NULL DEFAULT 1,
    subtotal     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    create_time  TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ecom_order_item IS '订单明细表';
COMMENT ON COLUMN ecom_order_item.id IS '明细ID';
COMMENT ON COLUMN ecom_order_item.order_id IS '订单ID，关联ecom_order.id';
COMMENT ON COLUMN ecom_order_item.product_id IS '商品ID，关联ecom_product.id';
COMMENT ON COLUMN ecom_order_item.product_name IS '商品名称（下单时快照）';
COMMENT ON COLUMN ecom_order_item.sku IS 'SKU编码（下单时快照）';
COMMENT ON COLUMN ecom_order_item.price IS '单价（下单时快照）';
COMMENT ON COLUMN ecom_order_item.quantity IS '购买数量';
COMMENT ON COLUMN ecom_order_item.subtotal IS '小计金额 = price * quantity';
COMMENT ON COLUMN ecom_order_item.create_time IS '创建时间';

CREATE INDEX idx_ecom_order_item_order_id ON ecom_order_item (order_id);
CREATE INDEX idx_ecom_order_item_product_id ON ecom_order_item (product_id);

ALTER TABLE ecom_order_item
    ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES ecom_order (id);
ALTER TABLE ecom_order_item
    ADD CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES ecom_product (id);

-- =============================================
-- 表名：ecom_payment
-- 说明：支付记录表
-- =============================================
CREATE TABLE ecom_payment
(
    id            SERIAL PRIMARY KEY,
    order_id      INT            NOT NULL,
    payment_no    VARCHAR(64)             DEFAULT NULL,
    pay_channel   INT2           NOT NULL DEFAULT 0,
    pay_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    pay_status    INT2           NOT NULL DEFAULT 0,
    pay_time      TIMESTAMP               DEFAULT NULL,
    refund_amount DECIMAL(12, 2)          DEFAULT 0,
    refund_time   TIMESTAMP               DEFAULT NULL,
    create_time   TIMESTAMP               DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ecom_payment IS '支付记录表';
COMMENT ON COLUMN ecom_payment.id IS '支付ID';
COMMENT ON COLUMN ecom_payment.order_id IS '订单ID，关联ecom_order.id';
COMMENT ON COLUMN ecom_payment.payment_no IS '第三方支付流水号';
COMMENT ON COLUMN ecom_payment.pay_channel IS '支付渠道：0-微信 1-支付宝 2-银行卡 3-余额';
COMMENT ON COLUMN ecom_payment.pay_amount IS '支付金额';
COMMENT ON COLUMN ecom_payment.pay_status IS '支付状态：0-待支付 1-支付成功 2-支付失败 3-已退款';
COMMENT ON COLUMN ecom_payment.pay_time IS '支付完成时间';
COMMENT ON COLUMN ecom_payment.refund_amount IS '退款金额';
COMMENT ON COLUMN ecom_payment.refund_time IS '退款时间';
COMMENT ON COLUMN ecom_payment.create_time IS '创建时间';

CREATE INDEX idx_ecom_payment_order_id ON ecom_payment (order_id);
CREATE INDEX idx_ecom_payment_pay_channel ON ecom_payment (pay_channel);
CREATE INDEX idx_ecom_payment_pay_status ON ecom_payment (pay_status);
CREATE INDEX idx_ecom_payment_pay_time ON ecom_payment (pay_time);

ALTER TABLE ecom_payment
    ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES ecom_order (id);

-- =============================================
-- 表名：ecom_inventory
-- 说明：库存表
-- =============================================
CREATE TABLE ecom_inventory
(
    id             SERIAL PRIMARY KEY,
    product_id     INT         NOT NULL,
    warehouse_code VARCHAR(20) NOT NULL DEFAULT 'WH01',
    available_qty  INT         NOT NULL DEFAULT 0,
    locked_qty     INT         NOT NULL DEFAULT 0,
    total_qty      INT         NOT NULL DEFAULT 0,
    safety_qty     INT                  DEFAULT 10,
    last_in_time   TIMESTAMP            DEFAULT NULL,
    last_out_time  TIMESTAMP            DEFAULT NULL,
    create_time    TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_time    TIMESTAMP            DEFAULT NULL
);

COMMENT ON TABLE ecom_inventory IS '库存表';
COMMENT ON COLUMN ecom_inventory.id IS '库存ID';
COMMENT ON COLUMN ecom_inventory.product_id IS '商品ID，关联ecom_product.id';
COMMENT ON COLUMN ecom_inventory.warehouse_code IS '仓库编码';
COMMENT ON COLUMN ecom_inventory.available_qty IS '可用库存';
COMMENT ON COLUMN ecom_inventory.locked_qty IS '锁定库存（已下单未发货）';
COMMENT ON COLUMN ecom_inventory.total_qty IS '总库存 = available_qty + locked_qty';
COMMENT ON COLUMN ecom_inventory.safety_qty IS '安全库存阈值';
COMMENT ON COLUMN ecom_inventory.last_in_time IS '最后入库时间';
COMMENT ON COLUMN ecom_inventory.last_out_time IS '最后出库时间';
COMMENT ON COLUMN ecom_inventory.create_time IS '创建时间';
COMMENT ON COLUMN ecom_inventory.modify_time IS '修改时间';

CREATE UNIQUE INDEX uk_ecom_inventory_product_warehouse ON ecom_inventory (product_id, warehouse_code);
CREATE INDEX idx_ecom_inventory_product_id ON ecom_inventory (product_id);
CREATE INDEX idx_ecom_inventory_warehouse ON ecom_inventory (warehouse_code);

ALTER TABLE ecom_inventory
    ADD CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES ecom_product (id);

-- =============================================
-- 表名：ecom_logistics
-- 说明：物流表
-- =============================================
CREATE TABLE ecom_logistics
(
    id               SERIAL PRIMARY KEY,
    order_id         INT  NOT NULL,
    logistics_no     VARCHAR(64)   DEFAULT NULL,
    carrier          VARCHAR(50)   DEFAULT NULL,
    ship_time        TIMESTAMP     DEFAULT NULL,
    receive_time     TIMESTAMP     DEFAULT NULL,
    logistics_status INT2 NOT NULL DEFAULT 0,
    receiver_name    VARCHAR(50)   DEFAULT NULL,
    receiver_phone   VARCHAR(20)   DEFAULT NULL,
    receiver_addr    VARCHAR(300)  DEFAULT NULL,
    create_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    modify_time      TIMESTAMP     DEFAULT NULL
);

COMMENT ON TABLE ecom_logistics IS '物流表';
COMMENT ON COLUMN ecom_logistics.id IS '物流ID';
COMMENT ON COLUMN ecom_logistics.order_id IS '订单ID，关联ecom_order.id';
COMMENT ON COLUMN ecom_logistics.logistics_no IS '物流单号';
COMMENT ON COLUMN ecom_logistics.carrier IS '承运商：顺丰/中通/圆通/韵达/申通';
COMMENT ON COLUMN ecom_logistics.ship_time IS '发货时间';
COMMENT ON COLUMN ecom_logistics.receive_time IS '签收时间';
COMMENT ON COLUMN ecom_logistics.logistics_status IS '物流状态：0-待发货 1-已发货 2-运输中 3-派送中 4-已签收 5-异常';
COMMENT ON COLUMN ecom_logistics.receiver_name IS '收货人姓名';
COMMENT ON COLUMN ecom_logistics.receiver_phone IS '收货人电话';
COMMENT ON COLUMN ecom_logistics.receiver_addr IS '收货地址';
COMMENT ON COLUMN ecom_logistics.create_time IS '创建时间';
COMMENT ON COLUMN ecom_logistics.modify_time IS '修改时间';

CREATE INDEX idx_ecom_logistics_order_id ON ecom_logistics (order_id);
CREATE INDEX idx_ecom_logistics_carrier ON ecom_logistics (carrier);
CREATE INDEX idx_ecom_logistics_status ON ecom_logistics (logistics_status);

ALTER TABLE ecom_logistics
    ADD CONSTRAINT fk_logistics_order FOREIGN KEY (order_id) REFERENCES ecom_order (id);

-- =============================================
-- 表名：ecom_product_tag
-- 说明：商品标签表
-- =============================================
CREATE TABLE ecom_product_tag
(
    id          SERIAL PRIMARY KEY,
    product_id  INT         NOT NULL,
    tag_name    VARCHAR(50) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ecom_product_tag IS '商品标签表';
COMMENT ON COLUMN ecom_product_tag.id IS '标签ID';
COMMENT ON COLUMN ecom_product_tag.product_id IS '商品ID，关联ecom_product.id';
COMMENT ON COLUMN ecom_product_tag.tag_name IS '标签名称，如：新品、热销、限时折扣、包邮';
COMMENT ON COLUMN ecom_product_tag.create_time IS '创建时间';

CREATE INDEX idx_ecom_product_tag_product_id ON ecom_product_tag (product_id);
CREATE INDEX idx_ecom_product_tag_tag_name ON ecom_product_tag (tag_name);

ALTER TABLE ecom_product_tag
    ADD CONSTRAINT fk_product_tag_product FOREIGN KEY (product_id) REFERENCES ecom_product (id);


-- =============================================
-- 电商业务场景 - 测试数据
-- 数据库：PostgreSQL
-- 说明：配合 ecommerce-ddl.sql 表结构使用
-- =============================================

-- =============================================
-- 商品分类
-- =============================================
INSERT INTO ecom_category (id, parent_id, name, level, sort_order, is_visible, status)
VALUES (1, NULL, '电子产品', 1, 1, 1, 1),
       (2, NULL, '服装鞋帽', 1, 2, 1, 1),
       (3, NULL, '家居生活', 1, 3, 1, 1),
       (4, NULL, '食品饮料', 1, 4, 1, 1),
       (5, NULL, '美妆个护', 1, 5, 1, 1),
       (6, 1, '手机通讯', 2, 1, 1, 1),
       (7, 1, '电脑办公', 2, 2, 1, 1),
       (8, 1, '智能穿戴', 2, 3, 1, 1),
       (9, 2, '男装', 2, 1, 1, 1),
       (10, 2, '女装', 2, 2, 1, 1),
       (11, 2, '运动鞋', 2, 3, 1, 1),
       (12, 3, '家具', 2, 1, 1, 1),
       (13, 3, '厨具', 2, 2, 1, 1),
       (14, 3, '家纺', 2, 3, 1, 1),
       (15, 4, '零食', 2, 1, 1, 1),
       (16, 4, '酒水', 2, 2, 1, 1),
       (17, 5, '护肤', 2, 1, 1, 1),
       (18, 5, '彩妆', 2, 2, 1, 1),
       (19, 6, '智能手机', 3, 1, 1, 1),
       (20, 6, '功能手机', 3, 2, 1, 1),
       (21, 7, '笔记本电脑', 3, 1, 1, 1),
       (22, 7, '台式机', 3, 2, 1, 1),
       (23, 8, '智能手表', 3, 1, 1, 1),
       (24, 8, '智能手环', 3, 2, 1, 1);

-- 重置序列
SELECT setval('ecom_category_id_seq', (SELECT MAX(id) FROM ecom_category));

-- =============================================
-- 品牌
-- =============================================
INSERT INTO ecom_brand (id, name, country, description, status)
VALUES (1, '华为', '中国', '全球领先的ICT基础设施和智能终端提供商', 1),
       (2, '苹果', '美国', '全球知名消费电子与软件公司', 1),
       (3, '小米', '中国', '以智能手机、智能硬件和IoT平台为核心的互联网公司', 1),
       (4, '联想', '中国', '全球PC领导企业', 1),
       (5, '戴尔', '美国', '全球知名IT产品和服务提供商', 1),
       (6, '耐克', '美国', '全球著名体育运动品牌', 1),
       (7, '阿迪达斯', '德国', '全球知名运动用品品牌', 1),
       (8, '优衣库', '日本', '全球知名休闲服饰品牌', 1),
       (9, '宜家', '瑞典', '全球知名家具和家居零售商', 1),
       (10, '茅台', '中国', '中国高端白酒品牌', 1),
       (11, '兰蔻', '法国', '全球高端美妆品牌', 1),
       (12, '雅诗兰黛', '美国', '全球顶级护肤和彩妆品牌', 1),
       (13, '三星', '韩国', '全球知名电子品牌', 1),
       (14, '索尼', '日本', '全球知名电子娱乐品牌', 1);

SELECT setval('ecom_brand_id_seq', (SELECT MAX(id) FROM ecom_brand));

-- =============================================
-- 供应商
-- =============================================
INSERT INTO ecom_supplier (id, name, contact_name, contact_phone, province, city, address, cooperation_start, status)
VALUES (1, '深圳华科供应链有限公司', '张伟', '13800001001', '广东', '深圳', '南山区科技园路1号', '2022-01-15', 1),
       (2, '上海恒通电子科技有限公司', '李娜', '13800001002', '上海', '上海', '浦东新区张江高科技园区', '2021-06-20',
        1),
       (3, '杭州丝路服饰有限公司', '王磊', '13800001003', '浙江', '杭州', '余杭区临平新城', '2022-03-10', 1),
       (4, '广州美尚化妆品有限公司', '陈芳', '13800001004', '广东', '广州', '白云区美湾产业园', '2023-01-05', 1),
       (5, '成都味之源食品有限公司', '刘洋', '13800001005', '四川', '成都', '郫都区川菜产业园', '2022-08-18', 1),
       (6, '北京宜家家居贸易有限公司', '赵敏', '13800001006', '北京', '北京', '朝阳区望京SOHO', '2021-11-30', 1),
       (7, '苏州精工智能设备有限公司', '孙浩', '13800001007', '江苏', '苏州', '工业园区星湖街', '2023-04-12', 1),
       (8, '东莞运动科技有限公司', '周杰', '13800001008', '广东', '东莞', '松山湖高新区', '2022-07-22', 1);

SELECT setval('ecom_supplier_id_seq', (SELECT MAX(id) FROM ecom_supplier));

-- =============================================
-- 商品
-- =============================================
INSERT INTO ecom_product (id, name, category_id, brand_id, supplier_id, sku, price, cost_price, unit, weight, status)
VALUES (1, '华为Mate 60 Pro 512GB', 19, 1, 1, 'HW-M60P-512', 6999.00, 4800.00, '台', 0.225, 1),
       (2, '华为Mate 60 Pro 256GB', 19, 1, 1, 'HW-M60P-256', 5999.00, 4100.00, '台', 0.220, 1),
       (3, 'iPhone 15 Pro Max 512GB', 19, 2, 2, 'AP-15PM-512', 9999.00, 7200.00, '台', 0.221, 1),
       (4, 'iPhone 15 Pro 256GB', 19, 2, 2, 'AP-15P-256', 7999.00, 5800.00, '台', 0.187, 1),
       (5, '小米14 Ultra 512GB', 19, 3, 1, 'MI-14U-512', 5999.00, 3800.00, '台', 0.220, 1),
       (6, '三星Galaxy S24 Ultra 512GB', 19, 13, 2, 'SS-S24U-512', 8999.00, 6200.00, '台', 0.232, 1),
       (7, '联想ThinkPad X1 Carbon Gen11', 21, 4, 7, 'LN-X1C-G11', 10999.00, 7800.00, '台', 1.120, 1),
       (8, '戴尔XPS 15 9530', 21, 5, 2, 'DL-XPS15-95', 12999.00, 9200.00, '台', 1.860, 1),
       (9, '华为MateBook X Pro 2024', 21, 1, 1, 'HW-MBX-P24', 9999.00, 6800.00, '台', 1.260, 1),
       (10, '苹果MacBook Pro 14 M3 Pro', 21, 2, 2, 'AP-MBP14-M3', 14999.00, 10800.00, '台', 1.550, 1),
       (11, '华为Watch GT4 46mm', 23, 1, 1, 'HW-WGT4-46', 1488.00, 680.00, '块', 0.048, 1),
       (12, '小米手环8 Pro', 24, 3, 1, 'MI-B8P', 299.00, 120.00, '个', 0.026, 1),
       (13, 'Apple Watch Ultra 2', 23, 2, 2, 'AP-AWU2', 5999.00, 3800.00, '块', 0.061, 1),
       (14, '耐克Air Max 270 运动鞋', 11, 6, 8, 'NK-AM270', 899.00, 380.00, '双', 0.350, 1),
       (15, '阿迪达斯Ultraboost 23 跑鞋', 11, 7, 8, 'AD-UB23', 1099.00, 480.00, '双', 0.310, 1),
       (16, '优衣库男装圆领T恤', 9, 8, 3, 'UQ-MT-001', 79.00, 28.00, '件', 0.180, 1),
       (17, '优衣库女装弹力直筒牛仔裤', 10, 8, 3, 'UQ-WJ-001', 199.00, 65.00, '件', 0.450, 1),
       (18, '宜家MALM 双人床架', 12, 9, 6, 'IK-MALM-DB', 1499.00, 680.00, '件', 35.00, 1),
       (19, '宜家POÄNG 扶手椅', 12, 9, 6, 'IK-POANG', 799.00, 320.00, '件', 12.50, 1),
       (20, '宜家365+ 不粘锅套装', 13, 9, 6, 'IK-365-SET', 299.00, 95.00, '套', 2.80, 1),
       (21, '茅台飞天53度 500ml', 16, 10, 5, 'MT-FT-53', 1499.00, 880.00, '瓶', 0.950, 1),
       (22, '兰蔻小黑瓶精华液 50ml', 17, 11, 4, 'LC-GEN-50', 760.00, 280.00, '瓶', 0.080, 1),
       (23, '雅诗兰黛小棕瓶眼霜 15ml', 17, 12, 4, 'EL-AN-15', 510.00, 190.00, '瓶', 0.030, 1),
       (24, '兰蔻持妆粉底液 30ml', 18, 11, 4, 'LC-TL-30', 420.00, 150.00, '瓶', 0.035, 1),
       (25, '索尼WH-1000XM5 头戴式降噪耳机', 7, 14, 2, 'SN-WH1K5', 2499.00, 1600.00, '副', 0.250, 1),
       (26, '小米14 256GB', 19, 3, 1, 'MI-14-256', 3999.00, 2500.00, '台', 0.193, 1),
       (27, '耐克Dunk Low 休闲鞋', 11, 6, 8, 'NK-DKL', 699.00, 280.00, '双', 0.380, 1),
       (28, '优衣库轻薄羽绒服', 9, 8, 3, 'UQ-MD-002', 399.00, 140.00, '件', 0.350, 1),
       (29, '华为FreeBuds Pro 3', 7, 1, 1, 'HW-FBP3', 1199.00, 520.00, '副', 0.058, 1),
       (30, '三星Galaxy Buds2 Pro', 7, 13, 2, 'SS-GB2P', 999.00, 420.00, '副', 0.052, 1);

SELECT setval('ecom_product_id_seq', (SELECT MAX(id) FROM ecom_product));

-- =============================================
-- 商品标签
-- =============================================
INSERT INTO ecom_product_tag (product_id, tag_name)
VALUES (1, '新品'),
       (1, '热销'),
       (1, '5G'),
       (3, '热销'),
       (3, '5G'),
       (5, '新品'),
       (5, '5G'),
       (5, '限时折扣'),
       (7, '热销'),
       (7, '商务'),
       (10, '热销'),
       (10, '商务'),
       (11, '新品'),
       (11, '包邮'),
       (14, '热销'),
       (14, '包邮'),
       (15, '新品'),
       (15, '限时折扣'),
       (21, '热销'),
       (21, '限量'),
       (22, '热销'),
       (22, '包邮'),
       (25, '新品'),
       (25, '热销'),
       (26, '新品'),
       (26, '5G'),
       (26, '限时折扣'),
       (29, '新品'),
       (29, '包邮');

-- =============================================
-- 客户
-- =============================================
INSERT INTO ecom_customer (id, name, phone, email, gender, birthday, province, city, address, member_level,
                           register_time, last_login, status)
VALUES (1, '张三', '13900001001', 'zhangsan@example.com', 1, '1990-05-15', '广东', '深圳', '南山区科技路1号', 3,
        '2021-03-10 09:30:00', '2024-12-01 10:20:00', 1),
       (2, '李四', '13900001002', 'lisi@example.com', 1, '1985-11-22', '上海', '上海', '浦东新区陆家嘴环路', 2,
        '2021-06-20 14:15:00', '2024-11-28 16:45:00', 1),
       (3, '王芳', '13900001003', 'wangfang@example.com', 2, '1995-03-08', '北京', '北京', '朝阳区望京东路', 3,
        '2020-09-05 11:00:00', '2024-12-05 08:30:00', 1),
       (4, '赵敏', '13900001004', 'zhaomin@example.com', 2, '1992-07-14', '浙江', '杭州', '西湖区文三路', 2,
        '2022-01-18 16:30:00', '2024-11-30 20:10:00', 1),
       (5, '刘洋', '13900001005', 'liuyang@example.com', 1, '1988-12-01', '四川', '成都', '武侯区天府大道', 1,
        '2022-04-22 10:45:00', '2024-12-02 09:00:00', 1),
       (6, '陈静', '13900001006', 'chenjing@example.com', 2, '1998-09-20', '江苏', '南京', '鼓楼区中山北路', 1,
        '2023-02-14 13:20:00', '2024-11-25 18:30:00', 1),
       (7, '孙浩', '13900001007', 'sunhao@example.com', 1, '1993-04-30', '广东', '广州', '天河区体育西路', 2,
        '2021-08-10 08:00:00', '2024-12-03 11:15:00', 1),
       (8, '周杰', '13900001008', 'zhoujie@example.com', 1, '1991-06-18', '湖北', '武汉', '洪山区光谷大道', 0,
        '2023-07-01 15:30:00', '2024-11-20 14:00:00', 1),
       (9, '吴丽', '13900001009', 'wuli@example.com', 2, '1996-02-14', '福建', '厦门', '思明区软件园', 1,
        '2022-11-05 09:10:00', '2024-12-04 07:45:00', 1),
       (10, '郑强', '13900001010', 'zhengqiang@example.com', 1, '1987-08-25', '山东', '青岛', '市南区香港中路', 0,
        '2023-09-20 11:00:00', '2024-11-22 19:30:00', 1),
       (11, '黄婷', '13900001011', 'huangting@example.com', 2, '1999-01-10', '湖南', '长沙', '岳麓区麓谷大道', 2,
        '2021-12-01 10:00:00', '2024-12-06 12:00:00', 1),
       (12, '林峰', '13900001012', 'linfeng@example.com', 1, '1984-10-05', '辽宁', '大连', '中山区人民路', 3,
        '2020-05-15 14:20:00', '2024-12-01 16:50:00', 1),
       (13, '何雪', '13900001013', 'hexue@example.com', 2, '1994-05-28', '云南', '昆明', '盘龙区白云路', 0,
        '2023-04-10 16:00:00', '2024-11-18 10:30:00', 1),
       (14, '马超', '13900001014', 'machao@example.com', 1, '1990-03-12', '陕西', '西安', '雁塔区高新路', 1,
        '2022-06-30 09:30:00', '2024-12-05 08:15:00', 1),
       (15, '罗琳', '13900001015', 'luolin@example.com', 2, '1997-11-08', '重庆', '重庆', '渝中区解放碑', 3,
        '2021-01-20 11:45:00', '2024-12-07 13:20:00', 1);

SELECT setval('ecom_customer_id_seq', (SELECT MAX(id) FROM ecom_customer));

-- =============================================
-- 订单
-- =============================================
INSERT INTO ecom_order (id, order_no, customer_id, total_amount, discount_amount, pay_amount, freight_amount,
                        order_status, pay_status, pay_time, order_source, province, city, receiver_name, receiver_phone,
                        receiver_addr, create_time)
VALUES (1, 'ORD20240101001', 1, 6999.00, 200.00, 6799.00, 0.00, 3, 1, '2024-01-01 10:05:00', 1, '广东', '深圳', '张三',
        '13900001001', '南山区科技路1号', '2024-01-01 10:00:00'),
       (2, 'ORD20240102001', 3, 9999.00, 500.00, 9499.00, 0.00, 3, 1, '2024-01-02 14:20:00', 0, '北京', '北京', '王芳',
        '13900001003', '朝阳区望京东路', '2024-01-02 14:15:00'),
       (3, 'ORD20240105001', 2, 10999.00, 300.00, 10699.00, 0.00, 3, 1, '2024-01-05 09:30:00', 2, '上海', '上海',
        '李四', '13900001002', '浦东新区陆家嘴环路', '2024-01-05 09:25:00'),
       (4, 'ORD20240108001', 5, 5999.00, 100.00, 5899.00, 0.00, 3, 1, '2024-01-08 16:40:00', 3, '四川', '成都', '刘洋',
        '13900001005', '武侯区天府大道', '2024-01-08 16:35:00'),
       (5, 'ORD20240110001', 7, 899.00, 0.00, 899.00, 10.00, 3, 1, '2024-01-10 11:00:00', 1, '广东', '广州', '孙浩',
        '13900001007', '天河区体育西路', '2024-01-10 10:55:00'),
       (6, 'ORD20240115001', 4, 760.00, 30.00, 730.00, 0.00, 3, 1, '2024-01-15 20:10:00', 1, '浙江', '杭州', '赵敏',
        '13900001004', '西湖区文三路', '2024-01-15 20:05:00'),
       (7, 'ORD20240201001', 1, 14999.00, 800.00, 14199.00, 0.00, 3, 1, '2024-02-01 08:30:00', 0, '广东', '深圳',
        '张三', '13900001001', '南山区科技路1号', '2024-02-01 08:25:00'),
       (8, 'ORD20240205001', 12, 6999.00, 200.00, 6799.00, 0.00, 3, 1, '2024-02-05 13:00:00', 2, '辽宁', '大连', '林峰',
        '13900001012', '中山区人民路', '2024-02-05 12:55:00'),
       (9, 'ORD20240210001', 11, 1099.00, 50.00, 1049.00, 0.00, 3, 1, '2024-02-10 17:45:00', 1, '湖南', '长沙', '黄婷',
        '13900001011', '岳麓区麓谷大道', '2024-02-10 17:40:00'),
       (10, 'ORD20240214001', 15, 1499.00, 0.00, 1499.00, 0.00, 3, 1, '2024-02-14 19:30:00', 3, '重庆', '重庆', '罗琳',
        '13900001015', '渝中区解放碑', '2024-02-14 19:25:00'),
       (11, 'ORD20240301001', 6, 399.00, 0.00, 399.00, 10.00, 3, 1, '2024-03-01 10:20:00', 1, '江苏', '南京', '陈静',
        '13900001006', '鼓楼区中山北路', '2024-03-01 10:15:00'),
       (12, 'ORD20240305001', 9, 5999.00, 300.00, 5699.00, 0.00, 3, 1, '2024-03-05 14:00:00', 2, '福建', '厦门', '吴丽',
        '13900001009', '思明区软件园', '2024-03-05 13:55:00'),
       (13, 'ORD20240310001', 10, 899.00, 0.00, 899.00, 10.00, 3, 1, '2024-03-10 11:30:00', 0, '山东', '青岛', '郑强',
        '13900001010', '市南区香港中路', '2024-03-10 11:25:00'),
       (14, 'ORD20240315001', 14, 299.00, 0.00, 299.00, 10.00, 3, 1, '2024-03-15 15:45:00', 1, '陕西', '西安', '马超',
        '13900001014', '雁塔区高新路', '2024-03-15 15:40:00'),
       (15, 'ORD20240320001', 8, 2499.00, 100.00, 2399.00, 0.00, 3, 1, '2024-03-20 09:15:00', 3, '湖北', '武汉', '周杰',
        '13900001008', '洪山区光谷大道', '2024-03-20 09:10:00'),
       (16, 'ORD20240401001', 1, 5999.00, 200.00, 5799.00, 0.00, 2, 1, '2024-04-01 10:00:00', 1, '广东', '深圳', '张三',
        '13900001001', '南山区科技路1号', '2024-04-01 09:55:00'),
       (17, 'ORD20240405001', 3, 7999.00, 400.00, 7599.00, 0.00, 3, 1, '2024-04-05 16:30:00', 0, '北京', '北京', '王芳',
        '13900001003', '朝阳区望京东路', '2024-04-05 16:25:00'),
       (18, 'ORD20240410001', 2, 1488.00, 0.00, 1488.00, 0.00, 3, 1, '2024-04-10 12:00:00', 2, '上海', '上海', '李四',
        '13900001002', '浦东新区陆家嘴环路', '2024-04-10 11:55:00'),
       (19, 'ORD20240415001', 7, 1099.00, 50.00, 1049.00, 0.00, 3, 1, '2024-04-15 18:00:00', 1, '广东', '广州', '孙浩',
        '13900001007', '天河区体育西路', '2024-04-15 17:55:00'),
       (20, 'ORD20240420001', 5, 1499.00, 0.00, 1499.00, 0.00, 3, 1, '2024-04-20 14:20:00', 3, '四川', '成都', '刘洋',
        '13900001005', '武侯区天府大道', '2024-04-20 14:15:00'),
       (21, 'ORD20240501001', 4, 420.00, 20.00, 400.00, 0.00, 3, 1, '2024-05-01 09:00:00', 1, '浙江', '杭州', '赵敏',
        '13900001004', '西湖区文三路', '2024-05-01 08:55:00'),
       (22, 'ORD20240505001', 11, 5999.00, 300.00, 5699.00, 0.00, 3, 1, '2024-05-05 11:10:00', 2, '湖南', '长沙',
        '黄婷', '13900001011', '岳麓区麓谷大道', '2024-05-05 11:05:00'),
       (23, 'ORD20240510001', 15, 799.00, 0.00, 799.00, 10.00, 3, 1, '2024-05-10 15:30:00', 1, '重庆', '重庆', '罗琳',
        '13900001015', '渝中区解放碑', '2024-05-10 15:25:00'),
       (24, 'ORD20240515001', 6, 3999.00, 100.00, 3899.00, 0.00, 3, 1, '2024-05-15 10:40:00', 0, '江苏', '南京', '陈静',
        '13900001006', '鼓楼区中山北路', '2024-05-15 10:35:00'),
       (25, 'ORD20240520001', 13, 510.00, 0.00, 510.00, 0.00, 3, 1, '2024-05-20 13:50:00', 1, '云南', '昆明', '何雪',
        '13900001013', '盘龙区白云路', '2024-05-20 13:45:00'),
       (26, 'ORD20240601001', 1, 1199.00, 50.00, 1149.00, 0.00, 1, 1, '2024-06-01 08:00:00', 1, '广东', '深圳', '张三',
        '13900001001', '南山区科技路1号', '2024-06-01 07:55:00'),
       (27, 'ORD20240605001', 3, 1499.00, 0.00, 1499.00, 0.00, 3, 1, '2024-06-05 14:30:00', 0, '北京', '北京', '王芳',
        '13900001003', '朝阳区望京东路', '2024-06-05 14:25:00'),
       (28, 'ORD20240610001', 12, 9999.00, 500.00, 9499.00, 0.00, 3, 1, '2024-06-10 16:00:00', 2, '辽宁', '大连',
        '林峰', '13900001012', '中山区人民路', '2024-06-10 15:55:00'),
       (29, 'ORD20240615001', 9, 699.00, 0.00, 699.00, 10.00, 3, 1, '2024-06-15 11:20:00', 1, '福建', '厦门', '吴丽',
        '13900001009', '思明区软件园', '2024-06-15 11:15:00'),
       (30, 'ORD20240620001', 2, 999.00, 0.00, 999.00, 10.00, 3, 1, '2024-06-20 09:45:00', 3, '上海', '上海', '李四',
        '13900001002', '浦东新区陆家嘴环路', '2024-06-20 09:40:00'),
       (31, 'ORD20240701001', 7, 5999.00, 200.00, 5799.00, 0.00, 3, 1, '2024-07-01 10:30:00', 1, '广东', '广州', '孙浩',
        '13900001007', '天河区体育西路', '2024-07-01 10:25:00'),
       (32, 'ORD20240705001', 14, 299.00, 0.00, 299.00, 10.00, 3, 1, '2024-07-05 15:00:00', 1, '陕西', '西安', '马超',
        '13900001014', '雁塔区高新路', '2024-07-05 14:55:00'),
       (33, 'ORD20240710001', 10, 899.00, 0.00, 899.00, 10.00, 3, 1, '2024-07-10 12:30:00', 0, '山东', '青岛', '郑强',
        '13900001010', '市南区香港中路', '2024-07-10 12:25:00'),
       (34, 'ORD20240715001', 5, 399.00, 0.00, 399.00, 10.00, 3, 1, '2024-07-15 17:10:00', 1, '四川', '成都', '刘洋',
        '13900001005', '武侯区天府大道', '2024-07-15 17:05:00'),
       (35, 'ORD20240720001', 8, 2499.00, 100.00, 2399.00, 0.00, 4, 2, '2024-07-20 09:00:00', 3, '湖北', '武汉', '周杰',
        '13900001008', '洪山区光谷大道', '2024-07-20 08:55:00'),
       (36, 'ORD20240801001', 1, 8999.00, 400.00, 8599.00, 0.00, 3, 1, '2024-08-01 10:00:00', 0, '广东', '深圳', '张三',
        '13900001001', '南山区科技路1号', '2024-08-01 09:55:00'),
       (37, 'ORD20240805001', 15, 760.00, 30.00, 730.00, 0.00, 3, 1, '2024-08-05 14:20:00', 1, '重庆', '重庆', '罗琳',
        '13900001015', '渝中区解放碑', '2024-08-05 14:15:00'),
       (38, 'ORD20240810001', 4, 5999.00, 200.00, 5799.00, 0.00, 3, 1, '2024-08-10 11:00:00', 2, '浙江', '杭州', '赵敏',
        '13900001004', '西湖区文三路', '2024-08-10 10:55:00'),
       (39, 'ORD20240815001', 11, 1099.00, 50.00, 1049.00, 0.00, 3, 1, '2024-08-15 16:30:00', 1, '湖南', '长沙', '黄婷',
        '13900001011', '岳麓区麓谷大道', '2024-08-15 16:25:00'),
       (40, 'ORD20240820001', 6, 6999.00, 300.00, 6699.00, 0.00, 3, 1, '2024-08-20 09:15:00', 0, '江苏', '南京', '陈静',
        '13900001006', '鼓楼区中山北路', '2024-08-20 09:10:00'),
       (41, 'ORD20240901001', 3, 14999.00, 1000.00, 13999.00, 0.00, 3, 1, '2024-09-01 08:30:00', 0, '北京', '北京',
        '王芳', '13900001003', '朝阳区望京东路', '2024-09-01 08:25:00'),
       (42, 'ORD20240905001', 2, 9999.00, 500.00, 9499.00, 0.00, 3, 1, '2024-09-05 13:00:00', 2, '上海', '上海', '李四',
        '13900001002', '浦东新区陆家嘴环路', '2024-09-05 12:55:00'),
       (43, 'ORD20240910001', 12, 5999.00, 200.00, 5799.00, 0.00, 3, 1, '2024-09-10 15:30:00', 2, '辽宁', '大连',
        '林峰', '13900001012', '中山区人民路', '2024-09-10 15:25:00'),
       (44, 'ORD20240915001', 9, 1488.00, 0.00, 1488.00, 0.00, 3, 1, '2024-09-15 10:00:00', 1, '福建', '厦门', '吴丽',
        '13900001009', '思明区软件园', '2024-09-15 09:55:00'),
       (45, 'ORD20240920001', 7, 699.00, 0.00, 699.00, 10.00, 3, 1, '2024-09-20 14:40:00', 1, '广东', '广州', '孙浩',
        '13900001007', '天河区体育西路', '2024-09-20 14:35:00'),
       (46, 'ORD20241001001', 1, 5999.00, 300.00, 5699.00, 0.00, 3, 1, '2024-10-01 10:00:00', 1, '广东', '深圳', '张三',
        '13900001001', '南山区科技路1号', '2024-10-01 09:55:00'),
       (47, 'ORD20241010001', 5, 1499.00, 0.00, 1499.00, 0.00, 3, 1, '2024-10-10 11:30:00', 3, '四川', '成都', '刘洋',
        '13900001005', '武侯区天府大道', '2024-10-10 11:25:00'),
       (48, 'ORD20241015001', 14, 510.00, 0.00, 510.00, 0.00, 3, 1, '2024-10-15 16:00:00', 1, '陕西', '西安', '马超',
        '13900001014', '雁塔区高新路', '2024-10-15 15:55:00'),
       (49, 'ORD20241020001', 8, 3999.00, 100.00, 3899.00, 0.00, 0, 0, NULL, 3, '湖北', '武汉', '周杰', '13900001008',
        '洪山区光谷大道', '2024-10-20 09:00:00'),
       (50, 'ORD20241025001', 13, 760.00, 30.00, 730.00, 0.00, 3, 1, '2024-10-25 14:20:00', 1, '云南', '昆明', '何雪',
        '13900001013', '盘龙区白云路', '2024-10-25 14:15:00');

SELECT setval('ecom_order_id_seq', (SELECT MAX(id) FROM ecom_order));

-- =============================================
-- 订单明细
-- =============================================
INSERT INTO ecom_order_item (order_id, product_id, product_name, sku, price, quantity, subtotal)
VALUES (1, 1, '华为Mate 60 Pro 512GB', 'HW-M60P-512', 6999.00, 1, 6999.00),
       (2, 3, 'iPhone 15 Pro Max 512GB', 'AP-15PM-512', 9999.00, 1, 9999.00),
       (3, 7, '联想ThinkPad X1 Carbon Gen11', 'LN-X1C-G11', 10999.00, 1, 10999.00),
       (4, 5, '小米14 Ultra 512GB', 'MI-14U-512', 5999.00, 1, 5999.00),
       (5, 14, '耐克Air Max 270 运动鞋', 'NK-AM270', 899.00, 1, 899.00),
       (6, 22, '兰蔻小黑瓶精华液 50ml', 'LC-GEN-50', 760.00, 1, 760.00),
       (7, 10, '苹果MacBook Pro 14 M3 Pro', 'AP-MBP14-M3', 14999.00, 1, 14999.00),
       (8, 2, '华为Mate 60 Pro 256GB', 'HW-M60P-256', 5999.00, 1, 5999.00),
       (9, 15, '阿迪达斯Ultraboost 23 跑鞋', 'AD-UB23', 1099.00, 1, 1099.00),
       (10, 21, '茅台飞天53度 500ml', 'MT-FT-53', 1499.00, 1, 1499.00),
       (11, 12, '小米手环8 Pro', 'MI-B8P', 299.00, 1, 299.00),
       (12, 4, 'iPhone 15 Pro 256GB', 'AP-15P-256', 7999.00, 1, 7999.00),
       (12, 29, '华为FreeBuds Pro 3', 'HW-FBP3', 1199.00, 1, 1199.00),
       (13, 27, '耐克Dunk Low 休闲鞋', 'NK-DKL', 699.00, 1, 699.00),
       (14, 12, '小米手环8 Pro', 'MI-B8P', 299.00, 1, 299.00),
       (15, 25, '索尼WH-1000XM5 头戴式降噪耳机', 'SN-WH1K5', 2499.00, 1, 2499.00),
       (16, 5, '小米14 Ultra 512GB', 'MI-14U-512', 5999.00, 1, 5999.00),
       (17, 4, 'iPhone 15 Pro 256GB', 'AP-15P-256', 7999.00, 1, 7999.00),
       (18, 11, '华为Watch GT4 46mm', 'HW-WGT4-46', 1488.00, 1, 1488.00),
       (19, 15, '阿迪达斯Ultraboost 23 跑鞋', 'AD-UB23', 1099.00, 1, 1099.00),
       (20, 21, '茅台飞天53度 500ml', 'MT-FT-53', 1499.00, 1, 1499.00),
       (21, 24, '兰蔻持妆粉底液 30ml', 'LC-TL-30', 420.00, 1, 420.00),
       (22, 6, '三星Galaxy S24 Ultra 512GB', 'SS-S24U-512', 8999.00, 1, 8999.00),
       (22, 30, '三星Galaxy Buds2 Pro', 'SS-GB2P', 999.00, 1, 999.00),
       (23, 19, '宜家POÄNG 扶手椅', 'IK-POANG', 799.00, 1, 799.00),
       (24, 26, '小米14 256GB', 'MI-14-256', 3999.00, 1, 3999.00),
       (25, 23, '雅诗兰黛小棕瓶眼霜 15ml', 'EL-AN-15', 510.00, 1, 510.00),
       (26, 29, '华为FreeBuds Pro 3', 'HW-FBP3', 1199.00, 1, 1199.00),
       (27, 21, '茅台飞天53度 500ml', 'MT-FT-53', 1499.00, 1, 1499.00),
       (28, 10, '苹果MacBook Pro 14 M3 Pro', 'AP-MBP14-M3', 14999.00, 1, 14999.00),
       (28, 13, 'Apple Watch Ultra 2', 'AP-AWU2', 5999.00, 1, 5999.00),
       (29, 14, '耐克Air Max 270 运动鞋', 'NK-AM270', 899.00, 1, 899.00),
       (30, 30, '三星Galaxy Buds2 Pro', 'SS-GB2P', 999.00, 1, 999.00),
       (31, 1, '华为Mate 60 Pro 512GB', 'HW-M60P-512', 6999.00, 1, 6999.00),
       (32, 12, '小米手环8 Pro', 'MI-B8P', 299.00, 1, 299.00),
       (33, 27, '耐克Dunk Low 休闲鞋', 'NK-DKL', 699.00, 1, 699.00),
       (34, 16, '优衣库男装圆领T恤', 'UQ-MT-001', 79.00, 5, 395.00),
       (35, 25, '索尼WH-1000XM5 头戴式降噪耳机', 'SN-WH1K5', 2499.00, 1, 2499.00),
       (36, 6, '三星Galaxy S24 Ultra 512GB', 'SS-S24U-512', 8999.00, 1, 8999.00),
       (37, 22, '兰蔻小黑瓶精华液 50ml', 'LC-GEN-50', 760.00, 1, 760.00),
       (38, 5, '小米14 Ultra 512GB', 'MI-14U-512', 5999.00, 1, 5999.00),
       (39, 7, '联想ThinkPad X1 Carbon Gen11', 'LN-X1C-G11', 10999.00, 1, 10999.00),
       (39, 29, '华为FreeBuds Pro 3', 'HW-FBP3', 1199.00, 1, 1199.00),
       (40, 1, '华为Mate 60 Pro 512GB', 'HW-M60P-512', 6999.00, 1, 6999.00),
       (41, 10, '苹果MacBook Pro 14 M3 Pro', 'AP-MBP14-M3', 14999.00, 1, 14999.00),
       (42, 9, '华为MateBook X Pro 2024', 'HW-MBX-P24', 9999.00, 1, 9999.00),
       (43, 1, '华为Mate 60 Pro 512GB', 'HW-M60P-512', 6999.00, 1, 6999.00),
       (43, 11, '华为Watch GT4 46mm', 'HW-WGT4-46', 1488.00, 1, 1488.00),
       (44, 11, '华为Watch GT4 46mm', 'HW-WGT4-46', 1488.00, 1, 1488.00),
       (45, 27, '耐克Dunk Low 休闲鞋', 'NK-DKL', 699.00, 1, 699.00),
       (46, 26, '小米14 256GB', 'MI-14-256', 3999.00, 1, 3999.00),
       (46, 29, '华为FreeBuds Pro 3', 'HW-FBP3', 1199.00, 1, 1199.00),
       (47, 21, '茅台飞天53度 500ml', 'MT-FT-53', 1499.00, 1, 1499.00),
       (48, 23, '雅诗兰黛小棕瓶眼霜 15ml', 'EL-AN-15', 510.00, 1, 510.00),
       (49, 26, '小米14 256GB', 'MI-14-256', 3999.00, 1, 3999.00),
       (50, 22, '兰蔻小黑瓶精华液 50ml', 'LC-GEN-50', 760.00, 1, 760.00);

-- =============================================
-- 支付记录
-- =============================================
INSERT INTO ecom_payment (order_id, payment_no, pay_channel, pay_amount, pay_status, pay_time, refund_amount,
                          refund_time)
VALUES (1, 'WX20240101100500001', 0, 6799.00, 1, '2024-01-01 10:05:00', 0, NULL),
       (2, 'ALI20240102142000001', 1, 9499.00, 1, '2024-01-02 14:20:00', 0, NULL),
       (3, 'WX20240105093000001', 0, 10699.00, 1, '2024-01-05 09:30:00', 0, NULL),
       (4, 'ALI20240108164000001', 1, 5899.00, 1, '2024-01-08 16:40:00', 0, NULL),
       (5, 'WX20240110110000001', 0, 899.00, 1, '2024-01-10 11:00:00', 0, NULL),
       (6, 'ALI20240115201000001', 1, 730.00, 1, '2024-01-15 20:10:00', 0, NULL),
       (7, 'WX20240201083000001', 0, 14199.00, 1, '2024-02-01 08:30:00', 0, NULL),
       (8, 'BANK20240205130000001', 2, 6799.00, 1, '2024-02-05 13:00:00', 0, NULL),
       (9, 'WX20240210174500001', 0, 1049.00, 1, '2024-02-10 17:45:00', 0, NULL),
       (10, 'ALI20240214193000001', 1, 1499.00, 1, '2024-02-14 19:30:00', 0, NULL),
       (11, 'WX20240301102000001', 0, 399.00, 1, '2024-03-01 10:20:00', 0, NULL),
       (12, 'BANK20240305140000001', 2, 5699.00, 1, '2024-03-05 14:00:00', 0, NULL),
       (13, 'ALI20240310113000001', 1, 899.00, 1, '2024-03-10 11:30:00', 0, NULL),
       (14, 'WX20240315154500001', 0, 299.00, 1, '2024-03-15 15:45:00', 0, NULL),
       (15, 'ALI20240320091500001', 1, 2399.00, 1, '2024-03-20 09:15:00', 0, NULL),
       (16, 'WX20240401100000001', 0, 5799.00, 1, '2024-04-01 10:00:00', 0, NULL),
       (17, 'ALI20240405163000001', 1, 7599.00, 1, '2024-04-05 16:30:00', 0, NULL),
       (18, 'WX20240410120000001', 0, 1488.00, 1, '2024-04-10 12:00:00', 0, NULL),
       (19, 'ALI20240415180000001', 1, 1049.00, 1, '2024-04-15 18:00:00', 0, NULL),
       (20, 'BANK20240420142000001', 2, 1499.00, 1, '2024-04-20 14:20:00', 0, NULL),
       (21, 'WX20240501090000001', 0, 400.00, 1, '2024-05-01 09:00:00', 0, NULL),
       (22, 'BANK20240505111000001', 2, 5699.00, 1, '2024-05-05 11:10:00', 0, NULL),
       (23, 'ALI20240510153000001', 1, 799.00, 1, '2024-05-10 15:30:00', 0, NULL),
       (24, 'WX20240515104000001', 0, 3899.00, 1, '2024-05-15 10:40:00', 0, NULL),
       (25, 'ALI20240520135000001', 1, 510.00, 1, '2024-05-20 13:50:00', 0, NULL),
       (26, 'WX20240601080000001', 0, 1149.00, 1, '2024-06-01 08:00:00', 0, NULL),
       (27, 'ALI20240605143000001', 1, 1499.00, 1, '2024-06-05 14:30:00', 0, NULL),
       (28, 'BANK20240610160000001', 2, 9499.00, 1, '2024-06-10 16:00:00', 0, NULL),
       (29, 'WX20240615112000001', 0, 699.00, 1, '2024-06-15 11:20:00', 0, NULL),
       (30, 'ALI20240620094500001', 1, 999.00, 1, '2024-06-20 09:45:00', 0, NULL),
       (31, 'WX20240701103000001', 0, 5799.00, 1, '2024-07-01 10:30:00', 0, NULL),
       (32, 'ALI20240705150000001', 1, 299.00, 1, '2024-07-05 15:00:00', 0, NULL),
       (33, 'WX20240710123000001', 0, 899.00, 1, '2024-07-10 12:30:00', 0, NULL),
       (34, 'ALI20240715171000001', 1, 399.00, 1, '2024-07-15 17:10:00', 0, NULL),
       (35, 'BANK20240720090000001', 2, 2399.00, 3, '2024-07-20 09:00:00', 2399.00, '2024-07-25 10:00:00'),
       (36, 'WX20240801100000001', 0, 8599.00, 1, '2024-08-01 10:00:00', 0, NULL),
       (37, 'ALI20240805142000001', 1, 730.00, 1, '2024-08-05 14:20:00', 0, NULL),
       (38, 'BANK20240810110000001', 2, 5799.00, 1, '2024-08-10 11:00:00', 0, NULL),
       (39, 'WX20240815163000001', 0, 1049.00, 1, '2024-08-15 16:30:00', 0, NULL),
       (40, 'ALI20240820091500001', 1, 6699.00, 1, '2024-08-20 09:15:00', 0, NULL),
       (41, 'WX20240901083000001', 0, 13999.00, 1, '2024-09-01 08:30:00', 0, NULL),
       (42, 'BANK20240905130000001', 2, 9499.00, 1, '2024-09-05 13:00:00', 0, NULL),
       (43, 'ALI20240910153000001', 1, 5799.00, 1, '2024-09-10 15:30:00', 0, NULL),
       (44, 'WX20240915100000001', 0, 1488.00, 1, '2024-09-15 10:00:00', 0, NULL),
       (45, 'ALI20240920144000001', 1, 699.00, 1, '2024-09-20 14:40:00', 0, NULL),
       (46, 'WX20241001100000001', 0, 5699.00, 1, '2024-10-01 10:00:00', 0, NULL),
       (47, 'BANK20241010113000001', 2, 1499.00, 1, '2024-10-10 11:30:00', 0, NULL),
       (48, 'ALI20241015160000001', 1, 510.00, 1, '2024-10-15 16:00:00', 0, NULL),
       (50, 'WX20241025142000001', 0, 730.00, 1, '2024-10-25 14:20:00', 0, NULL);

-- =============================================
-- 库存
-- =============================================
INSERT INTO ecom_inventory (product_id, warehouse_code, available_qty, locked_qty, total_qty, safety_qty, last_in_time,
                            last_out_time)
VALUES (1, 'WH01', 150, 10, 160, 20, '2024-10-01 08:00:00', '2024-10-25 16:00:00'),
       (2, 'WH01', 80, 5, 85, 15, '2024-09-15 09:00:00', '2024-10-20 14:00:00'),
       (3, 'WH01', 60, 8, 68, 10, '2024-10-05 10:00:00', '2024-10-22 11:00:00'),
       (4, 'WH01', 90, 5, 95, 15, '2024-09-20 08:30:00', '2024-10-18 15:00:00'),
       (5, 'WH01', 120, 12, 132, 20, '2024-10-10 09:00:00', '2024-10-24 10:00:00'),
       (6, 'WH01', 40, 3, 43, 10, '2024-09-25 10:00:00', '2024-10-15 16:00:00'),
       (7, 'WH01', 30, 2, 32, 5, '2024-08-01 08:00:00', '2024-10-10 09:00:00'),
       (8, 'WH01', 25, 1, 26, 5, '2024-08-15 09:00:00', '2024-09-28 14:00:00'),
       (9, 'WH01', 45, 3, 48, 8, '2024-09-01 08:00:00', '2024-10-12 10:00:00'),
       (10, 'WH01', 20, 2, 22, 5, '2024-09-10 09:00:00', '2024-10-08 11:00:00'),
       (11, 'WH01', 200, 15, 215, 30, '2024-10-01 08:00:00', '2024-10-25 15:00:00'),
       (12, 'WH01', 500, 20, 520, 50, '2024-10-05 08:00:00', '2024-10-26 10:00:00'),
       (13, 'WH01', 35, 2, 37, 5, '2024-09-20 09:00:00', '2024-10-15 14:00:00'),
       (14, 'WH01', 300, 25, 325, 40, '2024-10-01 08:00:00', '2024-10-25 16:00:00'),
       (15, 'WH01', 250, 18, 268, 30, '2024-10-03 08:00:00', '2024-10-24 11:00:00'),
       (16, 'WH01', 1000, 50, 1050, 100, '2024-10-01 08:00:00', '2024-10-26 09:00:00'),
       (17, 'WH01', 800, 30, 830, 80, '2024-10-02 08:00:00', '2024-10-25 14:00:00'),
       (18, 'WH01', 15, 1, 16, 3, '2024-08-10 08:00:00', '2024-10-05 10:00:00'),
       (19, 'WH01', 25, 2, 27, 5, '2024-09-01 08:00:00', '2024-10-18 11:00:00'),
       (20, 'WH01', 180, 10, 190, 20, '2024-09-15 08:00:00', '2024-10-22 15:00:00'),
       (21, 'WH01', 50, 5, 55, 10, '2024-09-25 08:00:00', '2024-10-20 14:00:00'),
       (22, 'WH01', 120, 8, 128, 15, '2024-10-01 08:00:00', '2024-10-25 16:00:00'),
       (23, 'WH01', 100, 6, 106, 12, '2024-10-05 08:00:00', '2024-10-23 10:00:00'),
       (24, 'WH01', 90, 5, 95, 10, '2024-09-20 08:00:00', '2024-10-21 14:00:00'),
       (25, 'WH01', 60, 4, 64, 10, '2024-09-10 08:00:00', '2024-10-19 11:00:00'),
       (26, 'WH01', 200, 15, 215, 25, '2024-10-08 08:00:00', '2024-10-26 09:00:00'),
       (27, 'WH01', 350, 20, 370, 40, '2024-10-01 08:00:00', '2024-10-25 15:00:00'),
       (28, 'WH01', 600, 30, 630, 60, '2024-10-03 08:00:00', '2024-10-24 10:00:00'),
       (29, 'WH01', 180, 10, 190, 20, '2024-10-01 08:00:00', '2024-10-25 16:00:00'),
       (30, 'WH01', 150, 8, 158, 15, '2024-10-05 08:00:00', '2024-10-24 11:00:00'),
       (1, 'WH02', 50, 3, 53, 10, '2024-09-01 08:00:00', '2024-10-20 14:00:00'),
       (3, 'WH02', 20, 2, 22, 5, '2024-09-15 08:00:00', '2024-10-18 10:00:00'),
       (5, 'WH02', 40, 2, 42, 8, '2024-09-20 08:00:00', '2024-10-22 11:00:00'),
       (14, 'WH02', 100, 8, 108, 15, '2024-09-25 08:00:00', '2024-10-24 15:00:00'),
       (21, 'WH02', 20, 1, 21, 5, '2024-09-10 08:00:00', '2024-10-15 14:00:00');

-- =============================================
-- 物流
-- =============================================
INSERT INTO ecom_logistics (order_id, logistics_no, carrier, ship_time, receive_time, logistics_status, receiver_name,
                            receiver_phone, receiver_addr)
VALUES (1, 'SF1024010110001', '顺丰', '2024-01-01 14:00:00', '2024-01-02 10:00:00', 4, '张三', '13900001001',
        '南山区科技路1号'),
       (2, 'SF1024010214001', '顺丰', '2024-01-02 16:00:00', '2024-01-03 14:00:00', 4, '王芳', '13900001003',
        '朝阳区望京东路'),
       (3, 'ZT2024010509001', '中通', '2024-01-05 14:00:00', '2024-01-07 11:00:00', 4, '李四', '13900001002',
        '浦东新区陆家嘴环路'),
       (4, 'SF1024010816001', '顺丰', '2024-01-08 18:00:00', '2024-01-09 16:00:00', 4, '刘洋', '13900001005',
        '武侯区天府大道'),
       (5, 'YT2024011011001', '圆通', '2024-01-10 15:00:00', '2024-01-12 10:00:00', 4, '孙浩', '13900001007',
        '天河区体育西路'),
       (6, 'ZT2024011520001', '中通', '2024-01-16 09:00:00', '2024-01-18 14:00:00', 4, '赵敏', '13900001004',
        '西湖区文三路'),
       (7, 'SF1024020108001', '顺丰', '2024-02-01 12:00:00', '2024-02-02 10:00:00', 4, '张三', '13900001001',
        '南山区科技路1号'),
       (8, 'YD2024020513001', '韵达', '2024-02-05 16:00:00', '2024-02-08 11:00:00', 4, '林峰', '13900001012',
        '中山区人民路'),
       (9, 'ST2024021017001', '申通', '2024-02-11 09:00:00', '2024-02-13 15:00:00', 4, '黄婷', '13900001011',
        '岳麓区麓谷大道'),
       (10, 'SF1024021419001', '顺丰', '2024-02-15 10:00:00', '2024-02-16 14:00:00', 4, '罗琳', '13900001015',
        '渝中区解放碑'),
       (11, 'ZT2024030110001', '中通', '2024-03-01 14:00:00', '2024-03-03 10:00:00', 4, '陈静', '13900001006',
        '鼓楼区中山北路'),
       (12, 'SF1024030514001', '顺丰', '2024-03-05 16:00:00', '2024-03-06 14:00:00', 4, '吴丽', '13900001009',
        '思明区软件园'),
       (13, 'YT2024031011001', '圆通', '2024-03-10 15:00:00', '2024-03-12 11:00:00', 4, '郑强', '13900001010',
        '市南区香港中路'),
       (14, 'YD2024031515001', '韵达', '2024-03-15 18:00:00', '2024-03-17 14:00:00', 4, '马超', '13900001014',
        '雁塔区高新路'),
       (15, 'ZT2024032009001', '中通', '2024-03-20 14:00:00', '2024-03-22 10:00:00', 4, '周杰', '13900001008',
        '洪山区光谷大道'),
       (16, 'SF1024040110001', '顺丰', '2024-04-01 14:00:00', NULL, 2, '张三', '13900001001', '南山区科技路1号'),
       (17, 'SF1024040516001', '顺丰', '2024-04-05 18:00:00', '2024-04-06 16:00:00', 4, '王芳', '13900001003',
        '朝阳区望京东路'),
       (18, 'ZT2024041012001', '中通', '2024-04-10 16:00:00', '2024-04-12 11:00:00', 4, '李四', '13900001002',
        '浦东新区陆家嘴环路'),
       (19, 'YD2024041518001', '韵达', '2024-04-16 09:00:00', '2024-04-18 14:00:00', 4, '孙浩', '13900001007',
        '天河区体育西路'),
       (20, 'SF1024042014001', '顺丰', '2024-04-20 18:00:00', '2024-04-21 16:00:00', 4, '刘洋', '13900001005',
        '武侯区天府大道'),
       (21, 'ST2024050109001', '申通', '2024-05-01 14:00:00', '2024-05-03 10:00:00', 4, '赵敏', '13900001004',
        '西湖区文三路'),
       (22, 'SF1024050511001', '顺丰', '2024-05-05 16:00:00', '2024-05-06 14:00:00', 4, '黄婷', '13900001011',
        '岳麓区麓谷大道'),
       (23, 'YT2024051015001', '圆通', '2024-05-10 18:00:00', '2024-05-12 14:00:00', 4, '罗琳', '13900001015',
        '渝中区解放碑'),
       (24, 'ZT2024051510001', '中通', '2024-05-15 14:00:00', '2024-05-17 10:00:00', 4, '陈静', '13900001006',
        '鼓楼区中山北路'),
       (25, 'YD2024052013001', '韵达', '2024-05-20 18:00:00', '2024-05-22 14:00:00', 4, '何雪', '13900001013',
        '盘龙区白云路'),
       (26, 'SF1024060108001', '顺丰', '2024-06-01 12:00:00', '2024-06-02 10:00:00', 4, '张三', '13900001001',
        '南山区科技路1号'),
       (27, 'SF1024060514001', '顺丰', '2024-06-05 18:00:00', '2024-06-06 16:00:00', 4, '王芳', '13900001003',
        '朝阳区望京东路'),
       (28, 'SF1024061016001', '顺丰', '2024-06-10 18:00:00', '2024-06-11 16:00:00', 4, '林峰', '13900001012',
        '中山区人民路'),
       (29, 'YT2024061511001', '圆通', '2024-06-15 15:00:00', '2024-06-17 11:00:00', 4, '吴丽', '13900001009',
        '思明区软件园'),
       (30, 'ZT2024062009001', '中通', '2024-06-20 14:00:00', '2024-06-22 10:00:00', 4, '李四', '13900001002',
        '浦东新区陆家嘴环路'),
       (31, 'SF1024070110001', '顺丰', '2024-07-01 14:00:00', '2024-07-02 12:00:00', 4, '孙浩', '13900001007',
        '天河区体育西路'),
       (32, 'YD2024070515001', '韵达', '2024-07-05 18:00:00', '2024-07-07 14:00:00', 4, '马超', '13900001014',
        '雁塔区高新路'),
       (33, 'YT2024071012001', '圆通', '2024-07-10 16:00:00', '2024-07-12 11:00:00', 4, '郑强', '13900001010',
        '市南区香港中路'),
       (34, 'ST2024071517001', '申通', '2024-07-15 20:00:00', '2024-07-17 15:00:00', 4, '刘洋', '13900001005',
        '武侯区天府大道'),
       (35, 'ZT2024072009001', '中通', '2024-07-20 14:00:00', NULL, 5, '周杰', '13900001008', '洪山区光谷大道'),
       (36, 'SF1024080110001', '顺丰', '2024-08-01 14:00:00', '2024-08-02 12:00:00', 4, '张三', '13900001001',
        '南山区科技路1号'),
       (37, 'YD2024080514001', '韵达', '2024-08-05 18:00:00', '2024-08-07 14:00:00', 4, '罗琳', '13900001015',
        '渝中区解放碑'),
       (38, 'SF1024081011001', '顺丰', '2024-08-10 16:00:00', '2024-08-11 14:00:00', 4, '赵敏', '13900001004',
        '西湖区文三路'),
       (39, 'SF1024081516001', '顺丰', '2024-08-15 18:00:00', '2024-08-16 16:00:00', 4, '黄婷', '13900001011',
        '岳麓区麓谷大道'),
       (40, 'SF1024082009001', '顺丰', '2024-08-20 14:00:00', '2024-08-21 12:00:00', 4, '陈静', '13900001006',
        '鼓楼区中山北路'),
       (41, 'SF1024090108001', '顺丰', '2024-09-01 12:00:00', '2024-09-02 10:00:00', 4, '王芳', '13900001003',
        '朝阳区望京东路'),
       (42, 'SF1024090513001', '顺丰', '2024-09-05 16:00:00', '2024-09-06 14:00:00', 4, '李四', '13900001002',
        '浦东新区陆家嘴环路'),
       (43, 'SF1024091015001', '顺丰', '2024-09-10 18:00:00', '2024-09-11 16:00:00', 4, '林峰', '13900001012',
        '中山区人民路'),
       (44, 'ZT2024091510001', '中通', '2024-09-15 14:00:00', '2024-09-17 10:00:00', 4, '吴丽', '13900001009',
        '思明区软件园'),
       (45, 'YD2024092014001', '韵达', '2024-09-20 18:00:00', '2024-09-22 14:00:00', 4, '孙浩', '13900001007',
        '天河区体育西路'),
       (46, 'SF1024100110001', '顺丰', '2024-10-01 14:00:00', '2024-10-02 12:00:00', 4, '张三', '13900001001',
        '南山区科技路1号'),
       (47, 'SF1024101011001', '顺丰', '2024-10-10 16:00:00', '2024-10-11 14:00:00', 4, '刘洋', '13900001005',
        '武侯区天府大道'),
       (48, 'YD2024101516001', '韵达', '2024-10-15 18:00:00', '2024-10-17 14:00:00', 4, '马超', '13900001014',
        '雁塔区高新路'),
       (50, 'ZT2024102514001', '中通', '2024-10-25 18:00:00', NULL, 2, '何雪', '13900001013', '盘龙区白云路');



-- =============================================
-- 配置数据
-- =============================================



INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('c8a152fe88ad940b371c3a1a34197396', 'ecom_brand', 'kit', 'master', '品牌表', 1, NULL, 'admin', '2026-06-10 17:28:50.868421', 'admin', '2026-06-10 17:28:50.868421', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('1dc6bba94be02ce87ef7062c2b754a51', 'ecom_category', 'kit', 'master', '商品分类表', 1, NULL, 'admin', '2026-06-10 17:29:05.001146', 'admin', '2026-06-10 17:29:05.001146', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('f9acf1c6096db4fbaefa927da1dfa146', 'ecom_customer', 'kit', 'master', '客户表', 1, NULL, 'admin', '2026-06-10 17:29:11.856427', 'admin', '2026-06-10 17:29:11.856427', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2aa80fa3183ee672c7caef6077068395', 'ecom_inventory', 'kit', 'master', '库存表', 1, NULL, 'admin', '2026-06-10 17:29:45.290422', 'admin', '2026-06-10 17:29:45.290422', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('e8bd61afe4c952c2cebc1d2c095955fb', 'ecom_logistics', 'kit', 'master', '物流表', 1, NULL, 'admin', '2026-06-10 17:29:54.184801', 'admin', '2026-06-10 17:29:54.184801', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('5a4f29ab823bccc98be4e7a6778f33c1', 'ecom_order', 'kit', 'master', '订单表', 1, NULL, 'admin', '2026-06-10 17:30:09.858034', 'admin', '2026-06-10 17:30:09.858034', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('cb0167431e656f5fea9238031cb4d8e7', 'ecom_order_item', 'kit', 'master', '订单明细表', 1, NULL, 'admin', '2026-06-10 17:30:19.043227', 'admin', '2026-06-10 17:30:19.043227', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('b7ced6bfa604629ad49049b6d9f4bca9', 'ecom_payment', 'kit', 'master', '支付记录表', 1, NULL, 'admin', '2026-06-10 17:30:35.786912', 'admin', '2026-06-10 17:30:35.786912', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('92aace30cdf314bb84a9c8eb94729cf9', 'ecom_product', 'kit', 'master', '商品表', 1, NULL, 'admin', '2026-06-10 17:30:44.602119', 'admin', '2026-06-10 17:30:44.602119', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('137dd09a98fda183c9e0679c17772602', 'ecom_product_tag', 'kit', 'master', '商品标签表', 1, NULL, 'admin', '2026-06-10 17:30:55.252077', 'admin', '2026-06-10 17:30:55.252077', 0, NULL, NULL);
INSERT INTO security_tablemodel_tables (id, table_name, module_prefix, data_source, table_comment, source_type, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('79f5cf0cd8505d2811fca509313fc386', 'ecom_supplier', 'kit', 'master', '供应商表', 1, NULL, 'admin', '2026-06-10 17:31:04.216167', 'admin', '2026-06-10 17:31:04.216167', 0, NULL, NULL);

INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986482720768', '1dc6bba94be02ce87ef7062c2b754a51', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_category_id_seq''::regclass)', '分类ID', 1, NULL, 'admin', '2026-06-10 17:29:05.009702', 'admin', '2026-06-10 17:29:05.009702', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986491109376', '1dc6bba94be02ce87ef7062c2b754a51', 'parent_id', 'int4', 10, 0, 1, 0, 0, NULL, '父分类ID，NULL表示顶级分类', 2, NULL, 'admin', '2026-06-10 17:29:05.01152', 'admin', '2026-06-10 17:29:05.01152', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986491109377', '1dc6bba94be02ce87ef7062c2b754a51', 'name', 'varchar', 100, 0, 0, 0, 0, NULL, '分类名称', 3, NULL, 'admin', '2026-06-10 17:29:05.011935', 'admin', '2026-06-10 17:29:05.011935', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986495303680', '1dc6bba94be02ce87ef7062c2b754a51', 'level', 'int2', 5, 0, 0, 0, 0, '1', '分类层级：1-一级 2-二级 3-三级', 4, NULL, 'admin', '2026-06-10 17:29:05.012095', 'admin', '2026-06-10 17:29:05.012095', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986495303681', '1dc6bba94be02ce87ef7062c2b754a51', 'sort_order', 'int4', 10, 0, 0, 0, 0, '0', '排序号', 5, NULL, 'admin', '2026-06-10 17:29:05.012834', 'admin', '2026-06-10 17:29:05.012834', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986495303682', '1dc6bba94be02ce87ef7062c2b754a51', 'icon', 'varchar', 200, 0, 1, 0, 0, 'NULL::character varying', '分类图标URL', 6, NULL, 'admin', '2026-06-10 17:29:05.013001', 'admin', '2026-06-10 17:29:05.013001', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986499497984', '1dc6bba94be02ce87ef7062c2b754a51', 'is_visible', 'int2', 5, 0, 0, 0, 0, '1', '是否可见：0-隐藏 1-可见', 7, NULL, 'admin', '2026-06-10 17:29:05.013551', 'admin', '2026-06-10 17:29:05.013551', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986503692288', '1dc6bba94be02ce87ef7062c2b754a51', 'status', 'int2', 5, 0, 0, 0, 0, '1', '状态：0-禁用 1-启用', 8, NULL, 'admin', '2026-06-10 17:29:05.014109', 'admin', '2026-06-10 17:29:05.014109', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986503692289', '1dc6bba94be02ce87ef7062c2b754a51', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 9, NULL, 'admin', '2026-06-10 17:29:05.014304', 'admin', '2026-06-10 17:29:05.014304', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640986507886592', '1dc6bba94be02ce87ef7062c2b754a51', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 10, NULL, 'admin', '2026-06-10 17:29:05.015436', 'admin', '2026-06-10 17:29:05.015436', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015238868992', 'f9acf1c6096db4fbaefa927da1dfa146', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_customer_id_seq''::regclass)', '客户ID', 1, NULL, 'admin', '2026-06-10 17:29:11.865214', 'admin', '2026-06-10 17:29:11.865214', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015238868993', 'f9acf1c6096db4fbaefa927da1dfa146', 'name', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '客户姓名', 2, NULL, 'admin', '2026-06-10 17:29:11.865827', 'admin', '2026-06-10 17:29:11.865827', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015243063296', 'f9acf1c6096db4fbaefa927da1dfa146', 'phone', 'varchar', 20, 0, 1, 0, 0, 'NULL::character varying', '手机号', 3, NULL, 'admin', '2026-06-10 17:29:11.866163', 'admin', '2026-06-10 17:29:11.866163', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015243063297', 'f9acf1c6096db4fbaefa927da1dfa146', 'email', 'varchar', 100, 0, 1, 0, 0, 'NULL::character varying', '邮箱', 4, NULL, 'admin', '2026-06-10 17:29:11.866501', 'admin', '2026-06-10 17:29:11.866501', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015243063298', 'f9acf1c6096db4fbaefa927da1dfa146', 'gender', 'int2', 5, 0, 1, 0, 0, NULL, '性别：0-未知 1-男 2-女', 5, NULL, 'admin', '2026-06-10 17:29:11.866938', 'admin', '2026-06-10 17:29:11.866938', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015247257600', 'f9acf1c6096db4fbaefa927da1dfa146', 'birthday', 'date', 13, 0, 1, 0, 0, NULL, '出生日期', 6, NULL, 'admin', '2026-06-10 17:29:11.867522', 'admin', '2026-06-10 17:29:11.867522', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015247257601', 'f9acf1c6096db4fbaefa927da1dfa146', 'province', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '省份', 7, NULL, 'admin', '2026-06-10 17:29:11.867826', 'admin', '2026-06-10 17:29:11.867826', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015251451904', 'f9acf1c6096db4fbaefa927da1dfa146', 'city', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '城市', 8, NULL, 'admin', '2026-06-10 17:29:11.868254', 'admin', '2026-06-10 17:29:11.868254', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015251451905', 'f9acf1c6096db4fbaefa927da1dfa146', 'address', 'varchar', 300, 0, 1, 0, 0, 'NULL::character varying', '详细地址', 9, NULL, 'admin', '2026-06-10 17:29:11.868575', 'admin', '2026-06-10 17:29:11.868575', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015251451906', 'f9acf1c6096db4fbaefa927da1dfa146', 'member_level', 'int2', 5, 0, 0, 0, 0, '0', '会员等级：0-普通 1-银卡 2-金卡 3-钻石', 10, NULL, 'admin', '2026-06-10 17:29:11.868849', 'admin', '2026-06-10 17:29:11.868849', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015255646208', 'f9acf1c6096db4fbaefa927da1dfa146', 'register_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '注册时间', 11, NULL, 'admin', '2026-06-10 17:29:11.86912', 'admin', '2026-06-10 17:29:11.86912', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015255646209', 'f9acf1c6096db4fbaefa927da1dfa146', 'last_login', 'timestamp', 29, 6, 1, 0, 0, NULL, '最后登录时间', 12, NULL, 'admin', '2026-06-10 17:29:11.869397', 'admin', '2026-06-10 17:29:11.869397', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641015255646210', 'f9acf1c6096db4fbaefa927da1dfa146', 'status', 'int2', 5, 0, 0, 0, 0, '1', '状态：0-禁用 1-正常', 13, NULL, 'admin', '2026-06-10 17:29:11.869837', 'admin', '2026-06-10 17:29:11.869837', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155467034624', '2aa80fa3183ee672c7caef6077068395', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_inventory_id_seq''::regclass)', '库存ID', 1, NULL, 'admin', '2026-06-10 17:29:45.298292', 'admin', '2026-06-10 17:29:45.298292', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155467034625', '2aa80fa3183ee672c7caef6077068395', 'product_id', 'int4', 10, 0, 0, 0, 0, NULL, '商品ID，关联ecom_product.id', 2, NULL, 'admin', '2026-06-10 17:29:45.298766', 'admin', '2026-06-10 17:29:45.298766', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155471228928', '2aa80fa3183ee672c7caef6077068395', 'warehouse_code', 'varchar', 20, 0, 0, 0, 0, '''WH01''::character varying', '仓库编码', 3, NULL, 'admin', '2026-06-10 17:29:45.299041', 'admin', '2026-06-10 17:29:45.299041', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155471228929', '2aa80fa3183ee672c7caef6077068395', 'available_qty', 'int4', 10, 0, 0, 0, 0, '0', '可用库存', 4, NULL, 'admin', '2026-06-10 17:29:45.299308', 'admin', '2026-06-10 17:29:45.299308', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155471228930', '2aa80fa3183ee672c7caef6077068395', 'locked_qty', 'int4', 10, 0, 0, 0, 0, '0', '锁定库存（已下单未发货）', 5, NULL, 'admin', '2026-06-10 17:29:45.299478', 'admin', '2026-06-10 17:29:45.299478', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155471228931', '2aa80fa3183ee672c7caef6077068395', 'total_qty', 'int4', 10, 0, 0, 0, 0, '0', '总库存 = available_qty + locked_qty', 6, NULL, 'admin', '2026-06-10 17:29:45.299663', 'admin', '2026-06-10 17:29:45.299663', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155471228932', '2aa80fa3183ee672c7caef6077068395', 'safety_qty', 'int4', 10, 0, 1, 0, 0, '10', '安全库存阈值', 7, NULL, 'admin', '2026-06-10 17:29:45.299839', 'admin', '2026-06-10 17:29:45.299839', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155471228933', '2aa80fa3183ee672c7caef6077068395', 'last_in_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '最后入库时间', 8, NULL, 'admin', '2026-06-10 17:29:45.299999', 'admin', '2026-06-10 17:29:45.299999', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155475423232', '2aa80fa3183ee672c7caef6077068395', 'last_out_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '最后出库时间', 9, NULL, 'admin', '2026-06-10 17:29:45.300271', 'admin', '2026-06-10 17:29:45.300271', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155475423233', '2aa80fa3183ee672c7caef6077068395', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 10, NULL, 'admin', '2026-06-10 17:29:45.300955', 'admin', '2026-06-10 17:29:45.300955', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641155479617536', '2aa80fa3183ee672c7caef6077068395', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 11, NULL, 'admin', '2026-06-10 17:29:45.301559', 'admin', '2026-06-10 17:29:45.301559', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192783757312', 'e8bd61afe4c952c2cebc1d2c095955fb', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_logistics_id_seq''::regclass)', '物流ID', 1, NULL, 'admin', '2026-06-10 17:29:54.195363', 'admin', '2026-06-10 17:29:54.195363', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192783757313', 'e8bd61afe4c952c2cebc1d2c095955fb', 'order_id', 'int4', 10, 0, 0, 0, 0, NULL, '订单ID，关联ecom_order.id', 2, NULL, 'admin', '2026-06-10 17:29:54.195989', 'admin', '2026-06-10 17:29:54.195989', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192787951616', 'e8bd61afe4c952c2cebc1d2c095955fb', 'logistics_no', 'varchar', 64, 0, 1, 0, 0, 'NULL::character varying', '物流单号', 3, NULL, 'admin', '2026-06-10 17:29:54.196531', 'admin', '2026-06-10 17:29:54.196531', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192792145920', 'e8bd61afe4c952c2cebc1d2c095955fb', 'carrier', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '承运商：顺丰/中通/圆通/韵达/申通', 4, NULL, 'admin', '2026-06-10 17:29:54.197036', 'admin', '2026-06-10 17:29:54.197036', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192792145921', 'e8bd61afe4c952c2cebc1d2c095955fb', 'ship_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '发货时间', 5, NULL, 'admin', '2026-06-10 17:29:54.197353', 'admin', '2026-06-10 17:29:54.197353', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192792145922', 'e8bd61afe4c952c2cebc1d2c095955fb', 'receive_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '签收时间', 6, NULL, 'admin', '2026-06-10 17:29:54.197892', 'admin', '2026-06-10 17:29:54.197892', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192796340224', 'e8bd61afe4c952c2cebc1d2c095955fb', 'logistics_status', 'int2', 5, 0, 0, 0, 0, '0', '物流状态：0-待发货 1-已发货 2-运输中 3-派送中 4-已签收 5-异常', 7, NULL, 'admin', '2026-06-10 17:29:54.198196', 'admin', '2026-06-10 17:29:54.198196', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192796340225', 'e8bd61afe4c952c2cebc1d2c095955fb', 'receiver_name', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '收货人姓名', 8, NULL, 'admin', '2026-06-10 17:29:54.198616', 'admin', '2026-06-10 17:29:54.198616', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192796340226', 'e8bd61afe4c952c2cebc1d2c095955fb', 'receiver_phone', 'varchar', 20, 0, 1, 0, 0, 'NULL::character varying', '收货人电话', 9, NULL, 'admin', '2026-06-10 17:29:54.198895', 'admin', '2026-06-10 17:29:54.198895', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192800534528', 'e8bd61afe4c952c2cebc1d2c095955fb', 'receiver_addr', 'varchar', 300, 0, 1, 0, 0, 'NULL::character varying', '收货地址', 10, NULL, 'admin', '2026-06-10 17:29:54.199204', 'admin', '2026-06-10 17:29:54.199204', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192800534529', 'e8bd61afe4c952c2cebc1d2c095955fb', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 11, NULL, 'admin', '2026-06-10 17:29:54.199479', 'admin', '2026-06-10 17:29:54.199479', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641192800534530', 'e8bd61afe4c952c2cebc1d2c095955fb', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 12, NULL, 'admin', '2026-06-10 17:29:54.199758', 'admin', '2026-06-10 17:29:54.199758', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258521083904', '5a4f29ab823bccc98be4e7a6778f33c1', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_order_id_seq''::regclass)', '订单ID', 1, NULL, 'admin', '2026-06-10 17:30:09.868997', 'admin', '2026-06-10 17:30:09.868997', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258529472512', '5a4f29ab823bccc98be4e7a6778f33c1', 'order_no', 'varchar', 32, 0, 0, 0, 0, NULL, '订单编号', 2, NULL, 'admin', '2026-06-10 17:30:09.870698', 'admin', '2026-06-10 17:30:09.870698', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258533666816', '5a4f29ab823bccc98be4e7a6778f33c1', 'customer_id', 'int4', 10, 0, 0, 0, 0, NULL, '客户ID，关联ecom_customer.id', 3, NULL, 'admin', '2026-06-10 17:30:09.871497', 'admin', '2026-06-10 17:30:09.871497', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258533666817', '5a4f29ab823bccc98be4e7a6778f33c1', 'total_amount', 'numeric', 12, 2, 0, 0, 0, '0', '商品总金额', 4, NULL, 'admin', '2026-06-10 17:30:09.871824', 'admin', '2026-06-10 17:30:09.871824', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258537861120', '5a4f29ab823bccc98be4e7a6778f33c1', 'discount_amount', 'numeric', 12, 2, 0, 0, 0, '0', '优惠金额', 5, NULL, 'admin', '2026-06-10 17:30:09.872288', 'admin', '2026-06-10 17:30:09.872288', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258537861121', '5a4f29ab823bccc98be4e7a6778f33c1', 'pay_amount', 'numeric', 12, 2, 0, 0, 0, '0', '实付金额', 6, NULL, 'admin', '2026-06-10 17:30:09.87255', 'admin', '2026-06-10 17:30:09.87255', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258537861122', '5a4f29ab823bccc98be4e7a6778f33c1', 'freight_amount', 'numeric', 10, 2, 1, 0, 0, '0', '运费', 7, NULL, 'admin', '2026-06-10 17:30:09.872839', 'admin', '2026-06-10 17:30:09.872839', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258542055424', '5a4f29ab823bccc98be4e7a6778f33c1', 'order_status', 'int2', 5, 0, 0, 0, 0, '0', '订单状态：0-待付款 1-待发货 2-已发货 3-已完成 4-已取消 5-退货中 6-已退货', 8, NULL, 'admin', '2026-06-10 17:30:09.87311', 'admin', '2026-06-10 17:30:09.87311', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258542055425', '5a4f29ab823bccc98be4e7a6778f33c1', 'pay_status', 'int2', 5, 0, 0, 0, 0, '0', '支付状态：0-未支付 1-已支付 2-已退款', 9, NULL, 'admin', '2026-06-10 17:30:09.873373', 'admin', '2026-06-10 17:30:09.873373', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258546249728', '5a4f29ab823bccc98be4e7a6778f33c1', 'pay_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '支付时间', 10, NULL, 'admin', '2026-06-10 17:30:09.87415', 'admin', '2026-06-10 17:30:09.87415', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258546249729', '5a4f29ab823bccc98be4e7a6778f33c1', 'order_source', 'int2', 5, 0, 1, 0, 0, '0', '订单来源：0-PC 1-H5 2-小程序 3-APP', 11, NULL, 'admin', '2026-06-10 17:30:09.874667', 'admin', '2026-06-10 17:30:09.874667', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258546249730', '5a4f29ab823bccc98be4e7a6778f33c1', 'remark', 'varchar', 500, 0, 1, 0, 0, 'NULL::character varying', '订单备注', 12, NULL, 'admin', '2026-06-10 17:30:09.874937', 'admin', '2026-06-10 17:30:09.874937', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258550444032', '5a4f29ab823bccc98be4e7a6778f33c1', 'province', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '收货省份', 13, NULL, 'admin', '2026-06-10 17:30:09.875118', 'admin', '2026-06-10 17:30:09.875118', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258550444033', '5a4f29ab823bccc98be4e7a6778f33c1', 'city', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '收货城市', 14, NULL, 'admin', '2026-06-10 17:30:09.875284', 'admin', '2026-06-10 17:30:09.875284', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258550444034', '5a4f29ab823bccc98be4e7a6778f33c1', 'receiver_name', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '收货人姓名', 15, NULL, 'admin', '2026-06-10 17:30:09.875449', 'admin', '2026-06-10 17:30:09.875449', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258550444035', '5a4f29ab823bccc98be4e7a6778f33c1', 'receiver_phone', 'varchar', 20, 0, 1, 0, 0, 'NULL::character varying', '收货人电话', 16, NULL, 'admin', '2026-06-10 17:30:09.875618', 'admin', '2026-06-10 17:30:09.875618', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258550444036', '5a4f29ab823bccc98be4e7a6778f33c1', 'receiver_addr', 'varchar', 300, 0, 1, 0, 0, 'NULL::character varying', '收货地址', 17, NULL, 'admin', '2026-06-10 17:30:09.875782', 'admin', '2026-06-10 17:30:09.875782', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258550444037', '5a4f29ab823bccc98be4e7a6778f33c1', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 18, NULL, 'admin', '2026-06-10 17:30:09.875938', 'admin', '2026-06-10 17:30:09.875938', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641258554638336', '5a4f29ab823bccc98be4e7a6778f33c1', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 19, NULL, 'admin', '2026-06-10 17:30:09.876099', 'admin', '2026-06-10 17:30:09.876099', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297045766144', 'cb0167431e656f5fea9238031cb4d8e7', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_order_item_id_seq''::regclass)', '明细ID', 1, NULL, 'admin', '2026-06-10 17:30:19.053606', 'admin', '2026-06-10 17:30:19.053606', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297049960448', 'cb0167431e656f5fea9238031cb4d8e7', 'order_id', 'int4', 10, 0, 0, 0, 0, NULL, '订单ID，关联ecom_order.id', 2, NULL, 'admin', '2026-06-10 17:30:19.054796', 'admin', '2026-06-10 17:30:19.054796', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297054154752', 'cb0167431e656f5fea9238031cb4d8e7', 'product_id', 'int4', 10, 0, 0, 0, 0, NULL, '商品ID，关联ecom_product.id', 3, NULL, 'admin', '2026-06-10 17:30:19.055175', 'admin', '2026-06-10 17:30:19.055175', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297054154753', 'cb0167431e656f5fea9238031cb4d8e7', 'product_name', 'varchar', 200, 0, 0, 0, 0, NULL, '商品名称（下单时快照）', 4, NULL, 'admin', '2026-06-10 17:30:19.055445', 'admin', '2026-06-10 17:30:19.055445', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297054154754', 'cb0167431e656f5fea9238031cb4d8e7', 'sku', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', 'SKU编码（下单时快照）', 5, NULL, 'admin', '2026-06-10 17:30:19.055686', 'admin', '2026-06-10 17:30:19.055686', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297058349056', 'cb0167431e656f5fea9238031cb4d8e7', 'price', 'numeric', 12, 2, 0, 0, 0, '0', '单价（下单时快照）', 6, NULL, 'admin', '2026-06-10 17:30:19.056055', 'admin', '2026-06-10 17:30:19.056055', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297058349057', 'cb0167431e656f5fea9238031cb4d8e7', 'quantity', 'int4', 10, 0, 0, 0, 0, '1', '购买数量', 7, NULL, 'admin', '2026-06-10 17:30:19.056297', 'admin', '2026-06-10 17:30:19.056297', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297058349058', 'cb0167431e656f5fea9238031cb4d8e7', 'subtotal', 'numeric', 12, 2, 0, 0, 0, '0', '小计金额 = price * quantity', 8, NULL, 'admin', '2026-06-10 17:30:19.056535', 'admin', '2026-06-10 17:30:19.056535', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641297058349059', 'cb0167431e656f5fea9238031cb4d8e7', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 9, NULL, 'admin', '2026-06-10 17:30:19.056775', 'admin', '2026-06-10 17:30:19.056775', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367275192320', 'b7ced6bfa604629ad49049b6d9f4bca9', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_payment_id_seq''::regclass)', '支付ID', 1, NULL, 'admin', '2026-06-10 17:30:35.797254', 'admin', '2026-06-10 17:30:35.797254', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367275192321', 'b7ced6bfa604629ad49049b6d9f4bca9', 'order_id', 'int4', 10, 0, 0, 0, 0, NULL, '订单ID，关联ecom_order.id', 2, NULL, 'admin', '2026-06-10 17:30:35.797945', 'admin', '2026-06-10 17:30:35.797945', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367279386624', 'b7ced6bfa604629ad49049b6d9f4bca9', 'payment_no', 'varchar', 64, 0, 1, 0, 0, 'NULL::character varying', '第三方支付流水号', 3, NULL, 'admin', '2026-06-10 17:30:35.798304', 'admin', '2026-06-10 17:30:35.798304', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367279386625', 'b7ced6bfa604629ad49049b6d9f4bca9', 'pay_channel', 'int2', 5, 0, 0, 0, 0, '0', '支付渠道：0-微信 1-支付宝 2-银行卡 3-余额', 4, NULL, 'admin', '2026-06-10 17:30:35.798667', 'admin', '2026-06-10 17:30:35.798667', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367279386626', 'b7ced6bfa604629ad49049b6d9f4bca9', 'pay_amount', 'numeric', 12, 2, 0, 0, 0, '0', '支付金额', 5, NULL, 'admin', '2026-06-10 17:30:35.798918', 'admin', '2026-06-10 17:30:35.798918', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367283580928', 'b7ced6bfa604629ad49049b6d9f4bca9', 'pay_status', 'int2', 5, 0, 0, 0, 0, '0', '支付状态：0-待支付 1-支付成功 2-支付失败 3-已退款', 6, NULL, 'admin', '2026-06-10 17:30:35.79915', 'admin', '2026-06-10 17:30:35.79915', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367283580929', 'b7ced6bfa604629ad49049b6d9f4bca9', 'pay_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '支付完成时间', 7, NULL, 'admin', '2026-06-10 17:30:35.799391', 'admin', '2026-06-10 17:30:35.799391', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367283580930', 'b7ced6bfa604629ad49049b6d9f4bca9', 'refund_amount', 'numeric', 12, 2, 1, 0, 0, '0', '退款金额', 8, NULL, 'admin', '2026-06-10 17:30:35.79973', 'admin', '2026-06-10 17:30:35.79973', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367287775232', 'b7ced6bfa604629ad49049b6d9f4bca9', 'refund_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '退款时间', 9, NULL, 'admin', '2026-06-10 17:30:35.800109', 'admin', '2026-06-10 17:30:35.800109', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641367287775233', 'b7ced6bfa604629ad49049b6d9f4bca9', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 10, NULL, 'admin', '2026-06-10 17:30:35.800462', 'admin', '2026-06-10 17:30:35.800462', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404247982080', '92aace30cdf314bb84a9c8eb94729cf9', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_product_id_seq''::regclass)', '商品ID', 1, NULL, 'admin', '2026-06-10 17:30:44.612206', 'admin', '2026-06-10 17:30:44.612206', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404252176384', '92aace30cdf314bb84a9c8eb94729cf9', 'name', 'varchar', 200, 0, 0, 0, 0, NULL, '商品名称', 2, NULL, 'admin', '2026-06-10 17:30:44.613642', 'admin', '2026-06-10 17:30:44.613642', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404256370688', '92aace30cdf314bb84a9c8eb94729cf9', 'category_id', 'int4', 10, 0, 0, 0, 0, NULL, '分类ID，关联ecom_category.id', 3, NULL, 'admin', '2026-06-10 17:30:44.61466', 'admin', '2026-06-10 17:30:44.61466', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404260564992', '92aace30cdf314bb84a9c8eb94729cf9', 'brand_id', 'int4', 10, 0, 1, 0, 0, NULL, '品牌ID，关联ecom_brand.id', 4, NULL, 'admin', '2026-06-10 17:30:44.615057', 'admin', '2026-06-10 17:30:44.615057', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404260564993', '92aace30cdf314bb84a9c8eb94729cf9', 'supplier_id', 'int4', 10, 0, 1, 0, 0, NULL, '供应商ID，关联ecom_supplier.id', 5, NULL, 'admin', '2026-06-10 17:30:44.615465', 'admin', '2026-06-10 17:30:44.615465', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404260564994', '92aace30cdf314bb84a9c8eb94729cf9', 'sku', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', 'SKU编码', 6, NULL, 'admin', '2026-06-10 17:30:44.615932', 'admin', '2026-06-10 17:30:44.615932', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404264759296', '92aace30cdf314bb84a9c8eb94729cf9', 'price', 'numeric', 12, 2, 0, 0, 0, '0', '销售价格', 7, NULL, 'admin', '2026-06-10 17:30:44.616549', 'admin', '2026-06-10 17:30:44.616549', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404268953600', '92aace30cdf314bb84a9c8eb94729cf9', 'cost_price', 'numeric', 12, 2, 1, 0, 0, '0', '成本价格', 8, NULL, 'admin', '2026-06-10 17:30:44.617013', 'admin', '2026-06-10 17:30:44.617013', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404268953601', '92aace30cdf314bb84a9c8eb94729cf9', 'unit', 'varchar', 20, 0, 1, 0, 0, '''件''::character varying', '计量单位', 9, NULL, 'admin', '2026-06-10 17:30:44.617681', 'admin', '2026-06-10 17:30:44.617681', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404273147904', '92aace30cdf314bb84a9c8eb94729cf9', 'weight', 'numeric', 8, 2, 1, 0, 0, 'NULL::numeric', '重量(kg)', 10, NULL, 'admin', '2026-06-10 17:30:44.618112', 'admin', '2026-06-10 17:30:44.618112', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404273147905', '92aace30cdf314bb84a9c8eb94729cf9', 'image_url', 'varchar', 500, 0, 1, 0, 0, 'NULL::character varying', '商品主图URL', 11, NULL, 'admin', '2026-06-10 17:30:44.618426', 'admin', '2026-06-10 17:30:44.618426', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404273147906', '92aace30cdf314bb84a9c8eb94729cf9', 'description', 'text', 2147483647, 0, 1, 0, 0, NULL, '商品描述', 12, NULL, 'admin', '2026-06-10 17:30:44.61873', 'admin', '2026-06-10 17:30:44.61873', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404277342208', '92aace30cdf314bb84a9c8eb94729cf9', 'status', 'int2', 5, 0, 0, 0, 0, '1', '状态：0-下架 1-上架 2-预售', 13, NULL, 'admin', '2026-06-10 17:30:44.619205', 'admin', '2026-06-10 17:30:44.619205', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404277342209', '92aace30cdf314bb84a9c8eb94729cf9', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 14, NULL, 'admin', '2026-06-10 17:30:44.619896', 'admin', '2026-06-10 17:30:44.619896', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641404281536512', '92aace30cdf314bb84a9c8eb94729cf9', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 15, NULL, 'admin', '2026-06-10 17:30:44.62034', 'admin', '2026-06-10 17:30:44.62034', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641448904736768', '137dd09a98fda183c9e0679c17772602', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_product_tag_id_seq''::regclass)', '标签ID', 1, NULL, 'admin', '2026-06-10 17:30:55.259842', 'admin', '2026-06-10 17:30:55.259842', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641448913125376', '137dd09a98fda183c9e0679c17772602', 'product_id', 'int4', 10, 0, 0, 0, 0, NULL, '商品ID，关联ecom_product.id', 2, NULL, 'admin', '2026-06-10 17:30:55.261047', 'admin', '2026-06-10 17:30:55.261047', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641448913125377', '137dd09a98fda183c9e0679c17772602', 'tag_name', 'varchar', 50, 0, 0, 0, 0, NULL, '标签名称，如：新品、热销、限时折扣、包邮', 3, NULL, 'admin', '2026-06-10 17:30:55.261427', 'admin', '2026-06-10 17:30:55.261427', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641448913125378', '137dd09a98fda183c9e0679c17772602', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 4, NULL, 'admin', '2026-06-10 17:30:55.261627', 'admin', '2026-06-10 17:30:55.261627', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486489894912', '79f5cf0cd8505d2811fca509313fc386', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_supplier_id_seq''::regclass)', '供应商ID', 1, NULL, 'admin', '2026-06-10 17:31:04.220337', 'admin', '2026-06-10 17:31:04.220337', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486489894913', '79f5cf0cd8505d2811fca509313fc386', 'name', 'varchar', 150, 0, 0, 0, 0, NULL, '供应商名称', 2, NULL, 'admin', '2026-06-10 17:31:04.22078', 'admin', '2026-06-10 17:31:04.22078', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486494089216', '79f5cf0cd8505d2811fca509313fc386', 'contact_name', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '联系人姓名', 3, NULL, 'admin', '2026-06-10 17:31:04.221099', 'admin', '2026-06-10 17:31:04.221099', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486494089217', '79f5cf0cd8505d2811fca509313fc386', 'contact_phone', 'varchar', 20, 0, 1, 0, 0, 'NULL::character varying', '联系电话', 4, NULL, 'admin', '2026-06-10 17:31:04.221335', 'admin', '2026-06-10 17:31:04.221335', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486494089218', '79f5cf0cd8505d2811fca509313fc386', 'province', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '省份', 5, NULL, 'admin', '2026-06-10 17:31:04.221486', 'admin', '2026-06-10 17:31:04.221486', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486494089219', '79f5cf0cd8505d2811fca509313fc386', 'city', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '城市', 6, NULL, 'admin', '2026-06-10 17:31:04.221633', 'admin', '2026-06-10 17:31:04.221633', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486494089220', '79f5cf0cd8505d2811fca509313fc386', 'address', 'varchar', 300, 0, 1, 0, 0, 'NULL::character varying', '详细地址', 7, NULL, 'admin', '2026-06-10 17:31:04.22177', 'admin', '2026-06-10 17:31:04.22177', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486494089221', '79f5cf0cd8505d2811fca509313fc386', 'cooperation_start', 'date', 13, 0, 1, 0, 0, NULL, '合作开始日期', 8, NULL, 'admin', '2026-06-10 17:31:04.221912', 'admin', '2026-06-10 17:31:04.221912', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486498283520', '79f5cf0cd8505d2811fca509313fc386', 'status', 'int2', 5, 0, 0, 0, 0, '1', '状态：0-停用 1-合作中', 9, NULL, 'admin', '2026-06-10 17:31:04.222116', 'admin', '2026-06-10 17:31:04.222116', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486498283521', '79f5cf0cd8505d2811fca509313fc386', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 10, NULL, 'admin', '2026-06-10 17:31:04.222324', 'admin', '2026-06-10 17:31:04.222324', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064641486498283522', '79f5cf0cd8505d2811fca509313fc386', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 11, NULL, 'admin', '2026-06-10 17:31:04.222464', 'admin', '2026-06-10 17:31:04.222464', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927288508416', 'c8a152fe88ad940b371c3a1a34197396', 'id', 'serial', 10, 0, 0, 1, 0, 'nextval(''ecom_brand_id_seq''::regclass)', '品牌ID', 1, NULL, 'admin', '2026-06-10 17:28:50.897048', 'admin', '2026-06-10 17:28:50.897048', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927292702720', 'c8a152fe88ad940b371c3a1a34197396', 'name', 'varchar', 100, 0, 0, 0, 0, NULL, '品牌名称', 2, NULL, 'admin', '2026-06-10 17:28:50.897972', 'admin', '2026-06-10 17:28:50.897972', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927296897024', 'c8a152fe88ad940b371c3a1a34197396', 'logo', 'varchar', 300, 0, 1, 0, 0, 'NULL::character varying', '品牌Logo URL', 3, NULL, 'admin', '2026-06-10 17:28:50.89884', 'admin', '2026-06-10 17:28:50.89884', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927301091328', 'c8a152fe88ad940b371c3a1a34197396', 'description', 'varchar', 500, 0, 1, 0, 0, 'NULL::character varying', '品牌描述', 4, NULL, 'admin', '2026-06-10 17:28:50.899596', 'admin', '2026-06-10 17:28:50.899596', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927301091329', 'c8a152fe88ad940b371c3a1a34197396', 'country', 'varchar', 50, 0, 1, 0, 0, 'NULL::character varying', '品牌所属国家', 5, NULL, 'admin', '2026-06-10 17:28:50.8999', 'admin', '2026-06-10 17:28:50.8999', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927305285632', 'c8a152fe88ad940b371c3a1a34197396', 'status', 'int2', 5, 0, 0, 0, 0, '1', '状态：0-禁用 1-启用', 6, NULL, 'admin', '2026-06-10 17:28:50.900166', 'admin', '2026-06-10 17:28:50.900166', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927305285633', 'c8a152fe88ad940b371c3a1a34197396', 'create_time', 'timestamp', 29, 6, 1, 0, 0, 'CURRENT_TIMESTAMP', '创建时间', 7, NULL, 'admin', '2026-06-10 17:28:50.90043', 'admin', '2026-06-10 17:28:50.90043', 0, NULL, NULL, NULL, NULL);
INSERT INTO security_tablemodel_columns (id, table_id, column_name, column_type, column_length, column_scale, is_nullable, is_primary_key, pk_position, default_value, column_comment, ordinal_position, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, field_config, dict_key) VALUES ('2064640927305285634', 'c8a152fe88ad940b371c3a1a34197396', 'modify_time', 'timestamp', 29, 6, 1, 0, 0, NULL, '修改时间', 8, NULL, 'admin', '2026-06-10 17:28:50.900684', 'admin', '2026-06-10 17:28:50.900684', 0, NULL, NULL, NULL, NULL);

INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641155529949184', 'fk_inventory_product', '2aa80fa3183ee672c7caef6077068395', 'product_id', 'ecom_product', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:29:45.31334', 'admin', '2026-06-10 17:29:45.31334', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641192871837696', 'fk_logistics_order', 'e8bd61afe4c952c2cebc1d2c095955fb', 'order_id', 'ecom_order', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:29:54.216151', 'admin', '2026-06-10 17:29:54.216151', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641258600775680', 'fk_order_customer', '5a4f29ab823bccc98be4e7a6778f33c1', 'customer_id', 'ecom_customer', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:09.887505', 'admin', '2026-06-10 17:30:09.887505', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641297091903488', 'fk_order_item_order', 'cb0167431e656f5fea9238031cb4d8e7', 'order_id', 'ecom_order', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:19.065007', 'admin', '2026-06-10 17:30:19.065007', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641297096097792', 'fk_order_item_product', 'cb0167431e656f5fea9238031cb4d8e7', 'product_id', 'ecom_product', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:19.065421', 'admin', '2026-06-10 17:30:19.065421', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641367329718272', 'fk_payment_order', 'b7ced6bfa604629ad49049b6d9f4bca9', 'order_id', 'ecom_order', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:35.810692', 'admin', '2026-06-10 17:30:35.810692', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641404344451072', 'fk_product_brand', '92aace30cdf314bb84a9c8eb94729cf9', 'brand_id', 'ecom_brand', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:44.635936', 'admin', '2026-06-10 17:30:44.635936', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641404348645376', 'fk_product_category', '92aace30cdf314bb84a9c8eb94729cf9', 'category_id', 'ecom_category', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:44.636607', 'admin', '2026-06-10 17:30:44.636607', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641404348645377', 'fk_product_supplier', '92aace30cdf314bb84a9c8eb94729cf9', 'supplier_id', 'ecom_supplier', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:44.636895', 'admin', '2026-06-10 17:30:44.636895', 0, NULL, NULL);
INSERT INTO security_tablemodel_foreign_keys (id, constraint_name, table_id, column_name, referenced_table_name, referenced_column_name, update_rule, delete_rule, data_type, remark, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064641448963457024', 'fk_product_tag_product', '137dd09a98fda183c9e0679c17772602', 'product_id', 'ecom_product', 'id', '3', '3', 0, NULL, NULL, 'admin', '2026-06-10 17:30:55.273569', 'admin', '2026-06-10 17:30:55.273569', 0, NULL, NULL);


INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064642645891989504', '2064640623016919040', '5a4f29ab823bccc98be4e7a6778f33c1', 0, NULL, 'admin', '2026-06-10 17:35:40.643043', 'admin', '2026-06-10 17:35:40.643043', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064642645908766720', '2064640623016919040', '92aace30cdf314bb84a9c8eb94729cf9', 0, NULL, 'admin', '2026-06-10 17:35:40.647282', 'admin', '2026-06-10 17:35:40.647282', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064642645912961024', '2064640623016919040', '1dc6bba94be02ce87ef7062c2b754a51', 0, NULL, 'admin', '2026-06-10 17:35:40.648686', 'admin', '2026-06-10 17:35:40.648686', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064642645917155328', '2064640623016919040', 'cb0167431e656f5fea9238031cb4d8e7', 0, NULL, 'admin', '2026-06-10 17:35:40.64954', 'admin', '2026-06-10 17:35:40.64954', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064642645921349632', '2064640623016919040', 'c8a152fe88ad940b371c3a1a34197396', 0, NULL, 'admin', '2026-06-10 17:35:40.650492', 'admin', '2026-06-10 17:35:40.650492', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643624003682304', '2064643623978516480', 'f9acf1c6096db4fbaefa927da1dfa146', 0, NULL, 'admin', '2026-06-10 17:39:33.84331', 'admin', '2026-06-10 17:39:33.84331', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643624007876608', '2064643623978516480', '5a4f29ab823bccc98be4e7a6778f33c1', 0, NULL, 'admin', '2026-06-10 17:39:33.844597', 'admin', '2026-06-10 17:39:33.844597', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643624012070912', '2064643623978516480', 'b7ced6bfa604629ad49049b6d9f4bca9', 0, NULL, 'admin', '2026-06-10 17:39:33.84555', 'admin', '2026-06-10 17:39:33.84555', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643624016265216', '2064643623978516480', 'cb0167431e656f5fea9238031cb4d8e7', 0, NULL, 'admin', '2026-06-10 17:39:33.846456', 'admin', '2026-06-10 17:39:33.846456', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643702676242432', '2064643311549005824', '1dc6bba94be02ce87ef7062c2b754a51', 0, NULL, 'admin', '2026-06-10 17:39:52.600635', 'admin', '2026-06-10 17:39:52.600635', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643702684631040', '2064643311549005824', '2aa80fa3183ee672c7caef6077068395', 0, NULL, 'admin', '2026-06-10 17:39:52.602057', 'admin', '2026-06-10 17:39:52.602057', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643702688825344', '2064643311549005824', '92aace30cdf314bb84a9c8eb94729cf9', 0, NULL, 'admin', '2026-06-10 17:39:52.603218', 'admin', '2026-06-10 17:39:52.603218', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064643702693019648', '2064643311549005824', '79f5cf0cd8505d2811fca509313fc386', 0, NULL, 'admin', '2026-06-10 17:39:52.604664', 'admin', '2026-06-10 17:39:52.604664', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644050480513024', '2064644050455347200', '5a4f29ab823bccc98be4e7a6778f33c1', 0, NULL, 'admin', '2026-06-10 17:41:15.523027', 'admin', '2026-06-10 17:41:15.523027', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644050493095936', '2064644050455347200', 'e8bd61afe4c952c2cebc1d2c095955fb', 0, NULL, 'admin', '2026-06-10 17:41:15.526064', 'admin', '2026-06-10 17:41:15.526064', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644050497290240', '2064644050455347200', 'f9acf1c6096db4fbaefa927da1dfa146', 0, NULL, 'admin', '2026-06-10 17:41:15.5273', 'admin', '2026-06-10 17:41:15.5273', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644309264875520', '2064644309227126784', '1dc6bba94be02ce87ef7062c2b754a51', 0, NULL, 'admin', '2026-06-10 17:42:17.222207', 'admin', '2026-06-10 17:42:17.222207', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644309273264128', '2064644309227126784', '92aace30cdf314bb84a9c8eb94729cf9', 0, NULL, 'admin', '2026-06-10 17:42:17.224276', 'admin', '2026-06-10 17:42:17.224276', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644309277458432', '2064644309227126784', 'cb0167431e656f5fea9238031cb4d8e7', 0, NULL, 'admin', '2026-06-10 17:42:17.225419', 'admin', '2026-06-10 17:42:17.225419', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644309281652736', '2064644309227126784', '5a4f29ab823bccc98be4e7a6778f33c1', 0, NULL, 'admin', '2026-06-10 17:42:17.226534', 'admin', '2026-06-10 17:42:17.226534', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644676971118592', '2064644676937564160', '5a4f29ab823bccc98be4e7a6778f33c1', 0, NULL, 'admin', '2026-06-10 17:43:44.890396', 'admin', '2026-06-10 17:43:44.890396', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644676975312896', '2064644676937564160', 'f9acf1c6096db4fbaefa927da1dfa146', 0, NULL, 'admin', '2026-06-10 17:43:44.891568', 'admin', '2026-06-10 17:43:44.891568', 0, NULL, NULL);
INSERT INTO security_business_function_table (id, business_id, table_model_id, sort_order, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2064644676979507200', '2064644676937564160', 'cb0167431e656f5fea9238031cb4d8e7', 0, NULL, 'admin', '2026-06-10 17:43:44.892455', 'admin', '2026-06-10 17:43:44.892455', 0, NULL, NULL);


