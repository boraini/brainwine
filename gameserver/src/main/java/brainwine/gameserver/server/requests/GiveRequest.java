package brainwine.gameserver.server.requests;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.PlayerRequest;
import brainwine.gameserver.server.RequestInfo;

/**
 * Handles the v3 client's "/give &lt;player|me&gt; &lt;item&gt; [quantity]" console command.
 *
 * The client intercepts /give with its own GiveConsoleCommand and sends a dedicated Give request
 * (id 200) carrying [playerName, itemCode, quantity] — it does NOT route through the chat/console
 * command pipeline. brainwine only implemented /give as a console command (id 47), so the client's
 * request hit no handler and was silently dropped. This wires it up. Admin-only, mirroring
 * {@code GiveCommand}.
 */
@RequestInfo(id = 200)
public class GiveRequest extends PlayerRequest {

    public String playerName;
    public int itemCode;
    public int quantity;

    @Override
    public void process(Player player) {
        // Never trust the client: gate on admin server-side (the console command is admin-only too).
        if(!player.isAdmin()) {
            return;
        }

        Player target = GameServer.getInstance().getPlayerManager().getPlayer(playerName);

        if(target == null) {
            player.notify("That player does not exist.", SYSTEM);
            return;
        }

        Item item = ItemRegistry.getItem(itemCode);

        if(item.isAir()) {
            player.notify("This item does not exist.", SYSTEM);
            return;
        }

        int amount = quantity > 0 ? quantity : 1;
        target.getInventory().addItem(item, amount, true);
        target.notify(String.format("You received %s %s from an administrator.", amount, item.getTitle()), SYSTEM);

        if(target != player) {
            player.notify(String.format("Gave %s %s to %s", amount, item.getTitle(), target.getName()), SYSTEM);
        }
    }
}
