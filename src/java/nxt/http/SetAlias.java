package nxt.http;


import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ALIAS_LENGTH;
import static nxt.http.JSONResponses.INCORRECT_ALIAS_NAME;
import static nxt.http.JSONResponses.INCORRECT_URI_LENGTH;
import static nxt.http.JSONResponses.MISSING_ALIAS_NAME;
import static nxt.http.JSONResponses.ERROR_ALIAS_USED;;

public final class SetAlias extends CreateTransaction {

    static final SetAlias instance = new SetAlias();

    private SetAlias() {
        super("aliasName", "aliasURI");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        String aliasURI = Convert.nullToEmpty(req.getParameter("aliasURI"));

        if (aliasName == null) {
            return MISSING_ALIAS_NAME;
        }

        aliasName = aliasName.trim();
        if (aliasName.length() == 0 || aliasName.length() > Constants.MAX_ALIAS_LENGTH) {
            return INCORRECT_ALIAS_LENGTH;
        }

        String normalizedAlias = aliasName.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INCORRECT_ALIAS_NAME;
            }
        }

        aliasURI = aliasURI.trim();
        if (aliasURI.length() > Constants.MAX_ALIAS_URI_LENGTH) {
            return INCORRECT_URI_LENGTH;
        }

        Account account = ParameterParser.getSenderAccount(req);

        Alias alias = Alias.getAlias(normalizedAlias);
        if (alias != null && !alias.getAccount().getId().equals(account.getId())) {           
            return ERROR_ALIAS_USED;
        }

        Attachment attachment = new Attachment.MessagingAliasAssignment(aliasName, aliasURI);
        return createTransaction(req, account, attachment);

    }

}
