package routing;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import routing.community.Duration;

public interface CentralityKu {

    public double [] getGlobalArrayCentrality (Map<DTNHost, List<Duration>> connHistory);

    


}
