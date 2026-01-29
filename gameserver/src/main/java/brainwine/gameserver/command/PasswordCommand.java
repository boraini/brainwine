package brainwine.gameserver.command;

import static brainwine.gameserver.player.NotificationType.SYSTEM;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.input.DialogTextInput;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.mindrot.jbcrypt.BCrypt;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.player.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

@CommandInfo(name = "password", description = "Manage player passwords. Admins can set passwords for any player, players can change their own password.")
public class PasswordCommand extends Command {
    
    @Override
    public void execute(CommandExecutor executor, String[] args) {
        Player player = (Player)executor;
        
        // No arguments - show usage
        if(args.length == 0) {
            sendUsageMessage(executor);
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        // Handle "set" subcommand (admin only)
        if(subcommand.equals("set")) {
            handleSetPassword(player, args);
            return;
        }
        
        // Handle "change" subcommand (any player)
        if(subcommand.equals("change")) {
            handleChangePassword(player, args);
            return;
        }
        
        // Unknown subcommand
        executor.notify("Unknown subcommand. Use 'set' or 'change'.", SYSTEM);
        sendUsageMessage(executor);
    }
    
    private void handleSetPassword(Player executor, String[] args) {
        // Check admin permission
        if(!executor.isAdmin()) {
            executor.notify("You need to be an admin to set other players' passwords.", SYSTEM);
            return;
        }
        
        // Check argument count
        if(args.length < 3) {
            executor.notify("Usage: /pw set <username> <password>", SYSTEM);
            return;
        }
        
        String targetUsername = args[1];
        String newPassword = args[2];
        
        // Find the target player
        Player targetPlayer = GameServer.getInstance().getPlayerManager().getPlayer(targetUsername);
        
        if(targetPlayer == null) {
            executor.notify("Player '" + targetUsername + "' does not exist.", SYSTEM);
            return;
        }
        
        // Validate password
        if(!isValidPassword(newPassword)) {
            executor.notify("Invalid password. Password must be 8-64 characters and contain at least one digit, one lowercase letter, and one uppercase letter.", SYSTEM);
            return;
        }
        
        // Set the password
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        targetPlayer.setPassword(hashedPassword);
        
        // Notify admin
        executor.notify("Password successfully set for player '" + targetUsername + "'. They will need to log in with the new password.", SYSTEM);
        
        // Notify target player if online
        if(targetPlayer.isOnline()) {
            targetPlayer.notify("Your password has been changed by an administrator. You will need to use the new password on your next login.", SYSTEM);
        }
    }
    
    private void handleChangePassword(Player executor, String[] args) {
        // Check argument count
        if(args.length < 1) {
            executor.notify("Usage: /pw change", SYSTEM);
            return;
        }

        executor.showDialog(
                new Dialog().setTitle("Change your Password")
                        .addSection(new DialogSection().setTitle("Current Password").setInput(new DialogTextInput().setPassword(true).setKey("old_password")))
                        .addSection(new DialogSection().setTitle("New Password").setInput(new DialogTextInput().setPassword(true).setKey("new_password"))),
                ans -> {
                    if(ans.length == 1 && "cancel".equals(ans[0])) return;
                    if(ans.length != 2) executor.notify("Bad dialog input.");
                }
        );

        String oldPassword = args[1];
        String newPassword = args[2];
        
        // Check if player is registered
        if(!executor.isRegistered()) {
            executor.notify("You need to register your account first using /register.", SYSTEM);
            return;
        }

        if(!GameServer.getInstance().getPlayerManager().checkPassword(executor, oldPassword)) {
            executor.notify("Old password does not match. Try again.", SYSTEM);
            return;
        }
        
        // Validate password
        if(!isValidPassword(newPassword)) {
            executor.notify("Invalid password. Password must be 8-64 characters and contain at least one digit, one lowercase letter, and one uppercase letter.", SYSTEM);
            return;
        }
        
        // Set the new password
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        executor.setPassword(hashedPassword);
        
        // Notify the player
        executor.notify("Your password has been successfully changed. You will need to use the new password on your next login.", SYSTEM);
    }
    
    private boolean isValidPassword(String password) {
        // Password must be 8-64 characters and contain at least one digit, 
        // one lowercase letter, and one uppercase letter
        return password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,64}$");
    }
    
    @Override
    public String getUsage(CommandExecutor executor) {
        if(executor.isAdmin()) {
            return "/pw set <username> <password> OR /pw change <old_password> <new_password>";
        } else {
            return "/pw change <old_password> <new_password>";
        }
    }
    
    @Override
    public boolean canExecute(CommandExecutor executor) {
        return executor instanceof Player;
    }

    @Override
    public boolean useSmartArguments() {
        return true;
    }

    @Override
    protected String getLogMessage(Player executor, String commandName, String[] args) {
        return new ParameterizedMessage("{} used command '/{}'", executor.getName(), commandName + (args.length == 0 ? "" : " " + Arrays.stream(args).map(arg -> "###").collect(Collectors.joining(" ")))).getFormattedMessage();
    }
}
