package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllAccounts extends APIServlet.APIRequestHandler {

    static final GetAllAccounts instance = new GetAllAccounts();

    private GetAllAccounts() {
        super(new APITag[] {APITag.ACCOUNTS});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray accountJSONArray = new JSONArray();
        response.put("accounts", accountJSONArray);
        for (Account account : Account.getAllAccounts()) {
        	 JSONObject json = new JSONObject();
             json.put("account", Convert.rsAccount(account.getId()));             
             accountJSONArray.add(json);
        }
        return response;
    }

}
