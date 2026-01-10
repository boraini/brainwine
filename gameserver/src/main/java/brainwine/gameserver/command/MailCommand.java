package brainwine.gameserver.command;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import java.time.format.DateTimeFormatter;
import java.util.List;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.chat.PlayerProfanity;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.input.DialogTextInput;
import brainwine.gameserver.mail.Mail;
import brainwine.gameserver.mail.MailBox;
import brainwine.gameserver.mail.PlayerMissive;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.EventMessage;

@CommandInfo(name = "mail", description = "Send and receive mail from other players.", aliases = { "pm" })
public class MailCommand extends Command {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @Override
    public void execute(CommandExecutor executor, String[] args) {
        if(!(executor instanceof Player)) {
            executor.notify("This command can only be used by players.", SYSTEM);
            return;
        }

        Player player = (Player)executor;

        // No arguments - show main menu
        if(args.length == 0) {
            showMainMenu(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch(subCommand) {
            case "inbox":
            case "i":
                showInbox(player);
                break;
            case "sent":
            case "s":
                showSent(player);
                break;
            case "send":
            case "new":
            case "compose":
                if(args.length < 2) {
                    player.notify("Usage: /mail send <player> [subject]", SYSTEM);
                    return;
                }
                String recipient = args[1];
                String subject = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : null;
                composeMail(player, recipient, subject);
                break;
            case "read":
            case "r":
                if(args.length < 2) {
                    player.notify("Usage: /mail read <number>", SYSTEM);
                    return;
                }
                try {
                    int index = Integer.parseInt(args[1]) - 1;
                    readMail(player, index, true);
                } catch(NumberFormatException e) {
                    player.notify("Please provide a valid mail number.", SYSTEM);
                }
                break;
            case "delete":
            case "del":
            case "d":
                if(args.length < 2) {
                    player.notify("Usage: /mail delete <number> [sent]", SYSTEM);
                    return;
                }
                try {
                    int index = Integer.parseInt(args[1]) - 1;
                    boolean fromSent = args.length > 2 && args[2].equalsIgnoreCase("sent");
                    deleteMail(player, index, fromSent);
                } catch(NumberFormatException e) {
                    player.notify("Please provide a valid mail number.", SYSTEM);
                }
                break;
            case "help":
            case "?":
                showHelp(player);
                break;
            default:
                player.notify("Unknown mail command. Use /mail help for available commands.", SYSTEM);
                break;
        }
    }

    @Override
    public String getUsage(CommandExecutor executor) {
        return "/mail [inbox|sent|send <player>|read <#>|delete <#>|help]";
    }

    private void showMainMenu(Player player) {
        MailBox mailBox = player.getMailBox();
        int unreadCount = mailBox.getUnreadCount();

        Dialog dialog = new Dialog()
                .setTitle("Mail System");

        // Show inbox option with unread count
        String inboxText = unreadCount > 0 ?
                String.format("View Inbox (%d unread)", unreadCount) :
                "View Inbox";
        dialog.addSection(new DialogSection().setText(inboxText).setChoice("inbox"));

        // Send new mail option
        dialog.addSection(new DialogSection().setText("Send New Mail").setChoice("send"));

        // View sent mail option
        dialog.addSection(new DialogSection().setText("View Sent Mail").setChoice("sent"));

        player.showDialog(dialog, input -> {
            if(input.length == 0 || input[0].equals("cancel")) {
                return;
            }

            String choice = input[0].toString();
            switch(choice) {
                case "inbox":
                    showInbox(player);
                    break;
                case "send":
                    startComposeMail(player);
                    break;
                case "sent":
                    showSent(player);
                    break;
            }
        });
    }

    private void showInbox(Player player) {
        MailBox mailBox = player.getMailBox();
        List<Mail> inbox = mailBox.getInbox();

        if(inbox.isEmpty()) {
            player.notify("Your inbox is empty.", SYSTEM);
            return;
        }

        Dialog dialog = new Dialog()
                .setTitle(String.format("Inbox (%d unread)", mailBox.getUnreadCount()));

        // Add each mail as a clickable button
        for(int i = 0; i < inbox.size(); i++) {
            Mail mail = inbox.get(i);
            String readIndicator = mail.isRead() ? "" : "[NEW] ";
            String dateStr = mail.getSentTime().format(DATE_FORMAT);
            String buttonText = String.format("%s%d. <color=#33AA33>%s</color> - %s\n   <color=#888888>%s</color>",
                    readIndicator, i + 1, mail.getSender(),
                    mail.getSubject() != null ? mail.getSubject() : "(No Subject)",
                    dateStr);

            dialog.addSection(new DialogSection().setText(buttonText).setChoice(String.valueOf(i)));
        }

        player.showDialog(dialog, input -> {
            if(input.length == 0 || input[0].equals("cancel")) {
                return;
            }

            try {
                int index = Integer.parseInt(input[0].toString());
                readMail(player, index, true);
            } catch(NumberFormatException e) {
                player.notify("Invalid mail selection.", SYSTEM);
            }
        });
    }

    private void startComposeMail(Player player) {
        // Step 1: Ask for recipient
        Dialog recipientDialog = new Dialog()
                .setTitle("New Mail - Step 1 of 3");

        recipientDialog.addSection(new DialogSection()
                .setText("Who would you like to send mail to?")
                .setInput(new DialogTextInput()
                        .setPlaceHolder("Player name")
                        .setMaxLength(50)
                        .setKey("recipient")));

        player.showDialog(recipientDialog, recipientInput -> {
            if(recipientInput.length == 0 || recipientInput[0].equals("cancel")) {
                player.notify("Mail cancelled.", SYSTEM);
                return;
            }

            String recipientName = recipientInput[0].toString().trim();
            if(recipientName.isEmpty()) {
                player.notify("Please provide a recipient name.", SYSTEM);
                return;
            }

            // Check if recipient exists
            Player recipient = GameServer.getInstance().getPlayerManager().getPlayer(recipientName);
            if(recipient == null) {
                player.notify(String.format("Player '%s' not found.", recipientName), SYSTEM);
                return;
            }

            composeMail(player, recipientName, null);
        });
    }

    private void showSent(Player player) {
        MailBox mailBox = player.getMailBox();
        List<Mail> sent = mailBox.getSent();

        if(sent.isEmpty()) {
            player.notify("You haven't sent any mail yet.", SYSTEM);
            return;
        }

        Dialog dialog = new Dialog().setTitle("Sent Mail");

        // Add each mail as a clickable button
        for(int i = 0; i < sent.size(); i++) {
            Mail mail = sent.get(i);
            String dateStr = mail.getSentTime().format(DATE_FORMAT);
            String buttonText = String.format("%d. To <color=#33AA33>%s</color> - %s\n   <color=#888888>%s</color>",
                    i + 1, mail.getRecipient(),
                    mail.getSubject() != null ? mail.getSubject() : "(No Subject)",
                    dateStr);

            dialog.addSection(new DialogSection().setText(buttonText).setChoice(String.valueOf(i)));
        }

        player.showDialog(dialog, input -> {
            if(input.length == 0 || input[0].equals("cancel")) {
                return;
            }

            try {
                int index = Integer.parseInt(input[0].toString());
                readMail(player, index, false);
            } catch(NumberFormatException e) {
                player.notify("Invalid mail selection.", SYSTEM);
            }
        });
    }

    private void composeMail(Player player, String recipientName, String subject) {
        // Check if recipient exists
        Player recipient = GameServer.getInstance().getPlayerManager().getPlayer(recipientName);
        if(recipient == null) {
            player.notify(String.format("Player '%s' not found.", recipientName), SYSTEM);
            return;
        }

        // Create compose dialog
        Dialog dialog = new Dialog()
                .setTitle(String.format("New Mail to %s", recipient.getName()));

        // Subject input (if not provided)
        if(subject == null) {
            dialog.addSection(new DialogSection()
                    .setText("Subject:")
                    .setInput(new DialogTextInput()
                            .setMaxLength(50)
                            .setKey("subject")));
        }

        // Message input
        dialog.addSection(new DialogSection()
                .setText("Message:")
                .setInput(new DialogTextInput()
                        .setMaxLength(500)
                        .setKey("message")));

        player.showDialog(dialog, input -> {
            if(input.length == 0 || input[0].equals("cancel")) {
                player.notify("Mail cancelled.", SYSTEM);
                return;
            }

            String finalSubject = subject;
            String message;

            if(subject == null) {
                // Subject and message provided in input
                if(input.length < 2) {
                    player.notify("Mail cancelled - missing message.", SYSTEM);
                    return;
                }
                finalSubject = input[0].toString();
                message = input[1].toString();
            } else {
                // Only message in input
                message = input[0].toString();
            }

            if(message == null || message.trim().isEmpty()) {
                player.notify("Cannot send empty mail.", SYSTEM);
                return;
            }

            // Check if recipient exists
            Player myRecipient = GameServer.getInstance().getPlayerManager().getPlayer(recipient.getName());
            if(myRecipient == null) {
                player.notify(String.format("Player '%s' not found.", recipient.getName()), SYSTEM);
                return;
            }

            // Apply profanity filter to subject and message
            String filteredSubject = finalSubject != null ? PlayerProfanity.filterAndPunish(player, finalSubject) : null;
            String filteredMessage = PlayerProfanity.filterAndPunish(player, message);

            // Create and send mail
            Mail mail = new Mail(player.getName(), myRecipient.getName(), filteredSubject, filteredMessage);
            myRecipient.getMailBox().receiveMail(mail);
            player.getMailBox().addSentMail(mail);

            player.notify(String.format("Mail sent to %s!", myRecipient.getName()), SYSTEM);

            // Notify recipient if online
            if(myRecipient.isOnline()) {
                myRecipient.notify(String.format("You've received new mail from %s!", player.getName()), SYSTEM);
                PlayerMissive.sendMissive(myRecipient, mail);
            }
        });
    }

    private void readMail(Player player, int index, boolean fromInbox) {
        MailBox mailBox = player.getMailBox();
        List<Mail> mailList = fromInbox ? mailBox.getInbox() : mailBox.getSent();

        if(index < 0 || index >= mailList.size()) {
            player.notify("Invalid mail number.", SYSTEM);
            return;
        }

        Mail mail = mailList.get(index);

        // Mark as read if it's from inbox
        if(fromInbox && !mail.isRead()) {
            mail.setRead(true);
            PlayerMissive.sendUnreadCount(player);
        }

        Dialog dialog = new Dialog()
                .setTitle(mail.getSubject() != null ? mail.getSubject() : "(No Subject)");

        String dateStr = mail.getSentTime().format(DATE_FORMAT);
        String header = fromInbox ?
                String.format("<color=#33AA33>From:</color> %s\n<color=#888888>%s</color>", mail.getSender(), dateStr) :
                String.format("<color=#33AA33>To:</color> %s\n<color=#888888>%s</color>", mail.getRecipient(), dateStr);

        dialog.addSection(new DialogSection().setText(header));
        dialog.addSection(new DialogSection().setText("-------------------"));
        dialog.addSection(new DialogSection().setText(mail.getMessage()));

        // Add reply option for inbox mail
        if(fromInbox) {
            dialog.addSection(new DialogSection().setText("Reply").setChoice("reply"));
            dialog.addSection(new DialogSection().setText("Delete").setChoice("delete"));
            dialog.addSection(new DialogSection().setText("Back to Inbox").setChoice("back"));
        } else {
            dialog.addSection(new DialogSection().setText("Back to Sent").setChoice("back"));
        }

        player.showDialog(dialog, input -> {
            if(input.length > 0 && !input[0].equals("cancel")) {
                String choice = input[0].toString();
                if(choice.equals("reply")) {
                    composeMail(player, mail.getSender(), "Re: " + mail.getSubject());
                } else if(choice.equals("delete")) {
                    deleteMail(player, index, false);
                } else if(choice.equals("back")) {
                    if(fromInbox) {
                        showInbox(player);
                    } else {
                        showSent(player);
                    }
                }
            }
        });
    }

    private void deleteMail(Player player, int index, boolean fromSent) {
        MailBox mailBox = player.getMailBox();
        List<Mail> mailList = fromSent ? mailBox.getSent() : mailBox.getInbox();

        if(index < 0 || index >= mailList.size()) {
            player.notify("Invalid mail number.", SYSTEM);
            return;
        }

        Mail mail = mailList.get(index);
        boolean deleted = fromSent ? mailBox.deleteSentMail(mail.getId()) : mailBox.deleteInboxMail(mail.getId());

        if(deleted) {
            player.notify("Mail deleted.", SYSTEM);
            PlayerMissive.sendUnreadCount(player);
        } else {
            player.notify("Failed to delete mail.", SYSTEM);
        }
    }

    private void showHelp(Player player) {
        Dialog dialog = new Dialog().setTitle("Mail System Help");

        StringBuilder help = new StringBuilder();
        help.append("<color=#FFD700>/mail</color> or <color=#FFD700>/mail inbox</color>\n");
        help.append("  View your inbox\n\n");
        help.append("<color=#FFD700>/mail sent</color>\n");
        help.append("  View sent mail\n\n");
        help.append("<color=#FFD700>/mail send <player> [subject]</color>\n");
        help.append("  Compose new mail to a player\n\n");
        help.append("<color=#FFD700>/mail read <number></color>\n");
        help.append("  Read a mail from your inbox\n\n");
        help.append("<color=#FFD700>/mail delete <number> [sent]</color>\n");
        help.append("  Delete mail from inbox or sent folder\n\n");
        help.append("<color=#888888>Aliases: /m</color>");

        dialog.addSection(new DialogSection().setText(help.toString()));

        player.showDialog(dialog);
    }

}
