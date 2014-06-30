package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Constants;
import nxt.crypto.Crypto;
import nxt.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

final class ParameterParser {

    static long getAmountNQT(HttpServletRequest req) throws ParameterException {
        String amountValueNQT = Convert.emptyToNull(req.getParameter("amountNQT"));
        if (amountValueNQT == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_AMOUNT"));
        }
        long amountNQT;
        try {
            amountNQT = Long.parseLong(amountValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_AMOUNT"));
        }
        if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_AMOUNT"));
        }
        return amountNQT;
    }

    static long getFeeNQT(HttpServletRequest req) throws ParameterException {
        String feeValueNQT = Convert.emptyToNull(req.getParameter("feeNQT"));
        if (feeValueNQT == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_FEE"));
        }
        long feeNQT;
        try {
            feeNQT = Long.parseLong(feeValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_FEE"));
        }
        if (feeNQT <= 0 || feeNQT >= Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_FEE"));
        }
        return feeNQT;
    }

    static long getPriceNQT(HttpServletRequest req) throws ParameterException {
        String priceValueNQT = Convert.emptyToNull(req.getParameter("priceNQT"));
        if (priceValueNQT == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_PRICE"));
        }
        long priceNQT;
        try {
            priceNQT = Long.parseLong(priceValueNQT);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_PRICE"));
        }
        if (priceNQT <= 0 || priceNQT > Constants.MAX_BALANCE_NQT) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_PRICE"));
        }
        return priceNQT;
    }

    static Asset getAsset(HttpServletRequest req) throws ParameterException {
        String assetValue = Convert.emptyToNull(req.getParameter("asset"));
        if (assetValue == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_ASSET"));
        }
        Asset asset;
        try {
            Long assetId = Convert.parseUnsignedLong(assetValue);
            asset = Asset.getAsset(assetId);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_ASSET"));
        }
        if (asset == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("UNKNOWN_ASSET"));
        }
        return asset;
    }

    static long getQuantityQNT(HttpServletRequest req) throws ParameterException {
        String quantityValueQNT = Convert.emptyToNull(req.getParameter("quantityQNT"));
        if (quantityValueQNT == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_QUANTITY"));
        }
        long quantityQNT;
        try {
            quantityQNT = Long.parseLong(quantityValueQNT);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_QUANTITY"));
        }
        if (quantityQNT <= 0 || quantityQNT > Constants.MAX_ASSET_QUANTITY_QNT) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_QUANTITY"));
        }
        return quantityQNT;
    }

    static Long getOrderId(HttpServletRequest req) throws ParameterException {
        String orderValue = Convert.emptyToNull(req.getParameter("order"));
        if (orderValue == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_ORDER"));
        }
        try {
            return Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_ORDER"));
        }
    }

    static Account getSenderAccount(HttpServletRequest req) throws ParameterException {
        Account account;
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyString = Convert.emptyToNull(req.getParameter("publicKey"));
        if (secretPhrase != null) {
            account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
        } else if (publicKeyString != null) {
            try {
                account = Account.getAccount(Convert.parseHexString(publicKeyString));
            } catch (RuntimeException e) {
                throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_PUBLIC_KEY"));
            }
        } else {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_SECRET_PHRASE_OR_PUBLIC_KEY"));
        }
        if (account == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("UNKNOWN_ACCOUNT"));
        }
        return account;
    }

    static Account getAccount(HttpServletRequest req) throws ParameterException {
        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        if (accountValue == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_ACCOUNT"));
        }
        try {
            Account account = Account.getAccount(Convert.parseAccountId(accountValue));
            if (account == null) {
                throw new ParameterException(JSONI18NResponses.getErrorResponse("UNKNOWN_ACCOUNT"));
            }
            return account;
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_ACCOUNT"));
        }
    }

    static List<Account> getAccounts(HttpServletRequest req) throws ParameterException {
        String[] accountValues = req.getParameterValues("account");
        if (accountValues == null || accountValues.length == 0) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_ACCOUNT"));
        }
        List<Account> result = new ArrayList<>();
        for (String accountValue : accountValues) {
            if (accountValue == null || accountValue.equals("")) {
                continue;
            }
            try {
                Account account = Account.getAccount(Convert.parseAccountId(accountValue));
                if (account == null) {
                    throw new ParameterException(JSONI18NResponses.getErrorResponse("UNKNOWN_ACCOUNT"));
                }
                result.add(account);
            } catch (RuntimeException e) {
                throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_ACCOUNT"));
            }
        }
        return result;
    }

    static int getTimestamp(HttpServletRequest req) throws ParameterException {
        String timestampValue = Convert.emptyToNull(req.getParameter("timestamp"));
        if (timestampValue == null) {
            return 0;
        }
        int timestamp;
        try {
            timestamp = Integer.parseInt(timestampValue);
        } catch (NumberFormatException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_TIMESTAMP"));
        }
        if (timestamp < 0) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_TIMESTAMP"));
        }
        return timestamp;
    }

    static Long getRecipientId(HttpServletRequest req) throws ParameterException {
        String recipientValue = Convert.emptyToNull(req.getParameter("recipient"));
        if (recipientValue == null || "0".equals(recipientValue)) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("MISSING_RECIPIENT"));
        }
        Long recipientId;
        try {
            recipientId = Convert.parseAccountId(recipientValue);
        } catch (RuntimeException e) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_RECIPIENT"));
        }
        if (recipientId == null) {
            throw new ParameterException(JSONI18NResponses.getErrorResponse("INCORRECT_RECIPIENT"));
        }
        return recipientId;
    }

    private ParameterParser() {} // never

}
