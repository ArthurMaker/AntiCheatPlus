package net.dynamicdev.anticheat.check.checks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.dynamicdev.anticheat.AntiCheat;
import net.dynamicdev.anticheat.check.AntiCheatCheck;
import net.dynamicdev.anticheat.check.CheckResult;
import net.dynamicdev.anticheat.manage.AntiCheatManager;
import net.dynamicdev.anticheat.util.Utilities;

public class EntityCheck extends AntiCheatCheck {

	private Map<String, Integer> projectilesShot = new HashMap<String, Integer>();
	private Map<String, Long> startEat = new HashMap<String, Long>();
    private Map<String, Long> lastHeal = new HashMap<String, Long>();
    private Map<String, Long> projectileTime = new HashMap<String, Long>();
    private Map<String, Long> bowWindUp = new HashMap<String, Long>();
    private Map<String, Long> sprinted = new HashMap<String, Long>();
    private Map<String, Long> lastAttack = new HashMap<String, Long>();

	
	public EntityCheck(AntiCheatManager instance) {
		super(instance);
	}
	
	public void logSprint(final Player player) {
        sprinted.put(player.getName(), System.currentTimeMillis());
    }
	
	public CheckResult checkFastBow(Player player, float force) {
        // Ignore magic numbers here, they are minecrafty vanilla stuff.
        int ticks = (int) ((((System.currentTimeMillis() - bowWindUp.get(player.getName())) * 20) / 1000) + 3);
        bowWindUp.remove(player.getName());
        float f = (float) ticks / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        f = f > 1.0F ? 1.0F : f;
        if (Math.abs(force - f) > magic.BOW_ERROR()) {
            return new CheckResult(CheckResult.Result.FAILED, player.getName() + " fired their bow too fast (actual force=" + force + ", calculated force=" + f + ")");
        } else {
            return PASS;
        }
    }
	
	public CheckResult checkProjectile(Player player) {
        increment(player, projectilesShot, 10);
        if (!projectileTime.containsKey(player.getName())) {
            projectileTime.put(player.getName(), System.currentTimeMillis());
            return new CheckResult(CheckResult.Result.PASSED);
        } else if (projectilesShot.get(player.getName()) == magic.PROJECTILE_CHECK()) {
            long time = System.currentTimeMillis() - projectileTime.get(player.getName());
            projectileTime.remove(player.getName());
            projectilesShot.remove(player.getName());
            if (time < magic.PROJECTILE_TIME_MIN()) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " wound up a bow too fast (actual time=" + time + ", min time=" + magic.PROJECTILE_TIME_MIN() + ")");
            }
        }
        return PASS;
    }
	
	public CheckResult checkLongReachDamage(Player player, double x, double y, double z) {
        String string = player.getName() + " reached too far for an entity";
        double i = x >= magic.ENTITY_MAX_DISTANCE() ? x : y > magic.ENTITY_MAX_DISTANCE() ? y : z > magic.ENTITY_MAX_DISTANCE() ? z : -1;
        if (i != -1) {
            return new CheckResult(CheckResult.Result.FAILED, string + " (distance=" + i + ", max=" + magic.ENTITY_MAX_DISTANCE() + ")");
        } else {
            return PASS;
        }
    }
	

    public CheckResult checkSight(Player player, Entity entity) {
        /*if (entity instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) entity;
            // Check to make sure the entity's head is not surrounded
            Block head = le.getWorld().getBlockAt((int) le.getLocation().getX(), (int) (le.getLocation().getY() + le.getEyeHeight()), (int) le.getLocation().getZ());
            boolean solid = false;
            // TODO: This sucks. See if it's possible to not have as many false-positives while still retaining most of the check.
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = -1; y < 2; y++) {
                        if (head.getRelative(x, y, z).getTypeId() != 0) {
                            if (head.getRelative(x, y, z).getType().isSolid()) {
                                solid = true;
                                break;
                            }

                        }
                    }
                }

            }
            if (solid) {
                return PASS;
            }
            // TODO: Needs proper testing
            Location mobLocation = le.getEyeLocation();
            for (Block block : player.getLineOfSight(transparent, 5)) {
                if (Math.abs(block.getLocation().getX() - mobLocation.getX()) < 2.3 || Math.abs(block.getLocation().getZ() - mobLocation.getZ()) < 2.3) {
                    return PASS;
                }
            }
            return new CheckResult(Result.FAILED, player.getName()+" tried to damage an entity ("+le.getType()+") out of sight ");
        }*/
        return PASS;
    }

    public void logBowWindUp(Player player) {
        bowWindUp.put(player.getName(), System.currentTimeMillis());
    }

    public void logEatingStart(Player player) {
        startEat.put(player.getName(), System.currentTimeMillis());
    }

    public void logHeal(Player player) {
        lastHeal.put(player.getName(), System.currentTimeMillis());
    }

    public CheckResult checkSprintDamage(Player player) {
        if (isDoing(player, sprinted, magic.SPRINT_MIN())) {
            return new CheckResult(CheckResult.Result.FAILED, player.getName() + " sprinted and damaged an entity too fast (min sprint=" + magic.SPRINT_MIN() + " ms)");
        } else {
            return PASS;
        }
    }
    
    public CheckResult checkFightSpeed(Player player)
    {
    	String name = player.getName();
    	if(!lastAttack.containsKey(name))
    	{
    		lastAttack.put(name, System.currentTimeMillis());
    	}
    	long math = System.currentTimeMillis() - lastAttack.get(name);
    	if(math < magic.FIGHT_TIME_MIN())
    	{
    		return new CheckResult(CheckResult.Result.FAILED, 
    				name + " attempted to attack faster than normal. (min="+magic.FIGHT_TIME_MIN()
    				+ " | them=" + math);
    	}
    	return PASS;
    }
    
    public CheckResult checkFightDistance(Player player, LivingEntity damaged)
    {
    	String name = player.getName();
    	Location entityLoc = damaged.getLocation().add(0, damaged.getEyeHeight(), 0);
    	Location playerLoc = player.getLocation().add(0, player.getEyeHeight(), 0);

    	double distance = Utilities.getDistance3D(entityLoc, playerLoc);
    	if(distance > magic.FIGHT_MIN_DISTANCE())
    	{
    		return new CheckResult(CheckResult.Result.FAILED, 
    				name + " attempted to attack something too far away. (min="+magic.FIGHT_MIN_DISTANCE()
    				+ " | them=" + distance);
    	}
    	return PASS;
    }
    
    public CheckResult checkFightRotation(Player player, LivingEntity damaged)
    {
    	double offset = 0.0D;

    	Location entityLoc = damaged.getLocation().add(0, damaged.getEyeHeight(), 0);
    	Location playerLoc = player.getLocation().add(0, player.getEyeHeight(), 0);

    	Vector playerRotation = new Vector(playerLoc.getYaw(), playerLoc.getPitch(), 0);
    	Vector expectedRotation = Utilities.getRotation(playerLoc, entityLoc);

    	double deltaYaw = Utilities.clamp180(playerRotation.getX() - expectedRotation.getX());
    	double deltaPitch = Utilities.clamp180(playerRotation.getY() - expectedRotation.getY());

    	double horizontalDistance = Utilities.getHorizontalDistance(playerLoc, entityLoc);
    	double distance = Utilities.getDistance3D(playerLoc, entityLoc);

    	double offsetX = deltaYaw * horizontalDistance * distance;
    	double offsetY = deltaPitch * Math.abs(entityLoc.getY() - playerLoc.getY()) * distance;

    	offset += Math.abs(offsetX);
    	offset += Math.abs(offsetY);

    	if (offset > magic.DIRECTION_MAX_BUFFER()) {
    		return new CheckResult(CheckResult.Result.FAILED, 
    				player.getName() + " attempted to attack something without looking at it.");
    	}
    	return PASS;

    }
    
    public CheckResult checkAnimation(Player player, Entity e) {
        if (!AntiCheat.getManager().getBackend().justAnimated(player)) {
            return new CheckResult(CheckResult.Result.FAILED, player.getName() + " didn't animate before damaging a " + e.getType());
        } else {
            return PASS;
        }
    }
    
    public CheckResult checkFastHeal(Player player) {
        if (lastHeal.containsKey(player.getName())) // Otherwise it was modified by a plugin, don't worry about it.
        {
            long l = lastHeal.get(player.getName());
            lastHeal.remove(player.getName());
            if ((System.currentTimeMillis() - l) < magic.HEAL_TIME_MIN()) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " healed too quickly (time=" + (System.currentTimeMillis() - l) + " ms, min=" + magic.HEAL_TIME_MIN() + " ms)");
            }
        }
        return PASS;
    }

    public CheckResult checkFastEat(Player player) {
        if (startEat.containsKey(player.getName())) // Otherwise it was modified by a plugin, don't worry about it.
        {
            long l = startEat.get(player.getName());
            startEat.remove(player.getName());
            if ((System.currentTimeMillis() - l) < magic.EAT_TIME_MIN()) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " ate too quickly (time=" + (System.currentTimeMillis() - l) + " ms, min=" + magic.EAT_TIME_MIN() + " ms)");
            }
        }
        return PASS;
    }

    
}
