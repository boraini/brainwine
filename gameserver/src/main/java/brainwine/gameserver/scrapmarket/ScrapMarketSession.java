package brainwine.gameserver.scrapmarket;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.player.Player;

public class ScrapMarketSession {
    private ScrapMarket shop;
    private Npc me;
    private Player player;

    public ScrapMarketSession(ScrapMarket shop, Npc me, Player player) {
        this.shop = shop;
        this.me = me;
        this.player = player;
    }

    public void showNextDialog() {
        Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Scrap Market");

        dialog.addSection(new DialogSection().setText("Visit Shop").setChoice("buy"));
        dialog.addSection(new DialogSection().setText("View My Own Offers").setChoice("my_offers"));

        player.showDialog(dialog, ans -> {
            if(ans.length == 0 || "cancel".equals(ans[0])) {
                return;
            }

            if("buy".equals(ans[0])) {
                new ScrapMarketBuySession(shop, me, player).showNextDialog();
            }

            if("my_offers".equals(ans[0])) {
                new ScrapMarketViewSession(shop, me, player).showNextDialog();
            }
        });
    }
}
