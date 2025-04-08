package routing;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import core.DTNHost;
import core.Settings;
import core.SimClock;

import routing.community.CommunityDetection;
import routing.community.Duration;

public class KCliqueCommunityDetection implements CommunityDetection {
// 		// 1. Each node starts with its own community
// 		// 2. Each node sends a message to its neighbors with its community
// 		// 3. Each node receives messages from its neighbors and adds them to its

// 		// community
// 		// 4. Each node sends a message to its neighbors with its community
// 		// 5. Each node receives messages from its neighbors and adds them to its
// 		// community

    public static final String K_SETTING = "K";
	public static final String FAMILIAR_SETTING = "familiarThreshold";
	
	protected Set<DTNHost> familiarSet;
	protected Set<DTNHost> localCommunity;
	protected Map<DTNHost, Set<DTNHost>> familiarsOfMyCommunity;
	
	protected double k;
	protected double familiarThreshold;

    public KCliqueCommunityDetection(Settings s)
    {
        this.k = s.getDouble(K_SETTING);
        this.familiarThreshold = s.getDouble(FAMILIAR_SETTING);
        this.familiarSet = new HashSet<>();
        this.localCommunity = new HashSet<>();
        this.familiarsOfMyCommunity = new HashMap<>();
    }

    public KCliqueCommunityDetection(KCliqueCommunityDetection proto)
    {
        this.k = proto.k;
        this.familiarThreshold = proto.familiarThreshold;
        familiarSet = new HashSet<DTNHost>();
        localCommunity = new HashSet<DTNHost>();
        this.familiarsOfMyCommunity = new HashMap<DTNHost, Set<DTNHost>>();
    }

    public void newConnection(DTNHost myHost, DTNHost peer,
            CommunityDetection peerCD)
    {
        KCliqueCommunityDetection scd = (KCliqueCommunityDetection)peerCD;
        
        // Ensure each node is in its own local community
        // (This is the first instance where we actually get the host for these 
        // objects)
        this.localCommunity.add(myHost);
        scd.localCommunity.add(peer);
        
        /*
         * The first few steps of the protocol are
         * 1. Each node starts with its own community
         * 2. Each node sends a message to its neighbors with its community
         * 3. Each node receives messages from its neighbors and adds them to its
         * community
         * 4. Each node sends a message to its neighbors with its community
         * 5. Each node receives messages from its neighbors and adds them to its
         * community
         */

        // Add peer to my local community if needed
		if(!this.localCommunity.contains(peer))
		{
			/*
			 * 
			 */
			
			// compute the intersection size
			int count=0;
			for(DTNHost h : scd.familiarSet)
				if(this.localCommunity.contains(h))
					count++;
			
			// if peer familiar has K nodes in common with this host's local community
			if(count >= this.k - 1)
			{
				this.localCommunity.add(peer);
				this.familiarsOfMyCommunity.put(peer, scd.familiarSet);
				
				// search the peer's local community for other nodes with K in common
				// (like a transitivity property)
				for(DTNHost h : scd.localCommunity)
				{
					if(h == myHost || h == peer) continue;
					
					// compute intersection size
					count = 0;
					for(DTNHost i : scd.familiarsOfMyCommunity.get(h))
						if(this.localCommunity.contains(i))
							count++;
					
					// add nodes if there are K in common with this local community
					if(count >= this.k - 1)
					{
						this.localCommunity.add(h);
						this.familiarsOfMyCommunity.put(h, 
								scd.familiarsOfMyCommunity.get(h));
					}
				}
			}
		}
		
		// Repeat process from peer's perspective
		if(!scd.localCommunity.contains(myHost))
		{
			int count = 0;
			for(DTNHost h : this.familiarSet)
				if(scd.localCommunity.contains(h))
					count++;
			if(count >= scd.k - 1)
			{
				scd.localCommunity.add(myHost);
				scd.familiarsOfMyCommunity.put(myHost, this.familiarSet);
				
				for(DTNHost h : this.localCommunity)
				{
					if(h == myHost || h == peer) continue;
					count = 0;
					for(DTNHost i : this.familiarsOfMyCommunity.get(h))
						if(scd.localCommunity.contains(i))
							count++;
					if(count >= scd.k - 1)
					{
						scd.localCommunity.add(h);
						scd.familiarsOfMyCommunity.put(h, 
								this.familiarsOfMyCommunity.get(h));
					}
				}
			}
		}
	}
    

    public void connectionLost(DTNHost myHost, DTNHost peer, 
			CommunityDetection peerCD, List<Duration> history)
	{
		if(this.familiarSet.contains(peer)) return;
		
		// Compute cummulative contact duration with this peer
		Iterator<Duration> i = history.iterator();
		double time = 0;
		while(i.hasNext())
		{
			Duration d = i.next();
			time += d.end - d.start;
		}
		
		// If cummulative duration is greater than threshold, add
		if(time > this.familiarThreshold)
		{
			KCliqueCommunityDetection scd = (KCliqueCommunityDetection)peerCD;
			this.familiarSet.add(peer);
			this.localCommunity.add(peer);
			this.familiarsOfMyCommunity.put(peer, scd.familiarSet);
		}
	}

    public boolean isHostInCommunity(DTNHost h)
    {
        return this.localCommunity.contains(h);
    }

    public CommunityDetection replicate()
    {
        return new KCliqueCommunityDetection(this);
    }
    public Set<DTNHost> getLocalCommunity()
    {
        return this.localCommunity;
    }

   

}
