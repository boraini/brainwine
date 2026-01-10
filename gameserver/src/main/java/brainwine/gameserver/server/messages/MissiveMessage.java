package brainwine.gameserver.server.messages;

import brainwine.gameserver.mail.Mail;
import brainwine.gameserver.server.Message;
import brainwine.gameserver.server.MessageInfo;

import java.time.OffsetDateTime;

@MessageInfo(id = 55)
public class MissiveMessage extends Message {
    private static String id;
    private static String type;
    private static OffsetDateTime date;
    String sender;
    String message;
    boolean read;

    public MissiveMessage(Mail mail) {
        id = mail.getId();
        type = "pm";
        date = mail.getSentTime();
        sender = mail.getSender();
        message = mail.getMessage();
        read = mail.isRead();
    }

    public static String getId() {
        return id;
    }

    public static String getType() {
        return type;
    }

    public static OffsetDateTime getDate() {
        return date;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }
}
