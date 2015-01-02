package net.dynamicdev.anticheat.check.checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dynamicdev.anticheat.AntiCheat;
import net.dynamicdev.anticheat.check.AntiCheatCheck;
import net.dynamicdev.anticheat.check.CheckResult;
import net.dynamicdev.anticheat.manage.AntiCheatManager;
import net.dynamicdev.anticheat.util.Distance;
import net.dynamicdev.anticheat.util.SimpleLocation;
import net.dynamicdev.anticheat.util.TimedLocation;
import net.dynamicdev.anticheat.util.Utilities;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffectType;

public class MovementCheck extends AntiCheatCheck {

	private List<String> isInWater = new ArrayList<String>();
    private List<String> isInWaterCache = new ArrayList<String>();
    private List<String> isAscending = new ArrayList<String>();
    private Map<String, Integer> ascensionCount = new HashMap<String, Integer>();
    private Map<String, Double> blocksOverFlight = new HashMap<String, Double>();
    private Map<String, Integer> nofallViolation = new HashMap<String, Integer>();
    private Map<String, Integer> speedViolation = new HashMap<String, Integer>();
    private Map<String, Integer> yAxisViolations = new HashMap<String, Integer>();
    private Map<String, Long> yAxisLastViolation = new HashMap<String, Long>();
    private Map<String, Double> lastYcoord = new HashMap<String, Double>();
    private Map<String, Long> lastYtime = new HashMap<String, Long>();
    private Map<String, Integer> waterAscensionViolation = new HashMap<String, Integer>();
    private Map<String, Integer> waterSpeedViolation = new HashMap<String, Integer>();
    private Map<String, Long> velocitized = new HashMap<String, Long>();
    private Map<String, Integer> steps = new HashMap<String, Integer>();
    private Map<String, Long> stepTime = new HashMap<String, Long>();
    private Map<String, Long> lastFallPacket = new HashMap<String, Long>();
    private Map<String, Integer> hoverTicks = new HashMap<String, Integer>();
    private Map<String, Integer> velocityFail = new HashMap<String, Integer>();
    private Map<String, TimedLocation> timedLoc = new HashMap<String, TimedLocation>();
    private Map<String, Integer> verticalCount = new HashMap<String, Integer>();
    private Map<String, Boolean> canMoveVert = new HashMap<String, Boolean>();
    private Map<String, Long> timeInWater = new HashMap<String, Long>();
    private Map<String, Integer> velocitytrack = new HashMap<String, Integer>();
    private Map<String, Long> movingExempt = new HashMap<String, Long>();
    private Map<String, Long> sneakExempt = new HashMap<String, Long>();
    private Map<String, Integer> timerBuffer = new HashMap<String, Integer>();
    private Map<String, Integer> glideBuffer = new HashMap<String, Integer>();
    private Map<String, Double> lastYDelta = new HashMap<String, Double>();
    private Map<String, SimpleLocation> lastTickLocation = new HashMap<String, SimpleLocation>();
    
    private static final double TIME_SECOND = 1.0;
    private static final double MIN_Y_DELTA = 0.3;
    
	public MovementCheck(AntiCheatManager instance) {
		super(instance);
	}
	
	public Map getMovingExempt()
	{
		return movingExempt;
	}
	
	public Map getSneakExempt()
	{
		return sneakExempt;
	}

    public boolean hasJumpPotion(Player player) {
        return player.hasPotionEffect(PotionEffectType.JUMP);
    }

    public boolean hasSpeedPotion(Player player) {
        return player.hasPotionEffect(PotionEffectType.SPEED);
    }

    public boolean isAscending(Player player) {
        return isAscending.contains(player.getName());
    }
	
	public void logTeleport(Player player)
	{
		/* Data for fly/speed should be reset */
        nofallViolation.remove(player.getName());
        blocksOverFlight.remove(player.getName());
        yAxisViolations.remove(player.getName());
        yAxisLastViolation.remove(player.getName());
        lastYcoord.remove(player.getName());
        lastYtime.remove(player.getName());
        lastTickLocation.put(player.getName(), new SimpleLocation(player.getLocation()));
        timedLoc.put(player.getName(), new TimedLocation(player.getLocation(), System.currentTimeMillis()));
	}
	
	public boolean isHoveringOverWaterAfterViolation(Player player) {
        if (waterSpeedViolation.containsKey(player.getName())) {
            if (waterSpeedViolation.get(player.getName()) >= magic.WATER_SPEED_VIOLATION_MAX() && Utilities.isHoveringOverWater(player.getLocation())) {
                return true;
            }
        }
        return false;
    }
	
	public void logVelocity(final Player player) {
        velocitized.put(player.getName(), System.currentTimeMillis());
    }

    public boolean justVelocity(Player player) {
        return (velocitized.containsKey(player.getName()) ? (System.currentTimeMillis() - velocitized.get(player.getName())) < magic.VELOCITY_CHECKTIME() : false);
    }

    public boolean extendVelocityTime(final Player player) {
        if (velocitytrack.containsKey(player.getName())) {
            velocitytrack.put(player.getName(), velocitytrack.get(player.getName()) + 1);
            if (velocitytrack.get(player.getName()) > magic.VELOCITY_MAXTIMES()) {
                velocitized.put(player.getName(), System.currentTimeMillis() + magic.VELOCITY_PREVENT());
                manager.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(manager.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        velocitytrack.put(player.getName(), 0);
                    }
                }, magic.VELOCITY_SCHETIME() * 20L);
                return true;
            }
        } else {
            velocitytrack.put(player.getName(), 0);
        }

        return false;
    }

	
	public CheckResult checkFreeze(Player player, double from, double to) {
        // TODO: Fix! This is causing false-positives
        /*
         * if((from-to) > 0) { boolean flag = false; if(lastFallPacket.containsKey(player.getName()) && lastFallPacket.get(player.getName()) > 0) { flag = (System.currentTimeMillis()-lastFallPacket.get(player.getName())) > 1000; } lastFallPacket.put(player.getName(), System.currentTimeMillis());
         * return flag; } else { lastFallPacket.put(player.getName(), -1L); }
         */
        return PASS;
    }
	
	public CheckResult checkSpider(Player player, double y) {
        String name = player.getName();
    	if(!verticalCount.containsKey(name))
    	{
    		verticalCount.put(name, 0);
    	}
    	if (y <= magic.LADDER_Y_MAX() && y >= magic.LADDER_Y_MIN() && !Utilities.isClimbableBlock(player.getLocation().getBlock())) {
        	verticalCount.put(name, verticalCount.get(name) + 1);
        	if(verticalCount.get(name) > magic.Y_MAXVIOLATIONS())
        	{
        		return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to climb a non-ladder (" + player.getLocation().getBlock().getType() + ")");
        	}
    	}
    	verticalCount.put(name, verticalCount.get(name) - 1);
    	return PASS;
    }
	
	public CheckResult checkYSpeed(Player player, double y) {
    	double multiPlier = (player.getEyeLocation().getBlock().getType() == Material.WEB ? magic.XZ_SPEED_WEB_MULTIPLIER() : 1.0);
        if (!AntiCheat.getManager().getBackend().isMovingExempt(player) && !player.isInsideVehicle() && !player.isSleeping() && y > (magic.Y_SPEED_MAX() * multiPlier) && !isDoing(player, velocitized, magic.VELOCITY_TIME()) && !player.hasPotionEffect(PotionEffectType.JUMP)) {
            return new CheckResult(CheckResult.Result.FAILED, player.getName() + "'s y speed was too high (speed=" + y + ", max=" + magic.Y_SPEED_MAX() + ")");
        } else {
            return PASS;
        }
    }
	
	public CheckResult checkNoFall(Player player, double y) {
        String name = player.getName();
        if (player.getGameMode() != GameMode.CREATIVE && !player.isInsideVehicle() && !player.isSleeping() && !AntiCheat.getManager().getBackend().isMovingExempt(player) && !AntiCheat.getManager().getBackend().getBlockCheck().justPlaced(player) && !Utilities.isInWater(player) && !Utilities.isInWeb(player)) {
            if (player.getFallDistance() == 0) {
                if (nofallViolation.get(name) == null) {
                    nofallViolation.put(name, 1);
                } else {
                    nofallViolation.put(name, nofallViolation.get(player.getName()) + 1);
                }

                int i = nofallViolation.get(name);
                if (i >= magic.NOFALL_LIMIT()) {
                    nofallViolation.put(player.getName(), 1);
                    return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to avoid fall damage (fall distance = 0 " + i + " times in a row, max=" + magic.NOFALL_LIMIT() + ")");
                } else {
                    return PASS;
                }
            } else {
                nofallViolation.put(name, 0);
                return PASS;
            }
        }
        return PASS;
    }

	
	public CheckResult checkVelocitized(Player player, Distance theDistance)
    {    	
    	String name = player.getName();
    	if(!velocityFail.containsKey(name))
    	{
    		velocityFail.put(name, 0);
    	}
    	if(!AntiCheat.getManager().getBackend().isMovingExempt(player) && player.getVehicle() == null && justVelocity(player))
    	{
    		double multi = player.hasPotionEffect(PotionEffectType.SLOW) ? 0.75 : 1.0;
    		if((theDistance.getXDifference() < magic.VELOCITY_MIN_DISTANCE()*multi) 
    				|| (theDistance.getZDifference() < magic.VELOCITY_MIN_DISTANCE()*multi))
    		{
    			velocityFail.put(name, velocityFail.get(name) + 1);
    			if(velocityFail.get(name) > magic.VELOCITY_DISTANCE_COUNT())
    			{
    				//TODO: Improve
    				//return new CheckResult(CheckResult.Result.FAILED, name + " failed to move after being velocitized " + velocityFail.get(name) + " times.");
    			}
    		}else
    		{
    			velocityFail.put(name, 0);
    		}
    	}
    	return PASS;
    }

	public CheckResult checkXZSpeed(Player player, double x, double z) {
    	if(!speedViolation.containsKey(player.getName()))
    	{
    		speedViolation.put(player.getName(), 1);
    	}
    	if(!lastTickLocation.containsKey(player.getName()))
    	{
    		lastTickLocation.put(player.getName(), new SimpleLocation(player.getLocation()));
    	}
    	SimpleLocation lastLocation = lastTickLocation.get(player.getName());
    	SimpleLocation currentLocation = new SimpleLocation(player.getLocation());
    	lastTickLocation.put(player.getName(), new SimpleLocation(player.getLocation()));
        if (!AntiCheat.getManager().getBackend().isSpeedExempt(player) && player.getVehicle() == null) {
            String reason = "";
            double max = magic.XZ_SPEED_MAX();
            if (player.getLocation().getBlock().getType() == Material.SOUL_SAND) {
                if (player.isSprinting()) {
                    reason = "on soulsand while sprinting ";
                    max = magic.XZ_SPEED_MAX_SOULSAND_SPRINT();
                } else if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                    reason = "on soulsand with speed potion ";
                    max = magic.XZ_SPEED_MAX_SOULSAND_POTION();
                } else {
                    reason = "on soulsand ";
                    max = magic.XZ_SPEED_MAX_SOULSAND();
                }
            } else if (player.isFlying()) {
                reason = "while flying ";
                max = magic.XZ_SPEED_MAX_FLY();
            } else if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                if (player.isSprinting()) {
                    reason = "with speed potion while sprinting ";
                    max = magic.XZ_SPEED_MAX_POTION_SPRINT();
                } else {
                    reason = "with speed potion ";
                    max = magic.XZ_SPEED_MAX_POTION();
                }
            } else if (player.isSprinting()) {
                reason = "while sprinting ";
                max = magic.XZ_SPEED_MAX_SPRINT();
            }
            
            if(!timeInWater.containsKey(player.getName()))
            {
            	timeInWater.put(player.getName(), System.currentTimeMillis());
            }
            /* TODO: Improve ALLLLL of this
            if(Utilities.isInWater(player) 
            		|| Utilities.cantStandAtWater(player.getLocation().getBlock()))
            {
            	
            	//Ignore this, it's Minecrafty stuff.
            	double multiPerLevel = 1.55;
            
            	int level = Utilities.getLevelForEnchantment(player, DEPTH_STRIDER_ENCHANT);
            	if(level != -1)
            	{
            		max = max * (level*multiPerLevel);
            	}else
            	{
            		if(System.currentTimeMillis() - timeInWater.get(player.getName()) < magic.TELEPORT_TIME()*1.5)
                	{
            			//As a supplement to the Waterwalk check, I give you: redundancy
                		max = max * 0.65;
                	}
            	}
            }else
            {
            	timeInWater.put(player.getName(), System.currentTimeMillis());
            }
            */

          //Ignore this, it's Minecrafty stuff.
        	double multiPerLevel = 1.55;
            
            int level = Utilities.getLevelForEnchantment(player, DEPTH_STRIDER_ENCHANT);
        	if(level != -1)
        	{
        		max = max * (level*multiPerLevel);
        	}
            
            float speed = player.getWalkSpeed();
            max += speed > 0 ? player.getWalkSpeed() - 0.2f : 0;
            
            //TODO: This
            boolean isEating = false;
            
            if(player.getLocation().getBlock().getType() == Material.ICE)
            {
            	max *= magic.XZ_SPEED_ICE_MULTIPLIER();
            }
            else if(Utilities.isInWeb(player)
            		|| player.isBlocking()
            		|| isEating)
            {
            	if(!(player.getGameMode() == GameMode.CREATIVE) && !player.isFlying() 
            			&& !(player.getLocation().getBlock().getType() == Material.SOUL_SAND))
            	{
            		max *= magic.XZ_SPEED_WEB_MULTIPLIER();
            	}
            }
            									// Handle really large teleports.
            if (x > max || z > max || Utilities.getHorizontalDistance(lastLocation, currentLocation) > magic.XZ_TICK_MAX()) {
                int num = this.increment(player, speedViolation, magic.SPEED_MAX());
                if (num >= magic.SPEED_MAX()) {
                    return new CheckResult(CheckResult.Result.FAILED, player.getName() + "'s speed was too high " + reason + num + " times in a row (max=" + magic.SPEED_MAX() + ", speed=" + (x > z ? x : z) + ", max speed=" + max + ")");
                }
            } else {
            	if(speedViolation.get(player.getName()) > 1)
            	{
            		speedViolation.put(player.getName(), speedViolation.get(player.getName()) - 1);
            	}
            }
        }
        
        return PASS;
    }
	
	public CheckResult checkSneak(Player player, double x, double z) {
        if (player.isSneaking() && !player.isFlying() 
        		&& !AntiCheat.getManager().getBackend().isSneakExempt(player) && !player.isInsideVehicle()) {
            double i = x > magic.XZ_SPEED_MAX_SNEAK() ? x : z > magic.XZ_SPEED_MAX_SNEAK() ? z : -1;
            if (i != -1) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " was sneaking too fast (speed=" + i + ", max=" + magic.XZ_SPEED_MAX_SNEAK() + ")");
            } else {
                return PASS;
            }
        } else {
            return PASS;
        }
    }

    public CheckResult checkSprintHungry(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (event.isSprinting() && player.getGameMode() != GameMode.CREATIVE && player.getFoodLevel() <= magic.SPRINT_FOOD_MIN()) {
            return new CheckResult(CheckResult.Result.FAILED, player.getName() + " sprinted while hungry (food=" + player.getFoodLevel() + ", min=" + magic.SPRINT_FOOD_MIN() + ")");
        } else {
            return PASS;
        }
    }

    public CheckResult checkSprintStill(Player player, Location from, Location to) {
        /*if(!isMovingExempt(player) && player.isSprinting() && from.getX() == to.getX() && from.getZ() == to.getZ()) {
            return new CheckResult(Result.FAILED, player.getName()+" sprinted while standing still (xyz = "+(int)from.getX()+","+(int)from.getY()+","+(int)from.getZ()+")");
        }*/
        return PASS;
    }

    public CheckResult checkWaterWalk(Player player, double x, double y, double z) {
        Block block = player.getLocation().getBlock();

        if (player.getVehicle() == null && !player.isFlying()) {
            if (block.isLiquid()) {
                if (isInWater.contains(player.getName())) {
                    if (isInWaterCache.contains(player.getName())) {
                        if (player.getNearbyEntities(1, 1, 1).isEmpty()) {
                            boolean b;
                            if (!Utilities.sprintFly(player)) {
                                b = x > magic.XZ_SPEED_MAX_WATER() || z > magic.XZ_SPEED_MAX_WATER();
                            } else {
                                b = x > magic.XZ_SPEED_MAX_WATER_SPRINT() || z > magic.XZ_SPEED_MAX_WATER_SPRINT();
                            }
                            if (!b && !Utilities.isFullyInWater(player.getLocation()) && Utilities.isHoveringOverWater(player.getLocation(), 1) && y == 0D && !block.getType().equals(Material.WATER_LILY)) {
                                b = true;
                            }
                            if (b) {
                                if (waterSpeedViolation.containsKey(player.getName())) {
                                    int v = waterSpeedViolation.get(player.getName());
                                    if (v >= magic.WATER_SPEED_VIOLATION_MAX()) {
                                        waterSpeedViolation.put(player.getName(), 0);
                                        return new CheckResult(CheckResult.Result.FAILED, player.getName() + " stood on water " + v + " times (can't stand on " + block.getType() + " or " + block.getRelative(BlockFace.DOWN).getType() + ")");
                                    } else {
                                        waterSpeedViolation.put(player.getName(), v + 1);
                                    }
                                } else {
                                    waterSpeedViolation.put(player.getName(), 1);
                                }
                            }
                        }
                    } else {
                        isInWaterCache.add(player.getName());
                        return PASS;
                    }
                } else {
                    isInWater.add(player.getName());
                    return PASS;
                }
            } else if (block.getRelative(BlockFace.DOWN).isLiquid() && isAscending(player) && Utilities.cantStandAt(block) && Utilities.cantStandAt(block.getRelative(BlockFace.DOWN))) {
                if (waterAscensionViolation.containsKey(player.getName())) {
                    int v = waterAscensionViolation.get(player.getName());
                    if (v >= magic.WATER_ASCENSION_VIOLATION_MAX()) {
                        waterAscensionViolation.put(player.getName(), 0);
                        return new CheckResult(CheckResult.Result.FAILED, player.getName() + " stood on water " + v + " times (can't stand on " + block.getType() + " or " + block.getRelative(BlockFace.DOWN).getType() + ")");
                    } else {
                        waterAscensionViolation.put(player.getName(), v + 1);
                    }
                } else {
                    waterAscensionViolation.put(player.getName(), waterAscensionViolation.get(player.getName())-1);
                }
            } else {
                isInWater.remove(player.getName());
                isInWaterCache.remove(player.getName());
            }
        }
        return PASS;
    }
    
    public CheckResult checkNoclip(Player player)
    {
    	Block block = player.getEyeLocation().getBlock();
    	Block otherBlock = player.getLocation().getBlock();
    	if((!Utilities.canStandWithin(block) && !Utilities.canStandWithin(otherBlock)) 
    			|| !Utilities.canStandWithin(block) 
    			&& !AntiCheat.getManager().getBackend().isMovingExempt(player))
    	{
    		return new CheckResult(CheckResult.Result.FAILED, player.getName() + 
    				" attempted to pass through a solid block.");
    	}
    	return PASS;
    }

    public CheckResult checkVClip(Player player, Distance distance) {
        double from = Math.round(distance.fromY());
        double to = Math.round(distance.toY());

        if (player.isInsideVehicle() || (from == to || from < to) || Math.round(distance.getYDifference()) < 2) {
            return PASS;
        }

        for (int i = 0; i < (Math.round(distance.getYDifference())) + 1; i++) {
            Block block = new Location(player.getWorld(), player.getLocation().getX(), to + i, player.getLocation().getZ()).getBlock();
            if (block.getType() != Material.AIR && block.getType().isSolid()) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to move through a solid block", (int) from + 3);
            }
        }

        return PASS;
    }

    public CheckResult checkYAxis(Player player, Distance distance) {
    	String name = player.getName();
    	if(!canMoveVert.containsKey(name))
    	{
    		canMoveVert.put(name, true);
    	}
        if (distance.getYDifference() > magic.TELEPORT_MIN() || distance.getYDifference() < 0) {
            return PASS;
        }
        if (!AntiCheat.getManager().getBackend().isMovingExempt(player) && !Utilities.isClimbableBlock(player.getLocation().getBlock()) && !Utilities.isClimbableBlock(player.getLocation().add(0, -1, 0).getBlock()) && !player.isInsideVehicle() && !Utilities.isInWater(player) && !hasJumpPotion(player)) {
            double y1 = player.getLocation().getY();
            double lastDelta = distance.getYActual();
            if(player.getLocation().getBlock().getType() != Material.AIR || player.isOnGround())
            {
            	//Because onGround
            	canMoveVert.put(name, true);
            }else
            {
            	if(canMoveVert.get(name))
            	{
            		if(lastDelta > 0)
            		{
            			canMoveVert.put(name, false);
            		}
            	}else
            	{
            		if(lastDelta < 0)
            		{
            			if(!yAxisViolations.containsKey(name))
            			{
            				yAxisViolations.put(name, 0);
            			}
            			yAxisViolations.put(name, yAxisViolations.get(name) + 1);
            			if(yAxisViolations.get(name) > magic.Y_MAXVIOLATIONS())
            			{
            				Location g = player.getLocation();
            				if (!silentMode()) {
            					g.setY(lastYcoord.get(name));
            					sendFormattedMessage(player, "Fly hacking on the y-axis detected.");
            					if (g.getBlock().getType() == Material.AIR) {
            						player.teleport(g);
            					}
            				}
            				return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to fly on y-axis " + hoverTicks.get(name) + " times (max =" + magic.Y_MAXVIOLATIONS() + ")");
            			}
            		}
            	}
            }
            
            
            if (!lastYcoord.containsKey(name) || !lastYtime.containsKey(name) || !yAxisLastViolation.containsKey(name) || !yAxisLastViolation.containsKey(name)) {
                lastYcoord.put(name, y1);
                yAxisViolations.put(name, 0);
                yAxisLastViolation.put(name, 0L);
                lastYtime.put(name, System.currentTimeMillis());
            } else {
            	//If not tracking the player, go ahead and start now.
            	if(!hoverTicks.containsKey(name))
        		{
        			hoverTicks.put(name, 0);
        		}
            	boolean overAir = Utilities.cantStandAtBetter(player.getLocation().getBlock()) && !player.isSneaking();
            	if(Math.abs(y1 - lastYcoord.get(name)) <= (magic.Y_HOVER_BUFFER() * 0.75) 
            			&& (overAir))
            	{
            		
            		hoverTicks.put(name, hoverTicks.get(name) + 1);
            		if(hoverTicks.get(name) > magic.Y_HOVER_TIME())
            		{
            			Location g = player.getLocation();
            			if (!silentMode()) {
                            g.setY(lastYcoord.get(name));
                            sendFormattedMessage(player, "Fly hacking on the y-axis detected.");
                            if (g.getBlock().getType() == Material.AIR) {
                                player.teleport(g);
                            }
                        }
                        return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to fly (hover) on y-axis " + hoverTicks.get(name) + " times (max =" + magic.Y_HOVER_TIME() + ")");
            		}
            	}
            	else
            	{
            		hoverTicks.put(name, 0);
            	}
                if (y1 > lastYcoord.get(name) && yAxisViolations.get(name) > magic.Y_MAXVIOLATIONS() && (System.currentTimeMillis() - yAxisLastViolation.get(name)) < magic.Y_MAXVIOTIME()) {
                    Location g = player.getLocation();
                    yAxisViolations.put(name, yAxisViolations.get(name) + 1);
                    yAxisLastViolation.put(name, System.currentTimeMillis());
                    if (!silentMode()) {
                        g.setY(lastYcoord.get(name));
                        sendFormattedMessage(player, "Fly hacking on the y-axis detected.  Please wait 5 seconds to prevent getting damage.");
                        if (g.getBlock().getType() == Material.AIR) {
                            player.teleport(g);
                        }
                    }
                    return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to fly on y-axis " + yAxisViolations.get(name) + " times (max =" + magic.Y_MAXVIOLATIONS() + ")");
                } else {
                    if (yAxisViolations.get(name) > magic.Y_MAXVIOLATIONS() && (System.currentTimeMillis() - yAxisLastViolation.get(name)) > magic.Y_MAXVIOTIME()) {
                        yAxisViolations.put(name, yAxisViolations.get(name) - 1);
                        yAxisLastViolation.put(name, 0L);
                    }
                }
                long i = System.currentTimeMillis() - lastYtime.get(name);
                double diff = magic.Y_MAXDIFF() + (Utilities.isStair(player.getLocation().add(0, -1, 0).getBlock()) ? 0.5 : 0.0);
                if ((y1 - lastYcoord.get(name)) > diff && i < magic.Y_TIME()) {
                	if(player != null)
                	{
                		Location g = player.getLocation();
                		yAxisViolations.put(name, yAxisViolations.get(name) + 1);
                		yAxisLastViolation.put(name, System.currentTimeMillis());
                		if (!silentMode()) {
                			g.setY(lastYcoord.get(name));
                			if (g.getBlock().getType() == Material.AIR) {
                				player.teleport(g);
                			}
                		}
                	}
                    return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to fly on y-axis in " + i + " ms (min =" + magic.Y_TIME() + ")");
                } else {
                    if ((y1 - lastYcoord.get(name)) > magic.Y_MAXDIFF() + 1 || (System.currentTimeMillis() - lastYtime.get(name)) > magic.Y_TIME()) {
                        lastYtime.put(name, System.currentTimeMillis());
                        lastYcoord.put(name, y1);
                    }
                }
            }
        }
        // Fix Y axis spam
        return PASS;
    }

    public CheckResult checkTimer(Player player) {
    	if(player == null || player.getName() == null)
    	{
    		//NPC's with no names? C'mon.
    		return PASS;
    	}
    	String name = player.getName();
        if(!stepTime.containsKey(name))
        {
        	stepTime.put(name, System.currentTimeMillis());
        }
        if(!timerBuffer.containsKey(name))
        {
        	timerBuffer.put(name, magic.TIMER_STEP_CHECK());
        }
        timerBuffer.put(name, timerBuffer.get(name) - 1);
        if(!AntiCheat.getManager().getBackend().isMovingExempt(player))
        {
        	if(timerBuffer.get(name) < 0)
        	{
        		if(!silentMode())
        		{
        			sendFormattedMessage(player, "Modification of game timer detected. Please stand still for a bit.");
        		}
        		incrementTimerBuffer(name);
        		return new CheckResult(CheckResult.Result.FAILED, 
        				name + " attempted to send packets too fast!");
        	}else
        	{
        		incrementTimerBuffer(name);
        	}
        }
        return PASS;
    }
    
    private void incrementTimerBuffer(String name)
    {
    	//To account for server lag
    	double timeSince = (System.currentTimeMillis() - stepTime.get(name)) / 1000;
    	if(timeSince > TIME_SECOND)
    	{
    		double allowedPackets = timeSince * magic.TIMER_TIMEMIN();
    		//Let's not go too crazy here...
    		if(timerBuffer.get(name) > 65)
    		{
    			timerBuffer.put(name, 65);
    		}
    		else
    		{
    			timerBuffer.put(name, (int) (timerBuffer.get(name) + allowedPackets));
    		}

        	stepTime.put(name, System.currentTimeMillis());
    	}
    }
    
    /**
     * Must always be run AFTER the flight check, combats glide hacks
     * @param player the player to check
     * @return failed if they fail it, pass if not
     */
    public CheckResult checkGlide(Player player)
    {
    	String name = player.getName();
    	if(!glideBuffer.containsKey(name))
    	{
    		glideBuffer.put(name, 0);
    	}
    	if(!lastYDelta.containsKey(name))
    	{
    		lastYDelta.put(name, 0.0);
    	}
    	double currentY = player.getLocation().getY();
    	double math = currentY - lastYcoord.get(name);
    	if(math < 0 && !AntiCheat.getManager().getBackend().isMovingExempt(player))
    	{
    		if(math <= lastYDelta.get(name) && !(player.getEyeLocation().getBlock().getType() == Material.LADDER)
    				&& !Utilities.isInWater(player) && !Utilities.isInWeb(player)
    				&& Utilities.cantStandAtSingle(player.getLocation().getBlock()))
    		{
    			int currentBuffer = glideBuffer.get(name);
    			glideBuffer.put(name, currentBuffer + 1);
    			if((currentBuffer + 1) >= magic.FLIGHT_LIMIT())
    			{
    				if(!silentMode())
    				{
    					sendFormattedMessage(player, "Fly hacking on the y-axis detected.");
    				}
    				lastYDelta.put(name, math);
    				return new CheckResult(CheckResult.Result.FAILED, name + " attempted to fall too slowly!");
    			}
    		}
    	}
    	lastYDelta.put(name, math);
    	return PASS;
    }


    public CheckResult checkFlight(Player player, Distance distance) {
        if (distance.getYDifference() > magic.TELEPORT_MIN()) {
            // This was a teleport, so we don't care about it.
            return PASS;
        }
        final String name = player.getName();
        final double y1 = distance.fromY();
        final double y2 = distance.toY();
        if (!AntiCheat.getManager().getBackend().isMovingExempt(player) && !Utilities.isHoveringOverWater(player.getLocation(), 1) && Utilities.cantStandAtExp(player.getLocation()) && Utilities.blockIsnt(player.getLocation().getBlock().getRelative(BlockFace.DOWN), new Material[]{Material.FENCE, Material.FENCE_GATE, Material.COBBLE_WALL})) {

            if (!blocksOverFlight.containsKey(name)) {
                blocksOverFlight.put(name, 0D);
            }

            blocksOverFlight.put(name, (blocksOverFlight.get(name) + distance.getXDifference() + distance.getYDifference() + distance.getZDifference()));

            if (y1 > y2) {
                blocksOverFlight.put(name, (blocksOverFlight.get(name) - distance.getYDifference()));
            }

            if (blocksOverFlight.get(name) > magic.FLIGHT_BLOCK_LIMIT() && (y1 <= y2)) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " flew over " + blocksOverFlight.get(name) + " blocks (max=" + magic.FLIGHT_BLOCK_LIMIT() + ")");
            }
        } else {
            blocksOverFlight.put(name, 0D);
        }

        return PASS;
    }


    public void logAscension(Player player, double y1, double y2) {
        String name = player.getName();
        if (y1 < y2 && !isAscending.contains(name)) {
            isAscending.add(name);
        } else {
            isAscending.remove(name);
        }
    }

    public CheckResult checkAscension(Player player, double y1, double y2) {
        int max = magic.ASCENSION_COUNT_MAX();
        String string = "";
        if (player.hasPotionEffect(PotionEffectType.JUMP)) {
            max += 12;
            string = " with jump potion";
        }
        Block block = player.getLocation().getBlock();
        if (!AntiCheat.getManager().getBackend().isMovingExempt(player) 
        		&& !Utilities.isInWater(player) 
        		&& !AntiCheat.getManager().getBackend().getBlockCheck().justBroke(player) && !Utilities.isClimbableBlock(player.getLocation().getBlock()) && !player.isInsideVehicle()) {
            String name = player.getName();
            if (y1 < y2) {
                if (!block.getRelative(BlockFace.NORTH).isLiquid() && !block.getRelative(BlockFace.SOUTH).isLiquid() && !block.getRelative(BlockFace.EAST).isLiquid() && !block.getRelative(BlockFace.WEST).isLiquid()) {
                    increment(player, ascensionCount, max);
                    if (ascensionCount.get(name) >= max) {
                        return new CheckResult(CheckResult.Result.FAILED, player.getName() + " ascended " + ascensionCount.get(name) + " times in a row (max = " + max + string + ")");
                    }
                }
            } else {
                ascensionCount.put(name, 0);
            }
        }
        return PASS;
    }


}
