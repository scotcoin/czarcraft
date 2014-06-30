package nxt.http;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBlock extends APIServlet.APIRequestHandler {

    static final GetBlock instance = new GetBlock();

    private GetBlock() {
        super("block");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String block = req.getParameter("block");
        if (block == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_BLOCK");
        }

        Block blockData;
        try {
            blockData = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(block));
            if (blockData == null) {
                return JSONI18NResponses.getErrorResponse("UNKNOWN_BLOCK");
            }
        } catch (RuntimeException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_BLOCK");
        }

        return JSONData.block(blockData);

    }

}