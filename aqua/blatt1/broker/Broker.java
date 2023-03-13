package aqua.blatt1.broker;

import aqua.blatt1.client.ClientCommunicator;
import aqua.blatt1.client.TankModel;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

public class Broker {
	Endpoint end = new Endpoint(4711);

	ClientCollection<InetSocketAddress> cc = new ClientCollection<InetSocketAddress>();
	public void broker() {
		while(true) {
			Message msg = end.blockingReceive();

			if (msg.getPayload() instanceof RegisterResponse)
				register(msg.getSender());

			if (msg.getPayload() instanceof DeregisterRequest)
				deregister(msg.getSender());

			if (msg.getPayload() instanceof HandoffRequest) {
				handoffFish(msg.getSender(), new HandoffRequest(((HandoffRequest)msg.getPayload()).getFish()));
			}

		}
	}

	// aufrufen bei RegisterRequest
	public void register(InetSocketAddress cli) {
		String id = "tank" + (cc.size()+1);
		cc.add(id, cli);
		end.send(cli, new RegisterResponse(id));
	}

	// aufrufen bei DeregisterRequest
	public void deregister(InetSocketAddress cli) {
		int idx = cc.indexOf(cli);
		cc.remove(idx);
	}

	// aufrufen bei HandoffRequest
	public void handoffFish(InetSocketAddress cli, HandoffRequest h) {
		int idx = cc.indexOf(cli);
		int next_idx = (idx%cc.size())+1;
		InetSocketAddress next = cc.getClient(next_idx);
		end.send(next, h);
	}

	public static void main(String[] args) {
		Broker b = new Broker();
		b.broker();
	}

}
