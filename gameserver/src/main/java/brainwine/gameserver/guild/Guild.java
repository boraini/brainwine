package brainwine.gameserver.guild;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

/**
 * A player guild (a.k.a. crew). Created by placing a Guild Obelisk
 * ({@code signs/guild}, code 915) and configured through its dialog. The
 * obelisk's metadata mirrors the guild's visual settings; members display the
 * guild's short name as a colored badge next to their name.
 *
 * <p>Ported from the canonical {@code models/guild.rb}.
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Guild {

    public static final int OBELISK_RANGE = 10;

    private String id;
    private String name;
    private String shortName;
    private String zoneId;
    private int x = -1;
    private int y = -1;
    private String leaderId;
    private Set<String> members = new LinkedHashSet<>();
    private String color1;
    private String color2;
    private String color3;
    private String color4;
    private String sign;
    private String signColor;

    @JsonCreator
    protected Guild() {}

    public Guild(String id, String leaderId, String zoneId, int x, int y) {
        this.id = id;
        this.leaderId = leaderId;
        this.zoneId = zoneId;
        this.x = x;
        this.y = y;
        this.members.add(leaderId);
    }

    /**
     * Applies obelisk dialog metadata (gn/gsn/c1-c4/s/sc) to the guild's fields.
     */
    public void applyMetadata(Map<String, Object> metadata) {
        if(metadata.containsKey("gn")) name = asString(metadata.get("gn"));
        if(metadata.containsKey("gsn")) shortName = asString(metadata.get("gsn"));
        if(metadata.containsKey("c1")) color1 = asString(metadata.get("c1"));
        if(metadata.containsKey("c2")) color2 = asString(metadata.get("c2"));
        if(metadata.containsKey("c3")) color3 = asString(metadata.get("c3"));
        if(metadata.containsKey("c4")) color4 = asString(metadata.get("c4"));
        if(metadata.containsKey("s")) sign = asString(metadata.get("s"));
        if(metadata.containsKey("sc")) signColor = asString(metadata.get("sc"));
    }

    /**
     * Builds the obelisk metadata map (gn/gsn/c1-c4/s/sc + guild id) from the
     * guild's fields, omitting any unset values.
     */
    public Map<String, Object> constructMetadata() {
        Map<String, Object> meta = new HashMap<>();
        putIfPresent(meta, "gn", name);
        putIfPresent(meta, "gsn", shortName);
        putIfPresent(meta, "c1", color1);
        putIfPresent(meta, "c2", color2);
        putIfPresent(meta, "c3", color3);
        putIfPresent(meta, "c4", color4);
        putIfPresent(meta, "s", sign);
        putIfPresent(meta, "sc", signColor);
        meta.put("g", id);
        return meta;
    }

    /**
     * @return {@code true} if every visual field has been configured.
     */
    public boolean isComplete() {
        return isPresent(name) && isPresent(shortName) && isPresent(color1) && isPresent(color2)
                && isPresent(color3) && isPresent(color4) && isPresent(signColor) && isPresent(sign);
    }

    public boolean isMember(String playerId) {
        return members.contains(playerId);
    }

    public boolean isLeader(String playerId) {
        return leaderId != null && leaderId.equals(playerId);
    }

    /**
     * Invites a player to join this guild via a yes/no dialog. They become a
     * member if they accept.
     */
    public void offerMembership(Player player) {
        Dialog dialog = new Dialog()
                .setActions("yesno")
                .addSection(new DialogSection()
                        .setTitle("Guild Membership")
                        .setText(String.format("You have been invited to join the \"%s\" guild. Would you like to join?", name)));

        player.showDialog(dialog, data -> {
            if(data.length == 1 && "cancel".equals(data[0])) {
                return;
            }

            if(player.getGuildId() != null) {
                player.notify("You already belong to a guild.");
                return;
            }

            addMember(player);
        });
    }

    /**
     * Offers guild leadership to a player via a yes/no dialog. They become the
     * leader if they accept.
     */
    public void offerLeadership(Player player) {
        Dialog dialog = new Dialog()
                .setActions("yesno")
                .addSection(new DialogSection()
                        .setTitle("Guild Leadership")
                        .setText(String.format("You have been invited to LEAD the \"%s\" guild. Do you accept?", name)));

        player.showDialog(dialog, data -> {
            if(data.length == 1 && "cancel".equals(data[0])) {
                return;
            }

            setLeader(player);
        });
    }

    public void addMember(Player player) {
        members.add(player.getDocumentId());
        player.setGuildId(id);
        persist(player);
        alert(leaderId, String.format("%s is now a member of the \"%s\" guild.", player.getName(), name));
        player.notify(String.format("You are now a member of the \"%s\" guild.", name));
        player.broadcastGuildAffiliation();
    }

    public void removeMember(Player player, boolean alertLeader) {
        members.remove(player.getDocumentId());
        player.setGuildId(null);
        persist(player);
        player.notify(String.format("You are no longer a member of the \"%s\" guild.", name));

        if(alertLeader) {
            alert(leaderId, String.format("%s has been removed from the \"%s\" guild.", player.getName(), name));
        }

        player.broadcastGuildAffiliation();
    }

    /**
     * Transfers leadership to the given player (who becomes a member if they
     * weren't already), keeping the previous leader as a regular member.
     */
    public void setLeader(Player player) {
        String previousLeaderId = leaderId;
        leaderId = player.getDocumentId();
        members.add(player.getDocumentId());
        persist(player);
        updateObeliskOwner(player);

        if(previousLeaderId != null) {
            alert(previousLeaderId, String.format("%s is now the leader of the \"%s\" guild.", player.getName(), name));
        }

        player.notify(String.format("You are now the leader of the \"%s\" guild.", name));
    }

    public void setLocation(String zoneId, int x, int y) {
        this.zoneId = zoneId;
        this.x = x;
        this.y = y;
    }

    public void clearLocation() {
        this.zoneId = null;
        this.x = -1;
        this.y = -1;
    }

    /**
     * @return {@code true} if the player is in the guild's home zone and within
     * {@link #OBELISK_RANGE} of its obelisk.
     */
    public boolean nearObelisk(Player player) {
        return zoneId != null && player.getZone() != null && zoneId.equals(player.getZone().getDocumentId())
                && MathUtils.inRange(x, y, player.getX(), player.getY(), OBELISK_RANGE);
    }

    /**
     * Broadcasts the short-name badge of every online member to their zones, so
     * clients can render the colored guild tag next to member names.
     */
    public void broadcastClientChanges() {
        PlayerManager playerManager = GameServer.getInstance().getPlayerManager();

        for(String memberId : members) {
            Player member = playerManager.getPlayerById(memberId);

            if(member != null && member.isOnline()) {
                member.broadcastGuildAffiliation();
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getZoneId() {
        return zoneId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public Set<String> getMembers() {
        return members;
    }

    private void persist(Player player) {
        GameServer.getInstance().getGuildManager().saveGuild(this);
        GameServer.getInstance().getPlayerManager().savePlayer(player);
    }

    /**
     * Transfers ownership of the obelisk block to the new leader so only they
     * can reconfigure the guild.
     */
    private void updateObeliskOwner(Player newLeader) {
        if(zoneId == null) {
            return;
        }

        Zone zone = GameServer.getInstance().getZoneManager().getZone(zoneId);

        if(zone != null) {
            MetaBlock metaBlock = zone.getMetaBlock(x, y);

            if(metaBlock != null) {
                metaBlock.setOwner(newLeader);
            }
        }
    }

    private void alert(String playerId, String message) {
        if(playerId == null) {
            return;
        }

        Player player = GameServer.getInstance().getPlayerManager().getPlayerById(playerId);

        if(player != null && player.isOnline()) {
            player.notify(message);
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isEmpty();
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if(isPresent(value)) {
            map.put(key, value);
        }
    }
}
