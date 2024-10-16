package brainwine;

import java.awt.Desktop;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import brainwine.gui.GuiPreferences;
import brainwine.gui.theme.ThemeManager;
import brainwine.gui.view.MainView;
import brainwine.util.OperatingSystem;
import brainwine.util.SwingUtils;

public class Main {

    private static Logger logger = LogManager.getLogger();
    private static boolean disableGui = false;
    private static boolean forceGui = false;
    private final List<ServerStatusListener> listeners = new ArrayList<>();
    private ServerThread serverThread;
    private boolean closeRequested;
    
    public static void main(String[] args) {
        for(String arg : args) {
            if(arg.equalsIgnoreCase("disablegui")) {
                disableGui = true;
            }
            
            if(arg.equalsIgnoreCase("forcegui")) {
                forceGui = true;
            }
        }
        
        new Main();
    }
    
    public Main() {
        // Create gui or directly start server if gui is disabled or not supported
        if(!disableGui && (Desktop.isDesktopSupported() || forceGui)) {
            try {
                createMainView();
            } catch (InvocationTargetException | InterruptedException e) {
                logger.fatal("Could not create main view", e);
                System.exit(1);
            }
        } else {
            // Check read/write permissions
            if(!checkReadWritePermissions()) {
                logger.error("=============================================================");
                logger.error("Brainwine has no read or write permissions in this directory.");
                logger.error("Please elevate the process or move it to another location.");
                logger.error("=============================================================");
                System.exit(1);
            }
            
            // Start the server (duh)
            startServer();
            
            // Start console listener thread
            ConsoleThread consoleThread = new ConsoleThread();
            consoleThread.addListener(input -> {
                executeCommand(input);
            });
            consoleThread.start();
        }
    }
    
    public void executeCommand(String commandLine) {
        if(isServerRunning()) {
            serverThread.executeCommand(commandLine);
        }
    }
    
    public void closeApplication() {
        if(!closeRequested) {
            closeRequested = true;
            
            if(isServerRunning()) {
                serverThread.stopGracefully();
            } else {
                System.exit(0);
            }
        }
    }
    
    private void createMainView() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            // Preamble
            ThemeManager.init();
            UIManager.put("Brainwine.playIcon", new ImageIcon(getClass().getResource("/playIcon16x.png")));
            UIManager.put("Brainwine.serverIcon", new ImageIcon(getClass().getResource("/serverIcon16x.png")));
            UIManager.put("Brainwine.settingsIcon", new ImageIcon(getClass().getResource("/settingsIcon16x.png")));
            UIManager.put("Brainwine.communityIcon", new ImageIcon(getClass().getResource("/communityIcon16x.png")));
            UIManager.put("Brainwine.powerIcon", new ImageIcon(getClass().getResource("/powerIcon16x.png")));
            UIManager.put("Brainwine.consoleFont", new Font(OperatingSystem.isMacOS() ? "Andale Mono" : "Consolas", Font.PLAIN, 12));
            UIManager.put("Spinner.editorAlignment", JTextField.LEFT);
            UIManager.put("TitlePane.unifiedBackground", false);
            UIManager.put("Button.foreground", UIManager.get("MenuBar.foreground"));
            SwingUtils.setDefaultFontSize(Math.min(28, Math.max(10, GuiPreferences.getFontSize())));
            
            // Check read/write permissions
            if(!checkReadWritePermissions()) {
                JOptionPane.showMessageDialog(null, "I have no read or write permissions at this location.\n"
                        + "Please move me somewhere else!", "Attention", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            
            // Create view
            new MainView(this);
        });
    }
    
    private boolean checkReadWritePermissions() {
        Path path = Paths.get("");
        return Files.isReadable(path) && Files.isWritable(path);
    }
    
    public void toggleServer() {
        if(isServerRunning()) {
            stopServer();
        } else {
            startServer();
        }
    }
    
    public void startServer() {
        if(!isServerRunning()) {
            listeners.forEach(ServerStatusListener::onServerStarting);
            serverThread = new ServerThread(this);
            serverThread.start();
        }
    }
    
    public void stopServer() {
        if(isServerRunning()) {
            listeners.forEach(ServerStatusListener::onServerStopping);
            serverThread.stopGracefully();
        }
    }
    
    public void onServerStarted() {
        listeners.forEach(ServerStatusListener::onServerStarted);
    }
    
    public void onServerStopped() {
        listeners.forEach(ServerStatusListener::onServerStopped);
        
        if(closeRequested) {
            System.exit(0);
        }
    }
    
    public boolean isServerRunning() {
        return serverThread != null && serverThread.isRunning();
    }
    
    public void addServerStatusListener(ServerStatusListener listener) {
        listeners.add(listener);
    }
}
