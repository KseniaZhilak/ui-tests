package io.qa.ui.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.qa.ui.core.config.ConfigProvider;
import io.qa.ui.core.config.FrameworkConfig;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Точка входа в БД: пул HikariCP + MyBatis (только аннотации, без XML).
 *
 * <p>{@link #dao(Class)} возвращает прокси, который на каждый вызов метода открывает
 * свою {@link SqlSession} в режиме auto-commit и сразу её закрывает. Поэтому DAO можно
 * держать в {@code @Shared}-поле и звать из параллельных спек: каждый запрос — отдельная
 * транзакция, о commit/close тест не думает.
 */
public final class Db {

    private static final FrameworkConfig CONFIG = ConfigProvider.get();

    private Db() {
    }

    public static <T> T dao(Class<T> daoType) {
        Holder.register(daoType);
        Object proxy = Proxy.newProxyInstance(daoType.getClassLoader(), new Class<?>[]{daoType},
                (self, method, args) -> invoke(daoType, method, args));
        return daoType.cast(proxy);
    }

    public static boolean isConfigured() {
        String url = CONFIG.dbUrl();
        return url != null && !url.isBlank();
    }

    private static Object invoke(Class<?> daoType, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> daoType.getSimpleName() + "(Db)";
                case "hashCode" -> System.identityHashCode(daoType);
                case "equals" -> args[0] == daoType;
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
        try (SqlSession session = Holder.FACTORY.openSession(true)) {
            return method.invoke(session.getMapper(daoType), args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static final class Holder {

        private static final SqlSessionFactory FACTORY = createFactory();

        private static synchronized void register(Class<?> daoType) {
            Configuration configuration = FACTORY.getConfiguration();
            if (!configuration.hasMapper(daoType)) {
                configuration.addMapper(daoType);
            }
        }

        private static SqlSessionFactory createFactory() {
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

            Configuration configuration = new Configuration(
                    new Environment("ui-tests", new JdbcTransactionFactory(), dataSource));
            // created_at -> createdAt
            configuration.setMapUnderscoreToCamelCase(true);
            // record заполняется через конструктор по именам параметров (нужен -parameters, он включён в сборке)
            configuration.setArgNameBasedConstructorAutoMapping(true);
            // без кэша: тест всегда видит актуальное состояние БД
            configuration.setCacheEnabled(false);
            configuration.addInterceptor(new AllureSqlLogger());
            return new SqlSessionFactoryBuilder().build(configuration);
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
