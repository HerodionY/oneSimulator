package routing;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import routing.community.Duration;

/**
 * CentralityKu interface defines the method to calculate global array centrality
 * for a given connection history of DTN hosts.
 * 
 * @author Herodion 
 */
public interface CentralityKu {

    public double [] getGlobalArrayCentrality (Map<DTNHost, List<Duration>> connHistory);

    


}
