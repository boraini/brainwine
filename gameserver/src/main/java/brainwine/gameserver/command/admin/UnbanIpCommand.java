package brainwine.gameserver.command.admin;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.IpBans;
import brainwine.gameserver.util.Cidr;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name = "unbanip", description = "Bans a player from the server.")
public class UnbanIpCommand extends Command {

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length < 1) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }

        Cidr target;
        try {
            target = Cidr.create(args[0]);
        } catch(IllegalArgumentException e) {
            executor.notify("Provided IP address is not in a valid format. " + e.getMessage(), SYSTEM);
            return;
        }

        if(!GameServer.getInstance().getIpBans().isCidrBanned(target)) {
            IpBans.Item item = GameServer.getInstance().getIpBans().getIpBanItem(target);
            if(item == null) {
                executor.notify("This specific CIDR was not banned. No changes have been made.", SYSTEM);
            } else {
                executor.notify("This specific CIDR was not banned, however " + item.getIpAddress() + " is. No changes have been made.", SYSTEM);
            }
            return;
        }

        GameServer.getInstance().getIpBans().unbanCidr(target);

        executor.notify(String.format("Players connecting from %s will no longer be kicked or banned.", target), SYSTEM);
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        return "/unbanip <ip or CIDR>";
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin();
    }
}
