package brainwine.gameserver.mail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a player's mailbox containing received mail.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MailBox {
    
    private List<Mail> inbox;
    private List<Mail> sent;
    
    @JsonCreator
    public MailBox() {
        this.inbox = new ArrayList<>();
        this.sent = new ArrayList<>();
    }
    
    /**
     * Adds mail to the inbox.
     */
    public void receiveMail(Mail mail) {
        inbox.add(mail);
    }
    
    /**
     * Adds mail to the sent folder.
     */
    public void addSentMail(Mail mail) {
        sent.add(mail);
    }
    
    /**
     * Gets all inbox mail sorted by sent time (newest first).
     */
    public List<Mail> getInbox() {
        return inbox.stream()
                .sorted(Comparator.comparing(Mail::getSentTime).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all sent mail sorted by sent time (newest first).
     */
    public List<Mail> getSent() {
        return sent.stream()
                .sorted(Comparator.comparing(Mail::getSentTime).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets mail by ID from either inbox or sent folder.
     */
    public Mail getMail(String id) {
        return inbox.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElse(sent.stream()
                        .filter(m -> m.getId().equals(id))
                        .findFirst()
                        .orElse(null));
    }
    
    /**
     * Deletes mail from the inbox by ID.
     */
    public boolean deleteInboxMail(String id) {
        return inbox.removeIf(m -> m.getId().equals(id));
    }
    
    /**
     * Deletes mail from the sent folder by ID.
     */
    public boolean deleteSentMail(String id) {
        return sent.removeIf(m -> m.getId().equals(id));
    }
    
    /**
     * Gets the number of unread messages.
     */
    public int getUnreadCount() {
        return (int) inbox.stream()
                .filter(m -> !m.isRead())
                .count();
    }
    
    /**
     * Marks all mail as read.
     */
    public void markAllRead() {
        inbox.forEach(m -> m.setRead(true));
    }
}
