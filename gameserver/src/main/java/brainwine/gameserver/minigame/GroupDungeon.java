package brainwine.gameserver.minigame;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.entity.EntityAttack;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.TradeSession;
import brainwine.gameserver.resource.ResourceFinder;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.zone.Block;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;
import brainwine.shared.JsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

public class GroupDungeon extends Minigame {
    private static final Logger logger = LogManager.getLogger();
    private static GroupDungeonConfig config = new GroupDungeonConfig();
    private static Set<Item> allDoorItems = new HashSet<>(config.getAllDoorItems());
    private static final String sirenOpenId = "mechanical/siren-open";
    public static final double MAX_ENEMY_DISTANCE = 50.0; // Maximum distance wave enemies can wander before they're teleported back

    private int potencyLevel = 0;
    private Map<String, Integer> potencyBumps = new HashMap<>();
    private List<MetaBlock> doors = new ArrayList<>();
    private int totalWaves;
    private int currentWave;
    private final Map<String, Integer> roundSpawns = new HashMap<>();
    private final List<Npc> spawns = new ArrayList<>();
    private final Random random = new Random();
    private boolean raidStarted = false;
    private long nextActionAt;

    public static void loadConfig() {
        logger.info(SERVER_MARKER, "Loading group dungeon configuration ...");

        try {
            GroupDungeon.config = JsonHelper.readValue(ResourceFinder.getResourceUrl("group-dungeon.json"), GroupDungeonConfig.class);
            GroupDungeon.allDoorItems = new HashSet<>(config.getAllDoorItems());
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Failed to load group dungeon config", e);
        }
    }

    public static GroupDungeonConfig getConfig() {
        return config;
    }

    public GroupDungeon(Zone zone, Player initiator, int x, int y) {
        super(zone, initiator, x, y);
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        long now = System.currentTimeMillis();

        // End minigame if block state is invalid
        if(!zone.isChunkLoaded(x, y) || !zone.getBlock(x, y).getFrontItem().hasId(sirenOpenId)) {
            finish();
            return;
        }

        if(!raidStarted) {
            if(now >= startedAt + config.getGracePeriod()) {
                raidStarted = true;
                setDoorsOpen(false);
                nextRound();
                zone.spawnEffect(x, y, "karma sound", 1);
                for(Player player : zone.getPlayers()) {
                    if(participants.containsKey(player)) {
                        player.notify("Oh no, the doors are shut! Now your only way out is to end these pesky brains.");
                    } else {
                        player.notify(String.format("The group dungeon at %s has locked down. You can't help raid it anymore.", zone.getReadableCoordinates(x, y)));
                    }
                }
            }
            return;
        }

        // Perform an action if it is time
        if(now >= nextActionAt) {
            nextAction();
            nextActionAt = (long)(now + (0.5 + currentWave * 0.05) * 1000); // Spawns become less frequent as the difficulty increases
        }
    }

    private void nextAction() {
        // Move on to the next round if all enemies have been killed
        if(spawns.isEmpty() && roundSpawns.isEmpty()) {
            nextRound();
            return;
        }

        addParticipantsInRange();

        // Spawn a random enemy if there are any remaining
        if(!roundSpawns.isEmpty()) {
            String entity = roundSpawns.keySet().stream().skip(random.nextInt(roundSpawns.size())).findAny().get();
            // Make multiple attempts to spawn the entity in an empty location
            for (int attempt = 0; attempt < 20; attempt++) {
                double angle = 2 * Math.PI * random.nextDouble();
                double parallelogram = random.nextDouble() + random.nextDouble();
                double radius = config.getSpawnRadius() * (parallelogram > 1.0 ? 2.0 - parallelogram : parallelogram);
                int spawnX = MathUtils.clamp((int) Math.round(x + radius * Math.cos(angle)), 0, zone.getWidth());
                int spawnY = MathUtils.clamp((int) Math.round(y + radius * Math.sin(angle)), 0, zone.getHeight());
                if(!zone.isBlockSolid(spawnX, spawnY)) {
                    roundSpawns.compute(entity, (key, value) -> value <= 1 ? null : value - 1);
                    Npc npc = zone.spawnEntity(entity, spawnX, spawnY, true);
                    npc.setMinigame(this);
                    spawns.add(npc);
                    break;
                }
            }
        }

        // Teleport all out-of-range enemies back to the box
        spawns.stream().filter(entity -> !entity.inRange(x, y, MAX_ENEMY_DISTANCE)).forEach(entity -> {
            entity.spawnEffect("bomb-teleport", 4);
            entity.setPosition(x + random.nextInt(2), y - random.nextInt(3));
            entity.spawnEffect("bomb-teleport", 4);
        });
    }

    private void nextRound() {
        addParticipantsInRange();

        // Finish if the final round has been cleared
        if(currentWave >= totalWaves) {
            complete();
            return;
        }

        // Increment round and fetch spawn data
        currentWave++;
        int key = config.getEnemies().keySet().stream().filter(x -> currentWave >= x).max(Integer::compareTo).orElse(1);
        List<Map<String, Integer>> configs = config.getEnemies().getOrDefault(key, Collections.emptyList());
        roundSpawns.clear();
        roundSpawns.putAll(configs.get(random.nextInt(configs.size()))); // Select a random wave of enemies

        // Randomly increase the number of spawns this round depending on the potency level
        int spawnBumps = potencyLevel / (currentWave < 10 ? 2 : 3);

        for(int i = 0; i < spawnBumps; i++) {
            Map.Entry<String, Integer> spawn = roundSpawns.entrySet().stream().skip(random.nextInt(roundSpawns.size())).findFirst().get();
            roundSpawns.put(spawn.getKey(), spawn.getValue() + 1);
        }

        // Notify all players in the zone that the next round is starting
        zone.notifyPlayers(String.format("Group dungeon wave %s of %s is beginning!", currentWave, totalWaves));
    }

    @Override
    public void onInteract(Player player) {
        addParticipant(player);

        // Increase potency if the minigame hasn't started yet
        if(!raidStarted) {
            boolean totalBumpsLimited = config.getMaxTotalBumps() > 0;
            int totalBumpsLeft = config.getMaxTotalBumps() - potencyLevel;
            if(totalBumpsLimited && totalBumpsLeft <= 0) {
                player.notify("Sorry, no more players can participate in this raid.");
                return;
            }
            if(!potencyBumps.containsKey(player.getDocumentId()) || player.isGodMode()) {
                potencyBumps.put(player.getDocumentId(), 1);
                zone.notifyPlayers(String.format("%s increased the group dungeon's potency level to %s!", player.getName(), ++potencyLevel), NotificationType.PEER_ACCOMPLISHMENT);
            } else {
                boolean playerBumpsLimited = config.getMaxPlayerBumps() > 0;
                int playerBumpsLeft = config.getMaxPlayerBumps() - potencyBumps.getOrDefault(player.getDocumentId(), 0);
                boolean bumpsLimited = totalBumpsLimited || playerBumpsLimited;
                int bumpsLeft = totalBumpsLimited && playerBumpsLimited ? Math.min(totalBumpsLeft, playerBumpsLeft) : totalBumpsLimited ? totalBumpsLeft : playerBumpsLeft;
                if(bumpsLimited && bumpsLeft <= 0) {
                    player.notify("Sorry, you can't make this raid harder anymore. Maybe ask more players to join?");
                    return;
                }

                Block blockInteractingWith = zone.getBlock(x, y);
                Item interactingWith = blockInteractingWith != null ? blockInteractingWith.getFrontItem() : Item.AIR;
                List<Item> whistles = new ArrayList<>();
                try {
                    if(interactingWith.getUse(ItemUseType.POTENCY_BUMP) instanceof List) {
                        ((List<String>) interactingWith.getUse(ItemUseType.POTENCY_BUMP)).stream()
                                .map(ItemRegistry::getItem)
                                .filter(item -> !item.isAir())
                                .forEach(whistles::add);
                    }
                } catch (Exception e) {
                    logger.error("Error while listing the potency bump items", e);
                }
                if(whistles.isEmpty()) {
                    player.notify("Don't know what items you can use to bump potency.", NotificationType.SYSTEM);
                    return;
                }
                List<Integer> whistleCounts = whistles.stream().map(item -> player.getInventory().getQuantity(item)).collect(Collectors.toList());
                if(!whistleCounts.stream().anyMatch(x -> x != 0)) {
                    player.notify("You need whistles to increase the potency level more. Maybe invite more players to this dungeon instead.");
                    return;
                }
                Dialog dialog = new Dialog().setTitle("Group Dungeon Potency Level");
                dialog.addSection(new DialogSection().setText("You can increase this dungeon's potency level even more using whistles."));
                for(int i = 0; i < whistleCounts.size(); i++) {
                    if(whistleCounts.get(i) > 0) {
                        Item whistle = whistles.get(i);
                        int allowed = Math.max(1, bumpsLeft / (int)Math.round(whistle.getPower()));
                        dialog.addSection(TradeSession.Dialogs.createQuantitySelector(whistle.getId(), allowed, 1, true).setTitle("Use how many " + whistle.getFancyTitle() + "? Increases level by " + whistle.getPower() + "."));
                    }
                }

                player.showDialog(dialog, ans -> {
                    if(ans.length >= 1 && "cancel".equals(ans[0])) {
                        return;
                    }
                    int ansI = 0;
                    int bumps = 0;
                    Map<Item, Integer> used = new HashMap<>();
                    try {
                        for(DialogSection section : dialog.getSections()) {
                            if(section.getInput() != null) {
                                String itemId = section.getInput().getKey();
                                if(ansI >= ans.length) {
                                    player.notify("Bad input. Too few choices submitted.");
                                    return;
                                }
                                Object val = ans[ansI++];
                                if(!(val instanceof String)) {
                                    player.notify("Bad input type.");
                                    return;
                                }
                                int qty = Integer.parseInt((String)val);
                                Item item = ItemRegistry.getItem(itemId);
                                if(!player.getInventory().hasItem(item, qty)) {
                                    player.notify("You don't have that many " + item.getFancyTitle() + " anymore.");
                                    return;
                                }
                                bumps = Math.min(bumpsLeft, qty * (int)Math.round(item.getPower()));
                                used.put(item, Math.max(0, Math.min(qty, (bumpsLeft - bumps) / (int)Math.round(item.getPower()))));
                            }
                            if(bumps >= bumpsLeft) break;
                        }
                        // Validation complete, now remove the whistles and increase the potency level
                        potencyLevel += bumps;
                        potencyBumps.put(player.getDocumentId(), potencyBumps.getOrDefault(player.getDocumentId(), 0) + 1);
                        for(Map.Entry<Item, Integer> usedItem : used.entrySet()) {
                            player.getInventory().removeItem(usedItem.getKey(), usedItem.getValue(), true);
                        }
                        zone.notifyPlayers(String.format("%s increased the group dungeon's potency level to %s!", player.getName(), potencyLevel), NotificationType.PEER_ACCOMPLISHMENT);
                    } catch (Exception e) {
                        player.notify("Error while parsing your input.");
                        logger.error("Error while handling potency bump dialog answers", e);
                    }
                });
            }
        }
    }

    @Override
    protected void onStart() {
        // Validations that don't interact with the world.
        if(config.getEnemies().isEmpty()) {
            notifyParticipants("Don't know what to spawn! Ending the raid now.");
            finish();
            return;
        }

        // Validations that do interact with the world
        MetaBlock siren = zone.getMetaBlock(x, y);
        if(siren == null) {
            initiator.notify("Siren metadata not found");
            finish();
            return;
        }

        String dungeonId = siren.getStringProperty("@");
        if(dungeonId == null) {
            initiator.notify("Siren doesn't appear to be inside a dungeon.");
            finish();
            return;
        }

        // Index meta-blocks in this dungeon
        for(MetaBlock mb : zone.getMetaBlocks()) {
            if(mb != siren && dungeonId.equals(mb.getStringProperty("@"))) {
                if(allDoorItems.contains(mb.getItem())) {
                    doors.add(mb);
                }
            }
        }

        // Compute the total number of waves needed
        totalWaves = config.getEnemies().size();

        zone.updateBlock(x, y, Layer.FRONT, ItemRegistry.getItem(sirenOpenId));

        // Everything went well
        zone.updateBlock(x, y, Layer.FRONT, sirenOpenId, 1);
        potencyBumps.put(initiator.getDocumentId(), 1);
        potencyLevel++;

        // Notify all players in the zone
        for(Player player : zone.getPlayers()) {
            player.notifyProfile(String.format("%s is raiding a Group Dungeon at %s! Join them before the gates close.", initiator.getName(), zone.getReadableCoordinates(x, y)), String.format("Tap on and/or use whistles on its siren in the next %d seconds to increase the dungeon's potency!", config.getGracePeriod() / 1000));
        }
    }

    @Override
    protected void onFinish() {
        setDoorsOpen(true);
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
        Item sirenOpen = ItemRegistry.getItem(sirenOpenId);
        String[] rewardLootCategories = sirenOpen.getLootCategories();
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
        zone.notifyPlayers(String.format("Dungeon has been raided! %s showed mastery with %s!", currentLeader.getPlayer().getName(), describeScore(currentLeader.getScore())));

        // Explode the siren
        zone.updateBlock(x, y, Layer.FRONT, Item.AIR);
        zone.spawnEffect(x + sirenOpen.getBlockWidth() / 2.0f - 0.5f, y - sirenOpen.getBlockHeight() / 2.0f + 0.5f, "bomb-electric", 2);
    }

    protected void setDoorsOpen(boolean open) {
        Map<GroupDungeonConfig.BlockState, GroupDungeonConfig.BlockState> toLookFor = new HashMap<>();
        for(GroupDungeonConfig.DoorState doorState: config.getDoors()) {
            if(open) {
                // closed to open
                toLookFor.put(doorState.getClosed(), doorState.getOpen());
            } else {
                // open to closed
                toLookFor.put(doorState.getOpen(), doorState.getClosed());
            }
        }
        for(int i = 0; i < doors.size(); i++) {
            MetaBlock door = doors.get(i);
            Block block = zone.getBlock(door.getX(), door.getY());
            if(block != null) {
                GroupDungeonConfig.BlockState wantedState = toLookFor.get(
                        new GroupDungeonConfig.BlockState(
                                block.getFrontItem(),
                                block.getFrontMod(),
                                door.getMetadata()
                        )
                );
                if(wantedState != null) {
                    int currentMod = block.getFrontMod();
                    if(wantedState.getItem() != block.getFrontItem() || wantedState.getMod() != currentMod) {
                        Map<String, Object> newMetadata = new HashMap<>(door.getMetadata());
                        if(wantedState.getMetadata() != null) {
                            newMetadata.putAll(wantedState.getMetadata());
                        }
                        zone.updateBlock(door.getX(), door.getY(), Layer.FRONT, wantedState.getItem(), wantedState.getMod(), null, newMetadata);
                        zone.spawnEffect(door.getX() + block.getFrontItem().getBlockWidth() / 2.0f - 0.5f, door.getY(), "steam", 4);
                    }
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
