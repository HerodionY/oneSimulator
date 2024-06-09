package routing;

import java.util.*;
import core.*;
import reinforcement.*;

// public class CCRouting extends ActiveRouter {
public class CCRouting extends QLearningRouter {
	// implements CongestionRate {
	private int msgReceived = 0;
	private int msgTransferred = 0;

	/** update interval diset dari settings */
	private double updateInterval;

	// Variable untuk Congestion Ratio
	private int dataReceived = 0;
	private int dataTransferred = 0;
	private double lastUpdateTime = 0;
	private double totalContactTime = 0;
	private List<Double> dataContact;
	private List<Double> listOfSumDataContact;
	private static final double SMOOTHING_FACTOR = 0.20;
	private double cr = 0.0;
	private double ema = 0.0;

	// Variable untuk learning
	private QLearning ql;
	private IExplorationPolicy explorationPolicy;
	private int totalState;
	private int totalAction;
	/** untuk menghitung discount factor */
	private Map<DTNHost, Double> totalRewardWithNode;
	private Map<DTNHost, Integer> visitCount;

	/** 
	 * Integer sebagai address node, 
	 * jika status masih <CODE>pending</CODE> 
	 * atau <CODE>true</CODE> maka tidak dikirim
	 * */
	private Map<DTNHost, Boolean> waitForReward;

	/**
	 * Candidate receivers
	 */
	private List<Connection> candidateReceiver;

	/** namespace settings ({@value}) */
	private static final String CCROUTING_NS = "CCRouting";
	/** nilai update interval atur di settings */
	private static final String UPDATE_INTERVAL = "updateInterval";
	/**
	 * Karena node dianggap sebagai state,
	 * maka state diinisalisasi sejumlah dengan total node
	 */
	private static final String TOTAL_STATE = "totalState";
	private static final String TOTAL_ACTION = "totalAction";

	/**
	 * Constructor
	 * 
	 * @param s
	 */
	public CCRouting(Settings s) {
		super(s);
		Settings ccSettings = new Settings(CCROUTING_NS);
		updateInterval = ccSettings.getInt(UPDATE_INTERVAL);
		totalState = ccSettings.getInt(TOTAL_STATE);
		totalAction = ccSettings.getInt(TOTAL_ACTION);

		waitForReward = new HashMap<>();
		candidateReceiver = new ArrayList<>();
		dataContact = new ArrayList<>();
		listOfSumDataContact = new ArrayList<>();
		initQL();
	}

	/**
	 * Copy constructor
	 * 
	 * @param r
	 */
	protected CCRouting(CCRouting r) {
		super(r);
		updateInterval = r.updateInterval;
		totalState = r.totalState;
		totalAction = r.totalAction;

		waitForReward = new HashMap<>();
		candidateReceiver = new ArrayList<>();
		dataContact = new ArrayList<>();
		listOfSumDataContact = new ArrayList<>();
		initQL();
	}

	protected void initQL() {
		this.explorationPolicy = new EpsilonGreedyExploration(0.989);

		this.ql = new QLearning(totalState, totalAction, this.explorationPolicy, false);

		this.totalRewardWithNode = new HashMap<>();
		this.visitCount = new HashMap<>();
	}

	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);
		
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		
		if (con.isUp()) {

			if(!this.waitForReward.containsKey(otherNode)) {
				this.waitForReward.put(otherNode, false);
			}

			if(!this.waitForReward.get(otherNode).booleanValue()) {
				this.candidateReceiver.add(con);	
			} 

		} else {
			this.totalContactTime += SimClock.getTime();
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		this.msgReceived++;
		this.dataReceived += m.getSize();

		return m;
	}

	@Override
	protected void transferDone(Connection con) {
		this.msgTransferred++;
		this.dataTransferred += con.getMessage().getSize();
	}

	@Override
	public void update() {
		super.update();

		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		tryOtherMessage();

		if ((SimClock.getTime() - lastUpdateTime) >= updateInterval) {

			lastUpdateTime = SimClock.getTime();

			// reset data receive & transmit dalam interval tertentu
			this.dataReceived = 0;
			this.dataTransferred = 0;

			this.msgReceived = 0;
			this.msgTransferred = 0;

			// ubah status pending menjadi available (wait for reward => false)
			for(Connection con : this.candidateReceiver) {
				DTNHost other = con.getOtherNode(getHost());
				CCRouting othRouter = (CCRouting) other.getRouter();

				int addressOther = other.getAddress();
				this.waitForReward.put(other, false);

				othRouter.countCongestionRatio(); // hitung CR
				othRouter.countEma(othRouter.cr); // hitung EMA
				double reward = 1 / othRouter.ema;

				int totalVisit = visitCount.get(other) != null
					? visitCount.get(other) + 1
					: 1;

				double totalRewardForDiscFac = totalRewardWithNode.get(other) != null
					? totalRewardWithNode.get(other) + reward
					: reward;

				this.visitCount.put(other, totalVisit);
				this.totalRewardWithNode.put(other, totalRewardForDiscFac);																		
				
				// Q-Learning
				int action = this.ql.GetAction(addressOther);
				this.ql.setLearningRate(totalVisit);
				this.ql.setDiscountFactor(totalRewardForDiscFac);
				this.ql.UpdateState(addressOther, action, reward, action);
			}

			this.candidateReceiver.clear();
		}
	}

	private Tuple<Message, Connection>  tryOtherMessage() {
		List<Tuple<Message, Connection>> messages = new ArrayList<>();
		List<Tuple<Message, Connection>> tempMessages = new ArrayList<>();

		Collection<Message> msgCollection = getMessageCollection();

		// order q-value paling tinggi
		Collections.sort(this.candidateReceiver, new ConnectionComparator());
		
		/**
		 * collect message terhadap node yg memiliki
		 * status available / tidak pending / waitForReward == false
		 */
		for (Connection con : this.candidateReceiver) {
			DTNHost other = con.getOtherNode(getHost());
			CCRouting othRouter = (CCRouting) other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}

				// pesan yg memiliki interest sama dari node dimasukan ke list
				if(isSameInterest(m, other)) {
					tempMessages.add(new Tuple<>(m,con));
				}
			}

			// order by interest similarity secara desc
			Collections.sort(tempMessages, new InteresetSimilarityComparator());

			messages.addAll(tempMessages);
			tempMessages.clear();

			this.waitForReward.put(other, true);
		}

		if (messages.isEmpty()) {
			return null;
		}

		return tryMessagesForConnected(messages);
	}

	/**
	 * Comparator untuk sorting Connection berdasarkan DTNHost
	 * yang memiliki Q-value tertinggi
	 * Sort DESC
	 */
	private class ConnectionComparator implements Comparator<Connection> {
		public int compare(Connection con1, Connection con2) {
			DTNHost h1 = con1.getOtherNode(getHost());
			DTNHost h2 = con2.getOtherNode(getHost());

			double qv1 = getQl().getQV(h1.getAddress(), h1.getAddress());
			double qv2 = getQl().getQV(h2.getAddress(), h2.getAddress());

			// Q-value lebih besar
			return Double.compare(qv2, qv1);
		}
	}

	/**
	 * Comparator untuk sorting message berdasarkan
	 * Interest Similarity tertinggi
	 * Sort DESC
	 */
	private class InteresetSimilarityComparator implements Comparator<Tuple<Message, Connection>> {
		public int compare(Tuple<Message, Connection> tuple1, Tuple<Message, Connection> tuple2) {
			double d1 = sumList(countInterestSimilarity(tuple1.getKey(), 
													tuple1.getValue().getOtherNode(getHost())));

			double d2 = sumList(countInterestSimilarity(tuple2.getKey(), 
													tuple2.getValue().getOtherNode(getHost())));

			return Double.compare(d2, d1);
		}
	}

	@Override
	public CCRouting replicate() {
		return new CCRouting(this);
	}

	public int getTotalDataRcv() {
		return this.dataReceived;
	}

	public int getTotalDataTrf() {
		return this.dataTransferred;
	}

	public int getMsgReceived() {
		return this.msgReceived;
	}

	public int getMsgTransferred() {
		return this.msgTransferred;
	}

	public double getCr() {
		return this.cr;
	}

	public double getEma() {
		return this.ema;
	}

	public void countCongestionRatio() {
		// double dataEachContact = (this.dataReceived + this.dataTransferred) / totalContactTime;
		double dataEachContact = (this.msgReceived + this.msgTransferred) / totalContactTime;

		this.dataContact.add(dataEachContact);

		double summedData = sumList(this.dataContact);

		this.listOfSumDataContact.add(summedData);

		this.cr = avgList(this.listOfSumDataContact);
	}

	public void countEma(double oLast) {
		double emaPrev = this.ema;
		double tempEma = oLast * SMOOTHING_FACTOR + emaPrev * (1 - SMOOTHING_FACTOR);

		this.ema = tempEma;
	}

	private double sumList(List<Double> lists) {
		double total = 0.0;

		for(double lst: lists) {
			total += lst;
		}

		return total;
	}

	private double avgList(List<Double> lists) {
		if (lists.isEmpty()) {
			return 0;
		}

		double value = 0;

		for (double i : lists) {
			value += i;
		}

		return value / lists.size();
	}

	public QLearning getQl() {
		return this.ql;
	}
	
}
