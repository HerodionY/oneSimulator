package routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import core.DTNHost;
import core.Settings;
import core.Message;
import core.Buffer;
import core.Connection;
import core.SimClock;
import core.Tuple;

import routing.community.Duration;
import routing.community.NodeRanking;


import routing.utils.TupleDe;





public class PeopleRankRouting implements RoutingDecisionEngine, NodeRanking {


    //initialitation variable dumping factor and treshold empty
    public static final String PEOPLE_RANK = "people_rank";

    //Dumping factor and treshold
    public static final String DUMPING_FACTOR = "dumpingfactor";
    public static final String TRESHOLDS = "treshold";
    public static final String MSG_COUNT_PROP = PEOPLE_RANK + "." + "copy";

    //detection community and dumping vector
    protected double dumpingFactor;
    protected double treshold;

    //data structure for people rank

    protected Map<DTNHost, TupleDe <Double, Integer>> peopleRankUpdate;
    protected Map<DTNHost, List<Duration>> connectionHistory;
    protected Map<DTNHost, Double> startTimeStamps;
    protected Set <DTNHost> hosts;
    

    /**  Constructor people rank 
     * @param s
     */
    public PeopleRankRouting(Settings s) {

        if(s.contains(DUMPING_FACTOR)) {//if the people rank contains the dumping factor
            dumpingFactor = s.getDouble(DUMPING_FACTOR);; //default value from paper
        }else {
            dumpingFactor = 0.85;
        }
        if(s.contains(TRESHOLDS)) {//if the people rank contains the treshold
            treshold = s.getDouble(TRESHOLDS);
        } else {
            treshold = 700;
        }
        //initialize the data structure
        connectionHistory = new HashMap<DTNHost, List<Duration>>();
        peopleRankUpdate = new HashMap<>();
        hosts = new HashSet<DTNHost>();
    
    }

    /**
     * Copy constructor
     * @param pr
     */
    public PeopleRankRouting(PeopleRankRouting pr) {
        this.dumpingFactor = pr.dumpingFactor;//dumping factor
        this.treshold = pr.treshold;//treshold
        this.startTimeStamps = new HashMap<DTNHost, Double>();//start time stamps
        this.connectionHistory = new HashMap<DTNHost, List<Duration>>();//connection history
        this.peopleRankUpdate = new HashMap<DTNHost, TupleDe<Double, Integer>>();//people rank update
        this.hosts = new HashSet<DTNHost>(pr.hosts);//hosts
    }

    // 
    
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // Get the local host from the connection
        DTNHost myHost = con.getOtherNode(peer);
        // Get the PeopleRank decision engine of the remote host (peer)
        PeopleRankRouting de = this.getOtherDecisionEngine(peer);

        // Update start timestamps for both hosts
        this.startTimeStamps.put(peer, SimClock.getTime());
        de.startTimeStamps.put(myHost, SimClock.getTime());
    }

    // @Override
    // public void connectionDown(DTNHost thisHost, DTNHost peer) {
    //     // Get the start time of the previous connection and the current time
    //     double time = getPreviousConnectionStartTime(thisHost, peer);
    //     double etime = SimClock.getTime();
        

    //     // Calculate the duration of the connection
    //     List<Duration> history;
        

    //     if (!connectionHistory.containsKey(peer)) {
    //         history = new LinkedList<Duration>();
    //         connectionHistory.put(peer, history);
    //     } else {
    //         history = connectionHistory.get(peer);
    //     }

    //     if (etime - time > treshold) {
    //         history.add(new Duration(time, etime));
    //         // Add peer to the friend list of thisHost
    //         hosts.add(peer);
    //     }

    //     // //update the PeopleRank of the host
    //     // Iterator<Map.Entry<DTNHost, List<Duration>>> iterator = connectionHistory.entrySet().iterator();
    //     // //iterate through the connection history
    //     // if(!iterator.hasNext()) {
    //     // while(iterator.hasNext()) {

            
    //     //     //get the entry
    //     //     Map.Entry<DTNHost, List<Duration>> entry = iterator.next();

    //     //     DTNHost hosts = iterator.next().getKey();
    //     //     //get the other host
    //     //     double friendRank = calculatePeopleRank(entry.getKey());

    //     //     //initialize the update
    //     //     Set<DTNHost> friends = new HashSet<DTNHost>();
    //     //     friends.add(peer);
    //     //     int totalFriends = friends.size();

    //     //     //initialize the update
    //     //     //create a new tuple for the update
    //     //     TupleDe<Double, Integer> update = new TupleDe<Double, Integer>(friendRank, totalFriends);

    //     //     peopleRankUpdate.put(hosts, update);
    //     //     // if(update == null) {
    //     //     //     //initialize the update
    //     //     //     update = new TupleDe<Double, Integer>(0.0, 0);
    //     //     //     //update the PeopleRank of the host
    //     //     //     peopleRankUpdate.put(peer, update);
    //     //     // }

    //     //     // //update the PeopleRank of the host based on the formula
    //     //     // double newRank = (1 - dumpingFactor) + dumpingFactor * (friendRank / totalFriends);
    //     // }}else {
    //     //     // Handle the case when the map is empty
    //     //     System.out.println("Connection history is empty.");
    //     // }

    //     for (Map.Entry<DTNHost, List<Duration>> entry : connectionHistory.entrySet()) {
    //           //get the entry

    //         DTNHost hosts = entry.getKey();
    //         //get the other host
    //         double friendRank = calculatePeopleRank(hosts);

    //         //initialize the update
    //         Set<DTNHost> friends = new HashSet<>();
    //         friends.add(peer);
    //         int totalFriends = friends.size();

    //         //initialize the update
    //         //create a new tuple for the update
    //         TupleDe<Double, Integer> update = new TupleDe<>(friendRank, totalFriends);

    //         peopleRankUpdate.put(hosts, update);
    //     }

    // }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = getPreviousConnectionStartTime(thisHost, peer);
        double etime = SimClock.getTime();

        // Tentukan durasi koneksi
        List<Duration> history;

        // Cek apakah koneksi untuk peer ini sudah ada di riwayat
        if (!connectionHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connectionHistory.put(peer, history);
        } else {
            history = connectionHistory.get(peer);
        }

        // Jika durasi koneksi lebih besar dari threshold, tambahkan ke riwayat
        if (etime - time >= treshold) {
            history.add(new Duration(time, etime));
            // Tambahkan peer ke dalam daftar teman untuk thisHost
            hosts.add(peer);
        }

        // Update PeopleRank untuk setiap host berdasarkan riwayat koneksi
        for (Map.Entry<DTNHost, List<Duration>> entry : connectionHistory.entrySet()) {
            DTNHost otherHost = entry.getKey();
            double friendRank = calculatePeopleRank(otherHost);

            // Hitung total teman untuk host ini
            Set<DTNHost> friends = new HashSet<>();
            friends.add(peer);
            int totalFriends = friends.size();

            // Update PeopleRank dan jumlah teman
            TupleDe<Double, Integer> update = new TupleDe<>(friendRank, totalFriends);
            peopleRankUpdate.put(otherHost, update);
        }
    }


     private PeopleRankRouting getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (PeopleRankRouting) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    

    /* 
        * @param thisHost
        * @param peer
        * @return
        * this method is used to get the previous connection start time
        * for the current host and peer
        */
     public double getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer) {
        // Check if there is a previous connection start time recorded for this host and
        // peer
        if (startTimeStamps.containsKey(thisHost)) {
            // If a record exists, return the start time of the previous connection
            return startTimeStamps.get(peer);
        } else {
            // If no record exists, return 0
            return 0;
        }
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankRouting(this);
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

     /**
     * Determines whether a message should be sent from this host to another host
     * based on the PeopleRank routing algorithm.
     * 
     * @param m         The message to be sent.
     * @param thisHost  The current host from which the message originates.
     * @param otherHost The destination host to which the message should be sent.
     * @return True if the message should be sent to the other host, false
     *         otherwise.
     */
    // @Override
    // public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {

    //     // if the destination is the other host
    //     if (m.getTo() == otherHost) {
    //         return true;
    //     }
    //     // //if the other host is the current host
    //     // if (thisHost == otherHost) {
    //     //     return false;
    //     // }
    //     double perThisHost = calculatePeopleRank(thisHost);
    //     double perOtherHost = calculatePeopleRank(otherHost);


    //     // Initialize the F(i) = friends as friends of i.
    //     Set<DTNHost> friends = new HashSet<>(connectionHistory.keySet());
    //     friends.add(otherHost);

    //     //if this host is already friend checked
    //     if (connectionHistory.containsKey(otherHost) || hosts.contains(otherHost)) {
    //         // If the other host is already a friend, send the message.
    //         while (true) {//while loop to check if the other host is a friend
    //             for (Map.Entry<DTNHost, List<Duration>> entry : connectionHistory.entrySet()) {
    //                 //if the other host is in the connection history
    //                 if (entry.getValue().contains(otherHost)) {
    //                     //if the other host is a friend
    //                     Iterator<DTNHost> iterator = friends.iterator();
    //                     //iterate through the friends
    //                     while (iterator.hasNext()) {
    //                         //get the friend
    //                         DTNHost friend = iterator.next();
    //                         //if the other host is a friend
    //                         if (otherHost.equals(friend)) {
    //                             return true;
    //                         } 
    //                         /*
    //                             if j !∈ F(i) then
    //                         }*/
    //                        //if the other host is not a friend
    //                         else if (!friend.equals(otherHost)) {
    //                             return false;
    //                         }
    //                     }
    //                 }


    //                 //Buffer message
    //                 Buffer messaBuffer = new Buffer();
    //                 int bufferSize = messaBuffer.getBufferSize(thisHost);
    //                 //if the buffer size is greater than 0
    //                 while(bufferSize > 0) {
    //                     //if the other host rank more than this host or the other host is the destination
    //                     //send the message
    //                     if(perOtherHost > perThisHost || otherHost.equals(m.getTo())) {
    //                         return true;
    //                     }
    //             }
    //         }
            
    //     }

    //     // If the PeopleRank of the other host is greater than the PeopleRank of the current host, send the message.
    // }
    // return false;
    // }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true; // Jika tujuan adalah host lain, kirim langsung
        }

        // Hitung PeopleRank untuk thisHost dan otherHost
        double perThisHost = calculatePeopleRank(thisHost);
        double perOtherHost = calculatePeopleRank(otherHost);

        // Periksa apakah host ini sudah berhubungan dengan host tujuan
        if (connectionHistory.containsKey(otherHost) || hosts.contains(otherHost)) {
            // Jika host tujuan adalah teman, kirim pesan
            if (perOtherHost >= perThisHost || otherHost.equals(m.getTo())) {
                return true;
            }
        }

        return false; // Jika tidak ada kondisi di atas, pesan tidak dikirim
    }


    // private Tuple<Message, Connection> tryOtherMessages() {
	// 	List<Tuple<Message, Connection>> messages = 
	// 		new ArrayList<Tuple<Message, Connection>>(); 
	
	// 	Collection<Message> msgCollection = getMessageCollection();
		
	// 	/* for all connected hosts collect all messages that have a higher
	// 	   probability of delivery by the other host */
	// 	for (Connection con : getConnections()) {
	// 		DTNHost other = con.getOtherNode(getHost());
	// 		ProphetRouter othRouter = (ProphetRouter)other.getRouter();
			
	// 		if (othRouter.isTransferring()) {
	// 			continue; // skip hosts that are transferring
	// 		}
			
	// 		for (Message m : msgCollection) {
	// 			if (othRouter.hasMessage(m.getId())) {
	// 				continue; // skip messages that the other one has
	// 			}
    //                             tryAllMessagesToAllConnections();
				
	// 		}			
	// 	}
		
	// 	if (messages.size() == 0) {
	// 		return null;
	// 	}
	// 	// System.out.println(messages);
	// 	// sort the message-connection tuples
	// 	Collections.sort(messages, new TupleComparator());
	// 	return tryMessagesForConnected(messages);	// try to send messages
	// }

    // private class TupleComparator implements Comparator 
	// 	<Tuple<Message, Connection>> {

	// 	public int compare(Tuple<Message, Connection> tuple1,
	// 			Tuple<Message, Connection> tuple2) {
	// 		// delivery probability of tuple1's message with tuple1's connection
	// 		double p1 = ((ProphetRouter)tuple1.getValue().
	// 				getOtherNode(getHost()).getRouter()).getPredFor(
	// 				tuple1.getKey().getTo());
	// 		// -"- tuple2...
	// 		double p2 = ((ProphetRouter)tuple2.getValue().
	// 				getOtherNode(getHost()).getRouter()).getPredFor(
	// 				tuple2.getKey().getTo());

	// 		// bigger probability should come first
	// 		if (p2-p1 == 0) {
	// 			/* equal probabilities -> let queue mode decide */
	// 			return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
	// 		}
	// 		else if (p2-p1 < 0) {
	// 			return -1;
	// 		}
	// 		else {
	// 			return 1;
	// 		}
	// 	}
	// }


    // @Override
    // public void changedConnection(Connection con){
    //     DTNHost other = con.getOtherNode(getHost());
    //     //if the connection is up
    //     if(con.isUp()) {
    //         startTimeStamps.put(other, SimClock.getTime());
    //     }//if the connection is down
    //     else {
    //         //update the connection history
    //         if(startTimeStamps.containsKey(other)) {
    //             //calculate the duration of the connection
    //             double time = startTimeStamps.remove(other);
    //             double durationTime = SimClock.getTime() - time;
    //             List<Duration> history;
    //             //if the other host is not in the connection history
    //             if(!connectionHistory.containsKey(other)) {
    //                 history = new LinkedList<Duration>();
    //                 connectionHistory.put(other, history);
    //             }
    //             //if the other host is in the connection history
    //             else {
    //                 history = connectionHistory.get(other);
    //             }
    //             //if the duration time is greater than 0
    //             if(durationTime - time > 0) {
    //                 history.add(new Duration(time, durationTime));
    //             }
    //         }
    //     }
    //     //update the PeopleRank of the host
    //     Iterator<Map.Entry<DTNHost, List<Duration>>> iterator = connectionHistory.entrySet().iterator();
    //     //iterate through the connection history
    //     while(iterator.hasNext()) {
    //         //get the entry
    //         Map.Entry<DTNHost, List<Duration>> entry = iterator.next();
    //         //get the other host
    //         double friendRank = calculatePeopleRank(entry.getKey());

    //         //initialize the update
    //         Set<DTNHost> friends = new HashSet<DTNHost>();
    //         friends.add(other);
    //         int totalFriends = friends.size();

    //         //initialize the update
    //         //create a new tuple for the update
    //         TupleDe<Double, Integer> update = peopleRankUpdate.get(other);
    //         if(update == null) {
    //             //initialize the update
    //             update = new TupleDe<Double, Integer>(0.0, 0);
    //             //update the PeopleRank of the host
    //             peopleRankUpdate.put(other, update);
    //         }

    //         // //update the PeopleRank of the host based on the formula
    //         // double newRank = (1 - dumpingFactor) + dumpingFactor * (friendRank / totalFriends);

            
    //     }
        
    // }

    // /**
    //  * Method to update the PeopleRank of a host based on the formula:
    //  * PeR(Ni) = (1 - d) + d * Σ PeR(Nj) / |F(Nj)|
    //  * 
    //  * Where:
    //  * - PeR(Ni) is the PeopleRank for the current host.
    //  * - d is the damping factor obtained from the setting. If not specified, it
    //  * defaults to 0.75.
    //  * - PeR(Nj) is the ranking of other connected nodes (friends).
    //  * - |F(Nj)| is the total number of friends of other nodes.
    //  * 
    //  * @param host The host for which to update the PeopleRank.
    //  */

    // /**
    //  * Method to send a message
    //  * 
    //  * Determines whether a message should be sent from this host to another host
    //  * based on the PeopleRank routing algorithm.
    //  * 
    //  * @param m         The message to be sent.
    //  * @param thisHost  The current host from which the message originates.
    //  * @param otherHost The destination host to which the message should be sent.
    //  * @return True if the message should be sent to the other host, false
    //  *         otherwise.
    //  * @return
    //  */

    // //method to send a message to another host
    // private boolean sendMessage (Message m, DTNHost thisHost, DTNHost otherHost) {
    //     //if the destination is the other host
    //     if (m.getTo() == otherHost) {
    //         return true;
    //     }
    //     //if the other host is the current host
    //     if (thisHost == otherHost) {
    //         return false;
    //     }
    //     double perThisHost = calculatePeopleRank(thisHost);
    //     double perOtherHost = calculatePeopleRank(otherHost);


    //     // Initialize the F(i) = friends as friends of i.
    //     Set<DTNHost> friends = new HashSet<DTNHost>();
    //     friends.add(otherHost);

    //     //if this host is already friend checked
    //     if (connectionHistory.containsKey(otherHost) || hosts.contains(otherHost)) {
    //         // If the other host is already a friend, send the message.
    //         while (true) {//while loop to check if the other host is a friend
    //             for (Map.Entry<DTNHost, List<Duration>> entry : connectionHistory.entrySet()) {
    //                 //if the other host is in the connection history
    //                 if (entry.getValue().contains(otherHost)) {
    //                     //if the other host is a friend
    //                     Iterator<DTNHost> iterator = friends.iterator();
    //                     //iterate through the friends
    //                     while (iterator.hasNext()) {
    //                         //get the friend
    //                         DTNHost friend = iterator.next();
    //                         //if the other host is a friend
    //                         if (otherHost.equals(friend)) {
    //                             return true;
    //                         } 
    //                         /*
    //                             if j !∈ F(i) then
    //                         }*/
    //                        //if the other host is not a friend
    //                         else if (!friend.equals(otherHost)) {
    //                             return false;
    //                         }
    //                     }
    //                 }


    //                 //Buffer message
    //                 Buffer messaBuffer = new Buffer();
    //                 int bufferSize = messaBuffer.getBufferSize(thisHost);
    //                 //if the buffer size is greater than 0
    //                 while(bufferSize > 0) {
    //                     //if the other host rank more than this host or the other host is the destination
    //                     //send the message
    //                     if(perOtherHost > perThisHost || otherHost.equals(m.getTo())) {
    //                         return true;
    //                     }
    //             }
    //         }
            
    //     }

    //     // If the PeopleRank of the other host is greater than the PeopleRank of the current host, send the message.
    // }
    // return false;
    // }
    
    
    // @Override //method to send a message
    // public Message messageTransferred(String id, DTNHost from){
    //     //get the message
    //     Message m = super.messageTransferred(id, from);
    //     //if the message is null
    //     if(m == null) {
    //         return null;
    //     }
    //     DTNHost currentHost = getHost();
    //     //get the connections
    //     List<Connection> connections = currentHost.getConnections();
    //     //iterate through the connections
    //     for (Connection c : connections) {
    //         //get the other host
    //         DTNHost otherHost = c.getOtherNode(currentHost);
    //         //if the message should be sent to the other host
    //         if (sendMessage(m, currentHost, otherHost)) {
    //             return m;
    //         }
    //     }
    //     return null;

    // }


    /**
     * Calculates the PeopleRank for a given host based on the formula:
     * PeR(Ni) = (1 - d) + d * Σ PeR(Nj) / |F(Nj)|
     * 
     * Where:
     * - PeR(Ni) is the PeopleRank for the current host.
     * - d is the damping factor obtained from the setting. If not specified, it
     * defaults to 0.75.
     * - PeR(Nj) is the ranking of other connected nodes (friends).
     * - |F(Nj)| is the total number of friends of other nodes.
     * 
     * @param host The host for which to calculate the PeopleRank.
     * @return The PeopleRank for the specified host.
     */


    // public double calculatePeopleRank(DTNHost host) {
    //     //initialize the PeopleRank
    //     double sum = 0.0;
    //     double dampingFactor = this.dumpingFactor;
    //     int friends = 0;

    //     for(TupleDe<Double, Integer> tuple : peopleRankUpdate.values()) {
    //         friends += tuple.getSecond();//update the PeopleRank of the host
           
    //     }
    //     //if the host is not in the connection history
    //     for (TupleDe<Double, Integer> tuple : peopleRankUpdate.values()) {
    //         //if the other host is in the connection history
    //         if (!tuple.getFirst().equals(host)) {
    //             //update the PeopleRank of the host
    //             double friendRank = tuple.getFirst();
    //             int friendOfOtherHost = tuple.getSecond();
    //             if(friendOfOtherHost > 0) {
    //             sum += friendRank/friends;//update the PeopleRank of the host
    //             }
    //         }
    //     }
    //     //if the host is not in the connection history
    //     return (1 - dampingFactor) + dampingFactor * sum;//calculate the PeopleRank

    // }

        public double calculatePeopleRank(DTNHost host) {
        double sum = 0.0;
        double dampingFactor = this.dumpingFactor;
        int totalFriends = 0;

        // Hitung total teman untuk host
        for (TupleDe<Double, Integer> tuple : peopleRankUpdate.values()) {
            totalFriends += tuple.getSecond();
        }

        // Iterasi untuk menghitung kontribusi dari setiap teman
        for (Map.Entry<DTNHost, TupleDe<Double, Integer>> entry : peopleRankUpdate.entrySet()) {
            DTNHost otherHost = entry.getKey();
            TupleDe<Double, Integer> value = entry.getValue();
            
            // Hanya pertimbangkan teman yang bukan host itu sendiri
            if (!otherHost.equals(host)) {
                double friendRank = value.getFirst();
                int friendsOfOtherHost = value.getSecond();
                if (friendsOfOtherHost > 0) {
                    sum += friendRank / totalFriends; // Tambahkan kontribusi dari teman
                }
            }
        }

        // Hitung PeopleRank berdasarkan formula: PeR(Ni) = (1 - d) + d * Σ PeR(Nj) / |F(Nj)|
        return (1 - dampingFactor) + dampingFactor * sum;
    }



    @Override
    public void update(DTNHost thisHost) {
    }

    public Map<DTNHost, Double> getAllRankings() {
        Map<DTNHost, Double> rankings = new HashMap<>();

        // Iterate over the per map to extract rankings
        for (Map.Entry<DTNHost, TupleDe<Double, Integer>> entry : peopleRankUpdate.entrySet()) {
            DTNHost currentHost = entry.getKey();
            TupleDe<Double, Integer> tuple = entry.getValue();

            // double getRank = entry.getValue();

            // Add the host and its ranking to the map
            rankings.put(currentHost, tuple.getFirst());
        }

        return rankings;
    }

    @Override
    public int getTotalFriends(DTNHost host) {
        DecisionEngineRouter d = (DecisionEngineRouter) host.getRouter();
        PeopleRankRouting othRouter = (PeopleRankRouting) d.getDecisionEngine();
        return othRouter.peopleRankUpdate.size();
    }

    


    // @Override
    // public PeopleRankRouting replicate() {//replicate the PeopleRankRouting
    //     return new PeopleRankRouting(this);
    // }
       


}

/*
PeopleRankRouter
PeopleRankRouter adalah sebuah komponen dalam sistem routing yang menggunakan algoritma PeopleRank untuk menentukan keputusan 
rute dalam jaringan. Algoritma PeopleRank didasarkan pada teori ranking atau penentuan pentingnya sebuah node (host) dalam 
jaringan dengan mempertimbangkan koneksi sosial antar node. Dalam konteks ini, setiap node diberi nilai yang mencerminkan 
pentingnya atau pengaruhnya dalam jaringan, dengan nilai yang lebih tinggi menunjukkan node yang lebih penting.

Fitur Utama:

Penghitungan PeopleRank:

PeopleRankRouter menghitung nilai PeopleRank setiap node dengan menggunakan algoritma iteratif yang mempertimbangkan pengaruh dari
node tetangga. Setiap node dihitung berdasarkan nilai PeopleRank tetangganya dan jumlah teman atau koneksi yang dimilikinya.
Routing Berdasarkan PeopleRank:

Menggunakan nilai PeopleRank yang dihitung, PeopleRankRouter membuat keputusan rute untuk pesan yang diterima, dengan lebih 
memilih untuk mengirim pesan ke node yang memiliki PeopleRank lebih tinggi. Hal ini diharapkan dapat meningkatkan efisiensi 
pengiriman pesan dalam jaringan yang dinamis dan terputus-putus, seperti dalam jaringan Delay Tolerant Networks (DTNs) atau 
Mobile Ad Hoc Networks (MANETs).Pembaruan Riwayat Koneksi:

PeopleRankRouter juga menyimpan riwayat koneksi antar node. Setiap kali koneksi terputus, informasi durasi koneksi antara node 
dicatat dan digunakan untuk memperbarui nilai PeopleRank.Fleksibilitas dalam Pengaturan Parameter:

Algoritma ini dilengkapi dengan parameter yang dapat dikonfigurasi, seperti faktor peluruhan (damping factor) dan threshold durasi 
koneksi, yang memungkinkan penyesuaian perilaku PeopleRankRouter agar sesuai dengan karakteristik jaringan tertentu. Pengolahan Pesan dan Pengiriman:

PeopleRankRouter berfungsi untuk memutuskan apakah pesan yang diterima harus diteruskan atau tidak berdasarkan PeopleRank dan
status koneksi antara node pengirim dan penerima. Selain itu, ia juga mengelola buffer pesan dan menangani penghapusan atau 
penyimpanan pesan sesuai dengan keputusan routing yang diambil.

Keandalan dan Pengiriman Pesan:

Meskipun berfokus pada routing pesan melalui node dengan PeopleRank tertinggi, sistem ini juga mempertimbangkan faktor-faktor seperti 
latensi, jumlah hop, dan waktu buffering untuk memaksimalkan keandalan pengiriman pesan.
Kegunaan:

PeopleRankRouter sangat berguna dalam jaringan yang memiliki topologi dinamis dan terputus-putus, seperti jaringan ponsel ad-hoc dan 
jaringan dengan konektivitas yang tidak stabil, di mana pengiriman pesan berbasis pada nilai pengaruh antar node lebih efektif daripada menggunakan 
algoritma routing tradisional.
Aplikasi:

PeopleRankRouter dapat diterapkan dalam sistem komunikasi terdesentralisasi, seperti aplikasi messaging peer-to-peer (P2P), jaringan sensor, dan sistem routing di lingkungan yang sering terputus, seperti dalam situasi darurat atau di daerah terpencil.


Deskripsi Kode PeopleRankRouting
PeopleRankRouting adalah kelas yang mengimplementasikan algoritma routing berbasis PeopleRank dalam konteks jaringan terdistribusi, seperti Mobile Ad Hoc Networks (MANETs) atau Delay Tolerant Networks (DTNs). Routing di sini dilakukan dengan mempertimbangkan peringkat sosial atau PeopleRank dari node (host) dalam jaringan. Algoritma ini mengarahkan pesan ke node yang memiliki PeopleRank lebih tinggi, yang dianggap lebih penting atau lebih terhubung dalam jaringan.

Berikut adalah penjelasan bagian per bagian dari kode PeopleRankRouting:

1. Deklarasi dan Inisialisasi Variabel
dumpingFactor: Faktor peluruhan yang digunakan untuk mengontrol seberapa besar pengaruh teman-teman pada PeopleRank.
treshold: Ambang batas durasi koneksi yang perlu dipertimbangkan untuk menghitung PeopleRank.
peopleRankUpdate: Map yang menyimpan pembaruan PeopleRank untuk setiap host, dengan nilai Tuple yang berisi peringkat dan jumlah teman.
connectionHistory: Menyimpan riwayat durasi koneksi antar host.
startTimeStamps: Menyimpan waktu mulai setiap koneksi untuk perhitungan durasi.
hosts: Set yang menyimpan host yang terhubung (teman) dalam jaringan.
2. Konstruktor
PeopleRankRouting(Settings s): Konstruktor ini menginisialisasi objek dengan parameter dari konfigurasi (seperti dumpingFactor dan treshold) dan menyiapkan struktur data yang diperlukan (seperti connectionHistory, peopleRankUpdate, dan hosts).

PeopleRankRouting(PeopleRankRouting pr): Konstruktor salinan yang membuat salinan dari objek PeopleRankRouting yang ada.

3. Metode connectionUp dan doExchangeForNewConnection
connectionUp(DTNHost thisHost, DTNHost peer): Menangani koneksi yang baru terhubung, meskipun dalam kode ini tidak ada implementasi lebih lanjut.
doExchangeForNewConnection(Connection con, DTNHost peer): Menghitung dan memperbarui waktu mulai koneksi untuk kedua host (thisHost dan peer) ketika sebuah koneksi baru terjalin.
4. Metode connectionDown
connectionDown(DTNHost thisHost, DTNHost peer): Dipanggil ketika koneksi antar dua host terputus. Metode ini mencatat durasi koneksi dan meng-update connectionHistory. Jika durasi koneksi lebih besar dari threshold, maka peer ditambahkan ke daftar teman (hosts) dan PeopleRank dihitung ulang.
5. Metode Pembantu
getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer): Mengambil waktu mulai koneksi terakhir antara dua host untuk menghitung durasi.
getOtherDecisionEngine(DTNHost h): Mengambil engine keputusan routing dari host lain yang terhubung (peer).
6. Metode Routing
isFinalDest(Message m, DTNHost aHost): Mengecek apakah pesan sudah mencapai tujuan akhirnya.
newMessage(Message m): Digunakan untuk mendeteksi pesan baru.
shouldDeleteOldMessage(Message m, DTNHost hostReportingOld): Memutuskan apakah pesan lama harus dihapus.
shouldDeleteSentMessage(Message m, DTNHost otherHost): Memutuskan apakah pesan yang sudah dikirim harus dihapus.
shouldSaveReceivedMessage(Message m, DTNHost thisHost): Memutuskan apakah pesan yang diterima harus disimpan.
shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost): Fungsi utama untuk menentukan apakah pesan harus dikirim ke host lain berdasarkan PeopleRank. Menggunakan PeopleRank untuk memutuskan apakah host tujuan memiliki peringkat lebih tinggi dari host pengirim.
7. Perhitungan PeopleRank
calculatePeopleRank(DTNHost host): Menghitung nilai PeopleRank untuk host tertentu menggunakan rumus:
PeR(𝑁𝑖)=(1−𝑑)+𝑑×∑PeR(𝑁𝑗)∣𝐹(𝑁𝑗)∣PeR(Ni)=(1−d)+d×∑∣F(Nj)∣PeR(Nj)
​Di mana d adalah damping factor, PeR(N_j) adalah PeopleRank dari teman, dan |F(N_j)| adalah jumlah teman dari node N_j. Fungsi ini menghitung PeopleRank berdasarkan kontribusi dari setiap teman yang terhubung.
8. Metode Replikasi dan Pembaruan
replicate(): Membuat salinan dari objek PeopleRankRouting untuk replikasi.
update(DTNHost thisHost): Digunakan untuk memperbarui status dari host tertentu (meskipun dalam kode ini belum ada implementasi spesifik).
9. Mengambil Semua Peringkat
getAllRankings(): Mengembalikan map yang berisi semua peringkat (PeopleRank) dari setiap host dalam jaringan.
10. Mengambil Total Teman
getTotalFriends(DTNHost host): Mengembalikan jumlah teman yang dimiliki oleh host tertentu, berdasarkan data yang disimpan dalam peopleRankUpdate.
*/

