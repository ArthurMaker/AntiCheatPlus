package net.dynamicdev.anticheat.check.checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

import net.dynamicdev.anticheat.AntiCheat;
import net.dynamicdev.anticheat.check.AntiCheatCheck;
import net.dynamicdev.anticheat.check.CheckResult;
import net.dynamicdev.anticheat.manage.AntiCheatManager;
import net.dynamicdev.anticheat.util.Utilities;

public class BlockCheck extends AntiCheatCheck {

	
	private Map<String, Integer> fastBreakViolation = new HashMap<String, Integer>();
    private Map<String, Integer> fastBreaks = new HashMap<String, Integer>();
    private Map<String, Boolean> blockBreakHolder = new HashMap<String, Boolean>();
    private Map<String, Long> lastBlockBroken = new HashMap<String, Long>();
    private Map<String, Integer> fastPlaceViolation = new HashMap<String, Integer>();
    private Map<String, Long> lastBlockPlaced = new HashMap<String, Long>();
    private Map<String, Long> lastBlockPlaceTime = new HashMap<String, Long>();
    private Map<String, Long> instantBreakExempt = new HashMap<String, Long>();
    private Map<String, Material> itemInHand = new HashMap<String, Material>();
    private HashSet<Byte> transparent = new HashSet<Byte>();

    private Map<String, Long> brokenBlock = new HashMap<String, Long>();
    private Map<String, Long> placedBlock = new HashMap<String, Long>();
    
	
	public BlockCheck(AntiCheatManager instance) {
		super(instance);
        transparent.add((byte) -1);
	}
	
	public boolean justPlaced(Player player) {
        return isDoing(player, placedBlock, magic.BLOCK_PLACE_MIN());
    }
	
    public void logBlockPlace(final Player player) {
        placedBlock.put(player.getName(), System.currentTimeMillis());
    }
    
	public void logInstantBreak(final Player player) {
        instantBreakExempt.put(player.getName(), System.currentTimeMillis());
    }

    public boolean isInstantBreakExempt(Player player) {
        return isDoing(player, instantBreakExempt, magic.INSTANT_BREAK_TIME());
    }
    
    public void logBlockBreak(final Player player) {
        brokenBlock.put(player.getName(), System.currentTimeMillis());
        AntiCheat.getManager().getBackend().resetAnimation(player);
    }

    public boolean justBroke(Player player) {
        return isDoing(player, brokenBlock, magic.BLOCK_BREAK_MIN());
    }

	
	public void logAnimation(final Player player) {
		AntiCheat.getManager().getBackend().animated.put(player.getName(), System.currentTimeMillis());
        increment(player, AntiCheat.getManager().getBackend().blockPunches, magic.BLOCK_PUNCH_MIN());
        itemInHand.put(player.getName(), player.getItemInHand().getType());
        AntiCheat.getManager().getBackend().interactionCount.put(player.getName(), 0);
    }
	
	public CheckResult checkLongReachBlock(Player player, double x, double y, double z) {
        if (isInstantBreakExempt(player)) {
            return new CheckResult(CheckResult.Result.PASSED);
        } else {
            String string = player.getName() + " reached too far for a block";
            double distance =
                    player.getGameMode() == GameMode.CREATIVE ? magic.BLOCK_MAX_DISTANCE_CREATIVE()
                            : player.getLocation().getDirection().getY() > 0.9 ? magic.BLOCK_MAX_DISTANCE_CREATIVE()
                            : magic.BLOCK_MAX_DISTANCE();
            double i = x >= distance ? x : y > distance ? y : z > distance ? z : -1;
            if (i != -1) {
                return new CheckResult(CheckResult.Result.FAILED, string + " (distance=" + i + ", max=" + magic.BLOCK_MAX_DISTANCE() + ")");
            } else {
                return PASS;
            }
        }
    }
	

    public CheckResult checkSwing(Player player, Block block) {
        String name = player.getName();
        if (!isInstantBreakExempt(player)) {
            if (!player.getInventory().getItemInHand().containsEnchantment(Enchantment.DIG_SPEED) && !(player.getInventory().getItemInHand().getType() == Material.SHEARS && block.getType() == Material.LEAVES)) {
                if (AntiCheat.getManager().getBackend().blockPunches.get(name) != null && player.getGameMode() != GameMode.CREATIVE) {
                    int i = AntiCheat.getManager().getBackend().blockPunches.get(name);
                    if (i < magic.BLOCK_PUNCH_MIN()) {
                        return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to break a block of " + block.getType() + " after only " + i + " punches (min=" + magic.BLOCK_PUNCH_MIN() + ")");
                    } else {
                    	AntiCheat.getManager().getBackend().blockPunches.put(name, 0); // it should reset after EACH block break.
                    }
                }
            }
        }
        return PASS;
    }

    public CheckResult checkFastBreak(Player player, Block block) {
        int violations = magic.FASTBREAK_MAXVIOLATIONS();
        long timemax = isInstantBreakExempt(player) ? 0 : Utilities.calcSurvivalFastBreak(player.getInventory().getItemInHand(), block.getType());
        if (player.getGameMode() == GameMode.CREATIVE) {
            violations = magic.FASTBREAK_MAXVIOLATIONS_CREATIVE();
            timemax = magic.FASTBREAK_TIMEMAX_CREATIVE();
        }
        String name = player.getName();
        if (!fastBreakViolation.containsKey(name)) {
            fastBreakViolation.put(name, 0);
        } else {
            Long math = System.currentTimeMillis() - lastBlockBroken.get(name);
            int i = fastBreakViolation.get(name);
            if (i > violations && math < magic.FASTBREAK_MAXVIOLATIONTIME()) {
                lastBlockBroken.put(name, System.currentTimeMillis());
                if (!silentMode()) {
                    sendFormattedMessage(player, "Fastbreaking detected. Please wait 10 seconds before breaking blocks.");
                }
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " broke blocks too fast " + i + " times in a row (max=" + violations + ")");
            } else if (fastBreakViolation.get(name) > 0 && math > magic.FASTBREAK_MAXVIOLATIONTIME()) {
                fastBreakViolation.put(name, 0);
            }
        }
        if (!fastBreaks.containsKey(name) || !lastBlockBroken.containsKey(name)) {
            if (!lastBlockBroken.containsKey(name)) {
                lastBlockBroken.put(name, System.currentTimeMillis());
            }
            if (!fastBreaks.containsKey(name)) {
                fastBreaks.put(name, 0);
            }
        } else {
            Long math = System.currentTimeMillis() - lastBlockBroken.get(name);
            if ((math != 0L && timemax != 0L)) {
                if (math < timemax) {
                    if (fastBreakViolation.containsKey(name) && fastBreakViolation.get(name) > 0) {
                        fastBreakViolation.put(name, fastBreakViolation.get(name) + 1);
                    } else {
                        fastBreaks.put(name, fastBreaks.get(name) + 1);
                    }
                    blockBreakHolder.put(name, false);
                }
                if (fastBreaks.get(name) >= magic.FASTBREAK_LIMIT() && math < timemax) {
                    int i = fastBreaks.get(name);
                    fastBreaks.put(name, 0);
                    fastBreakViolation.put(name, fastBreakViolation.get(name) + 1);
                    return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to break " + i + " blocks in " + math + " ms (max=" + magic.FASTBREAK_LIMIT() + " in " + timemax + " ms)");
                } else if (fastBreaks.get(name) >= magic.FASTBREAK_LIMIT() || fastBreakViolation.get(name) > 0) {
                    if (!blockBreakHolder.containsKey(name) || !blockBreakHolder.get(name)) {
                        blockBreakHolder.put(name, true);
                    } else {
                        fastBreaks.put(name, fastBreaks.get(name) - 1);
                        if (fastBreakViolation.get(name) > 0) {
                            fastBreakViolation.put(name, fastBreakViolation.get(name) - 1);
                        }
                        blockBreakHolder.put(name, false);
                    }
                }
            }
        }

        lastBlockBroken.put(name, System.currentTimeMillis()); // always keep a log going.
        return PASS;
    }

    public CheckResult checkFastPlace(Player player) {
        int violations = player.getGameMode() == GameMode.CREATIVE ? magic.FASTPLACE_MAXVIOLATIONS_CREATIVE() : magic.FASTPLACE_MAXVIOLATIONS();
        long time = System.currentTimeMillis();
        String name = player.getName();
        if (!lastBlockPlaceTime.containsKey(name) || !fastPlaceViolation.containsKey(name)) {
            lastBlockPlaceTime.put(name, 0L);
            if (!fastPlaceViolation.containsKey(name)) {
                fastPlaceViolation.put(name, 0);
            }
        } else if (fastPlaceViolation.containsKey(name) && fastPlaceViolation.get(name) > violations) {
            AntiCheat.debugLog("Noted that fastPlaceViolation contains key " + name + " with value " + fastPlaceViolation.get(name));
            Long math = System.currentTimeMillis() - lastBlockPlaced.get(name);
            AntiCheat.debugLog("Player lastBlockPlaced value = " + lastBlockPlaced + ", diff=" + math);
            double multiplier = 0.75; //Accounting for lowered tick delay in 1.8 and higher
            if (lastBlockPlaced.get(name) > 0 && math < magic.FASTPLACE_MAXVIOLATIONTIME() * multiplier) {
                lastBlockPlaced.put(name, time);
                if (!silentMode()) {
                    sendFormattedMessage(player, "Fastplacing detected. Please wait 10 seconds before placing blocks.");
                }
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " placed blocks too fast " + fastBreakViolation.get(name) + " times in a row (max=" + violations + ")");
            } else if (lastBlockPlaced.get(name) > 0 && math > magic.FASTPLACE_MAXVIOLATIONTIME()) {
                AntiCheat.debugLog("Reset facePlaceViolation for " + name);
                fastPlaceViolation.put(name, 0);
            }
        } else if (lastBlockPlaced.containsKey(name)) {
            long last = lastBlockPlaced.get(name);
            long lastTime = lastBlockPlaceTime.get(name);
            long thisTime = time - last;

            if (lastTime != 0 && thisTime < magic.FASTPLACE_TIMEMIN()) {
                lastBlockPlaceTime.put(name, (time - last));
                lastBlockPlaced.put(name, time);
                fastPlaceViolation.put(name, fastPlaceViolation.get(name) + 1);
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " tried to place a block " + thisTime + " ms after the last one (min=" + magic.FASTPLACE_TIMEMIN() + " ms)");
            }
            lastBlockPlaceTime.put(name, (time - last));
        }
        lastBlockPlaced.put(name, time);
        return PASS;
    }
    
    public CheckResult checkAutoTool(Player player) {
        if (itemInHand.containsKey(player.getName()) && itemInHand.get(player.getName()) != player.getItemInHand().getType()) {
            return new CheckResult(CheckResult.Result.FAILED, player.getName() + " switched tools too fast (had " + itemInHand.get(player.getName()) + ", has " + player.getItemInHand().getType() + ")");
        } else {
            return PASS;
        }
    }
    
    public CheckResult checkBlockRotation(Player player, BlockBreakEvent event)
    {
    	double offset = 0.0D;

    	Location blockLoc = event.getBlock().getLocation().add(0.5, 0.6, 0.5);
    	Location playerLoc = player.getLocation().add(0, player.getEyeHeight(), 0);

    	Vector playerRotation = new Vector(playerLoc.getYaw(), playerLoc.getPitch(), 0);
    	Vector expectedRotation = Utilities.getRotation(playerLoc, blockLoc);

    	double deltaYaw = Utilities.clamp180(playerRotation.getX() - expectedRotation.getX());
    	double deltaPitch = Utilities.clamp180(playerRotation.getY() - expectedRotation.getY());

    	double horizontalDistance = Utilities.getHorizontalDistance(playerLoc, blockLoc);
    	double distance = Utilities.getDistance3D(playerLoc, blockLoc);

    	double offsetX = deltaYaw * horizontalDistance * distance;
    	double offsetY = deltaPitch * Math.abs(blockLoc.getY() - playerLoc.getY()) * distance;

    	offset += Math.abs(offsetX);
    	offset += Math.abs(offsetY);

    	if (offset > magic.DIRECTION_MAX_BUFFER()) {
    		return new CheckResult(CheckResult.Result.FAILED, 
    				player.getName() + " attempted to break a block without looking at it.");
    	}
    	return PASS;
    }


}
