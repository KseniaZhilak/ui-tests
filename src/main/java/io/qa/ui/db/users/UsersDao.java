package io.qa.ui.db.users;

import io.qa.ui.page.users.UserRole;
import io.qa.ui.testdata.UserSeed;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(UserRecord.class)
public interface UsersDao {

    String COLUMNS = "id, email, full_name, role, active, last_login, created_at";

    @SqlUpdate("INSERT INTO users (email, full_name, role, active, last_login) "
            + "VALUES (:email, :fullName, :role, :active, :lastLogin)")
    @GetGeneratedKeys("id")
    long insert(@BindMethods UserSeed seed);

    @SqlQuery("SELECT " + COLUMNS + " FROM users WHERE id = :id")
    Optional<UserRecord> findById(long id);

    @SqlQuery("SELECT " + COLUMNS + " FROM users WHERE email = :email")
    Optional<UserRecord> findByEmail(String email);

    @SqlQuery("SELECT " + COLUMNS + " FROM users WHERE email LIKE :emailPrefix || '%' AND role = :role ORDER BY id")
    List<UserRecord> findByEmailPrefixAndRole(String emailPrefix, UserRole role);

    @SqlQuery("SELECT count(*) FROM users WHERE email LIKE :emailPrefix || '%' AND active")
    int countActive(String emailPrefix);

    @SqlUpdate("UPDATE users SET active = FALSE WHERE id = :id")
    int deactivate(long id);

    @SqlUpdate("DELETE FROM users WHERE email LIKE :emailPrefix || '%'")
    int deleteByEmailPrefix(String emailPrefix);
}
