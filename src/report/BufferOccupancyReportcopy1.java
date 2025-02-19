/* 
 * 
 * 
 */
package report;

/** 
 * Records the average buffer occupancy and its variance with format:
 * <p>
 * <Simulation time> <average buffer occupancy % [0..100]> <variance>
 * </p>
 * 
 * 
 */
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.UpdateListener;

public class BufferOccupancyReportcopy1 extends Report implements UpdateListener {
	/**
	 * Record occupancy every nth second -setting id ({@value}). 
	 * Defines the interval how often (seconds) a new snapshot of buffer
	 * occupancy is taken previous:5
	 */
	public static final String BUFFER_REPORT_INTERVAL = "occupancyInterval";
	/** Default value for the snapshot interval */
	public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 3600;
	
	private double lastRecord = Double.MIN_VALUE;
	private int interval;
	
	private Map<DTNHost, Double> bufferCounts = new HashMap<DTNHost, Double>();
	private Map<DTNHost, List<Double>> bufferPerNode = new HashMap<>();
	private int updateCounter = 0;  //new added
	
	
	public BufferOccupancyReportcopy1() {
		super();
		
		Settings settings = getSettings();
		if (settings.contains(BUFFER_REPORT_INTERVAL)) {
			interval = settings.getInt(BUFFER_REPORT_INTERVAL);
		} else {
			interval = -1; /* not found; use default */
		}
		
		if (interval < 0) { /* not found or invalid value -> use default */
			interval = DEFAULT_BUFFER_REPORT_INTERVAL;
		}
	}
	
	public void updated(List<DTNHost> hosts) {
		if (isWarmup()) {
			return;
		}
		
		if (SimClock.getTime() - lastRecord >= interval) {
			lastRecord = SimClock.getTime();
			printLine(hosts);
			updateCounter++; // new added
		}
		// 	/**
		// 	for (DTNHost ho : hosts ) {
		// 		double temp = ho.getBufferOccupancy();
		// 		temp = (temp<=100.0)?(temp):(100.0);
		// 		if (bufferCounts.containsKey(ho.getAddress()))
		// 			bufferCounts.put(ho.getAddress(), (bufferCounts.get(ho.getAddress()+temp))/2);	
		// 		else
		// 		bufferCounts.put(ho.getAddress(), temp);
		// 	}
		// 	}
		// */
	}
	
	/**
	 * Prints a snapshot of the average buffer occupancy
	 * @param hosts The list of hosts in the simulation
	 */
	 
	private void printLine(List<DTNHost> hosts) {
		
		double bufferOccupancy = 0.0;
		double bo2 = 0.0;
		

		if((SimClock.getTime() - lastRecord) >= interval) {
			for (DTNHost h : SimScenario.getInstance().getHosts()) {
				if(this.bufferCounts.containsKey(h)) {
					List<Double>  buffredList = this.bufferPerNode.get(h);
					buffredList.add(h.getBufferOccupancy());
					this.bufferPerNode.put(h, buffredList);
				} else {
					List<Double> buffredList = new java.util.ArrayList<Double>();
					buffredList.add(h.getBufferOccupancy());
					this.bufferPerNode.put(h, buffredList);
				}
			}
		}
		
		double E_X = bufferOccupancy / hosts.size();
        double Var_X = bo2 / hosts.size() - (E_X * E_X) / 100.0;

        String output = format(SimClock.getTime()) + " " + format(E_X) + " " + format(Var_X);
        write(output);

        // Print buffer occupancy for each node
        for (DTNHost h : hosts) {
            double temp = h.getBufferOccupancy();
            temp = (temp <= 100.0) ? (temp) : (100.0);
            write(format(SimClock.getTime()) + " Node " + h + " Buffer Occupancy: " + format(temp));
		}
		
//		for (DTNHost h : hosts ) {
//			double temp = h.getBufferOccupancy();
//			temp = (temp<=100.0)?(temp):(100.0);
//			if (bufferCounts.containsKey(h)){
//				//bufferCounts.put(h, (bufferCounts.get(h)+temp)/2); seems WRONG
//				
//				bufferCounts.put(h, bufferCounts.get(h)+temp);
//				//write (""+ bufferCounts.get(h));
//			}
//			else {
//			bufferCounts.put(h, temp);
//			//write (""+ bufferCounts.get(h));
//			}
//		}
	}
		
	@Override
	public void done()
	{	

		String test =" ";
		for (Map.Entry<DTNHost, Double> entry : bufferCounts.entrySet()) {
			test += " " + entry.getKey() + " " + entry.getValue();
			DTNHost a = entry.getKey();
			Integer b = a.getAddress();
			// Double avgBuffer = entry.getValue()/updateCounter;
			Integer c = entry.getValue().intValue();
			
			write("" + b + " " + a  + ' ' + c);
			
			//write("" + b + ' ' + entry.getValue());
		}
		this.write(test);
		super.done();
	}
}
