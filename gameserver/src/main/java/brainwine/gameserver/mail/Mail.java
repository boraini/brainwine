package brainwine.gameserver.mail;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a mail message that can be sent between players.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mail {

    private static final int TRUNCATION_SIZE = 50;

    private String id;
    private String sender;
    private String recipient;
    private String subject;
    private String message;
    private OffsetDateTime sentTime;
    private boolean read;
    
    @JsonCreator
    public Mail() {
        this.id = UUID.randomUUID().toString();
        this.sentTime = OffsetDateTime.now();
        this.read = false;
    }
    
    /**
     * Creates a new mail message.
     * 
     * @param sender The name of the player sending the mail
     * @param recipient The name of the player receiving the mail
     * @param subject The subject line of the mail
     * @param message The message content
     */
    public Mail(String sender, String recipient, String subject, String message) {
        this();
        this.sender = sender;
        this.recipient = recipient;
        this.subject = subject;
        this.message = message;
    }
    
    public String getId() {
        return id;
    }
    
    public String getSender() {
        return sender;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public OffsetDateTime getSentTime() {
        return sentTime;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    /**
     * Gets a short summary of the mail for display in the inbox.
     */
    public String getSummary() {
        String content = message != null && message.length() > TRUNCATION_SIZE ?
            message.substring(0, TRUNCATION_SIZE - 3) + "..." : message;
        return String.format("%s: %s", subject != null ? subject : "(No Subject)", content);
    }

    public Map<String, Object> toMissive() {
        Player senderPlayer = GameServer.getInstance().getPlayerManager().getPlayerById(sender);
        return MapHelper.map(String.class, Object.class,
                "player_id", recipient,
                "creator_id", sender,
                "creator_name", senderPlayer != null ? senderPlayer.getName() : "Unknown",
                "type", "pm",
                "message", message,
                "created_at", sentTime.toString(),
                "read", read
        );
    }
}
