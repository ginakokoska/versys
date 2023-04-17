package aqua.blatt1.broker;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class SnapToken implements Serializable {
    private final int sum;
    private final String id;
    public SnapToken(int sum, String id) {
        this.sum = sum;
        this.id = id;
    }
    public String getID() {return id;}
    public int getCount() {
        return sum;
    }

}
