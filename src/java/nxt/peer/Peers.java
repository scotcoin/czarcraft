package nxt.peer;

import nxt.Account;
import nxt.Nxt;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.DoSFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public final class Peers {

    public static enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static final int communicationLoggingMask;

    static final Set<String> wellKnownPeers;
    static final int connectTimeout;
    static final int readTimeout;
    static final int blacklistingPeriod;

    private static final int DEFAULT_PEER_PORT = 7874;
    private static final String myPlatform;
    private static final String myAddress;
    private static final int myPeerPort;
    private static final String myHallmark;
    private static final boolean shareMyAddress;
    private static final int maxNumberOfConnectedPublicPeers;
    private static final boolean enableHallmarkProtection;
    private static final int pushThreshold;
    private static final int pullThreshold;
    private static final int sendToPeersLimit;

    static final JSONStreamAware myPeerInfoRequest;
    static final JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);

    static {

        myPlatform = Nxt.getStringProperty("nxt.myPlatform", "PC");
        myAddress = Nxt.getStringProperty("nxt.myAddress", null);
        myPeerPort = Nxt.getIntProperty("nxt.peerServerPort", 7874);
        shareMyAddress = Nxt.getBooleanProperty("nxt.shareMyAddress", true);
        myHallmark = Nxt.getStringProperty("nxt.myHallmark", null);
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (! hallmark.isValid()) {
                    throw new RuntimeException();
                }
            } catch (RuntimeException e) {
                Logger.logMessage("Your hallmark is invalid: " + Peers.myHallmark);
                throw e;
            }
        }

        JSONObject json = new JSONObject();
        if (Peers.myAddress != null && Peers.myAddress.length() > 0) {
            json.put("announcedAddress", Peers.myAddress + (Peers.myPeerPort != Peers.DEFAULT_PEER_PORT ? ":" + Peers.myPeerPort : ""));
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
        }
        json.put("application", "NRS");
        json.put("version", Nxt.VERSION);
        json.put("platform", Peers.myPlatform);
        json.put("shareAddress", Peers.shareMyAddress);
        myPeerInfoResponse = JSON.prepare(json);
        json.put("requestType", "getInfo");
        myPeerInfoRequest = JSON.prepareRequest(json);

        String wellKnownPeersString = Nxt.getStringProperty("nxt.wellKnownPeers", null);
        Set<String> addresses = new HashSet<>();
        if (wellKnownPeersString != null && wellKnownPeersString.length() > 0) {
            for (String address : wellKnownPeersString.split(";")) {
                address = address.trim();
                if (address.length() > 0) {
                    addresses.add(address);
                }
            }
        } else {
            Logger.logMessage("No wellKnownPeers defined, using random nxtcrypto.org and nxtbase.com nodes");
            for (int i = 1; i <= 12; i++) {
                if (ThreadLocalRandom.current().nextInt(4) == 1) {
                    addresses.add("vps" + i + ".nxtcrypto.org");
                }
            }
            for (int i = 1; i <= 99; i++) {
                if (ThreadLocalRandom.current().nextInt(10) == 1) {
                    addresses.add("node" + i + ".nxtbase.com");
                }
            }
        }
        wellKnownPeers = Collections.unmodifiableSet(addresses);

        maxNumberOfConnectedPublicPeers = Nxt.getIntProperty("nxt.maxNumberOfConnectedPublicPeers", 20);
        connectTimeout = Nxt.getIntProperty("nxt.connectTimeout", 2000);
        readTimeout = Nxt.getIntProperty("nxt.readTimeout", 5000);
        enableHallmarkProtection = Nxt.getBooleanProperty("nxt.enableHallmarkProtection", true);
        pushThreshold = Nxt.getIntProperty("nxt.pushThreshold", 0);
        pullThreshold = Nxt.getIntProperty("nxt.pullThreshold", 0);

        blacklistingPeriod = Nxt.getIntProperty("nxt.blacklistingPeriod", 300000);
        communicationLoggingMask = Nxt.getIntProperty("nxt.communicationLoggingMask", 0);
        sendToPeersLimit = Nxt.getIntProperty("nxt.sendToPeersLimit", 10);

        StringBuilder buf = new StringBuilder();
        for (String address : wellKnownPeers) {
            Peer peer = Peers.addPeer(address);
            if (peer != null) {
                buf.append(peer.getPeerAddress()).append("; ");
            }
        }
        Logger.logDebugMessage("Well known peers: " + buf.toString());

    }

    private static class Init {
        
        static {
            if (Peers.shareMyAddress) {
                try {
                    Server peerServer = new Server(Peers.myPeerPort);
                    ServletHandler peerHandler = new ServletHandler();
                    peerHandler.addServletWithMapping(PeerServlet.class, "/*");
                    FilterHolder filterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
                    filterHolder.setInitParameter("maxRequestsPerSec", Nxt.getStringProperty("dosfilter.maxRequestsPerSec", "30"));
                    filterHolder.setInitParameter("delayMs", Nxt.getStringProperty("dosfilter.delayMs", "1000"));
                    filterHolder.setInitParameter("trackSessions", "false");
                    filterHolder.setAsyncSupported(true);

                    peerServer.setHandler(peerHandler);
                    peerServer.setStopAtShutdown(true);
                    peerServer.start();
                    Logger.logMessage("Started peer networking server at port: " + Peers.myPeerPort);
                } catch (Exception e) {
                    Logger.logDebugMessage("Failed to start peer networking server", e);
                    throw new RuntimeException(e.toString(), e);
                }
            } else {
                Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
            }
        }

        private static void init() {}

        private Init() {}

    }

    private static final Runnable peerUnBlacklistingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    long curTime = System.currentTimeMillis();
                    for (PeerImpl peer : peers.values()) {
                        peer.updateBlacklistedStatus(curTime);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error un-blacklisting peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable peerConnectingThread = new Runnable() {

        @Override
        public void run() {

            try {
                try {

                    if (getNumberOfConnectedPublicPeers() < Peers.maxNumberOfConnectedPublicPeers) {
                        PeerImpl peer = (PeerImpl)getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
                        if (peer != null) {
                            peer.connect();
                        }
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error connecting to peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    private static final Runnable getMorePeersThread = new Runnable() {

        private final JSONStreamAware getPeersRequest;
        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getPeers");
            getPeersRequest = JSON.prepareRequest(request);
        }

        @Override
        public void run() {

            try {
                try {

                    Peer peer = getAnyPeer(Peer.State.CONNECTED, true);
                    if (peer == null) {
                        return;
                    }
                    JSONObject response = peer.send(getPeersRequest);
                    if (response == null) {
                        return;
                    }
                    JSONArray peers = (JSONArray)response.get("peers");
                    for (Object announcedAddress : peers) {
                        addPeer((String) announcedAddress);
                    }

                } catch (Exception e) {
                    Logger.logDebugMessage("Error requesting peers from a peer", e);
                }
            } catch (Throwable t) {
                Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };

    static {
        Account.addListener(new Listener<Account>() {
            @Override
            public void notify(Account account) {
                for (PeerImpl peer : Peers.peers.values()) {
                    if (peer.getHallmark() != null && peer.getHallmark().getAccountId().equals(account.getId())) {
                        Peers.listeners.notify(peer, Peers.Event.WEIGHT);
                    }
                }
            }
        }, Account.Event.BALANCE);
    }

    static {
        ThreadPool.scheduleThread(Peers.peerConnectingThread, 5);
        ThreadPool.scheduleThread(Peers.peerUnBlacklistingThread, 1);
        ThreadPool.scheduleThread(Peers.getMorePeersThread, 5);
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        ThreadPool.shutdownExecutor(sendToPeersService);
    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static Peer getPeer(String peerAddress) {
        return peers.get(peerAddress);
    }

    public static Peer addPeer(String announcedAddress) {
        return addPeer(announcedAddress, announcedAddress);
    }

    static PeerImpl addPeer(final String address, final String announcedAddress) {

        String peerAddress = normalizeHostAndPort(address);
        if (peerAddress == null) {
            return null;
        }

        String announcedPeerAddress = normalizeHostAndPort(announcedAddress);

        if (Peers.myAddress != null && Peers.myAddress.length() > 0 && Peers.myAddress.equalsIgnoreCase(announcedPeerAddress)) {
            return null;
        }

        if (announcedPeerAddress != null) {
            peerAddress = announcedPeerAddress;
        }

        PeerImpl peer = peers.get(peerAddress);
        if (peer == null) {
            peer = new PeerImpl(peerAddress, announcedPeerAddress);
            peers.put(peerAddress, peer);
        }

        return peer;
    }

    static PeerImpl removePeer(PeerImpl peer) {
        return peers.remove(peer.getPeerAddress());
    }

    public static void sendToSomePeers(final JSONObject request) {

        final JSONStreamAware jsonRequest = JSON.prepareRequest(request);

        int successful = 0;
        List<Future<JSONObject>> expectedResponses = new ArrayList<>();
        for (final Peer peer : peers.values()) {

            if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                continue;
            }

            if (! peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                Future<JSONObject> futureResponse = sendToPeersService.submit(new Callable<JSONObject>() {
                    @Override
                    public JSONObject call() {
                        return peer.send(jsonRequest);
                    }
                });
                expectedResponses.add(futureResponse);
            }
            if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                for (Future<JSONObject> future : expectedResponses) {
                    try {
                        JSONObject response = future.get();
                        if (response != null && response.get("error") == null) {
                            successful += 1;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        Logger.logDebugMessage("Error in sendToSomePeers", e);
                    }

                }
                expectedResponses.clear();
            }
            if (successful >= Peers.sendToPeersLimit) {
                return;
            }

        }

    }

    public static Peer getAnyPeer(Peer.State state, boolean applyPullThreshold) {

        List<Peer> selectedPeers = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (! peer.isBlacklisted() && peer.getState() == state && peer.getAnnouncedAddress() != null
                    && (!applyPullThreshold || ! Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold)) {
                selectedPeers.add(peer);
            }
        }

        if (selectedPeers.size() > 0) {
            long totalWeight = 0;
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                totalWeight += weight;
            }

            long hit = ThreadLocalRandom.current().nextLong(totalWeight);
            for (Peer peer : selectedPeers) {
                long weight = peer.getWeight();
                if (weight == 0) {
                    weight = 1;
                }
                if ((hit -= weight) < 0) {
                    return peer;
                }
            }
        }
        return null;
    }

    static String normalizeHostAndPort(String address) {
        try {
            if (address == null) {
                return null;
            }
            URI uri = new URI("http://" + address.trim());
            String host = uri.getHost();
            if (host == null || host.equals("") || host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0:0:0:0:0:0:0:1")) {
                return null;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
                return null;
            }
            int port = uri.getPort();
            return port == -1 ? host : host + ':' + port;
        } catch (URISyntaxException |UnknownHostException e) {
            return null;
        }
    }

    private static int getNumberOfConnectedPublicPeers() {
        int numberOfConnectedPeers = 0;
        for (Peer peer : peers.values()) {
            if (peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                numberOfConnectedPeers++;
            }
        }
        return numberOfConnectedPeers;
    }

    private Peers() {} // never

}