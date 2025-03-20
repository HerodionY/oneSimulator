package routing.community;

import java.util.Map;
import core.DTNHost;

/**
 * This class is used to rank the nodes in the network based on some criteria.
 * The criteria can be the number of neighbors, the number of packets sent, etc.
 * The ranking is used to determine the order in which the nodes are visited
 * during the routing process
 */

public interface  NodeRanking {

    public Map<DTNHost, Double> getAllRankings();

    public int getTotalFriends(DTNHost host);
    

}
