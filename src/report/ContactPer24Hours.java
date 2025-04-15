package report;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import core.ConnectionListener;
import core.DTNHost;
import core.SimClock;

/**
 * Logs unique contacts per 24 hours for specific nodes (28, 56, 95)
 * and tracks the global popularity (interactions) of all nodes.
 */

public class ContactPer24Hours extends Report implements ConnectionListener {
    private LinkedList<Integer> contactCounts;
	private int currentHourCount;
	private int currentHour;

	// Set of nodes to track specifically
	private final Set<Integer> targetNodes = Set.of(28, 56, 95);

	// Format: node -> set of nodes it interacted with (for target nodes)
	private Map<Integer, Set<Integer>> connectionsPerTargetNode;

	// Global popularity: node -> total number of interactions
	private Map<Integer, Integer> globalPopularity;

	public ContactPer24Hours() {
		init();
	}

	@Override
	public void init() {
		super.init();
		contactCounts = new LinkedList<>();
		currentHour = 0;
		currentHourCount = 0;
		connectionsPerTargetNode = new HashMap<>();
		globalPopularity = new HashMap<>();

		// Initialize connections and global popularity maps
		for (int nodeId : targetNodes) {
			connectionsPerTargetNode.put(nodeId, new TreeSet<>());
		}
	}

	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		int timeInHours = SimClock.getIntTime() / 3600;

		// Update hourly contact count and check for new day
		while (timeInHours > currentHour) {
			contactCounts.add(currentHourCount);
			currentHourCount = 0;

			// Print daily data when a new day starts
			if (currentHour > 0 && currentHour % 24 == 0) {
				printDailyConnections(currentHour / 24);
				resetDailyConnections();
			}
			currentHour++;
		}

		currentHourCount++;

		// Get the IDs of both hosts
		int id1 = host1.getAddress();
		int id2 = host2.getAddress();

		// Update the global popularity
		updateGlobalPopularity(id1);
		updateGlobalPopularity(id2);

		// Add connections for the target nodes (28, 56, 95)
		if (targetNodes.contains(id1)) {
			connectionsPerTargetNode.get(id1).add(id2);
		} else if (targetNodes.contains(id2)) {
			connectionsPerTargetNode.get(id2).add(id1);
		}
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// no-op
	}

	@Override
	public void done() {
		// Print final day data
		if (!isDailyConnectionEmpty()) {
			printDailyConnections((currentHour / 24) + 1);
		}

		// Print hourly contact data
		int hour = 0;
		for (Integer count : contactCounts) {
			write("Hour " + hour + ":\t" + count + " contacts");
			hour++;
		}

		// Print global popularity of nodes
		write("\nGlobal Popularity (Total Interactions):");
		for (Map.Entry<Integer, Integer> entry : globalPopularity.entrySet()) {
			write("Node " + entry.getKey() + " interacted with " + entry.getValue() + " other nodes.");
		}

		super.done();
	}

	private void printDailyConnections(int day) {
		write("=== HARI KE-" + day + " ===");
		int totalPairs = 0;

		// Print the unique interactions for each target node
		for (int nodeId : targetNodes) {
			Set<Integer> contacts = connectionsPerTargetNode.get(nodeId);
			if (!contacts.isEmpty()) {
				write("Node " + nodeId + ":");
				for (int other : contacts) {
					write("  - " + other);
					totalPairs++;
				}
			}
		}

		write("Total pasangan unik (untuk node 28, 56, 95): " + totalPairs);
		write(""); // spacer
	}

	private void resetDailyConnections() {
		// Clear connections for the next day
		for (int nodeId : targetNodes) {
			connectionsPerTargetNode.get(nodeId).clear();
		}
	}

	private boolean isDailyConnectionEmpty() {
		// Check if any of the target nodes have connections
		for (Set<Integer> s : connectionsPerTargetNode.values()) {
			if (!s.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	// Update the global popularity of a node
	private void updateGlobalPopularity(int nodeId) {
		globalPopularity.put(nodeId, globalPopularity.getOrDefault(nodeId, 0) + 1);
	}

}


