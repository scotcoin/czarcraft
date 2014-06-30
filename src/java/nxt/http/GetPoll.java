package nxt.http;

import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPoll extends APIServlet.APIRequestHandler {

    static final GetPoll instance = new GetPoll();

    private GetPoll() {
        super("poll");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String poll = req.getParameter("poll");
        if (poll == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_POLL");
        }

        Poll pollData;
        try {
            pollData = Poll.getPoll(Convert.parseUnsignedLong(poll));
            if (pollData == null) {
                return JSONI18NResponses.getErrorResponse("UNKNOWN_POLL");
            }
        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_POLL");
        }

        return JSONData.poll(pollData);

    }

}
