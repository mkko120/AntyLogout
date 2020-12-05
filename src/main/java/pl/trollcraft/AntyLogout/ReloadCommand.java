package pl.trollcraft.AntyLogout;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ReloadCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        try {
            FileConfiguration usersconfig = new YamlConfiguration();
            usersconfig.load("users.yml");
            FileConfiguration config = new YamlConfiguration();
            config.load("config.yml");
            sender.sendMessage(Helper.color("&aPomyslnie przeladowano konfiguracje!"));
        } catch (Exception error) {
            sender.sendMessage(Helper.color("&6[&4Anti&cLogout&6] &cCos poszlo nie tak podczas przeladowania configu! Sprawdz konsolke."));
            error.printStackTrace();
            return true;
        }

        return true;
    }
}
