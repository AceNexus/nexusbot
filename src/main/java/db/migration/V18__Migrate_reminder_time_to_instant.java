package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * V18: 將舊資料的 reminder_time 轉換為 reminder_time_instant
 * 此 Java-based migration 兼容 H2 和 MySQL 資料庫
 * <p>
 * 轉換邏輯：
 * - H2: 使用 DATEDIFF 函數
 * - MySQL: 使用 UNIX_TIMESTAMP 函數
 */
public class V18__Migrate_reminder_time_to_instant extends BaseJavaMigration {

    private static final Logger logger = LoggerFactory.getLogger(V18__Migrate_reminder_time_to_instant.class);

    @Override
    public void migrate(Context context) throws Exception {
        String databaseProductName = context.getConnection().getMetaData().getDatabaseProductName();
        logger.info("Executing V18 migration on database: {}", databaseProductName);

        String updateSql;

        if (databaseProductName.toLowerCase().contains("h2")) {
            // H2 語法
            updateSql = """
                    UPDATE reminders
                    SET reminder_time_instant = DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', reminder_time)
                    WHERE reminder_time_instant IS NULL AND reminder_time IS NOT NULL
                    """;
            logger.info("Using H2 syntax for migration");

        } else if (databaseProductName.toLowerCase().contains("mysql") ||
                databaseProductName.toLowerCase().contains("mariadb")) {
            // MySQL/MariaDB 語法
            updateSql = """
                    UPDATE reminders
                    SET reminder_time_instant = UNIX_TIMESTAMP(reminder_time) * 1000
                    WHERE reminder_time_instant IS NULL AND reminder_time IS NOT NULL
                    """;
            logger.info("Using MySQL/MariaDB syntax for migration");

        } else {
            throw new IllegalStateException("Unsupported database: " + databaseProductName);
        }

        try (PreparedStatement statement = context.getConnection().prepareStatement(updateSql)) {
            int updatedRows = statement.executeUpdate();
            logger.info("V18 migration completed: {} rows updated", updatedRows);
        }

        // 驗證遷移結果
        String checkSql = "SELECT COUNT(*) FROM reminders WHERE reminder_time_instant IS NULL";
        try (PreparedStatement statement = context.getConnection().prepareStatement(checkSql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                long nullCount = rs.getLong(1);
                if (nullCount > 0) {
                    logger.warn("Warning: {} reminders still have NULL reminder_time_instant after migration", nullCount);
                } else {
                    logger.info("Verification passed: All reminders have reminder_time_instant value");
                }
            }
        }
    }
}
