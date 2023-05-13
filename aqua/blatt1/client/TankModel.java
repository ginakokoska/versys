package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import aqua.blatt1.broker.SnapToken;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

import static aqua.blatt1.client.TankModel.Mode.*;


public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected InetSocketAddress left;
	protected InetSocketAddress right;
	protected Boolean hasToken = false;
	protected Boolean hasSnapToken = false;
	protected Timer token_timer;
	protected Timer lease_timer;
	protected enum Mode {IDLE, LEFT, RIGHT, BOTH}
	protected Mode recmode = IDLE;
	protected int totalFishies = 0;
	protected int shCount = 0;
	protected boolean isSnapshotDone = false;


	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id, int lease_dur, boolean re) {
		this.id = id;
		if (!re) newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
		TimerTask lease = new TimerTask() {
			public void run() {
				forwarder.register();
				System.out.println("ReRegistered.");
			}
		};
		lease_timer = new Timer(true);
		lease_timer.schedule(lease, lease_dur);
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish, InetSocketAddress sender) {
		fish.setToStart();
		fishies.add(fish);
		// only update fishcount wenn marker noch nicht gesetzt ist
		if (this.recmode == BOTH) {
			totalFishies++;
		} else if (this.recmode == LEFT) {
			if (sender.equals(left)) {
				totalFishies++;
			}
		} else if (this.recmode == RIGHT) {
			if (sender.equals(right)) {
				totalFishies++;
			}
		}
	}

	synchronized void receiveToken() throws InterruptedException {
		hasToken = true;
		TimerTask sendToken = new TimerTask() {
			public void run() {
				hasToken = false;
				forwarder.forwardToken(TankModel.this); //?
			}
		};
		token_timer = new Timer(true);
		token_timer.schedule(sendToken, 2000);
	}

	synchronized Boolean hasToken(){
		return hasToken;
	}

	synchronized void updateNeighbors(InetSocketAddress left, InetSocketAddress right) {
		if (left != null) this.left = left;
		if (right != null) this.right = right;
	}

	protected void initiateSnapshot() {
		// starte aufzeichnungsmodus für alle Eingangskanäle
		this.recmode = BOTH;
		totalFishies = this.fishies.size()-shCount;
		hasSnapToken = true;
		forwarder.forwardSnapToken(left, new SnapToken(totalFishies, id));
		System.out.println("initiated token with count: " + totalFishies);

		// sende Markierungen an alle Ausgangskanäle
		if (this.left != this.right) {
			this.forwarder.sendMarker(this.left);
			this.forwarder.sendMarker(this.right);
		} else {
			this.forwarder.sendMarker(this.left);
		}

	}

	synchronized void nextSnapshot(int count, String tokenID) {
		if (this.recmode != IDLE) {
			totalFishies = this.fishies.size()-shCount;
		}

		int newCount = totalFishies + count;
		forwarder.forwardSnapToken(left, new SnapToken(newCount, tokenID));
	}

	synchronized void endRecord(InetSocketAddress sender) {
		if (this.recmode == BOTH) {
			if (sender.equals(left)) {
				this.recmode = RIGHT;
			} else if (sender.equals(right)) {
				this.recmode = LEFT;
			}
		} else if (this.recmode == LEFT) {
			if (sender.equals(left)) {
				this.recmode = IDLE;
			}
		} else if (this.recmode == RIGHT) {
			if (sender.equals(right)) {
				this.recmode = IDLE;
			}
		}
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge())
				if (hasToken) {
					forwarder.handOff(fish, this);
					++shCount;
				} else {
					fish.reverse();
				}


			if (fish.disappears()) {
				--shCount;
				it.remove();
			}
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}


}