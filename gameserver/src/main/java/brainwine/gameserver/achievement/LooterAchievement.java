package brainwine.gameserver.achievement;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import brainwine.gameserver.player.Player;

public class LooterAchievement extends Achievement {
    @Override
    public int getProgress(Player player) {
        return player.getStatistics().getContainersLooted();
    }
}
