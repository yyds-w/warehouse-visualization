package com.warehouse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 老库平滑升级：warehouse 表补齐「在线绘制仓库」模块新增列。
 * schema.sql 的 CREATE TABLE IF NOT EXISTS 对已存在的表不生效，
 * MySQL 又不支持 ADD COLUMN IF NOT EXISTS，因此这里按 information_schema 逐列判断后补列。
 */
@Component
@Order(1)
public class SchemaMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    /** {列名, 列定义}，与 schema.sql 中 warehouse 表保持一致 */
    private static final List<String[]> WAREHOUSE_COLUMNS = List.of(
            new String[]{"capacity", "DECIMAL(12,2)"},
            new String[]{"max_weight", "DECIMAL(12,2)"},
            new String[]{"volume", "DECIMAL(12,2)"},
            new String[]{"manager_name", "VARCHAR(64)"},
            new String[]{"department", "VARCHAR(64)"},
            new String[]{"phone", "VARCHAR(32)"},
            new String[]{"length_m", "DECIMAL(10,2)"},
            new String[]{"width_m", "DECIMAL(10,2)"},
            new String[]{"height_m", "DECIMAL(10,2)"},
            new String[]{"shape_type", "VARCHAR(16)"},
            new String[]{"position_x", "DECIMAL(10,2)"},
            new String[]{"position_y", "DECIMAL(10,2)"},
            new String[]{"remark", "VARCHAR(256)"}
    );

    /** {列名, 列定义}，与 schema.sql 中 inventory 表保持一致 */
    private static final List<String[]> INVENTORY_COLUMNS = List.of(
            new String[]{"volume", "DECIMAL(10,2)"},
            new String[]{"weight", "DECIMAL(10,2)"},
            new String[]{"expiry_date", "DATE"},
            new String[]{"lock_status", "TINYINT NOT NULL DEFAULT 0"}
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        int added = addMissingColumns("warehouse", WAREHOUSE_COLUMNS);
        added += addMissingColumns("inventory", INVENTORY_COLUMNS);
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `stock_flow` ("
                + "`id` BIGINT NOT NULL AUTO_INCREMENT,"
                + "`warehouse_id` BIGINT NOT NULL,"
                + "`flow_date` DATE NOT NULL,"
                + "`in_qty` INT NOT NULL DEFAULT 0,"
                + "`out_qty` INT NOT NULL DEFAULT 0,"
                + "`created_at` DATETIME,"
                + "PRIMARY KEY (`id`),"
                + "CONSTRAINT `uk_flow_wh_date` UNIQUE (`warehouse_id`, `flow_date`))");
        if (added > 0) {
            log.info("[SchemaMigrator] 共补充 {} 列，升级完成", added);
        }
    }

    private int addMissingColumns(String table, List<String[]> columns) {
        int added = 0;
        for (String[] col : columns) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, col[0]);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + col[0] + "` " + col[1]);
                added++;
                log.info("[SchemaMigrator] {} 表补充列 {} {}", table, col[0], col[1]);
            }
        }
        return added;
    }
}
