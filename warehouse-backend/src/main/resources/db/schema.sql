-- 简简仓库可视化系统 — 建表脚本（MySQL 8 / H2 MODE=MySQL 通用）
-- 幂等写法：IF NOT EXISTS，重复执行不会报错、不会清空已有数据。
-- 字段全部反引号包裹，避免 status / type 等潜在关键字冲突。
-- 说明：唯一约束已内联在建表语句中（保证业务唯一性）；
--       普通查询索引（原设计中的 idx_*）因 MySQL 不支持 CREATE INDEX IF NOT EXISTS，
--       为保持脚本幂等此处省略，可在数据量增长后按需手动添加。

-- 仓库信息表
-- 在线绘制仓库模块扩展字段：容量 / 最大重量 / 立方数 / 库管信息 / 形状与尺寸 / 画布坐标。
-- 老库由 SchemaMigrator 启动时自动补列（MySQL 不支持 ADD COLUMN IF NOT EXISTS）。
CREATE TABLE IF NOT EXISTS `warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `address` VARCHAR(256),
  `area` DECIMAL(10,2),
  `usable_area` DECIMAL(10,2),
  `manager_id` BIGINT,
  `status` TINYINT NOT NULL DEFAULT 1,
  `layout_id` BIGINT,
  `capacity` DECIMAL(12,2),
  `max_weight` DECIMAL(12,2),
  `volume` DECIMAL(12,2),
  `manager_name` VARCHAR(64),
  `department` VARCHAR(64),
  `phone` VARCHAR(32),
  `length_m` DECIMAL(10,2),
  `width_m` DECIMAL(10,2),
  `height_m` DECIMAL(10,2),
  `shape_type` VARCHAR(16),
  `position_x` DECIMAL(10,2),
  `position_y` DECIMAL(10,2),
  `remark` VARCHAR(256),
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_warehouse_code` UNIQUE (`code`)
);

-- 仓库平面图方案表（在线绘制仓库：整张画布 = 一个方案，画布上的图形 = 各仓库）
CREATE TABLE IF NOT EXISTS `warehouse_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL DEFAULT '仓库平面图',
  `width` DECIMAL(10,2) NOT NULL DEFAULT 60,
  `height` DECIMAL(10,2) NOT NULL DEFAULT 40,
  `grid_size` DECIMAL(6,2) NOT NULL DEFAULT 1,
  `bg_image` MEDIUMTEXT,
  `plan_data` MEDIUMTEXT,
  `source_type` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 子仓库表
CREATE TABLE IF NOT EXISTS `sub_warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `area` DECIMAL(10,2),
  `position_x` DECIMAL(10,2),
  `position_y` DECIMAL(10,2),
  `width` DECIMAL(10,2),
  `height` DECIMAL(10,2),
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 用户/库管表
CREATE TABLE IF NOT EXISTS `warehouse_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `real_name` VARCHAR(64),
  `phone` VARCHAR(32),
  `role` TINYINT NOT NULL DEFAULT 2,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_user_username` UNIQUE (`username`)
);

-- 用户-仓库关联表（多对多）
CREATE TABLE IF NOT EXISTS `warehouse_user_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME,
  PRIMARY KEY (`id`)
);

-- 平面图表
CREATE TABLE IF NOT EXISTS `warehouse_layout` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `width` DECIMAL(10,2),
  `height` DECIMAL(10,2),
  `bg_image` VARCHAR(512),
  `layout_data` TEXT,
  `source_type` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 货架表
CREATE TABLE IF NOT EXISTS `shelf` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `sub_warehouse_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `code` VARCHAR(64) NOT NULL,
  `type` TINYINT NOT NULL DEFAULT 1,
  `row_count` INT NOT NULL DEFAULT 1,
  `bay_count` INT NOT NULL DEFAULT 1,
  `col_count` INT NOT NULL DEFAULT 1,
  `layer_count` INT NOT NULL DEFAULT 1,
  `position_x` DECIMAL(10,2),
  `position_y` DECIMAL(10,2),
  `width` DECIMAL(10,2),
  `height` DECIMAL(10,2),
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 货位表
CREATE TABLE IF NOT EXISTS `location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `sub_warehouse_id` BIGINT NOT NULL,
  `shelf_id` BIGINT NOT NULL,
  `location_code` VARCHAR(64) NOT NULL,
  `row_index` INT NOT NULL DEFAULT 1,
  `bay_index` INT NOT NULL DEFAULT 1,
  `col_index` INT NOT NULL DEFAULT 1,
  `layer_index` INT NOT NULL DEFAULT 1,
  `position_x` DECIMAL(10,2),
  `position_y` DECIMAL(10,2),
  `status` TINYINT NOT NULL DEFAULT 1,
  `capacity` DECIMAL(10,2),
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_location_code` UNIQUE (`location_code`)
);

-- 商品信息表
CREATE TABLE IF NOT EXISTS `goods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sku_code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `category` VARCHAR(128),
  `spec` VARCHAR(256),
  `unit` VARCHAR(32),
  `status` TINYINT NOT NULL DEFAULT 1,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_goods_sku` UNIQUE (`sku_code`)
);

-- 库存表
-- 全景可视图模块扩展字段：占用体积 / 总重量 / 保质期至 / 锁定状态（0 正常 1 出货中 2 冻结）。
-- 老库由 SchemaMigrator 启动时自动补列。
CREATE TABLE IF NOT EXISTS `inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `location_id` BIGINT NOT NULL,
  `goods_id` BIGINT NOT NULL,
  `quantity` INT NOT NULL DEFAULT 0,
  `batch_no` VARCHAR(64),
  `status` TINYINT NOT NULL DEFAULT 1,
  `volume` DECIMAL(10,2),
  `weight` DECIMAL(10,2),
  `expiry_date` DATE,
  `lock_status` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME,
  `updated_at` DATETIME,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

-- 每日进出库流量表（全景可视图-数据看板「每日进出库流量」柱状图数据源）
CREATE TABLE IF NOT EXISTS `stock_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `warehouse_id` BIGINT NOT NULL,
  `flow_date` DATE NOT NULL,
  `in_qty` INT NOT NULL DEFAULT 0,
  `out_qty` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_flow_wh_date` UNIQUE (`warehouse_id`, `flow_date`)
);
