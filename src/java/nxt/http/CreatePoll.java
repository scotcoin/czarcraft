package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public final class CreatePoll extends CreateTransaction {

    static final CreatePoll instance = new CreatePoll();

    private CreatePoll() {
        super("name", "description", "minNumberOfOptions", "maxNumberOfOptions", "optionsAreBinary",
                "option1", "option2", "option3"); // hardcoded to 3 options for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String nameValue = req.getParameter("name");
        String descriptionValue = req.getParameter("description");
        String minNumberOfOptionsValue = req.getParameter("minNumberOfOptions");
        String maxNumberOfOptionsValue = req.getParameter("maxNumberOfOptions");
        String optionsAreBinaryValue = req.getParameter("optionsAreBinary");

        if (nameValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_NAME");
        } else if (descriptionValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_DESCRIPTION");
        } else if (minNumberOfOptionsValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_MINNUMBEROFOPTIONS");
        } else if (maxNumberOfOptionsValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_MAXNUMBEROFOPTIONS");
        } else if (optionsAreBinaryValue == null) {
            return JSONI18NResponses.getErrorResponse("MISSING_OPTIONSAREBINARY");
        }

        if (nameValue.length() > Constants.MAX_POLL_NAME_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_POLL_NAME_LENGTH");
        }

        if (descriptionValue.length() > Constants.MAX_POLL_DESCRIPTION_LENGTH) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_POLL_DESCRIPTION_LENGTH");
        }

        List<String> options = new ArrayList<>();
        while (options.size() < 100) {
            String optionValue = req.getParameter("option" + options.size());
            if (optionValue == null) {
                break;
            }
            if (optionValue.length() > Constants.MAX_POLL_OPTION_LENGTH) {
                return JSONI18NResponses.getErrorResponse("INCORRECT_POLL_OPTION_LENGTH");
            }
            options.add(optionValue.trim());
        }

        byte minNumberOfOptions;
        try {
            minNumberOfOptions = Byte.parseByte(minNumberOfOptionsValue);
        } catch (NumberFormatException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_MINNUMBEROFOPTIONS");
        }

        byte maxNumberOfOptions;
        try {
            maxNumberOfOptions = Byte.parseByte(maxNumberOfOptionsValue);
        } catch (NumberFormatException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_MAXNUMBEROFOPTIONS");
        }

        boolean optionsAreBinary;
        try {
            optionsAreBinary = Boolean.parseBoolean(optionsAreBinaryValue);
        } catch (NumberFormatException e) {
            return JSONI18NResponses.getErrorResponse("INCORRECT_OPTIONSAREBINARY");
        }

        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MessagingPollCreation(nameValue.trim(), descriptionValue.trim(),
                options.toArray(new String[options.size()]), minNumberOfOptions, maxNumberOfOptions, optionsAreBinary);
        return createTransaction(req, account, attachment);

    }

}
