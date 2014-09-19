package nxt.http;

import nxt.Account;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllAccountIds extends APIServlet.APIRequestHandler {

    static final GetAllAccountIds instance = new GetAllAccountIds();

    private GetAllAccountIds() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray accountJSONArray = new JSONArray();
        response.put("accounts", accountJSONArray);
        for (Account account : Account.getAllAccounts()) {
        	 JSONObject json = new JSONObject();        	 
        	 JSONData.putAccount(json, "account", account.getId());                          
             accountJSONArray.add(json);
        }
        return response;
    }

}
