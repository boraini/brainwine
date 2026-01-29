package brainwine.gameserver.command;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.player.Player;
import org.apache.logging.log4j.message.ParameterizedMessage;

public abstract class Command {
    
    public abstract void execute(CommandExecutor executor, String[] args);
    public abstract String getUsage(CommandExecutor executor);
    
    public boolean canExecute(CommandExecutor executor) {
        return true;
    }

    public boolean useSmartArguments() {
        return false;
    }
    
    protected final boolean checkArgumentCount(CommandExecutor executor, String[] args, int... counts) {
        int highestCount = 0;
        
        for(int count : counts) {
            if(count > highestCount) {
                highestCount = count;
            }
            
            if(args.length == count) {
                return true;
            }
        }
        
        if(args.length > highestCount) {
            return true;
        }
        
        sendUsageMessage(executor);
        return false;
    }
    
    protected final void sendUsageMessage(CommandExecutor executor) {
        executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
    }

    protected String getLogMessage(Player executor, String commandName, String[] args) {
        return new ParameterizedMessage("{} used command '/{}'", executor.getName(), commandName + (args.length == 0 ? "" : " " + String.join(" ", args))).getFormattedMessage();
    }

}
