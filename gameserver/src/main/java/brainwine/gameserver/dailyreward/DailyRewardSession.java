package brainwine.gameserver.dailyreward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dailyreward.DailyReward;
import brainwine.gameserver.dailyreward.DailyRewardManager;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
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

        try {
        Dialog dialog;
        switch (state) {
            case MODE_SELECT:
                dialog = new Dialog().setTitle("Filtering Daily Rewards").setActions("cancel");

                dialog.addSection(new DialogSection().setText("Please select how you would like to view the current rewards."));
                for (Mode mode : Mode.values()) {
                    dialog.addSection(new DialogSection().setChoice(mode.toString()).setText(mode.getDescription()));
                }

                dialog.addSection(new DialogSection().setText("You can also add a new reward."));
                dialog.addSection(new DialogSection().setChoice("new").setText("New Reward"));

                player.showDialog(dialog, ans -> {
                    if (ans.length == 0 || "cancel".equals(ans[0])) return;
                    if ("new".equals(ans[0])) {
                        new NewDailyRewardSession(player).showNextDialog();
                        return;
                    }
                    Mode mode = Mode.valueOf((String)ans[0]);
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
                List<DailyReward> all = this.mode == Mode.ACTIVE ? dailyRewardManager.getActiveRewards() : dailyRewardManager.getRewards();
                int numPages = (all.size() + 1) / PAGE_SIZE;
                page = Math.max(0, Math.min(numPages - 1, page));
                view = all.subList(page * PAGE_SIZE, Math.min(all.size(), page * PAGE_SIZE + 1));

                dialog = new Dialog().setTitle(mode.getDescription()).setActions("Prev", "Next");

                for(int i = 0; i < view.size(); i++) {
                    DailyReward reward = view.get(i);
                    makeRewardSection(reward, player.isV3()).stream().forEach(dialog::addSection);
                    dialog.addSection(new DialogSection().setChoice("remove_" + i).setText("Remove " + reward.getTitle()));
                }

                dialog.addSection(new DialogSection().setChoice("close").setText("Close Dialog"));

                player.showDialog(dialog, ans -> {
                    if(ans[0] == null || ((ans[0] instanceof String) && ((String)ans[0]).length() == 0)) {
                        page++;
                        this.showNextDialog();
                        return;
                    }

                    if("cancel".equals(ans[0])) {
                        if(page == 0) {
                            return;
                        }
                        page--;
                        this.showNextDialog();
                        return;
                    }

                    if("close".equals(ans[0])) {
                        return;
                    }
                    
                    if(ans[0] instanceof String && ((String)ans[0]).startsWith("remove_")) {
                        String numStr = ((String)ans[0]).substring("remove_".length());
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
                break;

            case REMOVE:
                DailyReward reward = view.get(i);
                dialog = new Dialog().setTitle("Removing " + reward.getTitle()).setActions("yesno");
                makeRewardSection(reward, player.isV3()).stream().forEach(dialog::addSection);
                dialog.addSection(new DialogSection().setText("Are you sure you want to remove this reward? Players will lose the XP progress they made towards it since the beginning time of the reward."));
                player.showDialog(dialog, ans -> {
                    if(ans[0] == null || "cancel".equals(ans[0]) || !player.isAdmin()) {
                        state = State.REWARDS;
                        this.showNextDialog();
                        return;
                    }

                    dailyRewardManager.removeReward(view.get(i));
                    this.showNextDialog();
                });
                break;
        }
        } catch(Exception e) {
            player.notify("An exception has occurred while processing!");
            e.printStackTrace();
        }
    }

    public List<DialogSection> makeRewardSection(DailyReward reward, boolean v3) {
        List<DialogSection> result = new ArrayList<>();

        result.add(new DialogSection().setText(String.format(v3 ? "<color=#00fffff>%s</color>" : "%s", reward.getTitle())));

        if(!reward.getItems().isEmpty()) {
            DialogSection itemsSection = new DialogSection().setTitle("Items");
            for(Map.Entry<Item, Integer> item : reward.getItems().entrySet()) {
                itemsSection.addItem(new DialogListItem().setItem(item.getKey().getCode()).setText(v3 ? item.getKey().getFancyTitle() : item.getKey().getTitle() + " x " + item.getValue()));
            }
            result.add(itemsSection);
        }

        if(reward.getLootCategories().length > 0) {
            DialogSection lootCategoriesSection = new DialogSection().setTitle("Loot Categories");
            for(String category : reward.getLootCategories()) {
                lootCategoriesSection.addItem(new DialogListItem().setText(category));
            }
            result.add(lootCategoriesSection);
        }

        return result;
    }
}
