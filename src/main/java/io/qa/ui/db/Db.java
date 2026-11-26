package io.qa.ui.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.qa.ui.core.config.ConfigProvider;
import io.qa.ui.core.config.FrameworkConfig;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public final class Db {

    private static final FrameworkConfig CONFIG = ConfigProvider.get();

    private Db() {
    }

    public static <T> T dao(Class<T> daoType) {
        return Holder.JDBI.onDemand(daoType);
    }

    public static boolean isConfigured() {
        String url = CONFIG.dbUrl();
        return url != null && !url.isBlank();
    }

    private static final class Holder {

        private static final Jdbi JDBI = createJdbi();

        private static Jdbi createJdbi() {
            requireSet("DB_URL", CONFIG.dbUrl());
            requireSet("DB_USER", CONFIG.dbUser());
            requireSet("DB_PASSWORD", CONFIG.dbPassword());

            HikariConfig hikari = new HikariConfig();
            hikari.setJdbcUrl(CONFIG.dbUrl());
            hikari.setUsername(CONFIG.dbUser());
            hikari.setPassword(CONFIG.dbPassword());
            hikari.setMaximumPoolSize(CONFIG.dbPoolSize());
            hikari.setPoolName("ui-tests-db");
            HikariDataSource dataSource = new HikariDataSource(hikari);
            Runtime.getRuntime().addShutdownHook(new Thread(dataSource::close, "db-pool-shutdown"));

            Jdbi jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
            jdbi.setSqlLogger(new AllureSqlLogger());
            return jdbi;
        }

        private static void requireSet(String key, String value) {
            if (value == null || value.isBlank()) {
                String env = ConfigProvider.env();
                throw new IllegalStateException("Не задан параметр БД " + key + " для стенда '" + env + "'. "
                        + "Локально: добавьте его в файл src/main/resources/.env." + env + ". "
                        + "В Jenkins: передайте как переменную окружения.");
            }
        }
    }
}
