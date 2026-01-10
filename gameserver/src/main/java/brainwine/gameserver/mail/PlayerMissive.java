package brainwine.gameserver.mail;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.MissiveInfoMessage;
import brainwine.gameserver.server.messages.MissiveMessage;

public class PlayerMissive {
    public static boolean shouldSendMissive(Player player) {
        return player.isOnline();
    }

    public static void sendInitialMissiveMessages(Player player) {
        if(player.getMailBox().getUnreadCount() > 0) {
            player.notify("You have " + player.getMailBox().getUnreadCount() + " unread mail!");
        }
        if(!shouldSendMissive(player)) return;
        sendUnreadCount(player);
        player.getMailBox().getInbox().forEach(mail -> {
            if(!mail.isRead()) player.sendMessage(new MissiveMessage(mail));
        });
    }

    public static void sendMissive(Player player, Mail mail) {
        if(!shouldSendMissive(player)) return;
        player.sendMessage(MissiveInfoMessage.unreadCount(player.getMailBox().getUnreadCount()));
        player.sendMessage(new MissiveMessage(mail));
    }

    public static void sendUnreadCount(Player player) {
        if(!shouldSendMissive(player)) return;
        player.sendMessage(MissiveInfoMessage.unreadCount(player.getMailBox().getUnreadCount()));
    }

}
