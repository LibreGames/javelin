package javelin.controller.db;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Used to read the file "preferences.properties".
 * 
 * @author alex
 */
public class Properties {
	private static final String BUNDLE_NAME = "preferences"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE =
			ResourceBundle.getBundle(BUNDLE_NAME);

	private Properties() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return null;
		}
	}

	static public Integer getInteger(String key, Integer fallback) {
		String value = getString(key);
		if (value == null) {
			return fallback;
		} else {
			return Integer.parseInt(value);
		}
	}
}