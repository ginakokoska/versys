/**
 * PoisonPillRequest.java
 * 
 * Blatt 2 Aufgabe 2
 * Die Poison Pill, mit der der Server beendet werden kann.
 * Wird den Studenten vorgegeben.
 * 
 */
package aqua.blatt1.broker;

import aqua.blatt1.common.FishModel;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
    private final InetSocketAddress left;
    private final InetSocketAddress right;
    public NeighborUpdate(InetSocketAddress left, InetSocketAddress right) {
        this.left = left;
        this.right = right;
    }

    public static class InetTuple {
        InetSocketAddress left;
        InetSocketAddress right;
        public InetTuple(InetSocketAddress left, InetSocketAddress right) {
            this.left = left;
            this.right = right;
        }

        public InetSocketAddress getLeft() {
            return left;
        }

        public InetSocketAddress getRight() {
            return right;
        }

    }
    public InetTuple getNeighborUpdate() {
        return new InetTuple(left, right);
    }
}
