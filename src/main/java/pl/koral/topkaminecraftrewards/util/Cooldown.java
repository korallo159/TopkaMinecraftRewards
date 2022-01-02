package pl.koral.topkaminecraftrewards.util;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class Cooldown {

    private final HashMap<String, Long> cooldown = new HashMap<>();

    public void setCooldown(Player player, Integer ms) {
        cooldown.put(player.getUniqueId().toString(), (System.currentTimeMillis() + ms));
    }

    public boolean hasCooldown(Player player) {
        return cooldown.containsKey(player.getUniqueId().toString()) && cooldown.get(player.getUniqueId().toString()) > System.currentTimeMillis();
    }
}