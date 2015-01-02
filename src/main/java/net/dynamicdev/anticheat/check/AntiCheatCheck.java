package net.dynamicdev.anticheat.check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.dynamicdev.anticheat.config.Configuration;
import net.dynamicdev.anticheat.config.providers.Lang;
import net.dynamicdev.anticheat.config.providers.Magic;
import net.dynamicdev.anticheat.manage.AntiCheatManager;
import net.dynamicdev.anticheat.util.TimedLocation;
import net.dynamicdev.anticheat.util.User;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AntiCheatCheck {
		
	
    protected Magic magic;
    protected AntiCheatManager manager = null;
    protected Lang lang = null;
    protected static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);
    
    //Used until a stable CB build for 1.8 is released/legal/etc
    protected static final String DEPTH_STRIDER_ENCHANT = "DEPTH_STRIDER";

    public AntiCheatCheck(AntiCheatManager instance)
    {
    	magic = instance.getConfiguration().getMagic();
        manager = instance;
        lang = manager.getConfiguration().getLang();
    }
    
    public void updateConfig(Configuration config) {
        magic = config.getMagic();
        lang = config.getLang();
    }
    
    public void setConfig(Configuration config)
    {
    	magic = config.getMagic();
        lang = config.getLang();
    }
    
    protected boolean isDoing(Player player, Map<String, Long> map, double max) {
        if (map.containsKey(player.getName())) {
            if (max != -1) {
                if (((System.currentTimeMillis() - map.get(player.getName())) / 1000) > max) {
                    map.remove(player.getName());
                    return false;
                } else {
                    return true;
                }
            } else {
                // Termination time has already been calculated
                if (map.get(player.getName()) < System.currentTimeMillis()) {
                    map.remove(player.getName());
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            return false;
        }
    }
    
    public int increment(Player player, Map<String, Integer> map, int num) {
        String name = player.getName();
        if (map.get(name) == null) {
            map.put(name, 1);
            return 1;
        } else {
            int amount = map.get(name) + 1;
            if (amount < num + 1) {
                map.put(name, amount);
                return amount;
            } else {
                map.put(name, num);
                return num;
            }
        }
    }

    public boolean silentMode() {
        return manager.getConfiguration().getConfig().silentMode.getValue();
    }
    
    public void sendFormattedMessage(Player player, String message)
    {
    	player.sendMessage(ChatColor.RED + "[AntiCheat+] " + message);
    }

}
