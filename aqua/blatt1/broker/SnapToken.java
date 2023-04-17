package aqua.blatt1.broker;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class SnapToken implements Serializable {
    private final int sum;
    public SnapToken(int sum) {
        this.sum = sum;
    }
    public int getSnapToken() {
        return sum;
    }
}
