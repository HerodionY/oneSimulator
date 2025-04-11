package report;

import java.util.HashMap;
import java.util.Vector;

import core.ConnectionListener;
import core.DTNHost;
import core.Settings;

/**
 * Reports the node inter-contact time distribution.
 * Report file contains the count of inter-contact intervals 
 * that lasted for a certain amount of time. Syntax:<br>
 * <code>time nrofEvents</code>
 * 
 * Also prints total and average inter-contact time.
 */

public class ContactTimesReportE extends Report implements ConnectionListener {
    protected HashMap<ConnectionInfo, ConnectionInfo> connections;
    private Vector<Integer> nrofContacts;
    private Vector<Double> interContactTimes = new Vector<>();
	private Vector<String> interContactLogs = new Vector<>();


    
    /** Granularity -setting id ({@value}). Defines how many simulated seconds
     * are grouped in one reported interval. */
    public static final String GRANULARITY = "granularity";
    /** How many seconds are grouped in one group */
    protected double granularity;

    // Untuk inter-contact time
    private HashMap<String, Double> lastDisconnectedTime = new HashMap<>();
    private double totalInterContactTime = 0.0;
    private int interContactCount = 0;

    
    /**
     * Constructor.
     */
    public ContactTimesReportE() {
        Settings settings = getSettings();
        if (settings.contains(GRANULARITY)) {
            this.granularity = settings.getDouble(GRANULARITY);
        }
        else {
            this.granularity = 1.0;
        }

        init();
    }
    
    @Override
    protected void init() {
        super.init();
        this.connections = new HashMap<ConnectionInfo,ConnectionInfo>();
        this.nrofContacts = new Vector<Integer>();
    }

    public void hostsConnected(DTNHost host1, DTNHost host2) {
    if (isWarmup()) {
        return;
    }

    String key = getHostPairKey(host1, host2);
    Double lastDisconnect = lastDisconnectedTime.get(key);

    if (lastDisconnect != null) {
        double now = getSimTime();
        double interContactTime = now - lastDisconnect;

        increaseTimeCount(interContactTime); // histogram
        interContactTimes.add(interContactTime); // simpan ke list

        totalInterContactTime += interContactTime;
        interContactCount++;

        String logMessage = "Inter-contact event at " + now + 
            "s | Inter-contact time: " + interContactTime + 
            "s | Avg: " + String.format("%.3f", getAverageInterContactTime()) + "s";

        interContactLogs.add(logMessage);
        System.out.println(logMessage);
    }

    lastDisconnectedTime.remove(key);

    addConnection(host1, host2);
    System.out.println("Connected: " + host1 + " <-> " + host2 + " at " + getSimTime());
}





    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
		return;
	}
	// simpan waktu disconnect
	String key = getHostPairKey(host1, host2);
	lastDisconnectedTime.put(key, getSimTime());
    System.out.println("Disconnected: " + host1 + " <-> " + host2 + " at " + getSimTime());

    }

    public void addConnection(DTNHost host1, DTNHost host2) {
        ConnectionInfo ci = new ConnectionInfo(host1, host2);
        connections.put(ci, ci);
    }
    public ConnectionInfo removeConnection(DTNHost host1, DTNHost host2) {
        ConnectionInfo ci = new ConnectionInfo(host1, host2);
        return connections.remove(ci);
    }

    /**
	 * Increases the amount of times a certain time value has been seen.
	 * @param time The time value that was seen
	 */
	protected void increaseTimeCount(double time) {
		int index = (int)(time/this.granularity);
		
		if (index >= this.nrofContacts.size()) {
			/* if biggest index so far, fill array with nulls up to 
			  index+2 to keep the last time count always zero */
			this.nrofContacts.setSize(index + 2);
		}
		
		Integer curValue = this.nrofContacts.get(index); 
		if (curValue == null) { // no value found -> put the first
			this.nrofContacts.set(index, 1); 
		}
		else { // value found -> increase the number by one
			this.nrofContacts.set(index, curValue+1);
		}
	}

            /**
         * Returns the average inter-contact time.
         * @return Average inter-contact time, or 0.0 if no data
         */
        private double getAverageInterContactTime() {
            if (interContactCount == 0) {
                return 0.0;
            }
            return totalInterContactTime / interContactCount;
        }


    private String getHostPairKey(DTNHost h1, DTNHost h2) {
	// int id1 = h1.getAddress();
	// int id2 = h2.getAddress();
	// return (id1 < id2) ? (id1 + "-" + id2) : (id2 + "-" + id1);
	return h1.getAddress() < h2.getAddress()
            ? h1 + "-" + h2
            : h2 + "-" + h1;
}

	
	@Override
        public void done() {
            // write("# Inter-Contact Time Distribution (interval -> number of events)");
            // for (int i = 0, n = this.nrofContacts.size(); i < n; i++) {
            //     Integer contacts = nrofContacts.get(i); 
            //     if (contacts == null) {
            //         contacts = 0;
            //     }
            //     write((i * this.granularity) + " " + contacts+ this.interContactTimes + " " + String.format("%.3f", getAverageInterContactTime()));
            // }

            // write("#");
            // write("# Total Inter-Contact Events: " + interContactCount);
            // write("# Average Inter-Contact Time: " + String.format("%.3f", getAverageInterContactTime()));
			write("#");
			write("# Inter-Contact Event Logs:");
			for (String log : interContactLogs) {
				write(log);
			}

			

            super.done();
        }


	
	/**
	 * Objects of this class store time information about contacts.
	 */
	protected class ConnectionInfo {
		private double startTime;
		private double endTime;
		private DTNHost h1;
		private DTNHost h2;
		
		public ConnectionInfo (DTNHost h1, DTNHost h2){
			this.h1 = h1;
			this.h2 = h2;
			this.startTime = getSimTime();
			this.endTime = -1;
		}
		
		/**
		 * Should be called when the connection ended to record the time.
		 * Otherwise {@link #getConnectionTime()} will use end time as
		 * the time of the request.
		 */
		public void connectionEnd() {
			this.endTime = getSimTime();
		}
		
		/**
		 * Returns the time that passed between creation of this info 
		 * and call to {@link #connectionEnd()}. Unless connectionEnd() is 
		 * called, the difference between start time and current sim time
		 * is returned.
		 * @return The amount of simulated seconds passed between creation of
		 * this info and calling connectionEnd()
		 */
		public double getConnectionTime() {
			if (this.endTime == -1) {
				return getSimTime() - this.startTime;
			}			
			else {
				return this.endTime - this.startTime;
			}
		}
		
		/**
		 * Returns true if the other connection info contains the same hosts. 
		 */
		public boolean equals(Object other) {
			if (!(other instanceof ConnectionInfo)) {
				return false;
			}
			
			ConnectionInfo ci = (ConnectionInfo)other;

			if ((h1 == ci.h1 && h2 == ci.h2)) {
				return true;
			}
			else if ((h1 == ci.h2 && h2 == ci.h1)) {
				// bidirectional connection the other way
				return true;
			}
			return false;
		}
		
		/**
		 * Returns the same hash for ConnectionInfos that have the
		 * same two hosts.
		 * @return Hash code
		 */
		public int hashCode() {
			String hostString;

			if (this.h1.compareTo(this.h2) < 0) {
				hostString = h1.toString() + "-" + h2.toString();
			}
			else {
				hostString = h2.toString() + "-" + h1.toString();
			}
			
			return hostString.hashCode();
		}
		
		/**
		 * Returns a string representation of the info object
		 * @return a string representation of the info object
		 */
		public String toString() {
			return this.h1 + "<->" + this.h2 + " [" + this.startTime
				+"-"+ (this.endTime >0 ? this.endTime : "n/a") + "]";
		}
	}




}
