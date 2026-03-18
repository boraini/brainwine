package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class SpecialRequestSession {
    private Npc me;
    private Player player;
    private Consumer<Boolean> onOutcome;
    private Optional<SpecialRequest> currentRequest = Optional.empty();

    public SpecialRequestSession(Npc me, Player player) {
        this(me, player, null);
    }

    public SpecialRequestSession(Npc me, Player player, Consumer<Boolean> onOutcome) {
        this.me = me;
        this.player = player;
        this.onOutcome = onOutcome;
    }

    public void showNextDialog() {
        if (!currentRequest.isPresent()) {
            showBarterTradesDialog();
            return;
        }
        showTradeConfirmation();
    }

    public void showBarterTradesDialog() {
        List<SpecialRequest> trades = SpecialRequestRegistry.getInstance().getTrades();

        if(trades.isEmpty()) {
            player.showDialog(DialogHelper.messageDialog(
                    "No Trades Available",
                    "Sorry, I don't have any special trades available right now."
            ).setType(DialogType.ANDROID));
            return;
        }

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Special Trades");
        dialog.addSection(new DialogSection().setText("Here are the trades I can offer you:"));

        for(int i = 0; i < trades.size(); i++) {
            SpecialRequest trade = trades.get(i);
            boolean canAfford = trade.canPlayerAfford(player);

            String tradeText = trade.getName();
            if(!canAfford) {
                tradeText += " (Not enough items)";
            }

            dialog.addSection(new DialogSection()
                    .setText(tradeText)
                    .setChoice("trade_" + i));
        }

        player.showDialog(dialog, ans -> {
            if(ans.length == 0) return;

            String choice = ans[0].toString();
            if(choice.startsWith("trade_")) {
                try {
                    int tradeIndex = Integer.parseInt(choice.substring(6));
                    if(tradeIndex >= 0 && tradeIndex < trades.size()) {
                        currentRequest = Optional.of(trades.get(tradeIndex));
                        showTradeConfirmation();
                    }
                } catch(NumberFormatException e) {
                    // Invalid trade index
                    end(false);
                }
            }
        });
    }

    /**
     * Show confirmation dialog for a specific trade
     */
    public void showTradeConfirmation() {
        if (!currentRequest.isPresent()) {
            player.notify("You haven't picked a request yet.");
            return;
        }
        SpecialRequest trade = currentRequest.get();
        boolean canAfford = trade.canPlayerAfford(player);

        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle(trade.getName());

        // Show description if available
        if(trade.getDescription() != null && !trade.getDescription().isEmpty()) {
            dialog.addSection(new DialogSection().setText(trade.getDescription()));
        }

        // Show required items
        DialogSection requiredSection = new DialogSection().setTitle("You give:");
        for(Map.Entry<Item, Integer> entry : trade.getRequiredItems().entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            int playerHas = player.getInventory().getQuantity(item);
            String itemTitle = getItemTitle(item);
            String text = itemTitle + " x " + quantity + " (You have: " + playerHas + ")";
            requiredSection.addItem(new DialogListItem().setItem(item.getCode()).setText(text));
        }
        dialog.addSection(requiredSection);

        // Show reward items
        DialogSection rewardSection = new DialogSection().setTitle("You get:");
        for(Map.Entry<Item, Integer> entry : trade.getRewardItems().entrySet()) {
            Item item = entry.getKey();
            int quantity = entry.getValue();
            String itemTitle = getItemTitle(item);
            rewardSection.addItem(new DialogListItem().setItem(item.getCode()).setText(itemTitle + " x " + quantity));
        }
        dialog.addSection(rewardSection);

        if(!canAfford) {
            dialog.addSection(new DialogSection().setText("You don't have enough items for this trade."));
            player.showDialog(dialog);
            end(false);
        } else {
            dialog.addSection(new DialogSection().setText("Do you want to make this trade?"));
            dialog.setActions("yesno");

            player.showDialog(dialog, ans -> {
                if(ans.length > 0 && !"cancel".equals(ans[0])) {
                    // Double-check player can still afford it
                    if(trade.canPlayerAfford(player)) {
                        trade.executeTrade(player);
                        me.emote("Pleasure doing business with you.");
                        player.notify("Trade completed!");
                        end(true);
                    } else {
                        player.notify("You don't have enough items for this trade.");
                        end(false);
                    }
                }
            });
        }
    }

    public void end(boolean outcome) {
        if(onOutcome != null) onOutcome.accept(outcome);
    }

    private String getItemTitle(Item item) {
        return item.getTitle() != null ? item.getTitle() : item.getId();
    }

}
