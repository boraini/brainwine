package brainwine.gameserver.command.admin;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.EffectMessage;
import brainwine.gameserver.util.MapHelper;

// To add more music add new emitters in config-particles.yml. The emitter name should be in form "music musicname"

@CommandInfo(name = "music", description = "Plays ambient sound in the world.", aliases = { "mus" })
public class MusicCommand extends Command {

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length < 1) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }

        String musicName = args[0];

        Map<String, String> emitters = MapHelper.getMap(GameConfiguration.getBaseConfig(), "emitters");

        if (emitters == null) {
            executor.notify("The music command is not configured for this server.", SYSTEM);
            return;
        }

        if ("list".equals(musicName)) {
            List<String> allMusic = emitters.keySet().stream()
              .filter(k -> k.startsWith("music "))
              .map(k -> {
                int start = k.indexOf(" ");
                if (start != 1) {
                    return k.substring(start + 1);
                } else {
                    return k;
                }
              }).toList();

            int pageSize = 8;
            int pageCount = (int)Math.ceil(allMusic.size() / (double)pageSize);
            int page = 1;
            
            if (args.length > 1) {
                page = Math.max(1, NumberUtils.toInt(args[1], page));
            }
            
            executor.notify(String.format("========== Music List (Page %s of %s) ==========", page, pageCount), SYSTEM);

            int start = Math.max(0, (page - 1) * pageSize);
            int stop = Math.min(allMusic.size(), page * pageSize);

            for (int i = start; i < stop; i++) {
                executor.notify(String.format("- %s", allMusic.get(i)), SYSTEM);
            }

            return;
        }

        if (!(executor instanceof Player)) {
            executor.notify("Cannot play music from the server command line yet.", SYSTEM);
            return;
        }

        String effectName = String.format("music %s", musicName);

        if (emitters.containsKey(effectName)) {
            Player player = (Player) executor;

            // TODO: sound moves with players
            if (player.getZone() != null) for (Player other : player.getZone().getPlayers()) {
                EffectMessage message = new EffectMessage(other.getX(), other.getY(), effectName, 10.0f);
                other.sendMessage(message);
            }

            return;
        } else {
            executor.notify(String.format("Music %s is not known to the server.", musicName), SYSTEM);
            return;
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/music list [page] or /music <name>";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        // we need the zone information for the player
        return executor.isAdmin();
    }

}
