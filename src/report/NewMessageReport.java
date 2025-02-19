// Source code is decompiled from a .class file using FernFlower decompiler.
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import core.UpdateListener;

public class NewMessageReport extends Report implements MessageListener, UpdateListener {

   public static final String totalContact_Interval = "perTotalContact";
   public static final int DEFAULT_CONTACT_COUNT = 500;
   private Map<DTNHost, Integer> deleteMessage;
   private Map<DTNHost, List<Double>> deleteTimes;
   private Map<Message, Integer> messageNew;
   private Map<DTNHost, Integer> droppedMessagesPerInterval;
   private Double lastRecord;
   private int interval;
   private int nrofDropped;
   private int nrofRemoved;
   private int nrofNew;

   public NewMessageReport() {
      this.init();

      Settings s = getSettings();
      if (s.contains(totalContact_Interval)) {
         this.interval = s.getInt(totalContact_Interval);
      } else {
         this.interval = DEFAULT_CONTACT_COUNT;
      }
   }

   public void init() {
      super.init();
      this.deleteMessage = new HashMap();
      this.deleteTimes = new HashMap<>();
      this.nrofDropped = 0;
      this.messageNew = new HashMap();
      this.nrofNew = 0;
      this.droppedMessagesPerInterval = new HashMap();
      this.lastRecord = Double.MIN_VALUE;
      this.interval = 0;
      
   }

   

   public void newMessage(Message var1) {
    if(this.messageNew.containsKey(var1)) {
        this.messageNew.put(var1, this.messageNew.get(var1) + 1);
    } else {
        this.messageNew.put(var1, 1);
    }
   }

   public void messageTransferStarted(Message var1, DTNHost var2, DTNHost var3) {
   }

   public void messageDeleted(Message var1, DTNHost var2, boolean var3) {
      if(var3) {
         if (this.deleteMessage.containsKey(var2)) {
         this.deleteMessage.put(var2, (Integer)this.deleteMessage.get(var2) + 1);
      } else {
         this.deleteMessage.put(var2, 1);
      }

      if(this.deleteTimes.containsKey(var2)) {
          this.deleteTimes.get(var2).add(SimClock.getTime());
      } else {
         List<Double> temp = new ArrayList<>();
         temp.add(SimClock.getTime());
         this.deleteTimes.put(var2, temp);
      }
      this.nrofDropped++;
      }

      if (SimClock.getTime() % interval == 0) {
            // Menghitung waktu awal interval
            double intervalStartTime = SimClock.getTime() - interval;

            // Melaporkan jumlah total pesan yang dihapus per host dalam interval tersebut
            reportTotalDropPerNode(intervalStartTime, getSimTime());

            // Menghapus data pesan yang dihapus dalam interval ini untuk persiapan interval
            // berikutnya
            droppedMessagesPerInterval.clear();
        }
      

   }

   private void reportTotalDropPerNode(double intervalStartTime, double intervalEndTime) {
        write("Total Dropped Message Per Node Interval " + interval + " Second");
        // Menampilkan waktu mulai interval
        write("Interval Start Time: " + intervalStartTime);
        for (DTNHost host : droppedMessagesPerInterval.keySet()) {
            write("Node || " + host + " : " + droppedMessagesPerInterval.get(host) + "|| Time : "
                    + intervalEndTime);
        }
        write("");
    }

   public void messageTransferAborted(Message var1, DTNHost var2, DTNHost var3) {
   }

   public void messageTransferred(Message var1, DTNHost var2, DTNHost var3, boolean var4) {
   }

   @Override
   public void updated(List<DTNHost> hosts) {
       double currentTime = SimClock.getTime();
        if (currentTime - this.lastRecord >= this.interval) {
            this.deleteMessage.put(this.nrofDropped, this.nrofRemoved);
            this.nrofDropped = 0;
            this.lastRecord = currentTime;
        }
   }

   public void done() {
      // this.write("Message\tPesan di Buat");
      // Iterator var12 = this.messageNew.entrySet().iterator();
      // Integer var102 = 0;
      // while(var12.hasNext()) {
      //    Map.Entry var2 = (Map.Entry)var12.next();
      //    Message var4 = (Message)var2.getKey();
      //    Integer var3 = (Integer)var2.getValue();
      //    String var101 = String.valueOf(var4);
         
      //    var102 = var102 + var3;
      //    this.write(var101 + "\t" + var3);
      // }
      // this.write("Total Message yang dibuat"+"\t"+var102);

       this.write("Interval\tHost\tPesan di Hapus");
        Integer var103 = 0;
        Iterator var1 = this.deleteMessage.entrySet().iterator();
        Iterator var101 = this.deleteTimes.entrySet().iterator();
        while (var1.hasNext() && var101.hasNext()) {
            Map.Entry var2 = (Map.Entry) var1.next();
            DTNHost var3 = (DTNHost) var2.getKey();
            Integer var4 = (Integer) var2.getValue();
            Map.Entry var5 = (Map.Entry) var101.next();
            List var6 = (List) var5.getValue();
            String var10001 = String.valueOf(var3);
            var103 = var103 + var4;
            this.write("[" + var6 + "]\t" + var10001 + "\t" + var4 + "\t");
        }
        this.write("Total Message yang dihapus" + "\t" + var103);

      //   this.write("Drop Report per Interval");
      //   Iterator var22 = this.droppedMessagesPerInterval.entrySet().iterator();
      //   for (Map.Entry<Double, Integer> entry : this.droppedMessagesPerInterval.entrySet()) {
      //       this.write("Interval [" + entry.getKey() + " - " + (entry.getKey() + this.interval) + ")\tDropped Messages: " + entry.getValue());
      //   }

        super.done();
   }
}
