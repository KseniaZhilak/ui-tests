package io.qa.ui.orders

import io.qa.ui.BaseSpec
import io.qa.ui.component.filter.DateRange
import io.qa.ui.component.pagination.PageSize
import io.qa.ui.component.table.SortDirection
import io.qa.ui.page.orders.OrderRow
import io.qa.ui.page.orders.OrderStatus
import io.qa.ui.page.orders.OrdersColumn
import io.qa.ui.page.orders.OrdersFilters
import io.qa.ui.page.orders.OrdersPage
import io.qa.ui.testdata.OrderSeed
import io.qa.ui.testdata.OrdersBackdoor
import io.qa.ui.testdata.TestData
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import spock.lang.Shared
import spock.lang.Unroll

import java.time.LocalDate

@Epic("Реестры")
@Feature("Orders: фильтрация, сортировка, пагинация")
class OrdersFilteringSpec extends BaseSpec {

    @Shared
    OrdersBackdoor backdoor = TestData.orders()

    @Shared
    String runTag = TestData.runTag()

    @Shared
    LocalDate today = LocalDate.now()

    def setupSpec() {
        // Данные прогона: ожидаемые значения заданы здесь, а не вычислены из UI
        backdoor.createAll([
                OrderSeed.builder().customer("$runTag-ACME").status(OrderStatus.PAID).total("500.00").createdAt(today).build(),
                OrderSeed.builder().customer("$runTag-ACME").status(OrderStatus.PAID).total("1500.00").createdAt(today.minusDays(1)).build(),
                OrderSeed.builder().customer("$runTag-GLOBEX").status(OrderStatus.NEW).total("250.00").createdAt(today.minusDays(10)).build(),
                OrderSeed.builder().customer("$runTag-GLOBEX").status(OrderStatus.SHIPPED).total("3000.00").createdAt(today.minusDays(20)).build(),
                OrderSeed.builder().customer("$runTag-INITECH").status(OrderStatus.CANCELLED).total("99.00").createdAt(today.minusDays(40)).build()
        ])
    }

    def cleanupSpec() {
        backdoor.deleteByCustomer(runTag)
    }

    @Unroll("фильтрация реестра заказов: #label")
    def "результат фильтрации соответствует заданному фильтру"() {
        given: "открыт реестр заказов"
        def page = OrdersPage.open()

        when: "применён фильтр прогона и проверяемый фильтр"
        page.filterBy { OrdersFilters filters ->
            filters.customer().apply(runTag)
            extraFilter.call(filters)
        }

        then: "каждая строка удовлетворяет условию фильтра"
        def orders = page.visibleOrders()
        verifyAll {
            orders.size() == expectedCount
            orders.every { OrderRow row -> expectation.call(row) }
            page.paginationSummary().total() == expectedCount
        }

        where:
        label                           | extraFilter                                                                               | expectedCount | expectation
        "только оплаченные"             | { it.status().apply(EnumSet.of(OrderStatus.PAID)) }                                       | 2             | { OrderRow r -> r.status() == OrderStatus.PAID }
        "оплаченные и отгруженные"      | { it.status().apply(EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED)) }                  | 3             | { OrderRow r -> r.status() in [OrderStatus.PAID, OrderStatus.SHIPPED] }
        "созданные за последние 7 дней" | { it.createdAt().apply(DateRange.of(today.minusDays(7), today)) }                         | 2             | { OrderRow r -> !r.createdAt().isBefore(today.minusDays(7)) }
        "клиент GLOBEX"                 | { it.customer().apply("$runTag-GLOBEX") }                                                 | 2             | { OrderRow r -> r.customer().endsWith("GLOBEX") }
        "GLOBEX + статус NEW"           | { it.customer().apply("$runTag-GLOBEX"); it.status().apply(EnumSet.of(OrderStatus.NEW)) } | 1             | { OrderRow r -> r.customer().endsWith("GLOBEX") && r.status() == OrderStatus.NEW }
    }

    @Unroll("сортировка по колонке #column (#direction) упорядочивает строки")
    def "сортировка применяется к данным реестра"() {
        given: "открыт реестр, отфильтрованный по данным прогона"
        def page = OrdersPage.open().filterBy { filters -> filters.customer().apply(runTag) }

        when: "включена сортировка по колонке"
        page.sortBy(column, direction)

        then: "строки упорядочены и состояние колонки отражено в UI"
        def actual = page.visibleOrders().collect(extractor)
        def expected = direction == SortDirection.ASCENDING ? actual.sort(false) : actual.sort(false).reverse()
        verifyAll {
            actual == expected
            page.currentSort(column) == direction
        }

        where:
        column                  | direction                | extractor
        OrdersColumn.TOTAL      | SortDirection.ASCENDING  | { OrderRow r -> r.total() }
        OrdersColumn.TOTAL      | SortDirection.DESCENDING | { OrderRow r -> r.total() }
        OrdersColumn.CREATED_AT | SortDirection.ASCENDING  | { OrderRow r -> r.createdAt() }
    }

    def "фильтр и пагинация работают совместно"() {
        given: "открыт реестр с размером страницы 10"
        def page = OrdersPage.open().setPageSize(PageSize.SIZE_10)

        when: "применён фильтр по данным прогона"
        page.filterBy { filters -> filters.customer().apply(runTag) }

        and: "сброшен фильтр"
        def filteredTotal = page.paginationSummary().total()
        page.resetFilters()

        then: "после сброса записей не меньше, чем было под фильтром"
        verifyAll {
            filteredTotal == 5
            page.paginationSummary().total() >= filteredTotal
        }
    }
}
