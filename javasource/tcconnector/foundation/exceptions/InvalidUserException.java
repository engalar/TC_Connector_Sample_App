// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation.exceptions;

import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.internal.foundation.Constants;

/**
 * Exception returned from the Teamcenter server for for an invalid session.
 * This indicates that the client application has not authenticated the user
 * yet, or the authenticated session for that user has time-out.
 */
public class InvalidUserException extends AuthException

{
	private static final long serialVersionUID = 1L;

	public static final String KEY_CODE = "code";
	public static final String KEY_LEVEL = "level";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_SSO_URL = "ssoServerURL";
	public static final String KEY_SSO_APP_ID = "ssoAppID";

	/**
	 * Create the InternalServerException from a JSON Object.
	 */
	public InvalidUserException(JSONObject jsonObj) {
		super(getMessage(jsonObj));

		validateKey(jsonObj, KEY_CODE);
		validateKey(jsonObj, KEY_LEVEL);

		code = jsonObj.getInt(KEY_CODE);
		level = Severity.values()[jsonObj.getInt(KEY_LEVEL)];
		ssoUrl = jsonObj.optString(KEY_SSO_URL, "");
		appId = jsonObj.optString(KEY_SSO_APP_ID, "");
	}

	private static void validateKey(JSONObject jsonObj, String key) {
		if (jsonObj.has(key))
			return;

		String message = "The JSON does not have the expected " + key
				+ " key, and thus cannot be parsed as an InvalidUserException.";
		Constants.LOGGER.error(message);
		throw new IllegalArgumentException(message);
	}

	private static String getMessage(JSONObject jsonObj) {
		validateKey(jsonObj, Constants.KEY_QNAME);
		validateKey(jsonObj, KEY_MESSAGE);

		String qName = jsonObj.getString(Constants.KEY_QNAME);
		if (qName.equals(Constants.QNAME_USER_EXP))
			return !jsonObj.isNull(KEY_MESSAGE) ? jsonObj.getString(KEY_MESSAGE)
					: Integer.toString(jsonObj.getInt(KEY_CODE));

		String message = "The JSON " + Constants.KEY_QNAME + " value is incorrect for an InvalidUserException (" + qName
				+ ").";
		Constants.LOGGER.error(message);
		throw new IllegalArgumentException(message);
	}
}
