package brainwine.gameserver.command.admin;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.command.Command;
import brainwine.gameserver.command.CommandExecutor;
import brainwine.gameserver.command.CommandInfo;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.quest.PlayerQuests;
import brainwine.gameserver.quest.Quest;
import brainwine.gameserver.quest.Quests;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

@CommandInfo(name = "cancelquest", description = "Cancel a quest for a specific player or everyone.")
public class CancelQuestCommand extends Command {
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(args.length < 2) {
            executor.notify(String.format("Usage: %s", getUsage(executor)), SYSTEM);
            return;
        }
        boolean everyone = args[0].equalsIgnoreCase("(everyone)");
        Player player = null;
        if(!everyone) {
            player = GameServer.getInstance().getPlayerManager().getPlayer(args[0]);
            if(player == null) {
                executor.notify("This player does not exist.", SYSTEM);
                return;
            }
        }

        Quest quest = player != null ? Quests.get(player, args[1]) : Quests.get(args[1]);

        if(quest == null) {
            String questId = Quests.findQuestIdByTitle(player, args[1]);
            if(questId != null) quest = player != null ? Quests.get(player, questId) : Quests.get(questId);
        }

        if(quest == null) {
            executor.notify("Quest not found!", SYSTEM);
            return;
        }

        final Quest foundQuest = quest;
        final Player singleTarget = player;

        final Runnable task = () -> {
            if(everyone) {
                int count = 0;
                for(Player target : GameServer.getInstance().getPlayerManager().getPlayers()) {
                    if(target.getQuestProgresses().containsKey(foundQuest.getId())) {
                        PlayerQuests.cancelQuest(target, foundQuest.getId(), true);
                        count++;
                    }
                }
                executor.notify("Cancelled this quest for " + count + (count == 1 ? "player." : "players."), SYSTEM);
            } else {
                if(singleTarget.getQuestProgresses().containsKey(foundQuest.getId())) {
                    PlayerQuests.cancelQuest(singleTarget, foundQuest.getId());
                } else {
                    executor.notify("Target player doesn't have this quest!", SYSTEM);
                }
            }
        };

        if(executor instanceof Player) {
            Dialog dialog = new Dialog()
                    .setTitle("Cancelling Quest \"" + quest.getTitle() + "\"")
                    .addSection(new DialogSection().setText("You will be cancelling " + foundQuest.getTitle() + " (" + foundQuest.getId() + ") for " + (everyone ? "everyone" : singleTarget.getName()) + ". Are you sure?"))
                    .setActions("yesno");

            ((Player)executor).showDialog(dialog, ans -> {
                if(ans.length == 0 || !"cancel".equals(ans[0])) {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/cancelquest <\"(everyone)\" including the parentheses or player name> <quest id or title>";
    }

    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor.isAdmin();
    }

    @Override
    public boolean useSmartArguments() {
        return true;
    }
}
