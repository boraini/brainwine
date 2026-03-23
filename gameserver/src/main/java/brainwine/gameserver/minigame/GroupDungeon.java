package brainwine.gameserver.minigame;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.entity.EntityAttack;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.TradeSession;
import brainwine.gameserver.zone.Block;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupDungeon extends Minigame {
    GroupDungeonConfig config = new GroupDungeonConfig();
    private final String sirenOpenId = "mechanical/siren-open";
    private final String speakerId = "mechanical/speaker";
    private final Set<String> doorIds = new HashSet<>(Arrays.asList("mechanical/door-beefy-closed-iron", "mechanical/door-beefy-closed-copper"));
    private final List<Item> whistles = Arrays.asList(ItemRegistry.getItem("accessories/whistle-onyx"), ItemRegistry.getItem("accessories/whistle-diamond"), ItemRegistry.getItem("accessories/whistle-brass"));

    boolean started = false;
    int potencyLevel = 0;
    Set<String> potencyBumps = new HashSet<>();
    List<MetaBlock> speakers = new ArrayList<>();
    List<MetaBlock> doors = new ArrayList<>();
    int initialNumSpeakers = 0;
    private final List<Npc> spawns = new ArrayList<>();

    public GroupDungeon(Zone zone, Player initiator, int x, int y) {
        super(zone, initiator, x, y);
        MetaBlock siren = zone.getMetaBlock(x, y);
        if(siren != null) {
            String dungeonId = siren.getStringProperty("@");
            if(dungeonId != null) {
                // Index metablocks in this dungeon
                for(MetaBlock mb : zone.getMetaBlocks()) {
                    if(mb != siren && dungeonId.equals(mb.getStringProperty("@"))) {
                        if(mb.getItem().hasId(speakerId)) {
                            speakers.add(mb);
                        } else if(doorIds.contains(mb.getItem().getId())) {
                            doors.add(mb);
                        }
                    }
                }
                if(speakers.size() >= 3) {
                    initialNumSpeakers = speakers.size();
                    // Success
                    return;
                } else {
                    initiator.notify("You can't raid this dungeon anymore because it has been tampered with.");
                }
            } else {
                initiator.notify("Siren doesn't appear to be inside a dungeon.");
            }
        } else {
            initiator.notify("Siren metadata not found");
        }

        // Failure
        finish();
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);

    }

    @Override
    public void onInteract(Player player) {
        addParticipant(player);

        // Increase potency if the minigame hasn't started yet
        if(!hasStarted()) {
            if(!potencyBumps.contains(player.getDocumentId()) || player.isGodMode()) {
                zone.notifyPlayers(String.format("%s increased the group dungeon's potency level to %s!", player.getName(), ++potencyLevel), NotificationType.PEER_ACCOMPLISHMENT);
            } else {
                List<Integer> whistleCounts = whistles.stream().map(item -> player.getInventory().getQuantity(item)).collect(Collectors.toList());
                if(!whistleCounts.stream().anyMatch(x -> x != 0)) {
                    player.notify("You need whistles to increase the chaos level more. Maybe invite more players to this dungeon instead.");
                    return;
                }
                Dialog dialog = new Dialog().setTitle("Group Dungeon Chaos Level");
                dialog.addSection(new DialogSection().setText("You can increase this dungeon's chaos level even more using whistles."));
                for(int i = 0; i < whistleCounts.size(); i++) {
                    if(whistleCounts.get(i) > 0) {
                        dialog.addSection(TradeSession.Dialogs.createQuantitySelector(whistles.get(i).getId(), Math.min(5, whistleCounts.get(i)), 1, true).setTitle("Use how many " + whistles.get(i).getFancyTitle() + "? Increases level by " + whistles.get(i).getPower() + "."));
                    }
                }

                player.showDialog(dialog, ans -> {
                    if(ans.length >= 1 && "cancel".equals(ans[0])) {
                        return;
                    }
                    int ansI = 0;
                    try {
                        for(DialogSection section : dialog.getSections()) {
                            if(section.getInput() != null) {
                                String itemId = section.getInput().getKey();
                                if(ansI >= ans.length) {
                                    player.notify("Bad input. Too few choices submitted.");
                                    return;
                                }
                                Object val = ans[ansI++];
                                if (!(val instanceof String)) {
                                    player.notify("Bad input type.");
                                    return;
                                }
                                int qty = Integer.parseInt((String)val);
                                Item item = ItemRegistry.getItem(itemId);
                                if(!player.getInventory().hasItem(item, qty)) {
                                    player.notify("You don't have that many " + item.getFancyTitle() + " anymore.");
                                }
                            }
                        }
                        // Validation complete, now remove the whistles and increase the potency level
                        ansI = 0;
                        for(DialogSection section : dialog.getSections()) {
                            if(section.getInput() != null) {
                                String itemId = section.getInput().getKey();
                                String val = (String)ans[ansI++];
                                int qty = Integer.parseInt(val);
                                Item item = ItemRegistry.getItem(itemId);
                                player.getInventory().removeItem(item, qty, true);
                                potencyLevel += qty * item.getPower();
                            }
                        }
                        zone.notifyPlayers(String.format("%s increased the group dungeon's potency level to %s!", player.getName(), potencyLevel), NotificationType.PEER_ACCOMPLISHMENT);
                    } catch (Exception e) {
                        player.notify("Error while parsing your input.");
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    protected void onStart() {
        zone.spawnEffect(x, y, "match start", 1);
        zone.updateBlock(x, y, Layer.FRONT, sirenOpenId, 1);
        potencyBumps.add(initiator.getDocumentId());
        potencyLevel++;

        // Notify all players in the zone
        for(Player player : zone.getPlayers()) {
            player.notifyProfile(String.format("%s is raiding a Group Dungeon at %s", initiator.getName(), zone.getReadableCoordinates(x, y)), String.format("Tap on and/or use whistles on its siren in the next %d seconds to build chaos!", config.getStartPeriod() / 1000));
        }
    }

    @Override
    protected void onFinish() {
        // Kill all spawned entities
        for(Npc entity : spawns) {
            entity.setMinigame(null);
            entity.setHealth(0.0F);
        }
    }

    protected void complete() {
        finish();
        double luckMultiplier = Math.min(10.0, potencyLevel);
        int baseLuck = Math.min(12, participants.size() * 4);
        int position = 0;

        // Give out rewards
        Item pandoraOpen = ItemRegistry.getItem(sirenOpenId);
        String[] rewardLootCategories = pandoraOpen.getLootCategories();
        for(Participant participant : leaderboard) {
            if(participant.isParticipating()) {
                int luck = (int)(Math.max(1, baseLuck - position * 4) * luckMultiplier);
                Player player = participant.getPlayer();

                Loot loot = GameServer.getInstance().getLootManager().getRandomLoot(luck, zone.getBiome(), player.getInventory().getWardrobe(), rewardLootCategories);

                if(loot != null) {
                    player.awardLoot(loot, String.format("You won %s place!", ordinalizeNumber(position + 1)));
                } else {
                    player.notify("Sorry, we couldn't find a suitable reward for you.");
                }
            }

            position++;
        }

        // Broadcast leader's score
        zone.notifyPlayers(String.format("Pandora has been contained! %s showed mastery with %s!", currentLeader.getPlayer().getName(), describeScore(currentLeader.getScore())));
    }

    protected boolean hasStarted() {
        return System.currentTimeMillis() < startedAt + config.getStartPeriod();
    }

    protected int getCurrentRound() {
        return initialNumSpeakers - speakers.size() + 1;
    }

    protected void setDoorsOpen(boolean open) {
        for(MetaBlock door : doors) {
            Block block = zone.getBlock(door.getX(), door.getY());
            if(block != null) {
                int wantedMod = open ? 1 : 0;
                int currentMod = block.getFrontMod();
                if(wantedMod != currentMod) {
                    zone.updateBlockMod(door.getX(), door.getY(), Layer.FRONT, wantedMod);
                    zone.spawnEffect(door.getX() + block.getFrontItem().getBlockWidth() / 2.0f, door.getY(), "steam", 4);
                }
            }
        }
    }

    public void entityKilled(Entity entity, EntityAttack cause) {
        spawns.remove(entity);
    }

    public void entityAttacked(Entity entity, EntityAttack attack, float damage) {
        // Do nothing if entity is not a wave enemy
        if(!spawns.contains(entity)) {
            return;
        }

        Entity attacker = attack.getAttacker();

        // Check if attacker is present
        if(attacker == null || !attacker.isPlayer()) {
            return;
        };

        Player player = (Player)attacker;
        Participant participant = addParticipant(player);
        participant.incrementScore(damage);
    }
}
