/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.UpdateListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class ReportContactIntervalTime extends Report implements MessageListener, ConnectionListener, UpdateListener {
	public static final String contact_Interval = "contactInterval";
	public static final int Default_Contact_Interval = 100;

	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times
    private Map<Integer, String> crNode;
	
	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
    private int nrofContact;
	private int interval;
	private int intervalContact;
	private int updateConnection;
	
	

	
	/**
	 * Constructor.
	 */
	public ReportContactIntervalTime() {
		init();
		
		
	}

	@Override
	protected void init() {
		
		
		super.init();
		this.creationTimes = new HashMap<String, Double>();
		this.latencies = new ArrayList<Double>();
		this.msgBufferTime = new ArrayList<Double>();
		this.hopCounts = new ArrayList<Integer>();
		this.rtt = new ArrayList<Double>();
		

		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
        this.nrofContact = 0;
		this.updateConnection = 0;
		this.intervalContact = 1000;
	}

	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}
		
		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}

	
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}
		
		this.nrofAborted++;
	}

	
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
		          boolean finalTarget) {
       if (isWarmupID(m.getId())) {
            return;
        }

        this.nrofRelayed++;
        if (finalTarget) {
            this.latencies.add(getSimTime() - this.creationTimes.get(m.getId()));
            this.nrofDelivered++;
            this.hopCounts.add(m.getHops().size() - 1);

            if (m.isResponse()) {
                this.rtt.add(getSimTime() - m.getRequest().getCreationTime());
                this.nrofResponseDelivered++;
            }
        }
			
        

	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		
		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
	}
	
	
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}
    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
		this.nrofContact++;
        
    }

   

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        // implementation goes here
    }

    @Override
	public void updated(List<DTNHost> hosts) {        
    if (this.nrofContact - this.updateConnection >= this.intervalContact){ // Cek setiap kelipatan 100
        	double overHead = 0;
			double deliveryProb = 0;
			int forward = this.nrofRelayed;//total forward
			int contact_box = this.nrofContact;
			getAverage(this.latencies);
			if (this.nrofDelivered > 0) {
				overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) / this.nrofDelivered;
			}
			if (this.nrofCreated > 0) {
				deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
			}
			write(contact_box+","+deliveryProb+","+overHead+","+getAverage(this.latencies)+","+forward);
			this.updateConnection = this.nrofContact;
    }
}

// public void updated(List<DTNHost> hosts ) {
// 		if (this.contact - this.updateConnection >= this.connection){
// 			double overHead = 0;
// 			double deliveryProb = 0;
// 			int forward = this.nrofRelayed;//total forward
// 			int contact_box = this.contact;
// 			getAverage(this.latencies);
// 			if (this.nrofDelivered > 0) {
// 				overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
// 						this.nrofDelivered;
// 			}
// 			if (this.nrofCreated > 0) {
// 				deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
// 			}
// 			write(contact_box+","+deliveryProb+","+overHead+","+getAverage(this.latencies)+","+forward);
// 			this.updateConnection = this.contact;

// 		}

// 	}

	

	@Override
	public void done() {
		// write("Message stats for scenario " + getScenarioName() + 
		// 		"\nsim_time: " + format(getSimTime()));
		// double deliveryProb = 0; // delivery probability
		// double responseProb = 0; // request-response success probability
		// double overHead = Double.NaN;	// overhead ratio
		
		// if (this.nrofCreated > 0) {
		// 	deliveryProb = (1.0 * this.nrofDelivered) / this.nrofCreated;
		// }
		// if (this.nrofDelivered > 0) {
		// 	overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) /
		// 		this.nrofDelivered;
		// }
		// if (this.nrofResponseReqCreated > 0) {
		// 	responseProb = (1.0* this.nrofResponseDelivered) / 
		// 		this.nrofResponseReqCreated;
		// }
		
		// String statsText = "created: " + this.nrofCreated + 
		// 	"\nstarted: " + this.nrofStarted + 
		// 	"\nrelayed: " + this.nrofRelayed +
		// 	"\naborted: " + this.nrofAborted +
		// 	"\ndropped: " + this.nrofDropped +
		// 	"\nremoved: " + this.nrofRemoved +
		// 	"\ndelivered: " + this.nrofDelivered +
		// 	"\ndelivery_prob: " + format(deliveryProb) +
		// 	"\nresponse_prob: " + format(responseProb) + 
		// 	"\noverhead_ratio: " + format(overHead) + 
		// 	"\nlatency_avg: " + getAverage(this.latencies) +
		// 	"\nlatency_med: " + getMedian(this.latencies) + 
		// 	"\nhopcount_avg: " + getIntAverage(this.hopCounts) +
		// 	"\nhopcount_med: " + getIntMedian(this.hopCounts) + 
		// 	"\nbuffertime_avg: " + getAverage(this.msgBufferTime) +
		// 	"\nbuffertime_med: " + getMedian(this.msgBufferTime) +
		// 	"\nrtt_avg: " + getAverage(this.rtt) +
		// 	"\nrtt_med: " + getMedian(this.rtt)
		// 	;

        // this.write(statsText);

		
		super.done();
	}
	
}




