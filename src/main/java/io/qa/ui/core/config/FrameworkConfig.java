package io.qa.ui.core.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:.env.${env}"
})
public interface FrameworkConfig extends Config {

    @Key("BASE_URL")
    String baseUrl();

    @Key("API_BASE_URL")
    String apiBaseUrl();

    @Key("browser")
    @DefaultValue("chrome")
    String browser();

    @Key("browser.size")
    @DefaultValue("1920x1080")
    String browserSize();

    @Key("headless")
    @DefaultValue("true")
    boolean headless();

    /** Пустая строка = локальный драйвер, иначе URL Selenoid hub. */
    @Key("remote.url")
    @DefaultValue("")
    String remoteUrl();

    @Key("selenoid.vnc")
    @DefaultValue("false")
    boolean selenoidVnc();

    @Key("selenoid.video")
    @DefaultValue("false")
    boolean selenoidVideo();

    @Key("timeout.ms")
    @DefaultValue("6000")
    long timeoutMs();

    /** Таймаут для «мгновенных» проверок существования элемента (fail fast). */
    @Key("fast.timeout.ms")
    @DefaultValue("2000")
    long fastTimeoutMs();

    @Key("table.refresh.timeout.ms")
    @DefaultValue("15000")
    long tableRefreshTimeoutMs();

    @Key("DB_URL")
    String dbUrl();

    @Key("DB_USER")
    String dbUser();

    @Key("DB_PASSWORD")
    String dbPassword();

    @Key("DB_POOL_SIZE")
    @DefaultValue("5")
    int dbPoolSize();
}
