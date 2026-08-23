package brainwine.gameserver.command.admin;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.ItemGroup;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.util.Vector2i;
import brainwine.gameserver.zone.MassTeleporterConfiguration;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.time.temporal.ChronoUnit;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name = "teleport", description = "Teleports you or another player to the specified target position or player.", aliases = "tp")
public class TeleportCommand extends Command {
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length == 0 || args.length > 4) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }

        TeleportCommandArguments arguments = TeleportCommandArguments.create(executor, args);

        if(arguments == null) {
            // Executor is already notified.
            return;
        }

        if(!validate(executor, arguments)) {
            return;
        }

        Player subject = arguments.getPlayer();
        Zone targetZone = arguments.getTargetZone();

        final Runnable task = () -> {
            if(!validate(executor, arguments)) {
                return;
            }
            if(targetZone == subject.getZone()) {
                if(arguments.getVariant() != TeleportVariant.ZONE) {
                    subject.teleport(arguments.getX(), arguments.getY());
                }
            } else {
                targetZone.giveTemporaryAccess(subject);
                if(arguments.getVariant() != TeleportVariant.ZONE) {
                    subject.changeZone(targetZone, arguments.getX(), arguments.getY());
                } else {
                    subject.changeZone(targetZone);
                }
            }
        };

        if(!(executor instanceof Player) || ((Player)executor).isGodMode() || subject.equals(executor)) {
            task.run();
        } else {
            if(subject.getZone().isActionOnCooldown("failed teleport request", 10, ChronoUnit.SECONDS)) {
                executor.notify("Sorry, further teleport requests have been blocked for 10 seconds.", SYSTEM);
                return;
            }

            double distance = executor instanceof Player ? MathUtils.distance(arguments.getX(), arguments.getY(), ((Player)executor).getX(), ((Player)executor).getY()) : Double.POSITIVE_INFINITY;
            String location = arguments.getVariant() == TeleportVariant.ZONE ? targetZone.getName() : targetZone.getReadableCoordinates(arguments.getX(), arguments.getY()) +
                    (distance <= 5.0 ? " (near themselves)" : "") +
                    (targetZone == subject.getZone()
                            ? "."
                            : " in " + targetZone.getName() + ".");
            subject.showDialog(
                    new Dialog()
                            .setTitle("Teleport Request")
                            .addSection(new DialogSection().setText(
                                            ((Player)executor).getName() +
                                            " wants to teleport you to " +
                                            location +
                                            " Click OK to accept."
                            )),
                    ans -> {
                        if(ans.length >= 1 && "cancel".equals(ans[0])) {
                            executor.notify(subject.getName() + " has dismissed your teleport request.", SYSTEM);
                            subject.getZone().recordActionTime("failed teleport request");
                        } else {
                            task.run();
                        }
                    });
            executor.notify("Your teleport request has been sent to " + subject.getName(), SYSTEM);
        }
    }

    private boolean validate(CommandExecutor executor, TeleportCommandArguments arguments) {
        Player subject = arguments.getPlayer();
        Zone targetZone = arguments.getTargetZone();

        if(arguments.getVariant() == TeleportVariant.COORDINATES) {
            if(!executor.isAdmin()) {
                executor.notify("Only admins are allowed to teleport to exact coordinates.", SYSTEM);
                return false;
            }
        }

        if(executor instanceof Player) {
            Player executorPlayer = (Player)executor;
            if(arguments.getVariant() == TeleportVariant.PLAYER) {
                Player targetPlayer = arguments.getTargetPlayer();
                if(arguments.getPlayer() == executorPlayer && targetPlayer == executorPlayer) {
                    executor.notify("You cannot teleport to yourself.", SYSTEM);
                    return false;
                }
            }
        }

        if(arguments.getVariant() == TeleportVariant.PLAYER) {
            if(arguments.getPlayer() == arguments.getTargetPlayer()) {
                executor.notify("You cannot teleport a player to themselves.", SYSTEM);
                return false;
            }
        }

        if(executor instanceof Player && !executor.isAdmin()) {
            Player executorPlayer = (Player)executor;
            if(arguments.getVariant() != TeleportVariant.ZONE && executorPlayer.getZone() != arguments.getTargetZone()) {
                executor.notify("Sorry, only admins can teleport players out of and across worlds.", SYSTEM);
                return false;
            }
            if(!arguments.getTargetZone().canJoin(executorPlayer)) {
                executor.notify("Sorry, but you cannot enter " + arguments.getTargetZone() + " yourself.", SYSTEM);
                return false;
            }
            if(arguments.getVariant() != TeleportVariant.ZONE) {
                MassTeleporterConfiguration config = targetZone.getMassTeleporterConfiguration();
                if(!config.isEnabled()) {
                    executorPlayer.notify("No mass teleportation machine is operational in this world.", SYSTEM);
                    return false;
                }
                if(subject == executorPlayer && !config.getTeleportToPlayerAccess().isPrivileged(executor, targetZone)) {
                    executorPlayer.notify("You are not allowed to teleport to players in the target world.", SYSTEM);
                    return false;
                }
                if(subject.getZone() != targetZone && !config.getSummonOtherPlayerAccess().isPrivileged(executor, targetZone)) {
                    executorPlayer.notify("You are not allowed to summon other players in this world.", SYSTEM);
                    return false;
                }
                if(arguments.getVariant() == TeleportVariant.PLAQUE) {
                    if(!config.getTeleportToPlaqueAccess().isPrivileged(executor, targetZone)) {
                        executorPlayer.notify("You are not allowed to teleport to plaques in this world.", SYSTEM);
                        return false;
                    }
                }
            }
        }

        if(!subject.isOnline()) {
            executor.notify(String.format("Player '%s' is not online.", subject.getName()), SYSTEM);
            return false;
        }

        if(arguments.getVariant() != TeleportVariant.ZONE) {
            int x = arguments.getX();
            int y = arguments.getY();

            if(!executor.isAdmin()) {
                if(!targetZone.isAreaExplored(x, y)) {
                    executor.notify("That area hasn't been explored yet.", SYSTEM);
                    return false;
                }

                if(targetZone.isChunkLoaded(x, y) && (targetZone.isBlockSolid(x, y) || targetZone.isBlockSolid(x, y - 1))) {
                    executor.notify("Teleportation destination is obstructed.", SYSTEM);
                    return false;
                }

                if(executor instanceof Player) {
                    Player executorPlayer = (Player)executor;
                    // We don't consider single blocks to be protected against teleportation.
                    if(!targetZone.getMassTeleporterConfiguration().getTeleportInProtectedAreaAccess().isPrivileged(executor, targetZone) && targetZone.isBlockProtected(x, y, executorPlayer, true)) {
                        executor.notify("Sorry, you can't teleport to areas protected against you in this world.", SYSTEM);
                        return false;
                    }
                }
            }

            // Check if coordinates are in bounds
            if(!targetZone.areCoordinatesInBounds(x, y)) {
                executor.notify("Cannot teleport out of bounds!", SYSTEM);
                return false;
            }
        }

        return true;
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/tp [target description]";
    }

    @Override
    public boolean useSmartArguments() {
        return true;
    }
}

enum TeleportVariant {
    PLAYER,
    PLAQUE,
    ZONE,
    COORDINATES,
}

class TeleportCommandArguments {
    private final TeleportVariant variant;
    private final CommandExecutor executor;
    private final Player player;
    private final Zone targetZone;
    private final Player targetPlayer;
    private final MetaBlock targetPlaque;
    private final int x;
    private final int y;

    private TeleportCommandArguments(TeleportVariant variant, CommandExecutor executor, Player player, Zone targetZone, Player targetPlayer, MetaBlock targetPlaque, int x, int y) {
        this.variant = variant;
        this.executor = executor;
        this.player = player;
        this.targetZone = targetZone;
        this.targetPlayer = targetPlayer;
        this.targetPlaque = targetPlaque;
        this.x = x;
        this.y = y;
    }

    public static TeleportCommandArguments create(CommandExecutor executor, String[] args) {
        if(args.length == 1) {
            if(!(executor instanceof Player)) {
                executor.notify("Only players can use a single argument.", SYSTEM);
                return null;
            }

            Player p = player(args[0]);
            if(p != null) {
                return new TeleportCommandArguments(TeleportVariant.PLAYER, executor, (Player)executor, p.getZone(), p, null, (int)p.getX(), (int)p.getY());
            }

            Zone executorZone = ((Player)executor).getZone();

            MetaBlock mb = plaque(executorZone, args[0]);
            if(mb != null) {
                return new TeleportCommandArguments(TeleportVariant.PLAQUE, executor, (Player)executor, executorZone, null, mb, mb.getX(), mb.getY());
            }

            Zone z = zone(args[0]);
            if(z != null) {
                return new TeleportCommandArguments(TeleportVariant.ZONE, executor, (Player)executor, z, null, null, 0, 0);
            }

            executor.notify(String.format("Player or landmark '%s' not found.", args[0]), SYSTEM);
            return null;
        }

        if(args.length == 2) {
            Player teleported = player(args[0]);
            if(teleported != null) {
                Player p = player(args[1]);
                if(p != null) {
                    return new TeleportCommandArguments(TeleportVariant.PLAYER, executor, teleported, p.getZone(), p, null, (int)p.getX(), (int)p.getY());
                }

                if(executor instanceof Player) {
                    Zone executorZone = ((Player)executor).getZone();
                    MetaBlock mb = plaque(executorZone, args[1]);
                    if(mb != null) {
                        return new TeleportCommandArguments(TeleportVariant.PLAQUE, executor, teleported, executorZone, null, mb, mb.getX(), mb.getY());
                    }
                }

                Zone z = zone(args[1]);
                if(z != null) {
                    return new TeleportCommandArguments(TeleportVariant.ZONE, executor, teleported, z, null, null, 0, 0);
                }
            }

            if(executor instanceof Player) {
                Zone targetZone = ((Player)executor).getZone();
                if(targetZone == null) {
                    executor.notify("Sorry, but you are not in a world right now.", SYSTEM);
                }
                Vector2i c = coords(targetZone, args[0], args[1]);
                if(c != null) {
                    return new TeleportCommandArguments(TeleportVariant.COORDINATES, executor, (Player)executor, targetZone, null, null, c.getX(), c.getY());
                }
            }

            executor.notify(String.format("Player '%s' not found.", args[0]), SYSTEM);
            return null;
        }

        if(args.length == 3) {
            Player p = player(args[0]);
            if(p != null && p.getZone() == null) {
                executor.notify("Sorry, but " + p.getName() + " is not in a world right now.", SYSTEM);
                return null;
            }
            if(p == null && !(executor instanceof Player)) {
                executor.notify(String.format("Player '%s' not found.", args[0]), SYSTEM);
                return null;
            }
            Zone targetZone = p != null ? p.getZone() : ((Player)executor).getZone();
            if(targetZone == null) {
                executor.notify(String.format("Player or world '%s' not found.", args[0]), SYSTEM);
                return null;
            }
            Vector2i c = coords(targetZone, args[1], args[2]);
            if(c != null) {
                if(p != null) {
                    return new TeleportCommandArguments(TeleportVariant.COORDINATES, executor, p, targetZone, null, null, c.getX(), c.getY());
                } else {
                    return new TeleportCommandArguments(TeleportVariant.COORDINATES, executor, (Player)executor, targetZone, null, null, c.getX(), c.getY());
                }
            }
        }

        if(args.length == 4) {
            Player p = player(args[0]);
            if(p != null) {
                Zone z = zone(args[1]);
                if(z != null) {
                    Vector2i c = coords(z, args[2], args[3]);
                    if(c != null) {
                        return new TeleportCommandArguments(TeleportVariant.COORDINATES, executor, p, z, null, null, c.getX(), c.getY());
                    }

                    executor.notify("Wrong coordinate arguments given.", SYSTEM);
                    return null;
                }

                executor.notify(String.format("Zone '%s' not found.", args[1]), SYSTEM);
                return null;
            }

            executor.notify(String.format("Player '%s' not found.", args[0]), SYSTEM);
            return null;
        }

        executor.notify("Wrong arguments given.", SYSTEM);
        return null;
    }

    static Vector2i coords(Zone targetZone, String xs, String ys) {
        if(targetZone == null) return null;
        try {
            int x = parseNumberWithDirection(xs, targetZone.getWidth() / 2, new String[] { "left", "west", "l", "w" }, new String[] { "right", "east", "r", "e" } );
            int y = parseNumberWithDirection(ys, targetZone.getGroundHeight(), new String[] { "above", "up", "a", "u" }, new String[] { "below", "down", "b", "d" } );
            return new Vector2i(x, y);
        } catch(Exception e) {
            return null;
        }
    }

    private static int parseNumberWithDirection(String value, int offset, String[] lowerDirection, String[] upperDirection) throws NumberFormatException {
        int direction = 0;
        int unitLength = 0;

        for(String unit : lowerDirection) {
            if(value.endsWith(unit)) {
                direction = -1;
                unitLength = unit.length();
                break;
            }
        }

        for(String unit : upperDirection) {
            if(value.endsWith(unit)) {
                direction = 1;
                unitLength = unit.length();
                break;
            }
        }

        if(unitLength == 0) {
            return Integer.parseInt(value);
        } else {
            return offset + direction * Integer.parseInt(value.substring(0, value.length() - unitLength));
        }
    }

    static Player player(String name) {
        return GameServer.getInstance().getPlayerManager().getPlayer(name);
    }

    static MetaBlock plaque(Zone zone, String name) {
        String wantedName = name.toLowerCase();

        for(MetaBlock metaBlock : zone.getGlobalMetaBlocks()) {
            if(metaBlock.getItem().getGroup() == ItemGroup.PLAQUE) {
                String n = metaBlock.getStringProperty("n");
                if(n == null) continue;
                String plaqueName = n.toLowerCase();
                if(wantedName.equals(plaqueName)) {
                    return metaBlock;
                }
            }
        }

        return null;
    }

    static Zone zone(String name) {
        return GameServer.getInstance().getZoneManager().getZoneByName(name);
    }

    public TeleportVariant getVariant() {
        return variant;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public Player getPlayer() {
        return player;
    }

    public Zone getTargetZone() {
        return targetZone;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public MetaBlock getTargetPlaque() {
        return targetPlaque;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
