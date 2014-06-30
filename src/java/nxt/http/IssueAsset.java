package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class IssueAsset extends CreateTransaction {

    static final IssueAsset instance = new IssueAsset();

    private IssueAsset() {
        super("name", "description", "quantityQNT", "decimals");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String decimalsValue = Convert.emptyToNull(req.getParameter("decimals"));

        if (name == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_NAME");
        }

        name = name.trim();
        if (name.length() < Constants.MIN_ASSET_NAME_LENGTH || name.length() > Constants.MAX_ASSET_NAME_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_ASSET_NAME_LENGTH");
        }
        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_ASSET_NAME");
            }
        }

        if (description != null && description.length() > Constants.MAX_ASSET_DESCRIPTION_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_ASSET_DESCRIPTION");
        }

        byte decimals = 0;
        if (decimalsValue != null) {
            try {
                decimals = Byte.parseByte(decimalsValue);
                if (decimals < 0 || decimals > 8) {
                    return JSONI18NResponses.getErrorResponse("INCORRECT_DECIMALS");
                }
            } catch (NumberFormatException e) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_DECIMALS");
            }
        }

        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.ColoredCoinsAssetIssuance(name, description, quantityQNT, decimals);
        return createTransaction(req, account, attachment);

    }

    @Override
    final long minimumFeeNQT() {
        return Constants.ASSET_ISSUANCE_FEE_NQT;
    }

}
