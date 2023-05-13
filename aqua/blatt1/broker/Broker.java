package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.awt.*;

import javax.sound.midi.Soundbank;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
	static Endpoint end = new Endpoint(4711);
	private ExecutorService pool;
	static boolean stopFlag = false;
	private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private static final Lock writeLock = readWriteLock.writeLock();
	private static final Lock readLock = readWriteLock.readLock();
	static int lease_dur = 5000;

	static ClientCollection<InetSocketAddress> cc = new ClientCollection<InetSocketAddress>();
	public void broker() {

		pool = Executors.newCachedThreadPool();
		runLeaseCleaner();
		while (!stopFlag) {
			Message msg = end.blockingReceive();
			BrokerTask t = new BrokerTask(msg);
			// for (int i = 0; i < poolSize; i++) pool[i] =
			//var tasks = IntStream.rangeClosed(1, 1_000_000).mapToObj(i -> t).toList();
			pool.execute(t);

		}
	}


	public static void main(String[] args) {
		//EingabeMaske m = new EingabeMaske();
		//Poisoner p = new Poisoner();
		Broker b = new Broker();
		//m.start();
		b.broker();
	}

	public static class EingabeMaske extends Thread {


		@Override
		public void run() {
			javax.swing.JOptionPane.showMessageDialog(null, "Broker beenden?");
			Poisoner p = new Poisoner();

		}
	}

	public static class BrokerTask implements Runnable {
		Message msg;

		public BrokerTask(Message msg) {
			this.msg = msg;
		}


		public void register(InetSocketAddress cli) {
			System.out.println(cli + " tried register in Broker");
			writeLock.lock();
			int idx = cc.indexOf(cli);
			System.out.println("idx: " + idx);
			System.out.println("size: " + cc.size());
			System.out.println("cc: " + cc);
			if (idx != -1) {
				System.out.println("New Timestamp update");
				cc.updateTimestamp(idx);
				String id = cc.getID(idx);
				end.send(cli, new RegisterResponse(lease_dur, id, true));
				writeLock.unlock();
				return;
			}

			String id = "tank" + (cc.size() + 1);
			cc.add(id, cli);
			System.out.println("Added id: " + id);
			System.out.println("size after add: " + cc.size());
			if (cc.size() == 1) {
				end.send(cli, new NeighborUpdate(cli, cli));
				end.send(cli, new Token());
			} else {
				InetSocketAddress left = cc.getLeftNeighorOf(idx);
				InetSocketAddress right = cc.getRightNeighorOf(idx);
				end.send(cli, new NeighborUpdate(left, right));
				end.send(left, new NeighborUpdate(null, cli));
				end.send(right, new NeighborUpdate(cli, null));
			}
			System.out.println("New Registration");
			end.send(cli, new RegisterResponse(lease_dur, id, false));
			writeLock.unlock();

			/*InetSocketAddress left_left;
			InetSocketAddress right_right;
			if (cc.size() == 0) {
				left_left = cli;
				right_right = cli;
			} else {
				left_left = cc.getLeftNeighorOf(cc.size()-1);
				right_right = cc.getRightNeighorOf(0);

			}

			if (cc.size() == 0) {
				end.send(cli, new Token());
			}
			String id = "tank" + (cc.size()+1);
			cc.add(id, cli);

			InetSocketAddress left = cc.getLeftNeighorOf(idx);
			InetSocketAddress right = cc.getRightNeighorOf(idx);
			NeighborUpdate n = new NeighborUpdate(left, right);
			NeighborUpdate left_n = new NeighborUpdate(left_left, cli);
			NeighborUpdate right_n = new NeighborUpdate(cli, right_right);
			if (cc.size() > 1) {
				end.send(cli, n);
				end.send(left, left_n);
				end.send(right, right_n);
			} else {
				end.send(cli, n);
				end.send(left, left_n); // wenn n = 2
			}
			end.send(cli, new RegisterResponse(lease_dur, id));
			*/


		}

		// aufrufen bei DeregisterRequest
		public void deregister(InetSocketAddress cli) {
			InetSocketAddress left_left;
			InetSocketAddress right_right;
			if (cc.size() == 0) {
				left_left = cli;
				right_right = cli;
			} else {
				left_left = cc.getLeftNeighorOf(cc.size()-1);
				right_right = cc.getRightNeighorOf(0);

			}
			int idx = cc.indexOf(cli);
			InetSocketAddress left = cc.getLeftNeighorOf(idx);
			InetSocketAddress right = cc.getRightNeighorOf(idx);
			NeighborUpdate n_left = new NeighborUpdate(left_left, right);
			NeighborUpdate n_right = new NeighborUpdate(left, right_right);
			if (left != right) {
				end.send(left, n_left);
				end.send(right, n_right);
			} else {
				end.send(left, n_left); // wenn n = 2
			}
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

			if (msg.getPayload() instanceof PoisonPill) {
				writeLock.lock();
				stopFlag = true;
				writeLock.unlock();
			}
		}
	}

	public static void runLeaseCleaner() {
		//System.out.println("Started lease cleanup");
		TimerTask task = new LeaseCleanup();
		Timer timer = new Timer(true);
		timer.schedule(task, 5000);
	}

	public static class LeaseCleanup extends TimerTask {
		@Override
		public void run() {
			//System.out.println("cleaning up...");
			for (int i = 0; i < cc.size(); i++) {
				writeLock.lock();
				Timestamp now = new Timestamp(System.currentTimeMillis());
				var x = now.getTime() - cc.getTimestamp(i).getTime();
				System.out.println("compare: " + x);
				if (now.getTime() - cc.getTimestamp(i).getTime() >= lease_dur) {
					InetSocketAddress leftNeighbor = cc.getLeftNeighorOf(i);
					InetSocketAddress rightNeighbor = cc.getRightNeighorOf(i);
					end.send(leftNeighbor, new NeighborUpdate(null, rightNeighbor));
					end.send(rightNeighbor, new NeighborUpdate(leftNeighbor, null));
					cc.remove(i);
					System.out.println("Deregistration of client at index: " + i + " due to lease expiry");
				}
				writeLock.unlock();
			}
			runLeaseCleaner();
		}
	}

}
