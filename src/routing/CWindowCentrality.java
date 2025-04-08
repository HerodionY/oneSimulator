package routing;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import core.Settings;
import core.SimClock;
import core.DTNHost;

import routing.community.Centrality;
import routing.community.Duration;
import routing.community.CommunityDetection;
public class CWindowCentrality implements Centrality{
    /** length of time to consider in each epoch -setting id {@value} */
	public static final String CENTRALITY_WINDOW_SETTING = "timeWindow";
	/** time interval between successive updates to centrality values -setting id 
	 * 		{@value} */
	public static final String COMPUTATION_INTERVAL_SETTING = "computeInterval";
	/** Number of time windows over which to average -setting id {@value} */
	public static final String EPOCH_COUNT_SETTING = "nrOfEpochsToAvg";
	
	/** Time to wait before recomputing centrality values (node degree) */
	protected static int COMPUTE_INTERVAL = 600; // seconds, i.e. 10 minutes
	/** Width of each time interval in which to count the node's degree */
	protected static int CENTRALITY_TIME_WINDOW = 21600; // 6 hours
	/** Number of time intervals to average the node's degree over */
	protected static int EPOCH_COUNT = 787; // CHANGED FROM 5,48;
	
	/** Saved global centrality from last computation */
	protected double globalCentrality;
	/** Saved local centrality from last computation */
	protected double localCentrality;
	
	/** timestamp of last global centrality computation */
	protected int lastGlobalComputationTime;
	/** timestamp of last local centrality computation */ 
	protected int lastLocalComputationTime;
	
	public CWindowCentrality(Settings s) 
	{
		if(s.contains(CENTRALITY_WINDOW_SETTING))
			CENTRALITY_TIME_WINDOW = s.getInt(CENTRALITY_WINDOW_SETTING);
		
		if(s.contains(COMPUTATION_INTERVAL_SETTING))
			COMPUTE_INTERVAL = s.getInt(COMPUTATION_INTERVAL_SETTING);
		
		if(s.contains(EPOCH_COUNT_SETTING))
			EPOCH_COUNT = s.getInt(EPOCH_COUNT_SETTING);
	}
	
	public CWindowCentrality(CWindowCentrality proto)
	{
		// set these back in time (negative values) to do one computation at the 
		// start of the sim
		this.lastGlobalComputationTime = this.lastLocalComputationTime = 
			-COMPUTE_INTERVAL;
	}
	
	public double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory)
	{
		if(SimClock.getIntTime() - this.lastGlobalComputationTime < COMPUTE_INTERVAL)
			return globalCentrality;
		
		// initialize
		int[] centralities = new int[EPOCH_COUNT];
		int epoch, timeNow = SimClock.getIntTime();
		Map<Integer, Set<DTNHost>> nodesCountedInEpoch = 
			new HashMap<Integer, Set<DTNHost>>();
		
		for(int i = 0; i < EPOCH_COUNT; i++)
			nodesCountedInEpoch.put(i, new HashSet<DTNHost>());
		
		/*
		 * For each node, loop through connection history until we crossed all
		 * the epochs we need to cover
		 */
		int epochControl=0;
		for(Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet())
		{
			DTNHost h = entry.getKey();
			for(Duration d : entry.getValue())
			{
				int timePassed = (int)(timeNow - d.end);
				
				// if we reached the end of the last epoch, we're done with this node
//				System.out.println("EPOCH_COUNT "+EPOCH_COUNT);
//				System.out.println("time now: "+timeNow);
//				System.out.println("timePassed: "+timePassed);
				if(timePassed > CENTRALITY_TIME_WINDOW * EPOCH_COUNT)//{
					//System.out.println("break\n");
					break;
			//	}
				// compute the epoch this contact belongs to
				epoch = timePassed / CENTRALITY_TIME_WINDOW;
				if(epoch>epochControl)
					epochControl=epoch;
//				System.out.println("epoch: "+epoch);
				
				// Only consider each node once per epoch
				Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
//				System.out.println("nodesAlreadyCounted: "+nodesAlreadyCounted);
//				System.out.println("h: "+h);
//				if(nodesAlreadyCounted!=null)
//					System.out.println("nodesAlreadyCounted.contains(h): "+nodesAlreadyCounted.contains(h));
//				else
//					System.out.println("nodesAlreadyCounted.contains(h): NULL");
//				if(nodesAlreadyCounted!=null && nodesAlreadyCounted.contains(h)){ 
				if(nodesAlreadyCounted.contains(h))
//				{	
//					System.out.println("continue\n");
					continue;
//				}
				// increment the degree for the given epoch
//				System.out.println("centralities["+epoch+"]: " + centralities[epoch] + ", size: " + centralities.length);
//				System.out.println("epoch: "+epoch);
				centralities[epoch]++;
				nodesAlreadyCounted.add(h);
//				System.out.println( "Add "+h+ " to nodesAlreadyCounted:"+nodesAlreadyCounted +"\n");
			}
		}
//		System.out.println("SAINDO PRIMEIRO FOR");

		// compute and return average node degree
		int control = 0, sum = 0;
		for(int i = 0; i < epochControl+1; i++){ // CHANGE FROM for(int i = 0; i < EPOCH_COUNT; i++){ 
//			System.out.println("centralities["+i+"]: "+centralities[i]);
			sum += centralities[i];
			control++;
		}
//		System.out.println("epochControl: "+ epochControl);
//		System.out.println("control: "+ control);
		this.globalCentrality = ((double)sum) / control; // CHANGED FROM this.globalCentrality = ((double)sum) / EPOCH_COUNT;
//		System.out.println(this+".globalCentrality: "+this.globalCentrality+"\n");
		
		this.lastGlobalComputationTime = SimClock.getIntTime();
		
		return this.globalCentrality;
	}

	public double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory,
			CommunityDetection cd)
	{
		if(SimClock.getIntTime() - this.lastLocalComputationTime < COMPUTE_INTERVAL)
			return localCentrality;
		
		// centralities will hold the count of unique encounters in each epoch
		int[] centralities = new int[EPOCH_COUNT];
		int epoch, timeNow = SimClock.getIntTime();
		Map<Integer, Set<DTNHost>> nodesCountedInEpoch = 
			new HashMap<Integer, Set<DTNHost>>();
		
		int epochControl=0;
		
		for(int i = 0; i < EPOCH_COUNT; i++)
			nodesCountedInEpoch.put(i, new HashSet<DTNHost>());
		
		// local centrality only considers nodes in the local community
		Set<DTNHost> community = cd.getLocalCommunity();
		
		/*
		 * For each node, loop through connection history until we crossed all
		 * the epochs we need to cover
		 */
		for(Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet())
		{
			DTNHost h = entry.getKey();
			
			// if the host isn't in the local community, we don't consider it
			if(!community.contains(h))
				continue;
			
			for(Duration d : entry.getValue())
			{
				int timePassed = (int)(timeNow - d.end);
				
				// if we reached the end of the last epoch, we're done with this node
				if(timePassed > CENTRALITY_TIME_WINDOW * EPOCH_COUNT)
					break;
				
				// compute the epoch this contact belongs to
				epoch = timePassed / CENTRALITY_TIME_WINDOW;
				if(epoch>epochControl)
					epochControl=epoch;
				
				// Only consider each node once per epoch
				Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
				if(nodesAlreadyCounted.contains(h))
					continue;
				
				// increment the degree for the given epoch
//				System.out.println("epoch: "+epoch);
				centralities[epoch]++;
				nodesAlreadyCounted.add(h);
			}
		}
//		System.out.println("SAINDO PRIMEIRO FOR");
		// compute and return average node degree
		int control = 0, sum = 0;
		for(int i = 0; i < epochControl+1; i++){ 
//			System.out.println("centralities["+i+"]: "+centralities[i]);
			sum += centralities[i];
			control++;
		}
//		System.out.println("epochControl: "+ epochControl);
//		System.out.println("control: "+ control);
		
		this.localCentrality = ((double)sum) / control; 
//		System.out.println(this+".localCentrality: "+this.localCentrality+"\n");
		
		this.lastLocalComputationTime = SimClock.getIntTime();
		
		return this.localCentrality;
	}

	public Centrality replicate()
	{
		return new CWindowCentrality(this);
	}

}
