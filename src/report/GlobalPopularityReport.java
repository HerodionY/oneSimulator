package report;

import java.util.List;

import core.DTNHost;
import core.SimScenario;
import routing.BubbleRap;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;




/**
 * Logs total interactions per 24 hours for each node and generates a global popularity report.
 */
public class GlobalPopularityReport extends Report {
    public GlobalPopularityReport() {
        init();
    }

    @Override
    public void done() {
        // Ambil semua host dari simulasi
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();
        
        // Loop melalui semua node untuk mendapatkan data centrality
        for (DTNHost h : nodes) {
            // Cek jika router adalah DecisionEngineRouter
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            
            // Ambil objek BubbleRap (RoutingDecisionEngine)
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            if (!(de instanceof BubbleRap)) {
                continue;
            }
            
            BubbleRap bubbleRap = (BubbleRap) de;
            
            // Ambil array global centrality dari BubbleRap
            double[] centralities = bubbleRap.getGlobalArrayCentralityPublic();
            
            // Tulis hasilnya ke dalam laporan
            write("\n=== Global Centrality Array for Host " + h.getAddress() + " ===");
            for (int i = 0; i < centralities.length; i++) {
                write("Epoch " + i + ": " + centralities[i]);
            }
        }
        
        super.done();
    }

}
