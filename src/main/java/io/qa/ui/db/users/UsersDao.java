package io.qa.ui.db.users;

import io.qa.ui.page.users.UserRole;
import io.qa.ui.testdata.UserSeed;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface UsersDao {

    String COLUMNS = "id, email, full_name, role, active, last_login, created_at";

    // INSERT ... RETURNING объявлен как @Select, чтобы метод вернул сгенерированный id
    @Select("INSERT INTO users (email, full_name, role, active, last_login) "
            + "VALUES (#{email}, #{fullName}, #{role}, #{active}, #{lastLogin}) RETURNING id")
    long insert(UserSeed seed);

    @Select("SELECT " + COLUMNS + " FROM users WHERE id = #{id}")
    Optional<UserRecord> findById(long id);

    @Select("SELECT " + COLUMNS + " FROM users WHERE email = #{email}")
    Optional<UserRecord> findByEmail(String email);

    @Select("SELECT " + COLUMNS + " FROM users WHERE email LIKE #{emailPrefix} || '%' AND role = #{role} ORDER BY id")
    List<UserRecord> findByEmailPrefixAndRole(String emailPrefix, UserRole role);

    @Select("SELECT count(*) FROM users WHERE email LIKE #{emailPrefix} || '%' AND active")
    int countActive(String emailPrefix);

    @Update("UPDATE users SET active = FALSE WHERE id = #{id}")
    int deactivate(long id);

    @Delete("DELETE FROM users WHERE email LIKE #{emailPrefix} || '%'")
    int deleteByEmailPrefix(String emailPrefix);
}
