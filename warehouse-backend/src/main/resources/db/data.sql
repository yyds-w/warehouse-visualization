-- 演示数据（id 显式指定，保证确定性；幂等写法 ON DUPLICATE KEY UPDATE，重启不会重复插入）
-- 货位与库存由 DataInitializer 在首次启动时按货架参数生成。

-- 仓库
INSERT INTO `warehouse` (`id`, `name`, `code`, `address`, `area`, `usable_area`, `manager_id`, `status`, `layout_id`, `created_at`, `updated_at`, `deleted`) VALUES
(1, '华东中心仓', 'WH01', '上海市青浦区物流大道 88 号', 12000.00, 9600.00, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, '华南备件仓', 'WH02', '广州市黄埔区保税区 3 号库', 6000.00, 4800.00, 3, 1, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 用户/库管
INSERT INTO `warehouse_user` (`id`, `username`, `real_name`, `phone`, `role`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'admin', '系统管理员', '13800000001', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'zhangsan', '张三', '13800000002', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'lisi', '李四', '13800000003', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 用户-仓库关联
INSERT INTO `warehouse_user_relation` (`id`, `warehouse_id`, `user_id`, `created_at`) VALUES
(1, 1, 2, CURRENT_TIMESTAMP),
(2, 2, 3, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 子仓库（坐标对应平面图 1200x800）
INSERT INTO `sub_warehouse` (`id`, `warehouse_id`, `name`, `code`, `area`, `position_x`, `position_y`, `width`, `height`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 'A区-整托区', 'A01', 4000.00, 50.00, 50.00, 400.00, 300.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 'B区-拆零区', 'B01', 3000.00, 500.00, 50.00, 300.00, 300.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 'C区-冷链区', 'C01', 2500.00, 50.00, 400.00, 300.00, 250.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 2, 'A区-备件区', 'A01', 4800.00, 50.00, 50.00, 500.00, 400.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 平面图（WH01，layout_data 描述区块元素）
INSERT INTO `warehouse_layout` (`id`, `warehouse_id`, `name`, `width`, `height`, `bg_image`, `layout_data`, `source_type`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, '华东中心仓一层平面图', 1200.00, 800.00, NULL,
'{"elements":[{"type":"rect","id":"zone-1","x":50,"y":50,"width":400,"height":300,"label":"A区-整托区"},{"type":"rect","id":"zone-2","x":500,"y":50,"width":300,"height":300,"label":"B区-拆零区"},{"type":"rect","id":"zone-3","x":50,"y":400,"width":300,"height":250,"label":"C区-冷链区"}]}',
1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 货架（货位由 DataInitializer 按 row*bay*col*layer 生成）
INSERT INTO `shelf` (`id`, `warehouse_id`, `sub_warehouse_id`, `name`, `code`, `type`, `row_count`, `bay_count`, `col_count`, `layer_count`, `position_x`, `position_y`, `width`, `height`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 1, 1, '整托货架 1 号', 'S01', 2, 2, 1, 4, 3, 80.00, 80.00, 150.00, 200.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 1, 1, '整托货架 2 号', 'S02', 2, 2, 1, 4, 3, 260.00, 80.00, 150.00, 200.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 1, 2, '拆零货架 1 号', 'S03', 1, 1, 1, 5, 3, 530.00, 80.00, 240.00, 200.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 2, 4, '备件货架 1 号', 'S01', 1, 1, 1, 6, 4, 80.00, 80.00, 200.00, 150.00, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 商品
INSERT INTO `goods` (`id`, `sku_code`, `name`, `category`, `spec`, `unit`, `status`, `created_at`, `updated_at`, `deleted`) VALUES
(1, 'SKU-1001', '液晶显示模组', '电子料', '15.6 寸 / FHD', '件', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(2, 'SKU-1002', '锂电池组', '电子料', '48V 20Ah', '组', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(3, 'SKU-2001', '纸箱包装套件', '包材', '600x400x400mm', '套', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(4, 'SKU-3001', '冷冻保鲜盒', '冷链耗材', '20L 食品级', '个', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(5, 'SKU-4001', '减速电机', '备件', '220V 0.75kW', '台', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(6, 'SKU-1101', '无线蓝牙耳机', '电子产品', 'TWS / 蓝牙5.3', '件', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(7, 'SKU-1102', '智能手表', '电子产品', '46mm / GPS', '件', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(8, 'SKU-2101', '纯棉短袖T恤', '服装', 'L码 / 白色', '件', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(9, 'SKU-2102', '加厚羽绒服', '服装', 'XL码 / 黑色', '件', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(10, 'SKU-3101', '每日坚果礼盒', '食品', '750g / 30包', '盒', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(11, 'SKU-3102', '有机纯牛奶', '食品', '250ml x 12', '箱', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(12, 'SKU-4101', '抽式纸巾', '日用品', '3层120抽 x 6', '提', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(13, 'SKU-4102', '洗衣液', '日用品', '3kg 瓶装', '瓶', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(14, 'SKU-5101', '冷轧钢板', '原材料', '2mm x 1m x 2m', '张', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
(15, 'SKU-5102', 'ABS塑料颗粒', '原材料', '25kg/袋', '袋', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
