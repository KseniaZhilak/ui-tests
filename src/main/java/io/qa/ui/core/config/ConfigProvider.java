package io.qa.ui.core.config;

import org.aeonbits.owner.ConfigFactory;

public final class ConfigProvider {

    private static final FrameworkConfig CONFIG = build();

    private ConfigProvider() {
    }

    private static FrameworkConfig build() {
        String env = System.getProperty("env", System.getenv().getOrDefault("ENV", "local"));
        ConfigFactory.setProperty("env", env);
        return ConfigFactory.create(FrameworkConfig.class, System.getProperties());
    }

    /** Immutable-после-инициализации: единственное статическое состояние во фреймворке. */
    public static FrameworkConfig get() {
        return CONFIG;
    }
}
