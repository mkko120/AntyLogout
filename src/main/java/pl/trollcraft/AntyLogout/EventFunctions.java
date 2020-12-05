package pl.trollcraft.AntyLogout;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;

public class EventFunctions {

    private static EventFunctions instance = new EventFunctions();
    public final HashMap<Player, Float> xp = new HashMap<>();
    public final HashMap<Player, Integer> lvl = new HashMap<>();
    private final ArrayList<String> pvp = (ArrayList<String>) AntyLogout.getInstance().getConfig().getList("regiony pvp");
    private final ArrayList<String> dungeon = (ArrayList<String>) AntyLogout.getInstance().getConfig().getList("regiony dungeon");
    public static EventFunctions getInstance() {
        return instance;
    }

    public void dropItems(Player player) {
        ItemStack[] stack = player.getInventory().getContents();
        ItemStack[] armorstack = player.getInventory().getArmorContents();
        for (ItemStack itemStack : armorstack) {
            if (!(itemStack == null)) {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            }
        }
        for (ItemStack itemStack : stack) {
            if (!(itemStack == null)) {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            }
        }
    }
    public float barChanger(Player player) {
        float x = TimestampManager.getInstance().getCooldown(player);
        float z = TimestampManager.DEFAULT_COOLDOWN;
        return x / z;

    }
    public boolean getPlayerRegion(Player player, ArrayList<String> strefa) {
        Location location = player.getLocation();
        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        assert regionManager != null;
        ArrayList<ProtectedRegion> regiony = new ArrayList<>();
        for(String region_name : strefa){
            regiony.add(regionManager.getRegion(region_name));
        }

        boolean test = false;
        for (ProtectedRegion region : regiony) {
            if (region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                test = true;
            } else {
                return false;
            }
        }
        return test;
    }
    public void cooldown(Player atacker, Player victim) {
        assert pvp != null;
        assert dungeon != null;
        if (getPlayerRegion(atacker, pvp) || getPlayerRegion(victim, pvp) || getPlayerRegion(atacker, dungeon) || getPlayerRegion(victim, dungeon)) {
            TimestampManager.getInstance().setCooldown(atacker, TimestampManager.DEFAULT_COOLDOWN);
            TimestampManager.getInstance().setCooldown(victim, TimestampManager.DEFAULT_COOLDOWN);
            new BukkitRunnable() {
                @Override
                public void run() {
                    int timeLeftAtacker = TimestampManager.getInstance().getCooldown(atacker);
                    int timeLeftVictim = TimestampManager.getInstance().getCooldown(victim);
                    if (!atacker.hasPermission("antilogout.override")) {
                        TimestampManager.getInstance().setCooldown(atacker, --timeLeftAtacker);
                    }
                    if (!atacker.hasPermission("antilogout.override")) {
                        TimestampManager.getInstance().setCooldown(victim, --timeLeftVictim);
                    }
                    if(timeLeftAtacker > 0 || timeLeftVictim > 0){
                        if (xp.get(atacker) == null && lvl.get(atacker) == null || xp.get(victim) == null && lvl.get(victim) == null) {
                            xp.put(atacker, atacker.getExp());
                            xp.put(victim, victim.getExp());
                            lvl.put(atacker, atacker.getLevel());
                            lvl.put(victim, victim.getLevel());
                        }
                        atacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(Helper.color("&6[&4Anti&cLogout&6] &cJestes w walce!")));
                        victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(Helper.color("&6[&4Anti&cLogout&6] &cJestes w walce!")));
                        atacker.setExp(barChanger(atacker));
                        victim.setExp(barChanger(victim));
                        atacker.setLevel(TimestampManager.getInstance().getCooldown(atacker));
                        victim.setLevel(TimestampManager.getInstance().getCooldown(victim));
                    } else {
                        if (xp.get(atacker) != null || lvl.get(atacker) != null || xp.get(victim) != null || lvl.get(victim) != null) {
                            atacker.setExp(xp.get(atacker));
                            victim.setExp((xp.get(victim)));
                            atacker.setLevel(lvl.get(atacker));
                            victim.setLevel(lvl.get(victim));
                            xp.remove(atacker);
                            xp.remove(victim);
                            lvl.remove(atacker);
                            lvl.remove(victim);
                        }
                        atacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(Helper.color("&6[&4Anti&cLogout&6] &aNie jestes juz w walce!")));
                        victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(Helper.color("&6[&4Anti&cLogout&6] &aNie jestes juz w walce!")));
                        this.cancel();
                        TimestampManager.getInstance().setCooldown(atacker, 0);
                        TimestampManager.getInstance().setCooldown(victim, 0);
                    }
                }
            }.runTaskTimer(AntyLogout.getPlugin(AntyLogout.class), 20, 20);

        }
    }
}
