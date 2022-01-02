package pl.koral.topkaminecraftrewards;

import mc.thelblack.custominventory.CInventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.koral.topkaminecraftrewards.commands.Rewards;
import pl.koral.topkaminecraftrewards.listeners.PlayerJoin;

public final class TopkaMinecraftRewards extends JavaPlugin {

    private static TopkaMinecraftRewards instance;
    private CInventoryManager inventoryManager;
    private Database database;

    @Override
    public void onEnable() {
        setInstance(this);
        saveDefaultConfig();
        setDatabase();
        inventoryManager = new CInventoryManager(this);


        Bukkit.getPluginManager().registerEvents(new PlayerJoin(), this);
        getCommand("rewards").setExecutor(new Rewards());
    }

    @Override
    public void onDisable() {

    }

    public CInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    private void setDatabase(){

        database = new Database()
                .setUsername(getConfig().getString("database.username"))
                .setPassword(getConfig().getString("database.password"))
                .setJdbcUrl(getConfig().getString("database.jdbcurl"))
                .setup();
        database.createTable();

    }

    public Database getDatabase() {
        return database;
    }



    private static void setInstance(TopkaMinecraftRewards inst){
        TopkaMinecraftRewards.instance = inst;
    }

    public static TopkaMinecraftRewards getInstance() {
        return instance;
    }
}
