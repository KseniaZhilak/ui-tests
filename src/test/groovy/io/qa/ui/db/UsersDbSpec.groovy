package io.qa.ui.db

import io.qa.ui.db.users.UsersDao
import io.qa.ui.page.users.UserRole
import io.qa.ui.testdata.TestData
import io.qa.ui.testdata.UserSeed
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import spock.lang.Requires
import spock.lang.Shared
import spock.lang.Specification

import java.time.LocalDate

@Epic("База данных")
@Feature("Пользователи: шаблон проверок в БД")
@Requires({ Db.isConfigured() })
class UsersDbSpec extends Specification {

    @Shared
    UsersDao usersDb = Db.dao(UsersDao)

    @Shared
    String runTag = TestData.runTag()

    @Shared
    String runEmails = runTag.toLowerCase(Locale.ROOT)

    def cleanupSpec() {
        usersDb.deleteByEmailPrefix(runEmails)
    }

    def "созданный пользователь сохраняется со всеми полями и значениями по умолчанию"() {
        given: "данные нового менеджера"
        def seed = UserSeed.builder(runTag)
                .fullName("Иван Петров")
                .role(UserRole.MANAGER)
                .lastLogin(LocalDate.of(2026, 1, 15))
                .build()

        when: "пользователь создан в БД"
        def id = usersDb.insert(seed)

        then: "запись читается по id и совпадает с данными"
        def saved = usersDb.findById(id).orElseThrow()
        verifyAll(saved) {
            email() == seed.email()
            fullName() == "Иван Петров"
            role() == UserRole.MANAGER
            active()
            lastLogin() == LocalDate.of(2026, 1, 15)
            createdAt() != null
        }
    }

    def "деактивация снимает флаг active только у указанного пользователя"() {
        given: "два активных пользователя"
        def target = usersDb.insert(UserSeed.builder(runTag).build())
        def other = usersDb.insert(UserSeed.builder(runTag).build())

        when: "деактивирован первый"
        def updated = usersDb.deactivate(target)

        then: "изменена ровно одна строка, второй пользователь не затронут"
        verifyAll {
            updated == 1
            !usersDb.findById(target).orElseThrow().active()
            usersDb.findById(other).orElseThrow().active()
        }
    }

    def "выборка по роли возвращает только пользователей этой роли"() {
        given: "пользователи с разными ролями"
        def admin = UserSeed.builder(runTag).role(UserRole.ADMIN).build()
        [admin,
         UserSeed.builder(runTag).role(UserRole.VIEWER).build(),
         UserSeed.builder(runTag).role(UserRole.VIEWER).build()].each { usersDb.insert(it) }

        when: "запрошены администраторы прогона"
        def admins = usersDb.findByEmailPrefixAndRole(runEmails, UserRole.ADMIN)

        then: "найден ровно один — созданный в тесте"
        admins*.email() == [admin.email()]
    }

    def "email уникален: повторная вставка отклоняется базой"() {
        given: "существующий пользователь"
        def seed = UserSeed.builder(runTag).build()
        usersDb.insert(seed)

        when: "создаётся пользователь с тем же email"
        usersDb.insert(seed)

        then: "БД отклонила вставку, дубликат не появился"
        thrown(UnableToExecuteStatementException)
        usersDb.findByEmail(seed.email()).isPresent()
    }
}
