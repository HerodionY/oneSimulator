package routing;

import java.util.*;
import core.*;
import routing.*;


public class SprayAndFocusDecisionEngine implements RoutingDecisionEngine {

	
	

	/** SprayAndFocus router's settings name space ({@value})*/ 
	public static final String SPRAYANDFOCUS_NS = "SprayAndFocusRouter";
	/** identifier for the initial number of copies setting ({@value})*/ 
	public static final String NROF_COPIES_S = "nrofCopies";
	/** identifier for the difference in timer values needed to forward on a message copy */
	public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
	/** Message property key for the remaining available copies of a message */
	public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";
	/** Message property key for summary vector messages exchanged between direct peers */
	public static final String SUMMARY_XCHG_PROP = "SprayAndFocus.protoXchg";
	
	protected static final String SUMMARY_XCHG_IDPREFIX = "summary";//"SprayAndFocusSummary";
	protected static final double defaultTransitivityThreshold = 60.0;//300.0;
	protected static final double DEFAULT_TIMEDIFF = 300;
	protected static int protocolMsgIdx = 0;
	
	protected int initialNrofCopies;
	protected double transitivityTimerThreshold;
	
	/** Stores information about nodes with which this host has come in contact */
	protected Map<DTNHost, Double> recentEncounters;
	
	public SprayAndFocusDecisionEngine(Settings s)
	{	// Implement the method logic here
		Settings snf = new Settings(SPRAYANDFOCUS_NS);
		initialNrofCopies = snf.getInt(NROF_COPIES_S);
		
		if(snf.contains(TIMER_THRESHOLD_S))
			transitivityTimerThreshold = snf.getDouble(TIMER_THRESHOLD_S);
		else
			transitivityTimerThreshold = defaultTransitivityThreshold;
		
		recentEncounters = new HashMap<DTNHost, Double>();
	}
	
	/**
	 * Copy Constructor.
	 * 
	 * @param r The router from which settings should be copied
	 */
	public SprayAndFocusDecisionEngine(SprayAndFocusDecisionEngine r)
	{	// Implement the method logic here
		this.initialNrofCopies = r.initialNrofCopies;//r.initialNrofCopies;
		this.transitivityTimerThreshold = r.transitivityTimerThreshold;//r.transitivityTimerThreshold;
		
		recentEncounters = new HashMap<DTNHost, Double>();//r.recentEncounters;
	}
	
	public RoutingDecisionEngine replicate()//replicate: membuat salinan dari objek yang ada
	{
		return new SprayAndFocusDecisionEngine(this);
	}
	

	public void connectionUp(DTNHost thisHost, DTNHost peer)//
	{
		
	}

	public void connectionDown(DTNHost thisHost, DTNHost peer)
	{
		
	}

	@Override
	public void update(DTNHost host) {
		// Implement the method logic here
	}

	/**
	 * Called whenever a connection goes up or comes down.
	 */
	// @Override
	// public void changedConnection(Connection con)
	// {
	// 	super.changedConnection(con);
		
	// 	/*
	// 	 * The paper for this router describes Message summary vectors 
	// 	 * (from the original Epidemic paper), which
	// 	 * are exchanged between hosts when a connection is established. This
	// 	 * functionality is already handled by the simulator in the protocol
	// 	 * implemented in startTransfer() and receiveMessage().
	// 	 * 
	// 	 * Below we need to implement sending the corresponding message.
	// 	 */
	// 	DTNHost thisHost = getHost();
	// 	DTNHost peer = con.getOtherNode(thisHost);
		
	// 	//do this when con is up and goes down (might have been up for awhile)
	// 	if(recentEncounters.containsKey(peer))
	// 	{ 
	// 		EncounterInfo info = recentEncounters.get(peer);
	// 		info.updateEncounterTime(SimClock.getTime());
	// 	}
	// 	else
	// 	{
	// 		recentEncounters.put(peer, new EncounterInfo(SimClock.getTime()));
	// 	}
		
	// 	if(!con.isUp())
	// 	{
	// 		neighborEncounters.remove(peer);
	// 		return;
	// 	}
		
	// 	/*
	// 	 * For this simulator, we just need a way to give the other node in this connection
	// 	 * access to the peers we recently encountered; so we duplicate the recentEncounters
	// 	 * Map and attach it to a message.
	// 	 */
	// 	int msgSize = recentEncounters.size() * 64 + getMessageCollection().size() * 8;
	// 	Message newMsg = new Message(thisHost, peer, SUMMARY_XCHG_IDPREFIX + protocolMsgIdx++, msgSize);
	// 	newMsg.addProperty(SUMMARY_XCHG_PROP, /*new HashMap<DTNHost, EncounterInfo>(*/recentEncounters);
		
	// 	createNewMessage(newMsg);
	// }

	public void doExchangeForNewConnection(Connection con, DTNHost peer)
	{	
		// Implement the method logic here
		SprayAndFocusDecisionEngine Sf = this.getOtherSnFDecisionEngine(peer);//getOtherSnFDecisionEngine(peer);
		DTNHost thisHost = con.getOtherNode(peer);
		double distTo = thisHost.getLocation().distance(peer.getLocation());
		double speed = peer.getPath() == null ? 0 : peer.getPath().getSpeed(),peerSpeed = peer.getPath() == null ? 0 : peer.getPath().getSpeed(),
			myTimediff, peerTimediff;
		

		if(speed == 0.0)//speed: kecepatan
			myTimediff = DEFAULT_TIMEDIFF;//DEFAULT_TIMEDIFF: 300
		else
			myTimediff = distTo/speed;//distTo: jarak ke host lain
		
		if(peerSpeed == 0.0)
			peerTimediff = DEFAULT_TIMEDIFF;//DEFAULT_TIMEDIFF: 300
		else
			peerTimediff = distTo/peerSpeed;//distTo: jarak ke host lain

		//do this when con is up and goes down (might have been up for awhile)
		recentEncounters.put(peer, SimClock.getTime());//SimClock.getTime(): waktu simulasi
		Sf.recentEncounters.put(thisHost, SimClock.getTime());//SimClock.getTime(): waktu simulasi

		//combine both tables
		Set<DTNHost> hosts = new HashSet<DTNHost>(this.recentEncounters.size() + Sf.recentEncounters.size());
		hosts.addAll(this.recentEncounters.keySet());//recentEncounters
		hosts.addAll(recentEncounters.keySet());//recentEncounters
		
		//update both tables
		for(DTNHost h : hosts)
		{
			double myTime, peerTime;//myTime: waktu saya, peerTime: waktu peer
			if(recentEncounters.containsKey(h))//recentEncounters: tabel waktu terakhir
				myTime = recentEncounters.get(h);
			else
				myTime = 0.0;//

			//update peer's table for host
			if(Sf.recentEncounters.containsKey(h))
			peerTime = Sf.recentEncounters.get(h);//recentEncounters: tabel waktu terakhir
			else
			peerTime = 0.0;

			//update my table for host
			if(myTime < 0.0 || myTime + myTimediff < peerTime)
			{
				recentEncounters.put(h, peerTime - myTimediff);
			}

			//update peer's table for host
			if(peerTime < 0.0 || peerTime + peerTimediff < myTime)
			{
				Sf.recentEncounters.put(h, myTime - peerTimediff);
			}
		}

	}

	public boolean isFinalDest(Message m, DTNHost host)
	{
		// Implement the method logic here
		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);//MSG_COUNT_PROP: SprayAndFocus.copies
		nrofCopies = (int)Math.ceil(nrofCopies/2.0);//ceil: pembulatan ke atas
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);//MSG_COUNT_PROP: SprayAndFocus.copies
		return m.getTo() == host;//getTo(): mendapatkan tujuan pesan
	}

	public boolean newMessage(Message m)
	{
		// Implement the method logic here
		//makeRoomForNewMessage(m.getSize());
		m.addProperty(MSG_COUNT_PROP, new Integer(initialNrofCopies));//MSG_COUNT_PROP: SprayAndFocus.copies
		//addToMessages(m, true);
		return true;
	}

	// @Override
	// public boolean createNewMessage(Message m)
	public boolean shouldDeleteOldMessage(Message m, DTNHost thisReportOld)
	{
		return m.getTo() == thisReportOld;//getTo(): mendapatkan tujuan pesan
	}

	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost host) {
		// Implement the method logic here
		int nrofCopies;
		
		nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);//MSG_COUNT_PROP: SprayAndFocus.copies
		//seperates the message into two copies if the number of copies is greater than 1
		if(nrofCopies > 1)
			nrofCopies /= 2;
		else
			return true;//return true if the number of copies is 1

		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
		return false;
	}


	public boolean shouldDeleteOldMessage(Message m, DTNHost otherHost, DTNHost thisHost)
	{
		// Implement the method logic here
		int nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);

		nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);

		//seperates the message into two copies if the number of copies is greater than 1
		if(nrofCopies > 1)
			nrofCopies /= 2;
		else
			return true;//return true if the number of copies is 1
		
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		return false;
	}

	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost)
	{
		return m.getTo() != thisHost;//getTo(): mendapatkan tujuan pesan
	}

	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost)
	{
		// Implement the method logic here
		//return true if the message is to be sent to the other host
		if(m.getTo() == otherHost)
			return true;

		//return true if the number of copies is greater than 1
		int numberOfCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		if(numberOfCopies > 1) return true;

		//return true if the other host has a newer encounter time with the destination
		DTNHost destination = m.getTo();

		//get the other host's SprayAndFocusDecisionEngine
		SprayAndFocusDecisionEngine de = this.getOtherSnFDecisionEngine(otherHost);

		//return false if the other host has not encountered the destination
		if(!de.recentEncounters.containsKey(destination))
			return false;
		//return true if the other host has a newer encounter time with the destination
		if(!recentEncounters.containsKey(destination))
			return true;
		//return true if the other host has a newer encounter time with the destination
		if(de.recentEncounters.get(destination) > this.recentEncounters.get(destination))
			return true;
			
		

		return false;
	}

	

	// @Override
	// public boolean createNewMessage(Message m)
	// {
	// 	makeRoomForNewMessage(m.getSize());

	// 	m.addProperty(MSG_COUNT_PROP, new Integer(initialNrofCopies));
	// 	addToMessages(m, true);
	// 	return true;
	// }

	// @Override
	// public Message messageTransferred(String id, DTNHost from)
	// {
	// 	Message m = super.messageTransferred(id, from);
		
	// 	/*
	// 	 * Here we update our last encounter times based on the information sent
	// 	 * from our peer. 
	// 	 */
	// 	Map<DTNHost, EncounterInfo> peerEncounters = (Map<DTNHost, EncounterInfo>)m.getProperty(SUMMARY_XCHG_PROP);
	// 	if(isDeliveredMessage(m) && peerEncounters != null)
	// 	{
	// 		double distTo = getHost().getLocation().distance(from.getLocation());
	// 		double speed = from.getPath() == null ? 0 : from.getPath().getSpeed();
			
	// 		if(speed == 0.0) return m;
			
	// 		double timediff = distTo/speed;
			
	// 		/*
	// 		 * We save the peer info for the utility based forwarding decisions, which are
	// 		 * implemented in update()
	// 		 */
	// 		neighborEncounters.put(from, peerEncounters); 
			
	// 		for(Map.Entry<DTNHost, EncounterInfo> entry : peerEncounters.entrySet())
	// 		{
	// 			DTNHost h = entry.getKey();
	// 			if(h == getHost()) continue;
				
	// 			EncounterInfo peerEncounter = entry.getValue();
	// 			EncounterInfo info = recentEncounters.get(h);
				
	// 			/*
	// 			 * We set our timestamp for some node, h, with whom our peer has come in contact
	// 			 * if our peer has a newer timestamp beyond some threshold.
	// 			 * 
	// 			 * The paper describes timers that count up from the time of contact. We use
	// 			 * fixed timestamps here to accomplish the same effect, but the computations
	// 			 * here are consequently a little different from the paper. 
	// 			 */
	// 			if(!recentEncounters.containsKey(h))
	// 			{
	// 				info = new EncounterInfo(peerEncounter.getLastSeenTime() - timediff);
	// 				recentEncounters.put(h, info);
	// 				continue;
	// 			}
				
				
	// 			if(info.getLastSeenTime() + timediff < peerEncounter.getLastSeenTime())
	// 			{
	// 				recentEncounters.get(h).updateEncounterTime(peerEncounter.getLastSeenTime() - 
	// 						timediff);
	// 			}
	// 		}
	// 		return m;
	// 	}
		
	// 	//Normal message beyond here
		
	// 	Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
		
	// 	nrofCopies = (int)Math.ceil(nrofCopies/2.0);
		
	// 	m.updateProperty(MSG_COUNT_PROP, nrofCopies);
		
	// 	return m;
	// }

	// @Override
	// protected void transferDone(Connection con) 
	// {
	// 	Integer nrofCopies;
	// 	String msgId = con.getMessage().getId();
	// 	/* get this router's copy of the message */
	// 	Message msg = getMessage(msgId);

	// 	if (msg == null) { // message has been dropped from the buffer after..
	// 		return; // ..start of transfer -> no need to reduce amount of copies
	// 	}
		
	// 	if(msg.getProperty(SUMMARY_XCHG_PROP) != null)
	// 	{
	// 		deleteMessage(msgId, false);
	// 		return;
	// 	}
		
	// 	/* 
	// 	 * reduce the amount of copies left. If the number of copies was at 1 and
	// 	 * we apparently just transferred the msg (focus phase), then we should
	// 	 * delete it. 
	// 	 */
	// 	nrofCopies = (Integer)msg.getProperty(MSG_COUNT_PROP);
	// 	if(nrofCopies > 1)
	// 		nrofCopies /= 2;
	// 	else
	// 		deleteMessage(msgId, false);
		
	// 	msg.updateProperty(MSG_COUNT_PROP, nrofCopies);
	// }
	
	

	// @Override
	// public void update()
	// {
	// 	super.update();
	// 	if (!canStartTransfer() || isTransferring()) {
	// 		return; // nothing to transfer or is currently transferring 
	// 	}

	// 	/* try messages that could be delivered to final recipient */
	// 	if (exchangeDeliverableMessages() != null) {
	// 		return;
	// 	}
		
	// 	List<Message> spraylist = new ArrayList<Message>();
	// 	List<Tuple<Message,Connection>> focuslist = new LinkedList<Tuple<Message,Connection>>();

	// 	for (Message m : getMessageCollection())
	// 	{
	// 		if(m.getProperty(SUMMARY_XCHG_PROP) != null) continue;
			
	// 		Integer nrofCopies = (Integer)m.getProperty(MSG_COUNT_PROP);
	// 		assert nrofCopies != null : "SnF message " + m + " didn't have " + 
	// 			"nrof copies property!";
	// 		if (nrofCopies > 1)
	// 		{
	// 			spraylist.add(m);
	// 		}
	// 		else
	// 		{
	// 			/*
	// 			 * Here we implement the single copy utility-based forwarding scheme.
	// 			 * The utility function is the last encounter time of the msg's 
	// 			 * destination node. If our peer has a newer time (beyond the threshold),
	// 			 * we forward the msg on to it. 
	// 			 */
	// 			DTNHost dest = m.getTo();
	// 			Connection toSend = null;
	// 			double maxPeerLastSeen = 0.0; //beginning of time (simulation time)
				
	// 			//Get the timestamp of the last time this Host saw the destination
	// 			double thisLastSeen = getLastEncounterTimeForHost(dest);
				
	// 			// for(Connection c : getHost())
	// 			for(Connection c : getConnections())
	// 			{
	// 				DTNHost peer = c.getOtherNode(getHost());
	// 				Map<DTNHost, EncounterInfo> peerEncounters = neighborEncounters.get(peer);
	// 				double peerLastSeen = 0.0;
					
	// 				if(peerEncounters != null && peerEncounters.containsKey(dest))
	// 					peerLastSeen = neighborEncounters.get(peer).get(dest).getLastSeenTime();
					
	// 				/*
	// 				 * We need to pick only one peer to send the copy on to; so lets find the
	// 				 * one with the newest encounter time.
	// 				 */
					
	// 					if(peerLastSeen > maxPeerLastSeen)
	// 					{
	// 						toSend = c;
	// 						maxPeerLastSeen = peerLastSeen;
	// 					}
							
	// 			}
	// 			if (toSend != null && maxPeerLastSeen > thisLastSeen + transitivityTimerThreshold)
	// 			{
	// 				focuslist.add(new Tuple<Message, Connection>(m, toSend));
	// 			}
	// 		}
	// 	}
		
	// 	//arbitrarily favor spraying
	// 	if(tryMessagesToAllConnections(spraylist) == null)
	// 	{
	// 		if(tryMessagesForConnected(focuslist) != null)
	// 		{
				
	// 		}
	// 	}
	// }

	// protected double getLastEncounterTimeForHost(DTNHost host)
	// {
	// 	if(recentEncounters.containsKey(host))
	// 		return recentEncounters.get(host).getLastSeenTime();
	// 	else
	// 		return 0.0;
	// }
	
	/**
	 * Stores all necessary info about encounters made by this host to some other host.
	 * At the moment, all that's needed is the timestamp of the last time these two hosts
	 * met.
	 * 
	 * @author PJ Dillon, University of Pittsburgh
	 */
	// protected class EncounterInfo
	// {
	// 	protected double seenAtTime;
		
	// 	public EncounterInfo(double atTime)
	// 	{
	// 		this.seenAtTime = atTime;
	// 	}
		
	// 	public void updateEncounterTime(double atTime)
	// 	{
	// 		this.seenAtTime = atTime;
	// 	}
		
	// 	public double getLastSeenTime()
	// 	{
	// 		return seenAtTime;
	// 	}
		
	// 	public void updateLastSeenTime(double atTime)
	// 	{
	// 		this.seenAtTime = atTime;
	// 	}


		private  SprayAndFocusDecisionEngine getOtherSnFDecisionEngine(DTNHost otherHost) {
			MessageRouter otherRouter = otherHost.getRouter();//getRouter(): mendapatkan router
		//getRouter(): mendapatkan router
			assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
		" with other routers of same type";//memastikan bahwa router adalah DecisionEngineRouter
		
		return (SprayAndFocusDecisionEngine) ((DecisionEngineRouter)otherRouter).getDecisionEngine();//getDecisionEngine(): mendapatkan engine keputusan
		//getDecisionEngine(): mendapatkan engine keputusan

		//assert: Ini adalah sebuah mekanisme untuk memastikan bahwa kondisi tertentu benar pada saat runtime. Jika kondisi yang diberikan setelah kata kunci assert bernilai false, maka program akan melemparkan sebuah pengecualian AssertionError dengan pesan yang diberikan.
		// otherRouter instanceof DecisionEngineRouter: Ini memeriksa apakah objek otherRouter adalah instance dari kelas DecisionEngineRouter atau subclass-nya.
		// Pesan Kesalahan: Jika kondisi otherRouter instanceof DecisionEngineRouter tidak terpenuhi, maka pesan berikut akan ditampilkan:
			// assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
			// " with other routers of same type";
		}
	}

