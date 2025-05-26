package brainwine.gameserver.achievement;

import brainwine.gameserver.player.Player;
import com.fasterxml.jackson.annotation.JacksonInject;

public class ArchitectAchievement extends Achievement {

    @Override
    public int getProgress(Player player) {
        return player.getStatistics().getLandmarkVotesReceived();
    }
}
