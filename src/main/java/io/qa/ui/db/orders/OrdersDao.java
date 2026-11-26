package io.qa.ui.db.orders;

import io.qa.ui.testdata.OrderSeed;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

public interface OrdersDao {

    String COLUMNS = "number, customer, status, total, created_at";

    @Select("SELECT " + COLUMNS + " FROM orders WHERE number = #{number}")
    Optional<OrderRecord> findByNumber(String number);

    @Select("SELECT " + COLUMNS + " FROM orders WHERE customer LIKE #{customerPrefix} || '%' ORDER BY number")
    List<OrderRecord> findByCustomerPrefix(String customerPrefix);

    @Select("SELECT count(*) FROM orders WHERE customer LIKE #{customerPrefix} || '%'")
    int countByCustomerPrefix(String customerPrefix);

    @Insert("INSERT INTO orders (" + COLUMNS + ") VALUES (#{number}, #{customer}, #{status}, #{total}, #{createdAt})")
    void insert(OrderSeed seed);

    @Delete("DELETE FROM orders WHERE customer LIKE #{customerPrefix} || '%'")
    int deleteByCustomerPrefix(String customerPrefix);
}
