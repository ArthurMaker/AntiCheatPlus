/*
 * AntiCheat for Bukkit.
 * Copyright (C) 2012-2014 AntiCheat Team | http://gravitydevelopment.net
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.dynamicdev.anticheat.check;

import java.util.HashMap;
import java.util.Map;

import net.dynamicdev.anticheat.check.checks.BlockCheck;
import net.dynamicdev.anticheat.check.checks.ChatCheck;
import net.dynamicdev.anticheat.check.checks.EntityCheck;
import net.dynamicdev.anticheat.check.checks.InventoryCheck;
import net.dynamicdev.anticheat.check.checks.MovementCheck;
import net.dynamicdev.anticheat.config.Configuration;
import net.dynamicdev.anticheat.config.providers.Magic;
import net.dynamicdev.anticheat.manage.AntiCheatManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Backend {
	
    public Map<String, Long> animated = new HashMap<String, Long>();
    public Map<String, Integer> interactionCount = new HashMap<String, Integer>();
    public Map<String, Integer> blockPunches = new HashMap<String, Integer>();
    
	private MovementCheck movementCheck;
	private BlockCheck blockCheck;
	private ChatCheck chatCheck;
	private EntityCheck entityCheck;
	private InventoryCheck inventoryCheck;
	
	private Magic magic;
	
    public Backend(AntiCheatManager instance) {
    	magic = instance.getConfiguration().getMagic();
    	
    	movementCheck = new MovementCheck(instance);
    	blockCheck = new BlockCheck(instance);
    	chatCheck = new ChatCheck(instance);
    	entityCheck = new EntityCheck(instance);
    	inventoryCheck = new InventoryCheck(instance);
    }
    
    public MovementCheck getMovementCheck()
    {
    	return movementCheck;
    }
    
    public BlockCheck getBlockCheck()
    {
    	return blockCheck;
    }
    
    public EntityCheck getEntityCheck()
    {
    	return entityCheck;
    }
    
    public ChatCheck getChatCheck()
    {
    	return chatCheck;
    }
    
    public InventoryCheck getInventoryCheck()
    {
    	return inventoryCheck;
    }
    
    public void updateConfig(Configuration config)
    {
    	movementCheck.setConfig(config);
    	blockCheck.setConfig(config);
    	chatCheck.setConfig(config);
    	entityCheck.setConfig(config);
    	inventoryCheck.setConfig(config);
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
    
    public void resetAnimation(final Player player) {
        animated.remove(player.getName());
        blockPunches.put(player.getName(), 0);
    }

    public boolean justAnimated(Player player) {
        String name = player.getName();
        if (animated.containsKey(name)) {
            long time = System.currentTimeMillis() - animated.get(name);
            int count = interactionCount.get(player.getName()) + 1;
            interactionCount.put(player.getName(), count);

            if (count > magic.ANIMATION_INTERACT_MAX()) {
                animated.remove(player.getName());
                return false;
            }
            return time < magic.ANIMATION_MIN();
        } else {
            return false;
        }
    }

    public void logDamage(final Player player, int type) {
        long time;
        switch (type) {
            case 1:
                time = magic.DAMAGE_TIME();
                break;
            case 2:
                time = magic.KNOCKBACK_DAMAGE_TIME();
                break;
            case 3:
                time = magic.EXPLOSION_DAMAGE_TIME();
                break;
            default:
                time = magic.DAMAGE_TIME();
                break;

        }
        movementCheck.getMovingExempt().put(player.getName(), System.currentTimeMillis() + time);
        // Only map in which termination time is calculated beforehand.
    }

    public void logEnterExit(final Player player) {
    	movementCheck.getMovingExempt().put(player.getName(), System.currentTimeMillis() + magic.ENTERED_EXITED_TIME());
    }

    public void logToggleSneak(final Player player) {
    	//Change to sneak exempt; otherwise everything is boned (Speed bypass with sneaking).
    	movementCheck.getSneakExempt().put(player.getName(), System.currentTimeMillis() + magic.SNEAK_TIME());
    }

    public void logTeleport(final Player player) {
        movementCheck.getMovingExempt().put(player.getName(), System.currentTimeMillis() + magic.TELEPORT_TIME());
        movementCheck.logTeleport(player);
    }

    public void logExitFly(final Player player) {
    	movementCheck.getMovingExempt().put(player.getName(), System.currentTimeMillis() + magic.EXIT_FLY_TIME());
    }

    public void logJoin(final Player player) {
    	movementCheck.getMovingExempt().put(player.getName(), System.currentTimeMillis() + magic.JOIN_TIME());
        if(player.getLocation().getBlock().getType() == Material.AIR)
        {
        	Location setLocation = player.getLocation();
        	int x = player.getLocation().getBlockX();
        	int z = player.getLocation().getBlockZ();
        	for(int y = player.getLocation().getBlockY(); y > 0; y--)
        	{
        		if(player.getWorld().getBlockAt(x, y, z).getType().isSolid())
        		{
        			setLocation = new Location(player.getWorld(), x, y + 1, z);
        			break;
        		}
        	}
        	player.teleport(setLocation);
        }
    }

    public boolean isMovingExempt(Player player) {
        return isDoing(player, movementCheck.getMovingExempt(), -1);
    }

    public boolean isSneakExempt(Player player)
    {
    	return isDoing(player, movementCheck.getSneakExempt(), -1);
    }

    public boolean isSpeedExempt(Player player) {
        return isMovingExempt(player) || movementCheck.justVelocity(player);
    }

    



    
}
