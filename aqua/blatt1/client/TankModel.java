package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.broker.SnapToken;
import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

import static aqua.blatt1.client.TankModel.Mode.IDLE;


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
	protected Timer timer;
	protected enum Mode {IDLE, LEFT, RIGHT, BOTH}
	protected Mode recmode = IDLE;
	protected int fishCount = 0;


	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
		this.timer = new Timer();
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
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

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);
		fishCount++;
	}

	synchronized void receiveToken() throws InterruptedException {
		hasToken = true;
		TimerTask sendToken = new TimerTask() {
			public void run() {
				hasToken = false;
				forwarder.forwardToken(TankModel.this); //?
			}
		};
		timer.schedule(sendToken, 2000);
	}

	synchronized Boolean hasToken(){
		return hasToken;
	}

	synchronized void updateNeighbors(InetSocketAddress left, InetSocketAddress right) {
		this.left = left;
		this.right = right;
	}

	synchronized void initiateSnapshot() {
		this.recmode = IDLE;
		forwarder.forwardSnapToken(left, new SnapToken(fishCount));
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
				} else {
					fish.reverse();
				}


			if (fish.disappears())
				it.remove();
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