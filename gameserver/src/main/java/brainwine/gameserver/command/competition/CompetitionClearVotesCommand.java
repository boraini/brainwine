package brainwine.gameserver.command.competition;

import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.competition.CompetitionPhase;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name="compclearvotes", aliases={"compoclearvotes", "competitionclearvotes"}, description="Clears the votes in a competition.")
public class CompetitionClearVotesCommand extends CompetitionCommand {
    @Override
    public void execute(Zone zone, Player player, String[] args) {
        if(!zone.hasCompetition() || zone.getCompetition().getPhase() != CompetitionPhase.FINISHED) {
            player.notify("This world has no ongoing competition right now.", SYSTEM);
            return;
        }

        String scope = args.length >= 1 ? args[0] : "everyone";

        if(!Arrays.asList("eveyone", "judges", "nonjudges").contains(scope)) {
            player.notify("Invalid scope: " + scope + ". Must be one of everyone, judges, nonjudges.", SYSTEM);
        }

        boolean shouldRemoveNonJudge = !"judges".equals(scope);
        boolean shouldRemoveJudge = !"nonjudges".equals(scope);
        for(MetaBlock metaBlock : zone.getMetaBlocksWithUse(ItemUseType.LANDMARK)) {
            Object v = metaBlock.getProperty("vc");
            if(v == null) continue;
            Map<String, Object> newVx = new HashMap<>();
            Object vx = metaBlock.getProperty("vx");
            if(vx instanceof Map<?,?>) {
                for(Map.Entry<?,?> entry : ((Map<?, ?>) vx).entrySet()) {
                    if(entry.getKey() instanceof String) {
                        newVx.put((String)entry.getKey(), entry.getValue());
                    }
                }
            }
            int votesToRemove = 0;
            if(v instanceof Map<?,?>) {
                Map<String, Object> leftover = new HashMap<>();
                Collection<String> judges = zone.getCompetition().getJudges();
                for(Map.Entry<?,?> entry : ((Map<?,?>)v).entrySet()) {
                    if(entry.getKey() instanceof String) {
                        if(shouldRemoveJudge && judges.contains(entry.getKey())
                                || shouldRemoveNonJudge && !judges.contains(entry.getKey())) {
                            newVx.put((String)entry.getKey(), entry.getValue());
                            votesToRemove++;
                        } else {
                            leftover.put((String)entry.getKey(), entry.getValue());
                        }
                    }
                }

                metaBlock.setProperty("v", leftover);
            }
            int vc = metaBlock.getIntProperty("vc");
            metaBlock.setProperty("vc", Math.max(0, vc - votesToRemove));
            zone.sendBlockMetaUpdate(metaBlock);
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/compclearvotes [everyone|judges|nonjudges]";
    }
}
