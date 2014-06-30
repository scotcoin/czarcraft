package nxt.http;

import nxt.NxtException;
import nxt.Order;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBidOrder extends APIServlet.APIRequestHandler {

    static final GetBidOrder instance = new GetBidOrder();

    private GetBidOrder() {
        super("order");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Long orderId = ParameterParser.getOrderId(req);
        Order.Bid bidOrder = Order.Bid.getBidOrder(orderId);
        if (bidOrder == null) {
            return JSONI18NResponses.getErrorResponse("UNKNOWN_ORDER");
        }
        return JSONData.bidOrder(bidOrder);
    }

}
