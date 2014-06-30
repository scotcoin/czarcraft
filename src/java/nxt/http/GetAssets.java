package nxt.http;

import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssets extends APIServlet.APIRequestHandler {

    static final GetAssets instance = new GetAssets();

    private GetAssets() {
        super("assets", "assets", "assets"); // limit to 3 for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String[] assets = req.getParameterValues("assets");

        JSONObject response = new JSONObject();
        JSONArray assetsJSONArray = new JSONArray();
        response.put("assets", assetsJSONArray);
        for (String assetIdString : assets) {
            if (assetIdString == null || assetIdString.equals("")) {
                continue;
            }
            try {
                Asset asset = Asset.getAsset(Convert.parseUnsignedLong(assetIdString));
                if (asset == null) {
                    return JSONI18NResponses.getErrorResponse("UNKNOWN_ASSET");
                }
                assetsJSONArray.add(JSONData.asset(asset));
            } catch (RuntimeException e) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_ASSET");
            }
        }
        return response;
    }

}
