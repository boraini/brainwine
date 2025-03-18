package brainwine.gameserver.minigames;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileMatchingMinigame extends WorldMinigame {
    @JsonProperty private int top;
    @JsonProperty private int left;
    @JsonProperty private int width;
    @JsonProperty private int height;
    @JsonProperty private String jokerTile;
    @JsonProperty private String hideTile = "containers/barrel";
    @JsonProperty private List<String> tiles;

    private boolean initialized;
    private long lastMoveTime;
    private long clearInterval = 500;
    private int firstIndex = -1;
    private List<String> shuffledTiles;
    private List<Boolean> isTilePopped;
    private List<Integer> revealedTiles = new ArrayList<>();
    int numPoppedTiles = 0;

    @Override
    protected void initialize() {
        if((width * height) % 2 == 0 && jokerTile != null) {
            logger.error("Joker tile exists yet the number of tiles is even.");
            return;
        }

        if((width * height) % 2 == 1 && jokerTile == null) {
            logger.error("Joker tile doesn't exist yet the number of tiles is odd.");
            return;
        }

        shuffledTiles = new ArrayList<>(width * height);
        isTilePopped = new ArrayList<>(width * height);

        for(int i = 0; i < width * height / 2; i++) {
            String tile = tiles.get(i % tiles.size());
            shuffledTiles.add(tile);
            shuffledTiles.add(tile);
            isTilePopped.add(false);
            isTilePopped.add(false);
        }

        if(jokerTile != null) {
            shuffledTiles.add(jokerTile);
            isTilePopped.add(false);
        }

        Collections.shuffle(shuffledTiles);

        for(int i = 0; i < width * height; i++) {
            zone.updateBlock(getWorldX(i), getWorldY(i), Layer.FRONT, hideTile);
        }

        initialized = true;
    }

    @Override
    public boolean useBlock(Player player, int x, int y, MetaBlock metaBlock) {
        if(player == null || metaBlock == null) {
            return true;
        }
        if(!initialized) {
            return false;
        }

        if(firstIndex != -1 && System.currentTimeMillis() < lastMoveTime + clearInterval) {
            return false;
        }

        if(!inBounds(metaBlock)) {
            return false;
        }

        // Tile is already popped.
        if(isTilePopped.get(getIndex(metaBlock))) return false;

        // First move.
        if(firstIndex == -1) {
            firstIndex = getIndex(metaBlock);
            revealTile(firstIndex);
            return false;
        }

        int secondIndex = getIndex(metaBlock);

        // In case the display block has use and the player clicks on it.
        if(firstIndex == secondIndex) return false;

        lastMoveTime = System.currentTimeMillis();
        revealTile(secondIndex);

        boolean firstIsJoker = jokerTile != null && jokerTile.equals(shuffledTiles.get(firstIndex));
        boolean secondIsJoker = jokerTile != null && jokerTile.equals(shuffledTiles.get(secondIndex));

        if(firstIsJoker || secondIsJoker) {
            int wantedIndex = firstIsJoker ? secondIndex : firstIndex;
            String wanted = shuffledTiles.get(wantedIndex);


            int thirdIndex = -1;

            for(int i = 0; i < shuffledTiles.size(); i++) {
                if(wanted.equals(shuffledTiles.get(i)) && i != wantedIndex) {
                    thirdIndex = 0;
                    break;
                }
            }

            if(thirdIndex != -1) {
                revealTile(thirdIndex);
                popTile(thirdIndex);
            }
            popTile(firstIndex);
            popTile(secondIndex);
            clearInterval = 1000;
        } else if(shuffledTiles.get(firstIndex).equals(shuffledTiles.get(secondIndex))) {
            popTile(firstIndex);
            popTile(secondIndex);
        }

        if(numPoppedTiles == width * height) {
            player.addExperience(100, "Completed minigame");
            player.showDialog(DialogHelper.getDialog("You win! You can now leave the zone."));
        }

        return false;
    }

    @Override
    public void tick(float deltaTime) {
        if(System.currentTimeMillis() > lastMoveTime + clearInterval && revealedTiles.size() >= 2) {
            doAnimationsBeforeNextMove();
            lastMoveTime = 0;
            clearInterval = 500;
            firstIndex = -1;
            revealedTiles.clear();
        }
    }

    @Override
    public void leaveZone(Player player) {
        if(zone.getPlayers().isEmpty() && zone.getName().contains("copied for")) {
            GameServer.getInstance().getZoneManager().deleteZone(zone);
        }
    }

    protected void revealTile(int index) {
        revealedTiles.add(index);
        zone.updateBlock(getWorldX(index), getWorldY(index), Layer.FRONT, shuffledTiles.get(index));
    }

    private void popTile(int index) {
        isTilePopped.set(index, true);
        numPoppedTiles++;
    }

    private void doAnimationsBeforeNextMove() {
        for(int index : revealedTiles) {
            if(isTilePopped.get(index)) {
                zone.spawnEffect(getWorldX(index), getWorldY(index), "bomb-electric", 2);
                zone.updateBlock(getWorldX(index), getWorldY(index), Layer.FRONT, Item.AIR);
            } else {
                zone.updateBlock(getWorldX(index), getWorldY(index), Layer.FRONT, hideTile);
            }
        }

        revealedTiles.clear();
    }

    protected boolean inBounds(MetaBlock metaBlock) {
        return inBounds(metaBlock.getX() - left, metaBlock.getY() - top);
    }

    protected boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    protected int getIndex(MetaBlock metaBlock) {
        return getIndex(metaBlock.getX() - left, metaBlock.getY() - top);
    }

    protected int getIndex(int x, int y) {
        return y * width + x;
    }

    protected int getWorldX(int index) {
        return left + (index % width);
    }

    protected int getWorldY(int index) {
        return top + (index / width);
    }
}
