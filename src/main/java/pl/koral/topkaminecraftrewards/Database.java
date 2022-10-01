package pl.koral.topkaminecraftrewards;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.koral.topkaminecraftrewards.model.Reward;
import pl.koral.topkaminecraftrewards.model.SimpleStatament;
import pl.koral.topkaminecraftrewards.model.VoteInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static pl.koral.topkaminecraftrewards.model.Reward.*;

public class Database {

    private HikariDataSource hikari;
    private final HikariConfig hikariConfig = new HikariConfig();

    public Database setUsername(String username) {
        hikariConfig.addDataSourceProperty("user", username);
        return this;
    }

    public Database setPassword(String password) {
        hikariConfig.addDataSourceProperty("password", password);
        return this;
    }

    public Database setJdbcUrl(String jdbcUrl) {
        hikariConfig.setJdbcUrl(jdbcUrl);
        return this;
    }

    public Database setup() {
        hikariConfig.setMaxLifetime(600000); // zeby uniknac wiekszy lifetime hikari niz mysql
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setMaximumPoolSize(20);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true"); //pozwala lepiej wspolpracowac z prepared statements
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari = new HikariDataSource(hikariConfig);
        return this;
    }

    public void createTable() {
        try (Connection connection = hikari.getConnection()) {
            String query = "CREATE TABLE IF NOT EXISTS Players(UUID VARCHAR(36), DAYS_IN_A_ROW INTEGER DEFAULT 0, LAST_DAILY_REWARD_TIME BIGINT DEFAULT 0, LAST_WEEKLY_REWARD_TIME BIGINT DEFAULT 0, LAST_MONTHLY_REWARD_TIME BIGINT DEFAULT 0, PRIMARY KEY (UUID))";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void initPlayer(Player player) {
        String update = "INSERT INTO Players (UUID) VALUES (?) ON DUPLICATE KEY UPDATE UUID=?";
        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getUniqueId().toString());
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setVote(Player player, pl.koral.topkaminecraftrewards.model.Reward reward) {
        setStatement("UPDATE Players SET " + reward.toSQLString() + " =?, DAYS_IN_A_ROW=? WHERE UUID=?", statement -> {
            statement.setLong(1, System.currentTimeMillis());

            if (reward == DAILY) { // Zwieksz/zresetuj glosowanie z rzedu tylko gdy jest odbierane daily, jezeli nie, to przepisz wartosc.
                if (isInARow(player))
                    statement.setInt(2, getPlayerVotesInARow(player) + 1);
                else statement.setInt(2, 1);
            } else statement.setInt(2, getPlayerVotesInARow(player));

            statement.setString(3, player.getUniqueId().toString());
            statement.execute();

            List<String> commands = TopkaMinecraftRewards.getInstance().getConfig().getStringList("rewards." + reward + ".commands");
            commands.replaceAll(e -> e.replace("%player%", player.getName()));
            for (String command : commands)
                Bukkit.getScheduler().runTask(TopkaMinecraftRewards.getInstance(), () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command));


        });

    }


    public VoteInfo getVoteInfo(Player player) {
        String query = "SELECT DAYS_IN_A_ROW, LAST_DAILY_REWARD_TIME, LAST_WEEKLY_REWARD_TIME, LAST_MONTHLY_REWARD_TIME FROM Players WHERE UUID=?";

        Map<String, Object> m = getResultSet(query, statement -> {
            statement.setString(1, player.getUniqueId().toString());
            statement.executeQuery();
        });
        return new VoteInfo(player.getUniqueId(), (Integer) m.get("DAYS_IN_A_ROW"), (Long) m.get("LAST_DAILY_REWARD_TIME"), (Long) m.get("LAST_WEEKLY_REWARD_TIME"), (Long) m.get("LAST_MONTHLY_REWARD_TIME"));


    }

    private void setStatement(String query, SimpleStatament simpleStatament) {
        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                simpleStatament.set(statement);
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private Map<String, Object> getResultSet(String query, SimpleStatament simpleStatament) {
        try (Connection connection = hikari.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                simpleStatament.set(statement);


                Map<String, Object> map = new HashMap<>();
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    for (int i = 1; i < resultSet.getMetaData().getColumnCount() + 1; i++) {
                        map.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
                    }
                }

                return map;

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public int getPlayerVotesInARow(Player player) {
        Map<String, Object> map = getResultSet("SELECT DAYS_IN_A_ROW FROM Players WHERE UUID=?", statement -> statement.setString(1, player.getUniqueId().toString()));
        return map.containsKey("DAYS_IN_A_ROW") ? (int) map.get("DAYS_IN_A_ROW") : 0;
    }

    private boolean isInARow(Player player) {
        Map<String, Object> map = getResultSet("SELECT LAST_DAILY_REWARD_TIME FROM Players WHERE UUID=?", statement -> statement.setString(1, player.getUniqueId().toString()));
        long lastRewarded;


        lastRewarded = map.containsKey("LAST_DAILY_REWARD_TIME") ? (long) map.get("LAST_DAILY_REWARD_TIME") : 0;
        LocalDateTime last_rewarded = Instant.ofEpochMilli(lastRewarded).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime d1 = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime().minusDays(2);
        LocalDateTime d2 = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(2);

        return last_rewarded.isAfter(d1) && last_rewarded.isBefore(d2);
    }



}
