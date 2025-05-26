package brainwine.gameserver.achievement;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import brainwine.gameserver.player.Player;

public class UndertakerAchievement extends Achievement {
    @Override
    public int getProgress(Player player) {
        return player.getStatistics().getUndertakings();
    }
}