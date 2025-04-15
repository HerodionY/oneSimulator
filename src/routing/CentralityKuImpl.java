package routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.DTNHost;
import core.SimClock;
import routing.community.Duration;
public class CentralityKuImpl implements CentralityKu {

    @Override
    public double[] getGlobalArrayCentrality(Map<DTNHost, List<Duration>> connHistory) {
        // Ambil waktu saat ini
        int timeNow = SimClock.getIntTime();
        int timeWindow = 86400; // 24 jam dalam detik
        
        // Cari waktu yang paling lama dari semua koneksi
        int maxTimePassed = 0;
        for (List<Duration> durations : connHistory.values()) {
            for (Duration d : durations) {
                int timePassed = timeNow - (int) d.end;
                if (timePassed > maxTimePassed) {
                    maxTimePassed = timePassed;
                }
            }
        }
        
        // Tentukan jumlah epoch berdasarkan waktu yang telah berlalu
        int totalEpochs = (maxTimePassed / timeWindow) + 1;
        double[] centralities = new double[totalEpochs];
        double[] popularity = new double[totalEpochs];
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

        // Inisialisasi struktur data untuk mencatat node yang dihitung pada setiap epoch
        for (int i = 0; i < totalEpochs; i++) {
            nodesCountedInEpoch.put(i, new HashSet<>());
        }

        // Iterasi untuk setiap host dan durasi koneksinya
        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();
            for (Duration d : entry.getValue()) {
                int timePassed = timeNow - (int) d.end;
                int epoch = timePassed / timeWindow;

                // Tambahkan host ke epoch yang sesuai jika belum dihitung
                Set<DTNHost> countedNodes = nodesCountedInEpoch.get(epoch);
                if (countedNodes.add(h)) {
                    centralities[epoch]++;
                    popularity[epoch] += (d.end - d.start);  // Menambahkan durasi koneksi
                }
            }
        }

        // Hitung centrality untuk setiap epoch berdasarkan popularitas dan jumlah node
        for (int i = 0; i < totalEpochs; i++) {
            if (centralities[i] > 0) {
                centralities[i] = popularity[i] / centralities[i];
            }
        }

        return centralities;
    }

}





