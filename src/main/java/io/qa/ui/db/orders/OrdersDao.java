package io.qa.ui.db.orders;

import io.qa.ui.testdata.OrderSeed;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(OrderRecord.class)
public interface OrdersDao {

    String COLUMNS = "number, customer, status, total, created_at";

    @SqlQuery("SELECT " + COLUMNS + " FROM orders WHERE number = :number")
    Optional<OrderRecord> findByNumber(String number);

    @SqlQuery("SELECT " + COLUMNS + " FROM orders WHERE customer LIKE :customerPrefix || '%' ORDER BY number")
    List<OrderRecord> findByCustomerPrefix(String customerPrefix);

    @SqlQuery("SELECT count(*) FROM orders WHERE customer LIKE :customerPrefix || '%'")
    int countByCustomerPrefix(String customerPrefix);

    @SqlUpdate("INSERT INTO orders (" + COLUMNS + ") VALUES (:number, :customer, :status, :total, :createdAt)")
    void insert(@BindMethods OrderSeed seed);

    @SqlUpdate("DELETE FROM orders WHERE customer LIKE :customerPrefix || '%'")
    int deleteByCustomerPrefix(String customerPrefix);
}
