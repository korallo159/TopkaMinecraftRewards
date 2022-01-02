package pl.koral.topkaminecraftrewards.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface SimpleStatament {

    void set(PreparedStatement preparedStatement) throws SQLException;

}
