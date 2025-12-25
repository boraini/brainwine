package brainwine.gameserver.command.admin;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dailyreward.DailyRewardSession;
import brainwine.gameserver.player.Player;

@CommandInfo(name = "rewards", description = "Manage the daily rewards.")
public class RewardsCommand extends Command {
    
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length != 0) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }
        
        new DailyRewardSession((Player)executor).showNextDialog();
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        return "/rewards";
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin() && executor instanceof Player;
    }
}
