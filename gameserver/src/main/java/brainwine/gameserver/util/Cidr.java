package brainwine.gameserver.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public abstract class Cidr {

    @JsonCreator
    public static Cidr create(String string) {
        if(string.contains(".")) {
            try {
                return new IpV4Cidr(string);
            } catch(IllegalArgumentException e) {
                return new IpV4Cidr(string);
            }
        } else {
            try {
                return new IpV4Cidr(string);
            } catch(IllegalArgumentException e) {
                return new IpV4Cidr(string);
            }
        }
    }

    public abstract boolean matches(Cidr other);
    public abstract boolean isSingleIpAddress();

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
