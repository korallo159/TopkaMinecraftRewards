package pl.koral.topkaminecraftrewards.commands;

import mc.thelblack.custominventory.CInventoryManager;
import mc.thelblack.custominventory.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.koral.topkaminecraftrewards.Database;
import pl.koral.topkaminecraftrewards.Reward;
import pl.koral.topkaminecraftrewards.TopkaMinecraftRewards;
import pl.koral.topkaminecraftrewards.model.Votes;
import pl.koral.topkaminecraftrewards.util.Cooldown;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Rewards implements TabExecutor {

    private final CInventoryManager inventoryManager = TopkaMinecraftRewards.getInstance().getInventoryManager();
    private final Configuration config = TopkaMinecraftRewards.getInstance().getConfig();
    private final Database db = TopkaMinecraftRewards.getInstance().getDatabase();
    private final Cooldown cooldown = new Cooldown();
    final static int SLOT_INFO = 4;
    final static int SLOT_DAILY = 11;
    final static int SLOT_WEEKLY = 13;
    final static int SLOT_MONTHLY = 15;
    final static String prefix = ChatColor.GOLD + "[TopkaMinecraft.pl]";


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player p = (Player) sender;
        if (cooldown.hasCooldown(p)) {
            p.sendMessage(prefix + " Musisz jeszcze poczekać chwilę zanim to zrobisz");
            return true;
        }
        cooldown.setCooldown(p, 500);
        CompletableFuture<Inventory> prepareInventory = CompletableFuture.supplyAsync(() -> {
            Votes votes = new Votes().fetchData(config.getLong("vote.id"), 1);
            long count = votes.getLikesHistory().keySet().stream().filter(name -> name.equals(p.getName())).count();

            Instant now = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toInstant();
            Duration diffDaily = Duration.between(now, Instant.ofEpochMilli(db.getPlayerLastVote(p.getPlayer(), Reward.DAILY)).atZone(ZoneId.systemDefault()).plusDays(1).toInstant());
            Duration diffWeekly = Duration.between(now, Instant.ofEpochMilli(db.getPlayerLastVote(p.getPlayer(), Reward.WEEKLY)).atZone(ZoneId.systemDefault()).plusDays(7).toInstant());
            Duration diffMonthly = Duration.between(now, Instant.ofEpochMilli(db.getPlayerLastVote(p.getPlayer(), Reward.MONTHLY)).atZone(ZoneId.systemDefault()).plusDays(30).toInstant());

            ItemStack info = ItemBuilder.builder().setType(Material.BOOK).accessItemMeta(ItemMeta.class, im -> {
                im.setDisplayName(ChatColor.YELLOW + "Głosuj i zdobywaj nagrody!");
                im.setLore(Arrays.asList(ChatColor.GOLD + "Kliknij aby otrzymać link głosowania!",
                        ChatColor.GRAY + "1. Wejdź na stronę TopkaMinecraft.pl",
                        ChatColor.GRAY + "2. Zagłosuj na nasz serwer",
                        ChatColor.GRAY + "3. Odbierz nagrodę!",
                        ChatColor.YELLOW + "Głosując systematycznie możesz odebrać nagrodę",
                        ChatColor.YELLOW + "tygodniową oraz miesięczną!"));
            }).build();
            ItemStack daily = ItemBuilder.builder().setType(Material.valueOf(config.getString("rewards.DAILY.item"))).accessItemMeta(ItemMeta.class, im -> {
                im.setDisplayName(ChatColor.YELLOW + "Odbierz dzienną nagrodę!");
                List<String> lore = config.getStringList("rewards.DAILY.lore");

                if (count == 0) {
                    lore.add(ChatColor.GRAY + "Musisz zagłosować na nasz serwer, aby odebrać nagrodę");
                    im.setLore(formatLore(lore));
                } else if (db.didVote(p, Reward.DAILY)) {
                    lore.add(ChatColor.GRAY + "Zagłosuj i odbierz za: " + ChatColor.GOLD + diffDaily.toHours() + "h" + diffDaily.minusHours(diffDaily.toHours()).toMinutes() + "m");
                    im.setLore(formatLore(lore));
                } else {
                    lore.add(ChatColor.GREEN + "Można odebrać!");
                    im.setLore(formatLore(lore));
                    im.addEnchant(Enchantment.DURABILITY, 1, false);
                    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }).build();
            ItemStack weekly = ItemBuilder.builder().setType(Material.valueOf(config.getString("rewards.WEEKLY.item"))).accessItemMeta(ItemMeta.class, im -> {
                im.setDisplayName(ChatColor.YELLOW + "Odbierz tygodniową nagrodę!");
                List<String> lore = config.getStringList("rewards.WEEKLY.lore");

                if (db.didVote(p, Reward.WEEKLY) && db.getPlayerLastVote(p, Reward.WEEKLY) > 7) {
                    lore.add(ChatColor.GRAY + "Zagłosuj i odbierz za: " + ChatColor.GOLD + diffWeekly.toDays() + "d" + diffWeekly.minusDays(diffWeekly.toDays()).toHours() + "h" + diffWeekly.minusHours(diffWeekly.toHours()).toMinutes() + "m");
                    im.setLore(formatLore(lore));
                } else if (db.didVote(p, Reward.WEEKLY)) {
                    lore.addAll(Arrays.asList(ChatColor.RED + "Zagłosuj i odbierz za: " + (7 - db.getPlayerVotesInARow(p) + " razy"), ChatColor.RED + "aby odebrać nagrodę tygodniową"));
                    im.setLore(formatLore(lore));
                } else {
                    lore.add(ChatColor.GREEN + "Można odebrać!");
                    im.setLore(formatLore(lore));
                    im.addEnchant(Enchantment.DURABILITY, 1, false);
                    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }).build();
            ItemStack monthly = ItemBuilder.builder().setType(Material.valueOf(config.getString("rewards.MONTHLY.item"))).accessItemMeta(ItemMeta.class, im -> {
                im.setDisplayName(ChatColor.YELLOW + "Odbierz miesięczną nagrodę!");
                List<String> lore = config.getStringList("rewards.MONTHLY.lore");
                if (db.didVote(p, Reward.MONTHLY) && db.getPlayerLastVote(p, Reward.MONTHLY) > 30) {
                    lore.add(ChatColor.GRAY + "Odbierz za: " + ChatColor.GOLD + diffMonthly.toDays() + "d" + diffMonthly.minusDays(diffMonthly.toDays()).toHours() + "h" + diffMonthly.minusHours(diffMonthly.toHours()).toMinutes() + "m");
                    im.setLore(formatLore(lore));
                } else if (db.didVote(p, Reward.MONTHLY)) {
                    lore.addAll(Arrays.asList(ChatColor.RED + "Głosuj jeszcze " + (30 - db.getPlayerVotesInARow(p)) + " razy", ChatColor.RED + "aby odebrać nagrodę miesięczną"));
                    im.setLore(formatLore(lore));
                } else {
                    lore.add(ChatColor.GREEN + "Można odebrać!");
                    im.setLore(formatLore(lore));
                    im.addEnchant(Enchantment.DURABILITY, 1, false);
                    im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

            }).build();

            return inventoryManager.builder().setRows(3)
                    .setItem(SLOT_INFO, info)
                    .setItem(SLOT_DAILY, daily)
                    .setItem(SLOT_WEEKLY, weekly)
                    .setItem(SLOT_MONTHLY, monthly)
                    .addEventInventoryClick(((player, event) -> event.setCancelled(true)))
                    .addEventInventoryClick(((player, event) -> Bukkit.getScheduler().runTaskAsynchronously(TopkaMinecraftRewards.getInstance(), () -> {
                        if (cooldown.hasCooldown(player)) {
                            p.sendMessage(prefix + " Musisz jeszcze poczekać chwilę zanim to zrobisz");
                            event.setCancelled(true);
                            return;
                        }
                        cooldown.setCooldown(player, 500);
                        if (event.getSlot() == SLOT_INFO) {
                            player.sendMessage(prefix + ChatColor.YELLOW + " " + config.getString("vote.url"));
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1, 1);
                        }
                        if (event.getSlot() == SLOT_DAILY) {
                            if (!db.didVote(player, Reward.DAILY) && count != 0) {
                                db.setVote(player, Reward.DAILY);
                                player.sendMessage(prefix + ChatColor.GREEN + " Odebrałeś nagrodę dzienną!");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
                                Bukkit.getScheduler().runTask(TopkaMinecraftRewards.getInstance(), () -> player.closeInventory());
                            } else {
                                player.sendMessage(prefix + ChatColor.RED + " Musisz jeszcze poczekać, aby odebrać nagrodę dzienną!");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                            }
                        }
                        if (event.getSlot() == SLOT_WEEKLY) {
                            if (!db.didVote(player, Reward.WEEKLY)) {
                                db.setVote(player, Reward.WEEKLY);
                                player.sendMessage(prefix + ChatColor.GREEN + " Odebrałeś nagrodę tygodniową!");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
                                Bukkit.getScheduler().runTask(TopkaMinecraftRewards.getInstance(), () -> player.closeInventory());
                            } else {
                                player.sendMessage(prefix + ChatColor.RED + " Musisz jeszcze poczekać, aby odebrać nagrodę tygodniową!");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                            }
                        }
                        if (event.getSlot() == SLOT_MONTHLY) {
                            if (!db.didVote(player, Reward.MONTHLY)) {
                                db.setVote(player, Reward.MONTHLY);
                                player.sendMessage(prefix + ChatColor.GREEN + " Odebrałeś nagrodę miesieczną!");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
                                Bukkit.getScheduler().runTask(TopkaMinecraftRewards.getInstance(), () -> player.closeInventory());
                            } else {
                                player.sendMessage(prefix + ChatColor.RED + " Musisz jeszcze poczekać, aby odebrać nagrodę miesieczną!");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                            }
                        }
                    })))
                    .setTitle(ChatColor.GOLD + "§lNagrody TopkaMinecraft.pl")
                    .build();
        });

        try {
            Inventory inv = prepareInventory.get(2500, TimeUnit.MILLISECONDS);
            p.openInventory(inv);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }


    private List<String> formatLore(List<String> l) {
        List<String> n = new ArrayList<>();
        l.forEach(element -> {
            n.add(element.replace("&", "§"));
        });

        return n;
    }
}
