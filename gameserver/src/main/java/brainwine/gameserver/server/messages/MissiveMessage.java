package brainwine.gameserver.server.messages;

import brainwine.gameserver.mail.Mail;
import brainwine.gameserver.server.Message;
import brainwine.gameserver.server.MessageInfo;

import java.time.OffsetDateTime;

@MessageInfo(id = 55)
public class MissiveMessage extends Message {
    private String id;
    private String type;
    private OffsetDateTime date;
    private String sender;
    private String message;
    private boolean read;

    public MissiveMessage(Mail mail) {
        id = mail.getId();
        type = "pm";
        date = mail.getSentTime();
        sender = mail.getSender();
        message = mail.getMessage();
        read = mail.isRead();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public OffsetDateTime getDate() {
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
