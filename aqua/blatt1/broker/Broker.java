package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JOptionPane;
import java.util.concurrent.*;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class Broker {
	static Endpoint end = new Endpoint(4711);
	private ExecutorService pool;
	boolean stopFlag = false;

	static ClientCollection<InetSocketAddress> cc = new ClientCollection<InetSocketAddress>();
	public void broker() {

		pool = Executors.newCachedThreadPool();
		while (!stopFlag) {
			Message msg = end.blockingReceive();
			BrokerTask t = new BrokerTask(msg);
			// for (int i = 0; i < poolSize; i++) pool[i] =
			//var tasks = IntStream.rangeClosed(1, 1_000_000).mapToObj(i -> t).toList();
			pool.execute(t);

		}
	}


	public static void main(String[] args) {
		EingabeMaske m = new EingabeMaske();
		Broker b = new Broker();
		m.run();
		b.broker();
	}

	public static class EingabeMaske extends Thread {
		JFrame frame = new JFrame("Thread beendet");
		JOptionPane


	}
	public static class BrokerTask implements Runnable {
		Message msg;
		private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		private final Lock writeLock = readWriteLock.writeLock();
		private final Lock readLock = readWriteLock.readLock();
		public BrokerTask(Message msg) {
			this.msg = msg;
		}



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
			int next_idx = (idx+1)%cc.size();
			InetSocketAddress next = cc.getClient(next_idx);
			end.send(next, h);
		}



		@Override
		public void run() {

			System.out.println(msg);
				if (msg.getPayload() instanceof RegisterRequest) {
					writeLock.lock();
					register(msg.getSender());
					writeLock.unlock();
				}

				if (msg.getPayload() instanceof DeregisterRequest) {
					writeLock.lock();
					deregister(msg.getSender());
					writeLock.unlock();
				}

				if (msg.getPayload() instanceof HandoffRequest) {
					readLock.lock();
					handoffFish(msg.getSender(), new HandoffRequest(((HandoffRequest)msg.getPayload()).getFish()));
					readLock.unlock();
				}


		}
	}

}
