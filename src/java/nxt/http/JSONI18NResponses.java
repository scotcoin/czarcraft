package nxt.http;

import nxt.util.JSON;
import nxt.util.Logger;
import nxt.util.ResponseErrorResourceBundleFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Locale;
import java.util.MissingResourceException;

public final class JSONI18NResponses {

	private static Locale defaultLocale; // default

	public static JSONStreamAware getErrorResponse(String messageKey) {
		return getErrorResponse(messageKey, defaultLocale);
	}

	public static JSONStreamAware getErrorResponse(String messageKey,
			Locale locale) {
		
		if (locale == null) locale=defaultLocale;
		
		String properties = null;
		JSONObject response = new JSONObject();

		try {
			properties = ResponseErrorResourceBundleFactory.getInstance(locale)
					.getString(messageKey);
		} catch (MissingResourceException mre) {
			return handleError(messageKey, properties, locale);
		}

		String[] error = properties.split(":::");

		if (error.length == 2) {
			response.put("errorCode", error[0]);
			response.put("errorDescription", error[1]);
			return JSON.prepare(response);
		}

		return handleError(messageKey, properties, locale);

	}

	public static Locale getDefaultLocale() {
		return (defaultLocale == null) ? Locale.ROOT : defaultLocale;
	}
	
	public static void setDefaultLocale(Locale locale) {
		JSONI18NResponses.defaultLocale = locale;
	}
	
	private static JSONStreamAware handleError(String messageKey,
			String properties, Locale locale) {
		
		JSONObject response = new JSONObject();
		if (locale.equals(Locale.ROOT)) {
			response.put("errorCode", 9);
			response.put("errorDescription",
					"RuntimeError: localization properties error key: "
							+ messageKey);
			Logger.logErrorMessage("localization properties error in key \""
					+ messageKey + "\", language \"" + locale.getLanguage()
					+ "\", value \"" + properties + "\"");
			return JSON.prepare(response);
		}
		// else
		Logger.logErrorMessage("localization properties error in key \""
				+ messageKey + "\", language \"" + locale.getLanguage()
				+ "\", value \"" + properties + "\"");
		Logger.logErrorMessage("falling back to Locale.ROOT");
		return getErrorResponse(messageKey, Locale.ROOT);

	}
}
