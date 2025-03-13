package routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import core.DTNHost;
import core.Settings;
import core.Message;
import core.Buffer;
import core.Connection;
import core.SimClock;

import routing.community.Duration;
import routing.utils.TupleDe;





public class PeopleRankRouting extends ActiveRouter {


    //initialitation variable dumping factor and treshold empty
    public static final String PEOPLE_RANK = "people_rank";
    public static final String PEOPLE_RANK_QUERY = "people_rank_query";
    public static final String PEOPLE_RANK_QUERY_REPLY = "people_rank_query_reply";
    public static final String PEOPLE_RANK_UPDATE = "people_rank_update";

    //Dumping factor and treshold
    public static final String DUMPING_FACTOR = "dumping_factor";
    public static final String TRESHOLD = "treshold";
    public static final String MSG_COUNT_PROP = PEOPLE_RANK + "." + "copy";

    //detection community and dumping vector
    protected double dumpingFactor;
    protected double treshold = 0.0001;

    //data structure for people rank

    protected Map<DTNHost, TupleDe <Double, Integer>> peopleRankUpdate;
    protected Map<DTNHost, List<Duration>> connectionHistory;
    protected Map<DTNHost, Double> startTimeStamps;
    protected Set <DTNHost> hosts;
    

    /**  Constructor people rank 
     * @param s
     */
    public PeopleRankRouting(Settings s) {
        super(s);
        Settings people = new Settings(PEOPLE_RANK);

        if(people.contains(DUMPING_FACTOR)) {//if the people rank contains the dumping factor
            dumpingFactor = 0.85; //default value from paper
        }
        if(people.contains(TRESHOLD)) {//if the people rank contains the treshold
            treshold = s.getDouble(TRESHOLD);
        } else {
            treshold = 700;
        }
        //initialize the data structure
        connectionHistory = new HashMap<DTNHost, List<Duration>>();
        peopleRankUpdate = new HashMap<DTNHost, TupleDe<Double, Integer>>();
        hosts = new HashSet<DTNHost>();
    
    }

    /**
     * Copy constructor
     * @param pr
     */
    public PeopleRankRouting(PeopleRankRouting pr) {
        super(pr);
        this.dumpingFactor = pr.dumpingFactor;//dumping factor
        this.treshold = pr.treshold;//treshold
        this.startTimeStamps = new HashMap<DTNHost, Double>();//start time stamps
        this.connectionHistory = new HashMap<DTNHost, List<Duration>>();//connection history
        this.peopleRankUpdate = new HashMap<DTNHost, TupleDe<Double, Integer>>();//people rank update
        this.hosts = new HashSet<DTNHost>(pr.hosts);//hosts
    }

    @Override
    public void update() {//update the PeopleRankRouting
        super.update();
        //if the host is not in the connection history
        if(!canStartTransfer() || !isTransferring()) {//if the host is not in the connection history
            return;
        }

        if(exchangeDeliverableMessages() != null) {//if the host is not in the connection history
            return;
        }
       
       
    }


    @Override
    public void changedConnection(Connection con){
        DTNHost other = con.getOtherNode(getHost());
        //if the connection is up
        if(con.isUp()) {
            startTimeStamps.put(other, SimClock.getTime());
        }//if the connection is down
        else {
            //update the connection history
            if(startTimeStamps.containsKey(other)) {
                //calculate the duration of the connection
                double time = startTimeStamps.remove(other);
                double durationTime = SimClock.getTime() - time;
                List<Duration> history;
                //if the other host is not in the connection history
                if(!connectionHistory.containsKey(other)) {
                    history = new LinkedList<Duration>();
                    connectionHistory.put(other, history);
                }
                //if the other host is in the connection history
                else {
                    history = connectionHistory.get(other);
                }
                //if the duration time is greater than 0
                if(durationTime - time > 0) {
                    history.add(new Duration(time, durationTime));
                }
            }
        }
        //update the PeopleRank of the host
        Iterator<Map.Entry<DTNHost, List<Duration>>> iterator = connectionHistory.entrySet().iterator();
        //iterate through the connection history
        while(iterator.hasNext()) {
            //get the entry
            Map.Entry<DTNHost, List<Duration>> entry = iterator.next();
            //get the other host
            double friendRank = calculatePeopleRank(entry.getKey());

            //initialize the update
            Set<DTNHost> friends = new HashSet<DTNHost>();
            friends.add(other);
            int totalFriends = friends.size();

            //initialize the update
            //create a new tuple for the update
            TupleDe<Double, Integer> update = peopleRankUpdate.get(other);
            if(update == null) {
                //initialize the update
                update = new TupleDe<Double, Integer>(0.0, 0);
                //update the PeopleRank of the host
                peopleRankUpdate.put(other, update);
            }

            // //update the PeopleRank of the host based on the formula
            // double newRank = (1 - dumpingFactor) + dumpingFactor * (friendRank / totalFriends);

            
        }
        
    }

    /**
     * Method to update the PeopleRank of a host based on the formula:
     * PeR(Ni) = (1 - d) + d * Σ PeR(Nj) / |F(Nj)|
     * 
     * Where:
     * - PeR(Ni) is the PeopleRank for the current host.
     * - d is the damping factor obtained from the setting. If not specified, it
     * defaults to 0.75.
     * - PeR(Nj) is the ranking of other connected nodes (friends).
     * - |F(Nj)| is the total number of friends of other nodes.
     * 
     * @param host The host for which to update the PeopleRank.
     */

    /**
     * Method to send a message
     * 
     * Determines whether a message should be sent from this host to another host
     * based on the PeopleRank routing algorithm.
     * 
     * @param m         The message to be sent.
     * @param thisHost  The current host from which the message originates.
     * @param otherHost The destination host to which the message should be sent.
     * @return True if the message should be sent to the other host, false
     *         otherwise.
     * @return
     */

    //method to send a message to another host
    private boolean sendMessage (Message m, DTNHost thisHost, DTNHost otherHost) {
        //if the destination is the other host
        if (m.getTo() == otherHost) {
            return false;
        }
        //if the other host is the current host
        if (thisHost == otherHost) {
            return false;
        }
        double perThisHost = calculatePeopleRank(thisHost);
        double perOtherHost = calculatePeopleRank(otherHost);


        // Initialize the F(i) = friends as friends of i.
        Set<DTNHost> friends = new HashSet<DTNHost>();
        friends.add(otherHost);

        //if this host is already friend checked
        if (connectionHistory.containsKey(otherHost) || hosts.contains(otherHost)) {
            // If the other host is already a friend, send the message.
            while (true) {//while loop to check if the other host is a friend
                for (Map.Entry<DTNHost, List<Duration>> entry : connectionHistory.entrySet()) {
                    //if the other host is in the connection history
                    if (entry.getValue().contains(otherHost)) {
                        //if the other host is a friend
                        Iterator<DTNHost> iterator = friends.iterator();
                        //iterate through the friends
                        while (iterator.hasNext()) {
                            //get the friend
                            DTNHost friend = iterator.next();
                            //if the other host is a friend
                            if (otherHost.equals(friend)) {
                                return true;
                            } 
                            /*
                                if j !∈ F(i) then
                            }*/
                           //if the other host is not a friend
                            else if (!friend.equals(otherHost)) {
                                return false;
                            }
                        }
                    }


                    //Buffer message
                    Buffer messaBuffer = new Buffer();
                    int bufferSize = messaBuffer.getBufferSize(thisHost);
                    //if the buffer size is greater than 0
                    while(bufferSize > 0) {
                        //if the other host rank more than this host or the other host is the destination
                        //send the message
                        if(perOtherHost > perThisHost || otherHost.equals(m.getTo())) {
                            return true;
                        }
                }
            }
            
        }

        // If the PeopleRank of the other host is greater than the PeopleRank of the current host, send the message.
    }
    return false;
    }
    
    
    @Override //method to send a message
    public Message messageTransferred(String id, DTNHost from){
        //get the message
        Message m = super.messageTransferred(id, from);
        //if the message is null
        if(m == null) {
            return null;
        }
        DTNHost hosts = getHost();
        //get the connections
        List<Connection> connections = hosts.getConnections();
        //iterate through the connections
        for (Connection c : connections) {
            //get the other host
            DTNHost otherHost = c.getOtherNode(hosts);
            //if the message should be sent to the other host
            if (sendMessage(m, hosts, otherHost)) {
                return m;
            }
        }
        return null;

    }


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


    public double calculatePeopleRank(DTNHost host) {
        //initialize the PeopleRank
        double peopleRank = 0.0;
        double sum = 0.0;
        double dampingFactor = this.dumpingFactor;
        int friends = 0;


        //if the host is not in the connection history
        for (DTNHost otherHost : connectionHistory.keySet()) {
            //if the other host is in the connection history
            if (connectionHistory.get(otherHost).contains(host)) {
                //update the PeopleRank of the host
                sum += peopleRankUpdate.get(otherHost).getFirst() / connectionHistory.get(otherHost).size();//update the PeopleRank of the host
                friends++;
            }
        }
        //if the host is not in the connection history
        peopleRank = (1 - dampingFactor) + dampingFactor * sum;//calculate the PeopleRank

        return peopleRank;
    }

    @Override
    public PeopleRankRouting replicate() {//replicate the PeopleRankRouting
        return new PeopleRankRouting(this);
    }
       


}
