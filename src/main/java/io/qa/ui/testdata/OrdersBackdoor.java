package io.qa.ui.testdata;

import java.util.List;

/**
 * Контракт подготовки предусловий в обход UI.
 *
 * create(seed)             — создаёт ровно один заказ; возвращает состояние, сохранённое бэкендом
 *                            (бэкенд может дополнить/нормализовать поля). Бросает IllegalStateException,
 *                            если ответ не 2xx.
 * createAll(seeds)         — пакетное создание, порядок результата соответствует порядку входа.
 * deleteByCustomer(prefix) — удаляет все заказы, у которых customer начинается с prefix.
 *                            Используется для уборки данных прогона; идемпотентна.
 *
 * Реализация обязана быть thread-safe: спеки выполняются параллельно.
 */
public interface OrdersBackdoor {

    OrderSeed create(OrderSeed seed);

    List<OrderSeed> createAll(List<OrderSeed> seeds);

    void deleteByCustomer(String customerPrefix);
}
