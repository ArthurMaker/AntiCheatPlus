package net.dynamicdev.anticheat.check.checks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import net.dynamicdev.anticheat.check.AntiCheatCheck;
import net.dynamicdev.anticheat.check.CheckResult;
import net.dynamicdev.anticheat.manage.AntiCheatManager;

public class InventoryCheck extends AntiCheatCheck {

	private Map<String, Integer> blocksDropped = new HashMap<String, Integer>();
	private Map<String, Long> lastInventoryTime = new HashMap<String, Long>();
    private Map<String, Long> inventoryTime = new HashMap<String, Long>();
    private Map<String, Integer> inventoryClicks = new HashMap<String, Integer>();
    private Map<String, Long> blockTime = new HashMap<String, Long>();
	
	public InventoryCheck(AntiCheatManager instance) {
		super(instance);
	}
	
	public CheckResult checkFastDrop(Player player) {
        increment(player, blocksDropped, 10);
        if (!blockTime.containsKey(player.getName())) {
            blockTime.put(player.getName(), System.currentTimeMillis());
            return new CheckResult(CheckResult.Result.PASSED);
        } else if (blocksDropped.get(player.getName()) == magic.DROP_CHECK()) {
            long time = System.currentTimeMillis() - blockTime.get(player.getName());
            blockTime.remove(player.getName());
            blocksDropped.remove(player.getName());
            if (time < magic.DROP_TIME_MIN()) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " dropped an item too fast (actual time=" + time + ", min time=" + magic.DROP_TIME_MIN() + ")");
            }
        }
        return PASS;
    }
	
	public CheckResult checkInventoryClicks(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return PASS;
        }
        String name = player.getName();
        int clicks = 1;
        if (inventoryClicks.containsKey(name)) {
            clicks = inventoryClicks.get(name) + 1;
        }
        inventoryClicks.put(name, clicks);
        if (clicks == 1) {
            inventoryTime.put(name, System.currentTimeMillis());
        } else if (clicks == magic.INVENTORY_CHECK()) {
            long time = System.currentTimeMillis() - inventoryTime.get(name);
            inventoryClicks.put(name, 0);
            if (time < magic.INVENTORY_TIMEMIN()) {
                return new CheckResult(CheckResult.Result.FAILED, player.getName() + " clicked inventory slots " + clicks + " times in " + time + " ms (max=" + magic.INVENTORY_CHECK() + " in " + magic.INVENTORY_TIMEMIN() + " ms)");
            }
        }
        return PASS;
    }

}
