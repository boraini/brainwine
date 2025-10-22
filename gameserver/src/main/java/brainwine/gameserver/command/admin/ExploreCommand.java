package brainwine.gameserver.command.admin;

import brainwine.gameserver.anticheat.Exploration;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

@CommandInfo(name = "explore", description = "Explores the sky and underground of the zone until reaching a given exploration progress.")
public class ExploreCommand extends Command {

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length > 2) {
            executor.notify("Usage: " + getUsage(executor), NotificationType.SYSTEM);
            return;
        }
        if(executor instanceof Player) {
            Zone zone = ((Player) executor).getZone();
            float progress = 1.0f;
            Exploration.Region region = Exploration.Region.ALL;

            // Parse the arguments. Any of them can be omitted.
            boolean argCheck = args.length == 0;
            if(args.length == 2) {
                try {
                    progress = parsePercent(args[1]);
                    region = Exploration.Region.valueOf(args[0].toUpperCase());
                    argCheck = true;
                } catch(Exception ignored) {}
            }
            if(args.length == 1) {
                try {
                    progress = parsePercent(args[0]);
                    argCheck = true;
                } catch(Exception ignored) {}
                try {
                    region = Exploration.Region.valueOf(args[0].toUpperCase());
                    argCheck = true;
                } catch(Exception ignored) {}
            }
            if(!argCheck) {
                executor.notify("Usage: " + getUsage(executor), NotificationType.SYSTEM);
                return;
            }

            if(progress < -0.01 || progress > 1.01) {
                executor.notify("Percent must be between 0.0-1.0 or 0%-100%", NotificationType.SYSTEM);
                return;
            }

            int skyChunksSeen = 0;
            int undergroundChunksSeen = 0;
            int skyChunksExplored = 0;
            int undergroundChunksExplored = 0;
            int chunksExploredNow = 0;
            for(int i = 0; i < zone.getNumChunksWidth(); i++) {
                int firstUnderground = zone.getSurface()[zone.getChunkWidth() * i + zone.getChunkWidth() / 2] / zone.getChunkHeight();
                if(region == Exploration.Region.ALL || region == Exploration.Region.SKY) for(int j = 0; j < firstUnderground; j++) {
                    skyChunksSeen++;
                    if(zone.getChunksExplored()[j * zone.getNumChunksWidth() + i]) skyChunksExplored++;
                    else if(skyChunksSeen * progress > skyChunksExplored) {
                        zone.exploreArea(i * zone.getChunkWidth(), j * zone.getChunkHeight(), (Player)executor);
                        skyChunksExplored++;
                        chunksExploredNow++;
                    }
                }
                if(region == Exploration.Region.ALL || region == Exploration.Region.UNDERGROUND) for(int j = firstUnderground; j < zone.getNumChunksHeight(); j++) {
                    undergroundChunksSeen++;
                    if(zone.getChunksExplored()[j * zone.getNumChunksWidth() + i]) undergroundChunksExplored++;
                    else if(undergroundChunksSeen * progress > undergroundChunksExplored) {
                        zone.exploreArea(i * zone.getChunkWidth(), j * zone.getChunkHeight(), (Player)executor);
                        undergroundChunksExplored++;
                        chunksExploredNow++;
                    }
                }
            }

            executor.notify(String.format("%d more chunks explored.", chunksExploredNow), NotificationType.SYSTEM);
            zone.recalculateChunksExploredCount();
        }
    }

    float parsePercent(String input) throws NumberFormatException {
        if(input.contains("%")) {
            return Float.parseFloat(input.replaceAll("%", "")) / 100.0f;
        } else {
            return Float.parseFloat(input);
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/explore [all|sky|underground] [0%-100% or 0.0-1.0]";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor instanceof Player && executor.isAdmin();
    }
}
