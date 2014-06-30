package nxt.http;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;

public final class CalculateFullHash extends APIServlet.APIRequestHandler {

    static final CalculateFullHash instance = new CalculateFullHash();

    private CalculateFullHash() {
        super("unsignedTransactionBytes", "signatureHash");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String unsignedBytesString = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String signatureHashString = Convert.emptyToNull(req.getParameter("signatureHash"));

        if (unsignedBytesString == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_UNSIGNED_BYTES");
        } else if (signatureHashString == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_SIGNATURE_HASH");
        }

        MessageDigest digest = Crypto.sha256();
        digest.update(Convert.parseHexString(unsignedBytesString));
        byte[] fullHash = digest.digest(Convert.parseHexString(signatureHashString));
        JSONObject response = new JSONObject();
        response.put("fullHash", Convert.toHexString(fullHash));

        return response;

    }

}
