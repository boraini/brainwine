package brainwine.gameserver.util;

import com.fasterxml.jackson.annotation.JsonCreator;

public abstract class Cidr {

    @JsonCreator
    public static Cidr create(String string) {
        return new IpV4Cidr(string);
    }

    public abstract boolean matches(Cidr other);

}
