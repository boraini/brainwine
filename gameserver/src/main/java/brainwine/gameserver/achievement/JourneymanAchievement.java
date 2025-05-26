package brainwine.gameserver.achievement;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import brainwine.gameserver.player.Player;

public class JourneymanAchievement extends Achievement {
    @Override
    public boolean isCompleted(Player player) {
        return player.getZone() != null && !player.getZone().isTutorial();
    }
}
