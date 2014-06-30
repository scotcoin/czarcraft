package nxt.http;

import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPeer extends APIServlet.APIRequestHandler {

    static final GetPeer instance = new GetPeer();

    private GetPeer() {
        super("peer");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String peerAddress = req.getParameter("peer");
        if (peerAddress == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_PEER");
        }

        Peer peer = Peers.getPeer(peerAddress);
        if (peer == null) {
            return JSONI18NResponses.getErrorResponse("UNKNOWN_PEER");
        }

        return JSONData.peer(peer);

    }

}
