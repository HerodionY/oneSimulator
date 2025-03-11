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
 * <P>
 * <strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class ReportMessageContact extends Report implements MessageListener, ConnectionListener, UpdateListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times

	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;
	private int totalContacts;
	private int intervalContacts;
	private int lastUpdatedContacts;

	/**
	 * Constructor.
	 */
	public ReportMessageContact() {
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

		this.totalContacts = 0;
		this.lastUpdatedContacts = 0;
		this.intervalContacts = 1000;
	}

	
	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}

		if (dropped) {
			this.nrofDropped++;
		} else {
			this.nrofRemoved++;
		}

		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}

	@Override
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofAborted++;
	}

	@Override
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			this.latencies.add(getSimTime() -
					this.creationTimes.get(m.getId()));
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);

			if (m.isResponse()) {
				this.rtt.add(getSimTime() - m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
	}


	
	@Override
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

	@Override
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}

	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (isWarmup()) {
			return;
		}
		this.totalContacts++;
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updated(List<DTNHost> hosts) {
		if ((this.totalContacts - this.lastUpdatedContacts) >= this.intervalContacts) {
			double deliveryRatio = 0;
			double overheadRatio = 0;
			double avglatency = 0;

			if (this.totalContacts > 0) {
				deliveryRatio = (1.0 * this.nrofDelivered) / this.nrofCreated;
			}
			if (this.nrofDelivered > 0) {
				overheadRatio = (1.0 * (this.nrofRelayed - this.nrofDelivered)) / this.nrofDelivered;
			}
			if (this.latencies.isEmpty()) {
				avglatency = Double.parseDouble(getAverage(this.latencies));
			}

			write(this.totalContacts + "," + deliveryRatio + "," + overheadRatio + "," + avglatency + ","
					+ this.nrofRelayed);

			this.lastUpdatedContacts = this.totalContacts;
		}

	}

	@Override
	public void done() {
		super.done();
	}

}