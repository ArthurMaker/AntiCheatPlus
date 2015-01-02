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

package net.dynamicdev.anticheat.event;

import net.dynamicdev.anticheat.AntiCheat;
import net.dynamicdev.anticheat.check.CheckResult;
import net.dynamicdev.anticheat.check.CheckType;
import net.dynamicdev.anticheat.util.Distance;
import net.dynamicdev.anticheat.util.Permission;
import net.dynamicdev.anticheat.util.User;
import net.dynamicdev.anticheat.util.Utilities;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.PlayerInventory;

public class PlayerListener extends EventListener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (getCheckManager().willCheck(player, CheckType.COMMAND_SPAM) && !Permission.getCommandExempt(player, event.getMessage().split(" ")[0])) {
            CheckResult result = getBackend().getChatCheck().checkCommandSpam(player, event.getMessage());
            if (result.failed()) {
                event.setCancelled(!silentMode());
                if (!silentMode()) player.sendMessage(ChatColor.RED + result.getMessage());
                getBackend().getChatCheck().processCommandSpammer(player);
                log(null, player, CheckType.COMMAND_SPAM);
            }
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) {
            getBackend().logEnterExit(event.getPlayer());
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.CREATIVE) {
            getBackend().logEnterExit(event.getPlayer());
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player player = (Player) event.getEntity().getShooter();

            if (event.getEntity() instanceof Arrow) {
                return;
            }

            if (getCheckManager().willCheck(player, CheckType.FAST_PROJECTILE)) {
                CheckResult result = getBackend().getEntityCheck().checkProjectile(player);
                if (result.failed()) {
                    event.setCancelled(!silentMode());
                    log(result.getMessage(), player, CheckType.FAST_PROJECTILE);
                }
            }
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.PLUGIN) {
            getBackend().logTeleport(event.getPlayer());
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerChangeWorlds(PlayerChangedWorldEvent event) {
        getBackend().logTeleport(event.getPlayer());

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            getBackend().logToggleSneak(event.getPlayer());
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        if (getCheckManager().willCheck(player, CheckType.FLY)) {
            if (getBackend().getMovementCheck().justVelocity(player) && getBackend().getMovementCheck().extendVelocityTime(player)) {
                event.setCancelled(!silentMode());
                return;
            }
            getBackend().getMovementCheck().logVelocity(player);
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (getCheckManager().willCheck(player, CheckType.CHAT_SPAM)) {
            CheckResult result = getBackend().getChatCheck().checkChatSpam(player, event.getMessage());
            if (result.failed()) {
                event.setCancelled(!silentMode());
                if (!result.getMessage().equals("") && !silentMode()) {
                    player.sendMessage(ChatColor.RED + result.getMessage());
                }
                getBackend().getChatCheck().processChatSpammer(player);
                log(null, player, CheckType.CHAT_SPAM);
            }
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        User user = getUserManager().getUser(event.getPlayer().getName());

        getConfig().getLevels().saveLevelFromUser(user);

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (!event.isSprinting()) {
            getBackend().logEnterExit(player);
        }
        if (getCheckManager().willCheck(player, CheckType.SPRINT)) {
            CheckResult result = getBackend().getMovementCheck().checkSprintHungry(event);
            if (result.failed()) {
                event.setCancelled(!silentMode());
                log(result.getMessage(), player, CheckType.SPRINT);
            } else {
                decrease(player);
            }
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material m = inv.getItemInHand().getType();
            if (m == Material.BOW) {
                getBackend().getEntityCheck().logBowWindUp(player);
            } else if (Utilities.isFood(m)) {
                getBackend().getEntityCheck().logEatingStart(player);
            }
        }
        Block block = event.getClickedBlock();

        if (block != null) {
            Distance distance = new Distance(player.getLocation(), block.getLocation());
            getBackend().getBlockCheck().checkLongReachBlock(player, distance.getXDifference(), distance.getYDifference(), distance.getZDifference());
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (getCheckManager().willCheck(player, CheckType.ITEM_SPAM)) {
            CheckResult result = getBackend().getInventoryCheck().checkFastDrop(player);
            if (result.failed()) {
                event.setCancelled(!silentMode());
                log(result.getMessage(), player, CheckType.ITEM_SPAM);
            }
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerEnterBed(PlayerBedEnterEvent event) {
        if (event.getBed().getType() != Material.BED) return;
        getBackend().logEnterExit(event.getPlayer());

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerExitBed(PlayerBedLeaveEvent event) {
        if (event.getBed().getType() != Material.BED) return;
        getBackend().logEnterExit(event.getPlayer());

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        getBackend().getBlockCheck().logAnimation(event.getPlayer());

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String section = "\u00a7";
        if (getCheckManager().willCheck(player, CheckType.ZOMBE_FLY)) {
            player.sendMessage(section + "f " + section + "f " + section + "1 " + section + "0 " + section + "2 " + section + "4");
        }
        if (getCheckManager().willCheck(player, CheckType.ZOMBE_CHEAT)) {
            player.sendMessage(section + "f " + section + "f " + section + "2 " + section + "0 " + section + "4 " + section + "8");
        }
        if (getCheckManager().willCheck(player, CheckType.ZOMBE_NOCLIP)) {
            player.sendMessage(section + "f " + section + "f " + section + "4 " + section + "0 " + section + "9 " + section + "6");
        }

        getBackend().logJoin(player);

        User user = new User(player.getName());
        user.setIsWaitingOnLevelSync(true);
        getConfig().getLevels().loadLevelToUser(user);
        getUserManager().addUser(user);

        if (player.hasMetadata(Utilities.SPY_METADATA)) {
            for (Player p : player.getServer().getOnlinePlayers()) {
                if (!Permission.SYSTEM_SPY.get(p)) {
                    p.hidePlayer(player);
                }
            }
        }

        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        if (getCheckManager().checkInWorld(player) && !getCheckManager().isOpExempt(player)) {
            final Location from = event.getFrom();
            final Location to = event.getTo();

            final Distance distance = new Distance(from, to);
            final double y = distance.getYDifference();
            getBackend().getMovementCheck().logAscension(player, from.getY(), to.getY());

            final User user = getUserManager().getUser(player.getName());
            user.setTo(to.getX(), to.getY(), to.getZ());

            if (getCheckManager().willCheckQuick(player, CheckType.SPEED)) {
                CheckResult result = getBackend().getMovementCheck().checkFreeze(player, from.getY(), to.getY());
                if (result.failed()) {
                    log(result.getMessage(), player, CheckType.SPEED);
                    if (!silentMode() && !AntiCheat.developerMode()) {
                        player.kickPlayer("Freezing client");
                    }
                }
            }
            if (getCheckManager().willCheckQuick(player, CheckType.SPRINT)) {
                CheckResult result = getBackend().getMovementCheck().checkSprintStill(player, from, to);
                if (result.failed()) {
                    event.setCancelled(!silentMode());
                    log(result.getMessage(), player, CheckType.SPRINT);
                }
            }
            if (getCheckManager().willCheckQuick(player, CheckType.FLY) && !player.isFlying()) {
                CheckResult result = getBackend().getMovementCheck().checkFlight(player, distance);
                if (result.failed()) {
                    if (!silentMode()) {
                        event.setTo(user.getGoodLocation(from.clone()));
                    }
                    log(result.getMessage(), player, CheckType.FLY);
                }
            }
            if (getCheckManager().willCheckQuick(player, CheckType.VCLIP) && event.getFrom().getY() > event.getTo().getY()) {
                CheckResult result = getBackend().getMovementCheck().checkVClip(player, new Distance(event.getFrom(), event.getTo()));
                if (result.failed()) {
                    if (!silentMode()) {
                        int data = result.getData() > 3 ? 3 : result.getData();
                        Location newloc = new Location(player.getWorld(), event.getFrom().getX(), event.getFrom().getY() + data, event.getFrom().getZ());
                        if (newloc.getBlock().getType() == Material.AIR) {
                            event.setTo(newloc);
                        } else {
                            event.setTo(user.getGoodLocation(from.clone()));
                        }
                        player.damage(3);
                    }
                    log(result.getMessage(), player, CheckType.VCLIP);
                }
            }
            if (getCheckManager().willCheckQuick(player, CheckType.VCLIP)) {
                CheckResult result = getBackend().getMovementCheck().checkNoclip(player);
                if (result.failed()) {
                    if (!silentMode()) {
                        Location newloc = new Location(player.getWorld(), event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ());
                        if (newloc.getBlock().getType() == Material.AIR) {
                            event.setTo(newloc);
                        } else {
                            event.setTo(user.getGoodLocation(from.clone()));
                        }
                        player.damage(3);
                    }
                    log(result.getMessage(), player, CheckType.VCLIP);
                }
            }
            if (getCheckManager().willCheckQuick(player, CheckType.NOFALL) && getCheckManager().willCheck(player, CheckType.FLY) && !Utilities.isClimbableBlock(player.getLocation().getBlock()) && event.getFrom().getY() > event.getTo().getY()) {
                CheckResult result = getBackend().getMovementCheck().checkNoFall(player, y);
                if (result.failed()) {
                    if (!silentMode()) {
                        event.setTo(user.getGoodLocation(from.clone()));
                        player.damage(1); // I added this in here so the player would still receive damage.
                    }
                    log(result.getMessage(), player, CheckType.NOFALL);
                }
            }

            boolean changed = false;
            if (event.getTo() != event.getFrom()) {
                double x = distance.getXDifference();
                double z = distance.getZDifference();
                if (getCheckManager().willCheckQuick(player, CheckType.SPEED) && getCheckManager().willCheck(player, CheckType.FLY)) {
                    if (event.getFrom().getY() < event.getTo().getY()) {
                        CheckResult result = getBackend().getMovementCheck().checkYSpeed(player, y);
                        if (result.failed()) {
                            if (!silentMode()) {
                                event.setTo(user.getGoodLocation(from.clone()));
                            }
                            log(result.getMessage(), player, CheckType.SPEED);
                            changed = true;
                        }
                    }
                    CheckResult result = getBackend().getMovementCheck().checkXZSpeed(player, x, z);
                    if (result.failed()) {
                        if (!silentMode()) {
                            event.setTo(user.getGoodLocation(from.clone()));
                        }
                        log(result.getMessage(), player, CheckType.SPEED);
                        changed = true;
                    }
                    if ((event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ())) {
                        CheckResult result1 = getBackend().getMovementCheck().checkTimer(player);
                        if(result1.failed()) {
                            if (!silentMode()) {
                                event.setTo(user.getGoodLocation(from.clone()));
                            }
                            log(result1.getMessage(), player, CheckType.MOREPACKETS);
                            changed = true;
                        }
                    }
                    
                }
                if (getCheckManager().willCheckQuick(player, CheckType.WATER_WALK)) {
                    CheckResult result = getBackend().getMovementCheck().checkWaterWalk(player, x, y, z);
                    if (result.failed()) {
                        if (!silentMode()) {
                            player.teleport(user.getGoodLocation(player.getLocation().add(0, -1, 0)));
                        }
                        log(result.getMessage(), player, CheckType.WATER_WALK);
                        changed = true;
                    }
                }
                if (getCheckManager().willCheckQuick(player, CheckType.SNEAK)) {
                    CheckResult result = getBackend().getMovementCheck().checkSneak(player, x, z);
                    if (result.failed()) {
                        if (!silentMode()) {
                            event.setTo(user.getGoodLocation(from.clone()));
                            player.setSneaking(false);
                        }
                        log(result.getMessage(), player, CheckType.SNEAK);
                        changed = true;
                    }
                }
                if (getCheckManager().willCheckQuick(player, CheckType.SPIDER)) {
                    CheckResult result = getBackend().getMovementCheck().checkSpider(player, y);
                    if (result.failed()) {
                        if (!silentMode()) {
                            event.setTo(user.getGoodLocation(from.clone()));
                        }
                        log(result.getMessage(), player, CheckType.SPIDER);
                        changed = true;
                    }
                }
                if (getCheckManager().willCheckQuick(player, CheckType.VELOCITY))
                {
                	CheckResult result = getBackend().getMovementCheck().checkVelocitized(player, distance);
                	if(result.failed())
                	{
                		log(result.getMessage(), player, CheckType.VELOCITY);
                	}
                }
                if (!changed) {
                    user.setGoodLocation(event.getFrom());
                }
            }
        }
        AntiCheat.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void checkFly(PlayerMoveEvent event) {
        // Check flight on highest to make sure other plugins have a chance to change the values first.
        final Player player = event.getPlayer();
        final User user = getUserManager().getUser(player.getName());
        final Location from = event.getFrom();
        final Location to = event.getTo();

        if (!user.checkTo(to.getX(), to.getY(), to.getZ())) {
            // The to value has been modified by another plugin
            return;
        }

        if (getCheckManager().willCheck(player, CheckType.FLY) && !player.isFlying()) {
            CheckResult result1 = getBackend().getMovementCheck().checkYAxis(player, new Distance(from, to));
            CheckResult result2 = getBackend().getMovementCheck().checkAscension(player, from.getY(), to.getY());
            String log = result1.failed() ? result1.getMessage() : result2.failed() ? result2.getMessage() : "";
            if (!log.equals("")) {
                if (!silentMode()) {
                    event.setTo(user.getGoodLocation(from.clone()));
                }
                log(log, player, CheckType.FLY);
            }
        }
    }
}
