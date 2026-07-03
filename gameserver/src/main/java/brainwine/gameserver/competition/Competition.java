package brainwine.gameserver.competition;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Competition {
    private Zone zone;
    String title;
    CompetitionPhase phase = CompetitionPhase.ACTIVE;
    Set<String> judges = new HashSet<>();
    private Map<Integer, CompetitionEntry> entriesByMetaBlock = new HashMap<>();
    private Map<String, Map<Integer, CompetitionEntry>> entriesByPlayer = new HashMap<>();

    public Competition() {}
    public Competition(String title) {
        this.title = title;
    }

    public void indexEntry(CompetitionEntry entry) {
        int blockIndex = zone.getBlockIndex(entry.getX(), entry.getY());
        entriesByMetaBlock.put(blockIndex, entry);
        entriesByPlayer.computeIfAbsent(entry.getPlayerDocumentId(), id -> new HashMap<>()).put(blockIndex, entry);
    }

    public CompetitionEntry getEntry(int x, int y) {
        return entriesByMetaBlock.get(zone.getBlockIndex(x, y));
    }

    public void addEntry(Player player, int x, int y) {
        Integer index = zone.getBlockIndex(x, y);
        CompetitionEntry entry = new CompetitionEntry(player, x, y);
        indexEntry(entry);
    }

    public void removeEntry(int x, int y) {
        CompetitionEntry entry = entriesByMetaBlock.get(zone.getBlockIndex(x, y));
        if (entry == null) return;
        MetaBlock metaBlock = ZoneCompetition.findProtector(zone, entry);
        entriesByMetaBlock.remove(zone.getBlockIndex(x, y));
        if(metaBlock != null && metaBlock.hasOwner()) {
            Map<Integer, CompetitionEntry> playerEntry = entriesByPlayer.get(metaBlock.getOwner().getDocumentId());
            if(playerEntry != null) {
                playerEntry.remove(zone.getBlockIndex(x, y));
                if(playerEntry.isEmpty()) {
                    entriesByPlayer.remove(metaBlock.getOwner().getDocumentId());
                }
            }
        }
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Collection<CompetitionEntry> getEntries() {
        return entriesByMetaBlock.values();
    }

    public Collection<CompetitionEntry> getEntriesByPlayer(String playerDocumentId) {
        return entriesByPlayer.getOrDefault(playerDocumentId, Collections.emptyMap()).values();
    }

    public void setEntries(Collection<CompetitionEntry> entries) {
        for (CompetitionEntry entry : entries) {
            indexEntry(entry);
        }
    }

    public CompetitionPhase getPhase() {
        return phase;
    }

    public void setPhase(CompetitionPhase phase) {
        this.phase = phase;
    }

    public void addJudge(String judgeDocumentId) {
        judges.add(judgeDocumentId);
    }

    public void removeJudge(String judgeDocumentId) {
        judges.remove(judgeDocumentId);
    }

    public Set<String> getJudges() {
        return Collections.unmodifiableSet(judges);
    }
}
