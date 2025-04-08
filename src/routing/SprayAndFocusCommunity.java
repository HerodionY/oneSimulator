package routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.community.AverageWinCentrality1;
import routing.community.Centrality;
import routing.community.CommunityDetection;
import routing.community.CommunityDetectionEngine;
import routing.community.Duration;
import routing.community.SimpleCommunityDetection;

public class SprayAndFocusCommunity implements RoutingDecisionEngine, CommunityDetectionEngine {

    public static final String SPRAYANDFOCUS_NS = "SprayAndFocusRouter";
    public static final String NROF_COPIES_S = "nrofCopies";
    public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
    public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";
    public static final String SUMMARY_XCHG_PROP = "SprayAndFocus.protoXchg";

    protected static final String SUMMARY_XCHG_IDPREFIX = "summary";
    protected static final double defaultTransitivityThreshold = 60.0;
    protected static final double DEFAULT_TIMEDIFF = 300;
    protected static int protocolMsgIdx = 0;

    protected int initialNrofCopies;
    protected double transitivityTimerThreshold;
    protected Map<DTNHost, Double> recentEncounters;

    public static final String COMMUNITY_ALG_SETTING = "communityDetectAlg";
    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;

    protected CommunityDetection community;
    protected Centrality centrality;

    public SprayAndFocusCommunity(Settings s) {
        Settings snf = new Settings(SPRAYANDFOCUS_NS);
        initialNrofCopies = snf.getInt(NROF_COPIES_S);
        transitivityTimerThreshold = snf.contains(TIMER_THRESHOLD_S) ? snf.getDouble(TIMER_THRESHOLD_S) : defaultTransitivityThreshold;

        if (s.contains(COMMUNITY_ALG_SETTING)) {
            this.community = (CommunityDetection) s.createIntializedObject(s.getSetting(COMMUNITY_ALG_SETTING));
        } else {
            this.community = new SimpleCommunityDetection(s);
        }

        if (s.contains(CENTRALITY_ALG_SETTING)) {
            this.centrality = (Centrality) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        } else {
            this.centrality = new AverageWinCentrality1(s);
        }

        recentEncounters = new HashMap<>();
        startTimestamps = new HashMap<>();
        connHistory = new HashMap<>();
    }

    public SprayAndFocusCommunity(SprayAndFocusCommunity r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.transitivityTimerThreshold = r.transitivityTimerThreshold;
        this.community = r.community.replicate();
        this.centrality = r.centrality.replicate();
        this.recentEncounters = new HashMap<>();
        this.startTimestamps = new HashMap<>();
        this.connHistory = new HashMap<>();
    }

    public RoutingDecisionEngine replicate() {
        return new SprayAndFocusCommunity(this);
    }

    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // Optional: connection started
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Optional: connection ended
        // double time = startTimestamps.get(peer);
        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        // add this connection to the list
        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }

        CommunityDetection peerCD = this.getOtherSnFDecisionEngine(peer).community; // added
        community.connectionLost(thisHost, peer, peerCD, history); // added

        startTimestamps.remove(peer);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            startTimestamps.get(peer);
        }
        return 0;
    }

    @Override
    public void update(DTNHost host) {
        // Optional periodic update logic
    }

    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        SprayAndFocusCommunity Sf = this.getOtherSnFDecisionEngine(peer);
        DTNHost thisHost = con.getOtherNode(peer);

        double distTo = thisHost.getLocation().distance(peer.getLocation());
        double speed = (peer.getPath() == null) ? 0 : peer.getPath().getSpeed();
        double myTimediff = (speed == 0) ? DEFAULT_TIMEDIFF : distTo / speed;
        double peerSpeed = speed;
        double peerTimediff = (peerSpeed == 0) ? DEFAULT_TIMEDIFF : distTo / peerSpeed;

        recentEncounters.put(peer, SimClock.getTime());
        Sf.recentEncounters.put(thisHost, SimClock.getTime());

        Set<DTNHost> hosts = new HashSet<>(recentEncounters.keySet());
        hosts.addAll(Sf.recentEncounters.keySet());

        for (DTNHost h : hosts) {
            double myTime = recentEncounters.getOrDefault(h, 0.0);
            double peerTime = Sf.recentEncounters.getOrDefault(h, 0.0);

            if (myTime + myTimediff < peerTime) {
                recentEncounters.put(h, peerTime - myTimediff);
            }
            if (peerTime + peerTimediff < myTime) {
                Sf.recentEncounters.put(h, myTime - peerTimediff);
            }
        }
    }

    public boolean isFinalDest(Message m, DTNHost host) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        m.updateProperty(MSG_COUNT_PROP, nrofCopies);
        return m.getTo() == host;
    }

    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
        return true;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost thisReportOld) {
        return m.getTo() == thisReportOld;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost host) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        if (nrofCopies > 1) {
            nrofCopies /= 2;
            m.updateProperty(MSG_COUNT_PROP, nrofCopies);
            return false;
        }
        return true;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost otherHost, DTNHost thisHost) {
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        if (nrofCopies > 1) {
            nrofCopies /= 2;
            m.updateProperty(MSG_COUNT_PROP, nrofCopies);
            return false;
        }
        return true;
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return !m.getTo().equals(thisHost);
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo().equals(otherHost)) return true;

        int numberOfCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        if (numberOfCopies > 1) return true;

        // FOCUS: only forward if otherHost has higher centrality rank
        SprayAndFocusCommunity de = this.getOtherSnFDecisionEngine(otherHost);

        double myRank = this.getLocalCentrality();
        double peerRank = de.getLocalCentrality();

        return peerRank > myRank;
    }

    private double getLocalCentrality() {
        return this.centrality.getLocalCentrality(connHistory, community);
    }

    private SprayAndFocusCommunity getOtherSnFDecisionEngine(DTNHost otherHost) {
        MessageRouter otherRouter = otherHost.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works with other routers of same type";
        return (SprayAndFocusCommunity) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public Set<DTNHost> getLocalCommunity() {
        return this.community.getLocalCommunity();
    }
}
