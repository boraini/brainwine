package brainwine.gameserver.dailyreward;

import java.util.Collections;
import java.util.List;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dailyreward.DailyReward;
import brainwine.gameserver.dailyreward.DailyRewardManager;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;

public class DailyRewardSession {
    DailyRewardManager dailyRewardManager = GameServer.getInstance().getDailyRewardManager();

    protected enum State {
        MODE_SELECT,
        REWARDS,
        REMOVE,
    }

    protected enum Mode {
        ALL("All Rewards"),
        ACTIVE("Non-Expired-only"),
        ;

        private String description;
        private Mode(String description) {
            this.description = description;
        }
        public String getDescription() {
            return description;
        }
    }

    private static int PAGE_SIZE = 10;

    private State state = State.MODE_SELECT;
    private Mode mode = Mode.ALL;

    private Player player;
    private List<DailyReward> view = Collections.emptyList();
    private int i = 0;
    private int page = 0;

    public DailyRewardSession(Player player) {
        this.player = player;
    }

    public void showNextDialog() {
        if (!player.isAdmin())
            return;

        Dialog dialog;
        switch (state) {
            case MODE_SELECT:
                dialog = new Dialog().setTitle("Filtering Daily Rewards").setOptions("cancel");

                dialog.addSection("Please select how you would like to view the current rewards.");
                for (Mode mode : Mode.values) {
                    dialog.addSection(new DialogSection().setChoice(mode.toString()).setText(mode.getDescription()));
                }

                dialog.addSection("You can also add a new reward.");
                dialog.addSection(new DialogSection().setChoice("new").setText("New Reward"));

                player.showDialog(dialog, ans -> {
                    if (ans.length == 0 || "cancel".equals(ans[0])) return;
                    if ("new".equals(ans[0])) {
                        new NewDailyRewardSession(player).showNextDialog();
                        return;
                    }
                    Mode mode = Mode.valueOf(ans[0]);
                    if(mode == null) {
                        player.notify("You have bad input: " + ans[0]);
                        return;
                    }

                    this.mode = mode;
                    this.state = State.REWARDS;
                    this.showNextDialog();
                });
                break;
            

            case REWARDS:
                List<String> all = this.mode == Mode.ACTIVE ? dailyRewardManager.getActiveRewards() : dailyRewardManager.getRewards();
                int numPages = (all.size() + 1) / PAGE_SIZE;
                page = Math.max(0, Math.min(numPages - 1, page));
                view = all.subList(page * PAGE_SIZE, Math.min(all.size(), page * PAGE_SIZE + 1));

                dialog = new Dialog().setTitle(mode.getDescription()).setOptions("Prev", "Next");

                for(int i = 0; i < view.size(); i++) {
                    DailyReward reward = view.get(i);
                    dialog.addSection(makeRewardSection(reward));
                    dialog.addSection(new DialogSection().setChoice("remove_" + i).setText("Remove " + reward.getTitle()));
                }

                dialog.addSection(new DialogSection().setChoice("close").setText("Close Dialog"));

                player.showDialog(dialog, ans -> {
                    if(ans[0] == null || ans[0].length() == 0) {
                        page++;
                        this.showNextDialog();
                        return;
                    }

                    if("cancel".equals(ans[0])) {
                        if(pag == 0) {
                            return;
                        }
                        page--;
                        this.showNextDialog();
                        return;
                    }

                    if("close".equals(ans[0])) {
                        return;
                    }
                    
                    if(ans[0].startsWith("remove_")) {
                        String numStr = ans[0].substring("remove_".length());
                        try {
                            int index = Integer.parseInt(numStr);
                            if(index >= view.size() || index < 0) {
                                player.notify("Index out of bounds: " + index);
                                return;
                            }
                            this.i = index;
                            this.state = State.REMOVE;
                            this.showNextDialog();
                            return;
                        } catch(NumberFormatException e) {
                            player.notify("Invalid input: " + ans[0]);
                            return;
                        }
                    }
                });

            case REMOVE:
                DailyReward reward = view.get(i);
                dialog = new Dialog().setTitle("Removing " + reward.getTitle()).setOptions("yesno").addSection(makeRewardSection()).addSection(new DialogSection().setText("Are you sure you want to remove this reward? Players will lose the XP progress they made towards it since the beginning time of the reward."));
                player.showDialog(dialog, ans -> {
                    if(ans[0] == null || "cancel".equals(ans[0]) || !player.isAdmin()) {
                        state = State.REWARDS;
                        this.showNextDialog();
                        return;
                    }

                    dailyRewardManager.removeReward(view.get(i));
                    this.showNextDialog();
                });
        }
    }
}
