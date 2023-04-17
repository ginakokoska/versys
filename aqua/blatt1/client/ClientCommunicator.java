package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Objects;

import aqua.blatt1.broker.NeighborUpdate;
import aqua.blatt1.broker.SnapToken;
import aqua.blatt1.broker.SnapshotMarker;
import aqua.blatt1.broker.Token;
import aqua.blatt1.common.Direction;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {

			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, TankModel tank) {

			Direction dir = fish.getDirection();
			InetSocketAddress left = tank.left;
			InetSocketAddress right = tank.right;
			if (dir == Direction.LEFT) {
				endpoint.send(left, new HandoffRequest(fish));
			} else {
				endpoint.send(right, new HandoffRequest(fish));
			}
		}

		public void forwardToken(TankModel tank) {
			System.out.println("IP:"+this.broker+" tried to give away Token to "+tank.left+"(forwardTokenMethod)");
			endpoint.send(tank.left, new Token());
			System.out.println("after");
		}
		public void forwardSnapToken(InetSocketAddress a, SnapToken s) {endpoint.send(a, s);}
		public void sendMarker(InetSocketAddress next) {endpoint.send(next, new SnapshotMarker());
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();
				System.out.println(msg);
				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighborUpdate) {
					tankModel.updateNeighbors((((NeighborUpdate) msg.getPayload()).getNeighborUpdate()).getLeft(), (((NeighborUpdate) msg.getPayload()).getNeighborUpdate()).getRight());
				}
				if (msg.getPayload() instanceof SnapshotMarker) {
					InetSocketAddress sender = msg.getSender();
					tankModel.endRecord(sender);
				}
				if (msg.getPayload() instanceof SnapToken) {

					if (Objects.equals(((SnapToken) msg.getPayload()).getID(), tankModel.id)) {
						tankModel.isSnapshotDone = true;
					}
					else {
						int newCount = tankModel.totalFishies + ((SnapToken) msg.getPayload()).getCount();
						String tokenID = ((SnapToken) msg.getPayload()).getID();
						tankModel.nextSnapshot(newCount, tokenID);
					}
				}
			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
