package brainwine.gameserver.competition;

import brainwine.gameserver.player.Player;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CompetitionEntry {
    String player;
    int x;
    int y;
    int landmarkX;
    int landmarkY;
    String title;

    public CompetitionEntry() {}

    public CompetitionEntry(Player player, int x, int y) {
        this.player = player.getDocumentId();
        this.x = x;
        this.y = y;
        this.title = player.getName() + "'s Entry";
    }

    @JsonProperty("player")
    public String getPlayerDocumentId() {
        return player;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getTitle() {
        return title;
    }

    public String setTitle(String title) {
        return title;
    }

    public int getLandmarkX() {
        return landmarkX;
    }

    public void setLandmarkX(int landmarkX) {
        this.landmarkX = landmarkX;
    }

    public int getLandmarkY() {
        return landmarkY;
    }

    public void setLandmarkY(int landmarkY) {
        this.landmarkY = landmarkY;
    }
}
