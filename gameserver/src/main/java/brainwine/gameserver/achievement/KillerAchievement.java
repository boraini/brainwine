package brainwine.gameserver.achievement;

import brainwine.gameserver.player.Player;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

public class KillerAchievement extends Achievement {
    @JsonCreator
    public KillerAchievement(@JacksonInject("title") String title) {
        super(title);
    }

    @Override
    public int getProgress(Player player) {
        return player.getStatistics().getPlayersKilled();
    }
}
