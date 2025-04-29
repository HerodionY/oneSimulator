package routing;

/* * This implementation is based on the paper:
 * "An Efficient Routing Protocol Using the History of Delivery Predictability in Opportunistic Networks"
 * by Eun Hak Lee, Dong Yeong Seo, and Yun Won Chung.
 * 
 * 
 * * The protocol improves upon the traditional PRoPHET routing algorithm by incorporating * the historical delivery predictability (preP) of the sending node into the message forwarding decision. 
 *  * The implementation is designed for use in the ONE Simulator for Delay Tolerant Networks.
 */
//
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;


/**
 * ProphetProposed routing protocol implementation.
 * 
 * This is an enhanced version of the original PRoPHET protocol that 
 * incorporates a previous delivery predictability (preP) mechanism 
 * to improve message forwarding efficiency in Delay Tolerant Networks (DTNs).
 * 
 * The forwarding decision is made based on two factors: 
 * the current delivery predictability and the historical delivery 
 * performance of the sender node (preP). A message is forwarded 
 * from node A to node B for destination D only if B's current 
 * delivery predictability to D is greater than both A’s current 
 * predictability and A’s preP value for D.
 * 
 * This strategy minimizes unnecessary transmissions and buffer overflows 
 * by avoiding repeated forwarding to nodes with only marginal improvements, 
 * leading to higher delivery ratios and reduced overhead.
 * 
 * The protocol was inspired by and builds on the original PRoPHET algorithm:
 * "Probabilistic Routing in Intermittently Connected Networks" 
 * by Anders Lindgren, Avri Doria, and Olov Schelén.
 * 
 * 
 */

/**
 * ProphetProposed routing protocol implementation.
 * 
 * This implementation is based on the paper:
 * "An Efficient Routing Protocol Using the History of Delivery Predictability in Opportunistic Networks"
 * by Eun Hak Lee, Dong Yeong Seo, and Yun Won Chung.
 * 
 * The protocol improves upon the traditional PRoPHET routing algorithm by incorporating
 * the historical delivery predictability (preP) of the sending node into the message forwarding decision.
 * 
 * A message from node A to destination D is forwarded to node B only if:
 * - B does not already have the message,
 * - B is the destination, or
 * - B's current delivery predictability to D is greater than A's current value,
 *   and either A has no historical (preP) value yet or B's current value is also greater than A's preP.
 * 
 * This design reduces unnecessary message replication and buffer overflow,
 * leading to improved delivery ratio, reduced overhead, and lower latency.
 * 
 * The implementation is designed for use in the ONE Simulator for Delay Tolerant Networks.
 * 
 * @author Herodion Yulis Putra Anugrah
 */


public class ProphetProposed extends ActiveRouter{
    /** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHET_NS = "ProphetProposed";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of 
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
	
	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;

    private Map<DTNHost, Double> predPMap;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public ProphetProposed(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}

		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetProposed(ProphetProposed r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		initPreds();
	}
	
	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
        this.predPMap = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}
	
	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}
	
	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}

    /**
     * 
     */
    public double getPredPFor(DTNHost host) {
        if (predPMap.containsKey(host)) {
            return predPMap.get(host);
        }
        else {
            return 0;
        }
        
    }
	
	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetProposed : "PRoPHET only works " + 
			" with other routers of same type";
		
		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 
			((ProphetProposed)otherRouter).getDeliveryPreds();
		
		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}
			
			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / 
			secondsInTimeUnit;
		
		if (timeDiff == 0) {
			return;
		}
		
		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}
		
		this.lastAgeUpdate = SimClock.getTime();
	}
	
	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		tryOtherMessages();		
	}
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
     * When A meets B:
        • If B already has message ➔ Skip
        • If B is Destination ➔ Deliver
        • Else:
           • If P(B, D) > P(A, D):
              • If preP(A, D) exists:
                • If P(B, D) > preP(A, D) ➔ Forward
                • Else ➔ Don't forward
              • Else ➔ Forward
           • Else ➔ Don't forward
        
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetProposed othRouter = (ProphetProposed)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}

                if(other.equals(m.getTo())) {
                    // the other one is the final recipient
                    messages.add(new Tuple<Message, Connection>(m,con));
                    continue; // skip messages that the other one has
                }

        tryAllMessagesToAllConnections();

				// if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
				// 	// the other node has higher probability of delivery
				// 	messages.add(new Tuple<Message, Connection>(m,con));
                
				// }

                double pSender = getPredFor(m.getTo());
                double pReceiver = othRouter.getPredFor(m.getTo());
                Double prePSender = getPredPFor(m.getTo()); // you must implement getPrePFor

                if(pSender > pReceiver) {
                    if(prePSender != null) {
                        if(pReceiver > prePSender) {
                            messages.add(new Tuple<Message, Connection>(m,con));
                        }
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
                

                
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// sort the message-connection tuples
		Collections.sort(messages, new TupleComparator());
		return tryMessagesForConnected(messages);	// try to send messages
	}
	
	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the 
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator 
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
            /** tuple1 has higher probability of delivery */
			double p1 = ((ProphetProposed)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetProposed)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide 
                means that tuple1 should come before tuple2*/
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
                /** retuns -1 means that tuple2 should come before tuple1 */
				return -1;
			}
			else {
                /** retuns 1 means that tuple1 should come before tuple2 */
				return 1;
			}
		}
	}
	
	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + 
				" delivery prediction(s)");
		
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();
			
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					host, value)));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		ProphetProposed r = new ProphetProposed(this);
		return r;
	}

}
