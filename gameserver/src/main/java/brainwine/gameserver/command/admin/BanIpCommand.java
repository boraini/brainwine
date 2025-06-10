package brainwine.gameserver.command.admin;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;
import brainwine.gameserver.util.Cidr;
import brainwine.gameserver.util.DateTimeUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name = "banip", description = "Bans a player from the server.")
public class BanIpCommand extends Command {

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

        Player scapegoat = null;
        if(args.length >= 2) {
            scapegoat = GameServer.getInstance().getPlayerManager().getPlayer(args[1]);
            if(scapegoat == null) {
                executor.notify("Scapegoat " + args[1] + " does not exist.", SYSTEM);
                return;
            }
        }

        if(executor instanceof Player && target.matches(((Player)executor).getConnection().getIpAddress())) {
            executor.notify("You would be banning your own IP this way.", SYSTEM);
            return;
        }

        GameServer.getInstance().getIpBans().banCidr(target, scapegoat);

        if(scapegoat == null) {
            executor.notify(String.format("Connections from %s will be blocked.", target), SYSTEM);
        } else {
            executor.notify(String.format("Players connecting from %s will be banned for the same reason as %s.", target, scapegoat.getName()), SYSTEM);
        }

        for(Player player : GameServer.getInstance().getPlayerManager().getOnlinePlayers()) {
            if(target.matches(player.getConnection().getIpAddress())) {
                player.kick("Your IP address has been blocked.");
            }
        }
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        return "/banip <ip or CIDR> [scapegoat player]";
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin();
    }
}
