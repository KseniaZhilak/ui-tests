import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.process.CommandLineArgumentProvider

plugins {
    java
    groovy
}

repositories { mavenCentral() }

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

// Совместная компиляция: groovyc компилирует И src/test/groovy, И src/test/java одним проходом,
// поэтому Groovy-спеки видят Java-классы из src/test/java (и наоборот).
// Java-классы из src/main/java видны через обычный compile classpath.
sourceSets {
    test {
        extensions.getByType(GroovySourceDirectorySet::class.java)
            .setSrcDirs(listOf("src/test/groovy", "src/test/java"))
        java.setSrcDirs(emptyList<String>())
    }
}

val allureVersion = "2.27.0"
val jacksonVersion = "2.17.1"
val jdbiVersion = "3.45.4"
val aspectjAgent: Configuration by configurations.creating

dependencies {
    implementation("com.codeborne:selenide:7.16.0")
    implementation("org.aeonbits.owner:owner-java8:1.0.12")
    implementation("io.qameta.allure:allure-java-commons:$allureVersion")
    implementation("io.qameta.allure:allure-selenide:$allureVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    implementation("org.jdbi:jdbi3-core:$jdbiVersion")
    implementation("org.jdbi:jdbi3-sqlobject:$jdbiVersion")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    testImplementation("org.codehaus.groovy:groovy:3.0.21")
    testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
    testImplementation("io.qameta.allure:allure-spock2:$allureVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    aspectjAgent("org.aspectj:aspectjweaver:1.9.22")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -parameters нужен, чтобы Allure подставлял значения в шаблоны @Step("... {column}")
    options.compilerArgs.add("-parameters")
}
tasks.withType<GroovyCompile>().configureEach {
    options.encoding = "UTF-8"
    groovyOptions.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()

    // AspectJ-агент нужен для работы аннотаций @Step/@Attachment Allure
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-javaagent:${aspectjAgent.singleFile.absolutePath}")
    })

    systemProperty(
        "allure.results.directory",
        layout.buildDirectory.dir("allure-results").get().asFile.absolutePath
    )

    // Прокидываем -D... из командной строки Gradle внутрь JVM тестов
    listOf(
        "env", "base.url", "api.base.url", "browser", "headless",
        "remote.url", "parallel", "parallel.threads", "selenoid.vnc", "selenoid.video",
        "DB_URL", "DB_USER", "DB_PASSWORD", "DB_POOL_SIZE"
    ).forEach { key -> System.getProperty(key)?.let { systemProperty(key, it) } }

    testLogging { events("passed", "failed", "skipped") }
    outputs.upToDateWhen { false }
}
