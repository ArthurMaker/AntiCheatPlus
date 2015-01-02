package net.dynamicdev.anticheat.check.checks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import net.dynamicdev.anticheat.check.AntiCheatCheck;
import net.dynamicdev.anticheat.check.CheckResult;
import net.dynamicdev.anticheat.check.CheckType;
import net.dynamicdev.anticheat.manage.AntiCheatManager;
import net.dynamicdev.anticheat.util.User;
import net.dynamicdev.anticheat.util.Utilities;

public class ChatCheck extends AntiCheatCheck {

	private Map<String, Integer> chatLevel = new HashMap<String, Integer>();
    private Map<String, Integer> commandLevel = new HashMap<String, Integer>();
    
	
	public ChatCheck(AntiCheatManager instance) {
		super(instance);
	}
	
	public void resetChatLevel(User user) {
        chatLevel.put(user.getName(), 0);
    }
	
	public void processChatSpammer(Player player) {
        User user = manager.getUserManager().getUser(player.getName());
        int level = chatLevel.containsKey(user.getName()) ? chatLevel.get(user.getName()) : 0;
        if (player != null && player.isOnline() && level >= magic.CHAT_ACTION_ONE_LEVEL()) {
            String event = level >= magic.CHAT_ACTION_TWO_LEVEL() ? manager.getConfiguration().getConfig().chatSpamActionTwo.getValue() : manager.getConfiguration().getConfig().chatSpamActionOne.getValue();
            manager.getUserManager().execute(manager.getUserManager().getUser(player.getName()), Utilities.stringToList(event), CheckType.CHAT_SPAM, lang.SPAM_KICK_REASON(), Utilities.stringToList(lang.SPAM_WARNING()), lang.SPAM_BAN_REASON());
        }
        chatLevel.put(user.getName(), level + 1);
    }
	
	public void processCommandSpammer(Player player) {
        User user = manager.getUserManager().getUser(player.getName());
        int level = commandLevel.containsKey(user.getName()) ? commandLevel.get(user.getName()) : 0;
        if (player != null && player.isOnline() && level >= magic.COMMAND_ACTION_ONE_LEVEL()) {
            String event = level >= magic.COMMAND_ACTION_TWO_LEVEL() ? manager.getConfiguration().getConfig().commandSpamActionTwo.getValue() : manager.getConfiguration().getConfig().commandSpamActionOne.getValue();
            manager.getUserManager().execute(manager.getUserManager().getUser(player.getName()), Utilities.stringToList(event), CheckType.COMMAND_SPAM, lang.SPAM_KICK_REASON(), Utilities.stringToList(lang.SPAM_WARNING()), lang.SPAM_BAN_REASON());
        }
        commandLevel.put(user.getName(), level + 1);
    }
	
	public CheckResult checkChatSpam(Player player, String msg) {
        String name = player.getName();
        User user = manager.getUserManager().getUser(name);
        if (user.getLastMessageTime() != -1) {
            for (int i = 0; i < 2; i++) {
                String m = user.getMessage(i);
                if (m == null) {
                    break;
                }
                Long l = user.getMessageTime(i);

                if (System.currentTimeMillis() - l > magic.CHAT_REPEAT_MIN() * 100) {
                    user.clearMessages();
                    break;
                } else {
                    if (manager.getConfiguration().getConfig().blockChatSpamRepetition.getValue() && m.equalsIgnoreCase(msg) && i == 1) {
                        manager.getLoggingManager().logFineInfo(player.getName() + " spam-repeated \"" + msg + "\"");
                        return new CheckResult(CheckResult.Result.FAILED, lang.SPAM_WARNING());
                    } else if (manager.getConfiguration().getConfig().blockChatSpamSpeed.getValue() && System.currentTimeMillis() - user.getLastCommandTime() < magic.COMMAND_MIN() * 2) {
                        manager.getLoggingManager().logFineInfo(player.getName() + " spammed quickly \"" + msg + "\"");
                        return new CheckResult(CheckResult.Result.FAILED, lang.SPAM_WARNING());
                    }
                }
            }
        }
        user.addMessage(msg);
        return PASS;
    }

    public CheckResult checkCommandSpam(Player player, String cmd) {
        String name = player.getName();
        User user = manager.getUserManager().getUser(name);
        if (user.getLastCommandTime() != -1) {
            for (int i = 0; i < 2; i++) {
                String m = user.getCommand(i);
                if (m == null) {
                    break;
                }
                Long l = user.getCommandTime(i);

                if (System.currentTimeMillis() - l > magic.COMMAND_REPEAT_MIN() * 100) {
                    user.clearCommands();
                    break;
                } else {
                    if (manager.getConfiguration().getConfig().blockCommandSpamRepetition.getValue() && m.equalsIgnoreCase(cmd) && i == 1) {
                        return new CheckResult(CheckResult.Result.FAILED, lang.SPAM_WARNING());
                    } else if (manager.getConfiguration().getConfig().blockCommandSpamSpeed.getValue() && System.currentTimeMillis() - user.getLastCommandTime() < magic.COMMAND_MIN() * 2) {
                        return new CheckResult(CheckResult.Result.FAILED, lang.SPAM_WARNING());
                    }
                }
            }
        }
        user.addCommand(cmd);
        return PASS;
    }

}
