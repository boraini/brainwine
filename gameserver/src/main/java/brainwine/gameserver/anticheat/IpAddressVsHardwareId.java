package brainwine.gameserver.anticheat;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.Cidr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpAddressVsHardwareId {
    public static final int NUM_ALLOWED_CIDR_VIOLATIONS = 3;
    public static final int NUM_ALLOWED_HWID_VIOLATIONS = 5;
    public static final int NUM_ALLOWED_PLAYER_VIOLATIONS = 3;
    public static final long VIOLATIONS_WINDOW = 60 * 60 * 1000;
    public static final long MIN_VIOLATIONS_INTERVAL = 24 * 60 * 60 * 1000;
    public static final String violationActionKey = "ipAddressVsHardwareIdViolation";

    private static final Logger logger = LogManager.getLogger();

    public static Map<String, ArrayList<Violation>> playerToViolations = new HashMap<>();
    public static Map<Cidr, ArrayList<Violation>> cidrToViolations = new HashMap<>();
    public static Map<String, ArrayList<Violation>> hwIdToViolations = new HashMap<>();

    public void handleConnection(Player player) {
        if(player.getConnection() == null) return;

        Cidr cidr = player.getConnection().getIpAddress();
        String hwId = player.getHardwareUid();

        trackNewConnectionInfo(playerToViolations.computeIfAbsent(player.getDocumentId(), (String key) -> new ArrayList<Violation>()), NUM_ALLOWED_PLAYER_VIOLATIONS, player, cidr, hwId);
        trackNewConnectionInfo(cidrToViolations.computeIfAbsent(cidr, (Cidr key) -> new ArrayList<Violation>()), NUM_ALLOWED_CIDR_VIOLATIONS, player, cidr, hwId);
        trackNewConnectionInfo(hwIdToViolations.computeIfAbsent(hwId, (String key) -> new ArrayList<Violation>()), NUM_ALLOWED_HWID_VIOLATIONS, player, cidr, hwId);
    }

    private void trackNewConnectionInfo(List<Violation> existingViolations, int allowed, Player player, Cidr ipAddress, String hardwareId) {
        if(existingViolations.isEmpty()) {
            existingViolations.add(new Violation(OffsetDateTime.now(), player.getDocumentId(), ipAddress, hardwareId));
            return;
        }

        Violation latestViolation = existingViolations.get(existingViolations.size() - 1);

        if(latestViolation.getPlayerId().equals(player.getDocumentId()) && latestViolation.getHardwareId().equals(hardwareId) && latestViolation.getIpAddress().matches(ipAddress)) {
            existingViolations.set(existingViolations.size() - 1, new Violation(OffsetDateTime.now(), player.getDocumentId(), ipAddress, hardwareId));
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        existingViolations.add(new Violation(OffsetDateTime.now(), player.getDocumentId(), ipAddress, hardwareId));

        while(!existingViolations.isEmpty() && ChronoUnit.MILLIS.between(existingViolations.get(0).getTime(), now) >= VIOLATIONS_WINDOW) {
            existingViolations.remove(0);
        }

        if(existingViolations.size() > allowed) {
            player.recordActionTime(violationActionKey);
            logger.info("Player " + player.getName() + " <" + player.getDocumentId() + "> has a ip address-cookie match violation with ip address " + player.getConnection().getIpAddress() + " and cookie " + player.getHardwareUid() + ".");
        }

        while(existingViolations.size() > allowed) {
            existingViolations.remove(0);
        }
    }

    public static class Violation {
        private final OffsetDateTime time;
        private final String playerId;
        private final Cidr ipAddress;
        private final String hardwareId;

        public Violation(OffsetDateTime time, String playerId, Cidr ipAddress, String hardwareId) {
            this.time = time;
            this.playerId = playerId;
            this.ipAddress = ipAddress;
            this.hardwareId = hardwareId;
        }

        public OffsetDateTime getTime() {
            return time;
        }

        public String getPlayerId() {
            return playerId;
        }

        public Cidr getIpAddress() {
            return ipAddress;
        }

        public String getHardwareId() {
            return hardwareId;
        }
    }
}
