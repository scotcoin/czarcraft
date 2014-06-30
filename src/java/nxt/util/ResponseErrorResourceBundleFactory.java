package nxt.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public class ResponseErrorResourceBundleFactory {
	
	private static HashMap<String, ResourceBundle> rbMap = new HashMap<String, ResourceBundle>();

	// hashmap und so
	
	public static ResourceBundle getInstance(Locale locale) {
		if (locale == null)
			return null;
		final String key = locale.getLanguage();
		
		ResourceBundle rb = rbMap.get(key);

		if (rb == null) {
			rb = ResourceBundle.getBundle("localization", locale);
			rbMap.put(key, rb);
		}

		return rb;
	}
}
