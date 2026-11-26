package io.qa.ui.db;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.SQLException;

final class AllureSqlLogger implements SqlLogger {

    @Override
    public void logAfterExecution(StatementContext context) {
        step(context, null);
    }

    @Override
    public void logException(StatementContext context, SQLException ex) {
        step(context, ex);
    }

    private static void step(StatementContext context, SQLException error) {
        String sql = context.getRenderedSql().replaceAll("\\s+", " ").trim();
        String params = String.valueOf(context.getBinding());
        if (error != null) {
            Allure.step("БД (ошибка): " + sql + " | " + params + " | " + error.getMessage(), Status.FAILED);
            return;
        }
        Allure.step("БД: " + sql, () -> Allure.addAttachment("Параметры запроса", params));
    }
}
