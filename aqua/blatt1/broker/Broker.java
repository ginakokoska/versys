package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.awt.*;

import javax.swing.*;
import java.util.concurrent.*;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {
	static Endpoint end = new Endpoint(4711);
	private ExecutorService pool;
	static boolean stopFlag = false;

	static ClientCollection<InetSocketAddress> cc = new ClientCollection<InetSocketAddress>();
	public void broker() {

		pool = Executors.newCachedThreadPool();
		while (!stopFlag) {
			System.out.println(stopFlag);
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
		private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		private final Lock writeLock = readWriteLock.writeLock();
		private final Lock readLock = readWriteLock.readLock();
		public BrokerTask(Message msg) {
			this.msg = msg;
		}



		public void register(InetSocketAddress cli) {
			InetSocketAddress left_left;
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
			int idx = cc.indexOf(cli);
			InetSocketAddress left = cc.getLeftNeighorOf(idx);
			InetSocketAddress right = cc.getRightNeighorOf(idx);
			NeighborUpdate n = new NeighborUpdate(left, right);
			NeighborUpdate left_n = new NeighborUpdate(left_left, cli);
			NeighborUpdate right_n = new NeighborUpdate(cli, right_right);
			if (left != right) {
				//end.send(cli, n);
				end.send(left, left_n);
				end.send(right, right_n);
			} else {
				end.send(left, left_n); // wenn n = 2
			}
			end.send(cli, new RegisterResponse(id));
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

				if (msg.getPayload() instanceof PoisonPill) {
					writeLock.lock();
					stopFlag = true;
					writeLock.unlock();
				}
		}
	}

}
