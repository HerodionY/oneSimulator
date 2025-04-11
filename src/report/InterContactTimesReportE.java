// package report;

// import core.DTNHost;
// public class InterContactTimesReportE extends ContactTimesReport {

//     private double totalInterContactTime = 0.0;
//     private int interContactCount = 0;

//     @Override
//     public void hostsConnected(DTNHost host1, DTNHost host2) {
//         ConnectionInfo ci = this.removeConnection(host1, host2);

//         if (ci != null) { // connected again
//             newEvent();
//             ci.connectionEnd();
//             double interContact = ci.getConnectionTime();
//             increaseTimeCount(interContact);

//             // Tambahan: hitung total dan jumlah
//             totalInterContactTime += interContact;
//             interContactCount++;
//         }
//     }

//     @Override
//     public void hostsDisconnected(DTNHost host1, DTNHost host2) {
//         if (isWarmup()) {
//             return;
//         }
//         // start counting time to next connection
//         this.addConnection(host1, host2);
//     }

//     public double getAverageInterContactTime() {
//         if (interContactCount == 0) {
//             return 0.0;
//         }
//         return totalInterContactTime / interContactCount;
//     }


//     // @Override
//     // public void done() {
//     //     // Tetap tulis distribusi waktu ke file lewat parent method
//     //     super.done();

//     //     // Output tambahan hanya ke console (tidak ke file)
//     //     System.out.println("========== InterContactTimesReportE ==========");
//     //     System.out.println("Total Inter-Contact Events : " + interContactCount);
//     //     System.out.println("Average Inter-Contact Time : " + getAverageInterContactTime() + " seconds");
//     // }

    


// }

package report;

import java.util.HashMap;

import core.DTNHost;

public class InterContactTimesReportE extends ContactTimesReportE {

    private double totalInterContactTime = 0.0;
    private int interContactCount = 0;

    // Menyimpan waktu terakhir disconnected untuk tiap pasangan host
    private HashMap<String, Double> lastDisconnectedTime = new HashMap<>();

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        String pairKey = getPairKey(host1, host2);
        Double lastTime = lastDisconnectedTime.get(pairKey);

        if (lastTime != null && !isWarmup()) {
            double now = getSimTime();
            double interContact = now - lastTime;

            // Tambahkan ke distribusi waktu
            increaseTimeCount(interContact);

            // Tambahkan ke statistik
            totalInterContactTime += interContact;
            interContactCount++;
        }

        // Hapus entry karena sudah terhubung kembali
        lastDisconnectedTime.remove(pairKey);
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        if (isWarmup()) return;

        String pairKey = getPairKey(host1, host2);
        double now = getSimTime();

        // Simpan waktu putus koneksi
        lastDisconnectedTime.put(pairKey, now);
        
    }

    public double getAverageInterContactTime() {
        if (interContactCount == 0) return 0.0;
        return totalInterContactTime / interContactCount;
    }

    @Override
    public void done() {
        super.done();
        System.out.println("========= InterContactTimesReportE =========");
        System.out.println("Total Inter-Contact Events : " + interContactCount);
        System.out.println("Average Inter-Contact Time : " + getAverageInterContactTime() + " seconds");
    }

    private String getPairKey(DTNHost h1, DTNHost h2) {
        return h1.getAddress() < h2.getAddress()
            ? h1 + "-" + h2
            : h2 + "-" + h1;
    }
}

