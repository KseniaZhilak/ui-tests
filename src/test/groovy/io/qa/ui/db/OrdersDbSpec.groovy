package io.qa.ui.db

import io.qa.ui.BaseSpec
import io.qa.ui.db.orders.OrdersDao
import io.qa.ui.page.orders.OrderStatus
import io.qa.ui.page.orders.OrdersPage
import io.qa.ui.testdata.OrderSeed
import io.qa.ui.testdata.TestData
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import spock.lang.Requires
import spock.lang.Shared

@Epic("Реестры")
@Feature("Orders: согласованность UI и БД")
@Requires({ Db.isConfigured() })
class OrdersDbSpec extends BaseSpec {

    @Shared
    OrdersDao ordersDb = Db.dao(OrdersDao)

    @Shared
    String runTag = TestData.runTag()

    def cleanupSpec() {
        ordersDb.deleteByCustomerPrefix(runTag)
    }

    def "заказ, вставленный в БД, отображается в реестре"() {
        given: "в БД есть оплаченный заказ прогона"
        def seed = OrderSeed.builder().customer("$runTag-ACME").status(OrderStatus.PAID).total("750.00").build()
        ordersDb.insert(seed)

        when: "открыт реестр, отфильтрованный по данным прогона"
        def orders = OrdersPage.open()
                .filterBy { filters -> filters.customer().apply("$runTag-ACME") }
                .visibleOrders()

        then: "в реестре ровно этот заказ с теми же значениями"
        verifyAll {
            orders*.number() == [seed.number()]
            orders[0].status() == OrderStatus.PAID
            orders[0].total() == seed.total()
        }
    }

    def "заказ, созданный через backdoor-API, сохранён в БД"() {
        given: "сид заказа"
        def seed = OrderSeed.builder().customer("$runTag-GLOBEX").status(OrderStatus.SHIPPED).build()

        when: "заказ создан через API"
        TestData.orders().create(seed)

        then: "запись в БД совпадает с сидом"
        def saved = ordersDb.findByNumber(seed.number()).orElseThrow()
        verifyAll {
            saved.customer() == seed.customer()
            saved.status() == OrderStatus.SHIPPED
            saved.total() == seed.total()
            saved.createdAt() == seed.createdAt()
        }
    }
}
