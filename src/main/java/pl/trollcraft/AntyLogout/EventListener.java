package pl.trollcraft.AntyLogout;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pl.trollcraft.AntyLogout.PvpPoints.PVPUser;
import pl.trollcraft.AntyLogout.PvpPoints.PVPUsersController;

import java.util.ArrayList;
import java.util.HashMap;

public class EventListener implements Listener {
    private final PVPUsersController controller = AntyLogout.getPlugin(AntyLogout.class).getPvpUsersController();
    private final ArrayList<String> pvp = (ArrayList<String>) AntyLogout.getInstance().getConfig().getList("regiony pvp");
    private final ArrayList<String> dungeon = (ArrayList<String>) AntyLogout.getInstance().getConfig().getList("regiony dungeon");
    private final HashMap<Player, Float> xp = EventFunctions.getInstance().xp;
    private final HashMap<Player, Integer> lvl = EventFunctions.getInstance().lvl;

    @EventHandler
    public void onDamagePvp(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) event;
            if (ev.getEntity() instanceof Player) {
                Entity atacker = ev.getDamager();
                Player victim = ((Player) ev.getEntity()).getPlayer();
                String player1 = atacker.getName();
                String player2 = victim.getName();
                PVPUser user = controller.find(player1);
                PVPUser user2 = controller.find(player2);
                if (user == null){
                    user = new PVPUser(player1, 0, 0);
                    controller.register(user);
                }
                if (user2 == null) {
                    user2 = new PVPUser(player2, 0, 0);
                    controller.register(user2);
                }
                if (ev.getDamager() instanceof Player){
                    EventFunctions.getInstance().cooldown((Player)atacker, victim);
                } else if (ev.getDamager() instanceof Arrow) {
                    Player arrowDamager = (Player) ((Arrow) ev.getDamager()).getShooter();
                    EventFunctions.getInstance().cooldown(arrowDamager, victim);
                }
                else {
                    return;
                }
            }
        }
    }
    @EventHandler
    public void onDamageDungeon(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent ev = (EntityDamageByEntityEvent) event;
            if (ev.getEntity() instanceof Player && !(ev.getDamager() instanceof Player)) {
                String username = ev.getEntity().getName();
                PVPUser user = controller.find(username);
                if (user == null){
                    user = new PVPUser(username, 0, 0);
                    controller.register(user);
                }
                Player victim = ((Player) ev.getEntity()).getPlayer();
                assert dungeon != null;
                assert victim != null;
                if (EventFunctions.getInstance().getPlayerRegion(victim, dungeon)) {
                    if (!victim.hasPermission("antilogout.override")) {
                        TimestampManager.getInstance().setCooldown(victim, TimestampManager.DEFAULT_COOLDOWN);
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            int timeLeftVictim = TimestampManager.getInstance().getCooldown(victim);
                            if (!victim.hasPermission("antilogout.override")) {
                                TimestampManager.getInstance().setCooldown(victim, --timeLeftVictim);
                            }
                            if(timeLeftVictim > 0){
                                if (xp.get(victim) == null && lvl.get(victim) == null) {
                                    xp.put(victim, victim.getExp());
                                    lvl.put(victim, victim.getLevel());
                                }
                                victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText(Helper.color("&6[&4Anti&cLogout&6] &cJestes w walce!")));
                                victim.setExp(EventFunctions.getInstance().barChanger(victim));
                                victim.setLevel(TimestampManager.getInstance().getCooldown(victim));
                            } else {
                                if (xp.get(victim) != null && lvl.get(victim) != null) {
                                    victim.setExp(xp.get(victim));
                                    victim.setLevel(lvl.get(victim));
                                    xp.remove(victim);
                                    lvl.remove(victim);
                                }
                                this.cancel();
                                TimestampManager.getInstance().setCooldown(victim, 0);
                                victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText(Helper.color("&6[&4Anti&cLogout&6] &aNie jestes juz w walce!")));
                            }
                        }
                    }.runTaskTimer(AntyLogout.getPlugin(AntyLogout.class), 20, 20);

                }

            }
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PVPUser user = controller.find(player.getName());
        double cooldown = TimestampManager
                .getInstance()
                .getCooldown(event.getPlayer());
        if (cooldown > 0) {
            Entity damage = event.getPlayer().getLastDamageCause().getEntity();
            TimestampManager.getInstance().setCooldown((Player) damage, 0);
            EventFunctions.getInstance().dropItems(player);
            player.getInventory().clear();
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName(Helper.color("&cInformacja"));
            meta.getLore().set(1, "Lognales podczas walki wiec tracisz itemy!");
            paper.setItemMeta(meta);
            player.getInventory().setItemInMainHand(paper);
            if (damage instanceof Player) {
                int xp = (int) event.getPlayer().getExp();
                int level = event.getPlayer().getLevel();
                if (xp != 0) {
                    ((Player) damage).getPlayer().giveExp(xp);
                }
                if (level != 0){
                    ((Player) damage).getPlayer().giveExpLevels(level);
                }
            }
            Bukkit.broadcastMessage(Helper.color("&6[&4Anti&cLogout&6] &6Gracz &l" + player.getName() + "&r&6wylogowal sie podczas walki!"));
        }
        controller.unregister(user);

    }
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        double cooldown = TimestampManager
                .getInstance()
                .getCooldown(event.getPlayer());
        if (cooldown > 0) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Helper.color("&6[&4Anti&cLogout&6] &cJestes w walce! Walcz, a nie uzywasz komend!\n&6[&4Anti&cLogout&6] &cDo końca walki pozostało: " + cooldown + "s."));
        }
    }
    @EventHandler
    public void onKill (PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (!(victim.getKiller() instanceof Player)){
            return;
        }
        event.setDroppedExp(0);
        PVPUser killerUser = controller.find(killer.getName());
        assert pvp != null;
        if (EventFunctions.getInstance().getPlayerRegion(killer, pvp)) {
            PVPUser victimUser = controller.find(victim.getName());
            victimUser.addDeaths();
            killerUser.addKills();
        } else {
            killerUser.substractKills();
        }
        TimestampManager.getInstance().setCooldown(victim, 0);
        TimestampManager.getInstance().setCooldown(killer, 0);
    }
}
