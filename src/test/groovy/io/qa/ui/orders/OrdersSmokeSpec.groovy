package io.qa.ui.orders

import io.qa.ui.BaseSpec
import io.qa.ui.component.pagination.PageSize
import io.qa.ui.page.orders.OrderStatus
import io.qa.ui.page.orders.OrdersPage
import io.qa.ui.testdata.OrderSeed
import io.qa.ui.testdata.OrdersBackdoor
import io.qa.ui.testdata.TestData
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import spock.lang.Shared

@Epic("Реестры")
@Feature("Orders: базовые инварианты")
class OrdersSmokeSpec extends BaseSpec {

    @Shared
    OrdersBackdoor backdoor = TestData.orders()

    @Shared
    String runTag = TestData.runTag()

    def setupSpec() {
        backdoor.createAll((1..25).collect { index ->
            OrderSeed.builder()
                    .customer("$runTag-ACME")
                    .status(index % 2 == 0 ? OrderStatus.PAID : OrderStatus.NEW)
                    .total("${1000 + index}.00")
                    .build()
        })
    }

    def cleanupSpec() {
        backdoor.deleteByCustomer(runTag)
    }

    def "реестр заказов открывается и показывает данные прогона"() {
        given: "открыт реестр заказов"
        def page = OrdersPage.open()

        when: "применён фильтр по клиенту текущего прогона"
        page.filterBy { filters -> filters.customer().apply(runTag) }

        then: "таблица содержит только заказы этого прогона"
        def orders = page.visibleOrders()
        verifyAll {
            !orders.isEmpty()
            orders.every { it.customer().startsWith(runTag) }
            !page.isEmptyStateShown()
        }
    }

    def "размер страницы управляет количеством строк и счётчиком записей"() {
        given: "открыт реестр, отфильтрованный по данным прогона"
        def page = OrdersPage.open().filterBy { filters -> filters.customer().apply(runTag) }

        when: "выбран размер страницы 10"
        page.setPageSize(PageSize.SIZE_10)

        then: "на странице ровно 10 строк и счётчик согласован с таблицей"
        def summary = page.paginationSummary()
        def orders = page.visibleOrders()
        verifyAll {
            page.selectedPageSize() == PageSize.SIZE_10
            orders.size() == PageSize.SIZE_10.value()
            summary.size() == orders.size()
            summary.from() == 1
            summary.total() == 25
        }

        when: "выполнен переход на вторую страницу"
        def firstPageNumbers = orders*.number()
        page.goToPage(2)

        then: "набор строк полностью обновился, а счётчик сдвинулся на размер страницы"
        def secondPage = page.visibleOrders()
        def secondSummary = page.paginationSummary()
        verifyAll {
            page.currentPage() == 2
            secondSummary.from() == 11
            secondSummary.to() == 20
            secondPage*.number().intersect(firstPageNumbers).isEmpty()
        }
    }

    def "пустое состояние показывается, когда под фильтр не подходит ни одна запись"() {
        given: "открыт реестр заказов"
        def page = OrdersPage.open()

        when: "применён фильтр по заведомо несуществующему клиенту"
        page.filterBy { filters -> filters.customer().apply("$runTag-NO-SUCH-CUSTOMER") }

        then: "таблица пуста, счётчик показывает ноль записей"
        verifyAll {
            page.isEmptyStateShown()
            page.visibleOrders().isEmpty()
            page.paginationSummary().total() == 0
        }
    }
}
