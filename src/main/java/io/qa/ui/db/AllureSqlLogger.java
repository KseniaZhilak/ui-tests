package io.qa.ui.db;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.LinkedHashMap;
import java.util.Map;

/** Каждый SQL-запрос MyBatis — отдельный шаг Allure «БД: ...» с параметрами во вложении. */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
final class AllureSqlLogger implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = statement.getBoundSql(parameter);
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        String params = String.valueOf(parameters(statement.getConfiguration(), boundSql, parameter));
        try {
            Object result = invocation.proceed();
            Allure.step("БД: " + sql, () -> Allure.addAttachment("Параметры запроса", params));
            return result;
        } catch (Throwable error) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            Allure.step("БД (ошибка): " + sql + " | " + params + " | " + cause.getMessage(), Status.FAILED);
            throw error;
        }
    }

    /** Значения в порядке плейсхолдеров #{...} — так же, как их подставит MyBatis. */
    private static Map<String, Object> parameters(Configuration configuration, BoundSql boundSql, Object parameter) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (parameter == null) {
            return values;
        }
        boolean simple = configuration.getTypeHandlerRegistry().hasTypeHandler(parameter.getClass());
        MetaObject meta = simple ? null : configuration.newMetaObject(parameter);
        for (ParameterMapping mapping : boundSql.getParameterMappings()) {
            String name = mapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(name)) {
                value = boundSql.getAdditionalParameter(name);
            } else if (simple) {
                value = parameter;
            } else {
                value = meta.getValue(name);
            }
            values.put(name, value);
        }
        return values;
    }
}
