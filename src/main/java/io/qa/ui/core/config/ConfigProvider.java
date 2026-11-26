package io.qa.ui.core.config;

import org.aeonbits.owner.ConfigFactory;

public final class ConfigProvider {

    private static final String ENV = resolveEnv();
    private static final FrameworkConfig CONFIG = build();

    private ConfigProvider() {
    }

    private static String resolveEnv() {
        String env = System.getProperty("env", System.getenv("ENV"));
        if (env == null || env.isBlank()) {
            throw new IllegalStateException("Не указан стенд. Запустите с -Denv=dev или -Denv=ift.");
        }
        return env;
    }

    private static FrameworkConfig build() {
        ConfigFactory.setProperty("env", ENV);
        FrameworkConfig config = ConfigFactory.create(FrameworkConfig.class, System.getProperties());
        if (config.baseUrl() == null || config.baseUrl().isBlank()) {
            throw new IllegalStateException("Стенд '" + ENV + "' не настроен: не задан BASE_URL. "
                    + "Локально: нет файла src/main/resources/.env." + ENV + " (или в нём нет BASE_URL). "
                    + "В Jenkins: передайте BASE_URL как переменную окружения.");
        }
        return config;
    }

    /** Immutable-после-инициализации: единственное статическое состояние во фреймворке. */
    public static FrameworkConfig get() {
        return CONFIG;
    }

    public static String env() {
        return ENV;
    }
}
