package brainwine.gameserver.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public class IpV4Cidr extends Cidr {
    private final int ip;
    private final int maskBits;

    @JsonCreator
    public IpV4Cidr(String string) throws IllegalArgumentException {
        if(string == null || string.isEmpty()) {
            throw new IllegalArgumentException("Null or empty.");
        }

        String[] cidrParts = string.split("/");
        String[] ipParts = cidrParts[0].split("\\.");

        if(cidrParts.length != 1 && cidrParts.length != 2) {
            throw new IllegalArgumentException("Not 1 or 2 parts in the CIDR notation");
        }

        if(ipParts.length != 4) {
            throw new IllegalArgumentException("Not 4 parts for the IP address");
        }

        try {
            int a = Integer.parseInt(ipParts[0]);
            int b = Integer.parseInt(ipParts[1]);
            int c = Integer.parseInt(ipParts[2]);
            int d = Integer.parseInt(ipParts[3]);
            ip = a << 24 | b << 16 | c << 8 | d;
            if(cidrParts.length == 2) {
                maskBits = Integer.parseInt(cidrParts[1]);
            } else {
                maskBits = 32;
            }
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Malformed number component");
        }
    }

    @Override
    public boolean matches(Cidr obj) {
        if(obj instanceof IpV4Cidr) {
            IpV4Cidr other = (IpV4Cidr)obj;
            int mask = -1 << Math.min(this.maskBits, other.maskBits);
            return (this.ip & mask) == (other.ip & mask);
        }
        return false;
    }

    @Override
    public boolean isSingleIpAddress() {
        return maskBits == 32;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpV4Cidr && ((IpV4Cidr) other).ip == ip && ((IpV4Cidr)other).maskBits == maskBits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, maskBits);
    }

    @Override
    @JsonValue
    public String toString() {
        String result = ((ip >>> 24) & 255) + "." + ((ip >>> 16) & 255) + "." + ((ip >>> 8) & 255) + "." + (ip & 255);
        if(maskBits != 32) {
            result += "/" + maskBits;
        }
        return result;
    }
}
