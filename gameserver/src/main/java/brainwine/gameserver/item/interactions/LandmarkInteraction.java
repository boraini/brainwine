package brainwine.gameserver.item.interactions;

import brainwine.gameserver.competition.CompetitionPhase;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.util.HashMap;
import java.util.Map;

public class LandmarkInteraction implements ItemInteraction {
    private static final int VOTING_INTERVAL = 1000;
    private static final int NOMINATION_THRESHOLD = 3;
    @Override
    public void interact(Zone zone, Entity entity, int x, int y, Layer layer, Item item, int mod, MetaBlock metaBlock, Object config, Object[] data) {
        if(!entity.isPlayer()) return;

        // Do nothing if data is invalid
        if(data != null) {
            return;
        }

        Player player = (Player)entity;

        if(player.getLevel() < 10) {
            player.notify("Sorry, you must be level 10 or higher to vote on landmarks.");
            return;
        }

        long now = System.currentTimeMillis();
        if(player.getLastLandmarkVoteAt() + VOTING_INTERVAL > now) {
            player.notify("You must wait a bit before voting again.");
            return;
        }

        if(!metaBlock.hasOwner()) {
            player.notify("Sorry, this landmark is inactive.");
            return;
        }

        if(metaBlock.getOwner() == player) {
            player.notify("Sorry, you cannot upvote your own landmark.");
            return;
        }

        Map<String, Object> v = MapHelper.getMap(metaBlock.getMetadata(), "v");
        if(v != null && v.containsKey(player.getDocumentId())) {
            player.notify("You have already upvoted this landmark.");
            return;
        }

        if(zone.hasCompetition() && metaBlock.hasOwner()) {
            if(zone.getCompetition().getPhase() == CompetitionPhase.ACTIVE) {
                player.notify("This " + item.getTitle() + " is owned by " + metaBlock.getOwner().getName() + ". Come back soon to vote!");
                return;
            }
            if(zone.getCompetition().getPhase() == CompetitionPhase.NOMINATION
                    && !zone.getCompetition().getJudges().contains(player.getDocumentId())) {
                player.notify("Nominations are in progress, come back soon to vote!");
                return;
            }
            if(zone.getCompetition().getPhase() == CompetitionPhase.JUDGING && metaBlock.getIntProperty("vc") < NOMINATION_THRESHOLD) {
                player.notify("This entry did not receive enough nominations to participate in voting.");
                return;
            }
            if(zone.getCompetition().getPhase() == CompetitionPhase.FINISHED) {
                player.notify("The competition is finished.");
                return;
            }
        }

        String name = metaBlock.getStringProperty("n");
        Dialog dialog = new Dialog()
                .setTitle("Landmark Upvote")
                .setActions("Cancel", "Yes")
                .addSection(new DialogSection().setTitle("Upvote " + name + "?"));

        player.showDialog(dialog, ans -> {
            if(ans.length == 0) return;
            if("Yes".equals(ans[0])) {
                MetaBlock mb = zone.getMetaBlock(metaBlock.getX(), metaBlock.getY());
                if(mb == null) return;
                boolean nominating = false;
                if(zone.hasCompetition()) {
                    if(zone.getCompetition().getPhase() == CompetitionPhase.NOMINATION && zone.getCompetition().getJudges().contains(player.getDocumentId())) {
                        nominating = true;
                    }
                    else if(!(zone.getCompetition().getPhase() == CompetitionPhase.JUDGING && mb.getIntProperty("vc") >= NOMINATION_THRESHOLD)) {
                        player.notify("You cannot vote anymore in this competition.");
                        return;
                    }
                }

                player.showDialog(DialogHelper.messageDialog("Vote Received", "Thanks for your upvote!"));

                Map<String, Object> currentVotes = MapHelper.getMap(mb.getMetadata(), nominating ? "vj" : "v", new HashMap<>());
                currentVotes.put(player.getDocumentId(), now);

                if(nominating) return;

                int current = mb.getIntProperty("vc"); // will return 0 if null
                mb.setProperty("vc", current + 1);

                mb.getMetadata().put("v", currentVotes);
                zone.updateBlockMod(mb.getX(), mb.getY(), Layer.FRONT, 1);
                zone.sendBlockMetaUpdate(mb);

                player.setLastLandmarkVoteAt(now);

                Object vx = mb.getProperty("vx");
                if(!(vx instanceof Map<?,?>) || !((Map<?,?>)vx).containsKey(player.getDocumentId())) {
                    player.addExperience(10);
                    player.getStatistics().trackLandmarksUpvoted();

                    Player owner = mb.getOwner();
                    if(owner != null) {
                        owner.getStatistics().trackLandmarkVotesReceived();
                    }
                }
            }
        });
    }
}
