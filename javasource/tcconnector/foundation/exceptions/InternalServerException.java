// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation.exceptions;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.internal.foundation.Constants;

/**
 * Exception returned from the Teamcenter server for any non-service related
 * error. This includes (but is not limited to) communication errors, parsing
 * errors, configuration errors.
 */
public class InternalServerException extends BaseServiceException {
	private static final long serialVersionUID = 1L;

	public static final String KEY_MESSAGES = "messages";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_CODE = "code";
	public static final String KEY_LEVEL = "level";

	/**
	 * Nested exception messages on a InternalServerExcpetion.
	 */
	public static class ISECause extends BaseServiceException {
		private static final long serialVersionUID = 1L;

		private ISECause(JSONArray messages, int index) {
			super(messages.getJSONObject(index).getString(KEY_MESSAGE), constructCause(messages, index - 1));
			JSONObject primary = messages.getJSONObject(index);
			validateKey(primary, KEY_CODE);
			validateKey(primary, KEY_LEVEL);
			code = primary.getInt(KEY_CODE);
			level = Severity.values()[primary.getInt(KEY_LEVEL)];
		}
	}

	/**
	 * Create the InternalServerException from a JSON Object. The JSON string
	 * returned from the Teamcenter server has one or more messages (each with a
	 * code, level, and message). The array of messages are in reverse order of
	 * importance (the low level error that occurred first on the Teamcenter sever
	 * is added to the array first. The top-level error is listed last on the array.
	 */
	public InternalServerException(JSONObject jsonObj) {
		super(getPrimaryMessage(getMessages(jsonObj)), constructCause(getMessages(jsonObj)));

		JSONArray messages = getMessages(jsonObj);
		JSONObject primary = messages.getJSONObject(messages.length() - 1);
		validateKey(primary, KEY_CODE);
		validateKey(primary, KEY_LEVEL);
		code = primary.getInt(KEY_CODE);
		level = Severity.values()[primary.getInt(KEY_LEVEL)];
	}

	private static JSONArray getMessages(JSONObject jsonObj) {
		validateKey(jsonObj, Constants.KEY_QNAME);
		validateKey(jsonObj, KEY_MESSAGES);

		String qName = jsonObj.getString(Constants.KEY_QNAME);
		if (qName.equals(Constants.QNAME_SERVER_EXP))
			return jsonObj.getJSONArray(KEY_MESSAGES);

		String message = "The JSON " + Constants.KEY_QNAME + " value is incorrect for an InternlaServerExcpetion ("
				+ qName + ").";
		Constants.LOGGER.error(message);
		throw new IllegalArgumentException(message);
	}

	private static void validateKey(JSONObject jsonObj, String key) {
		if (jsonObj.has(key))
			return;

		String message = "The JSON does not have the expected " + key
				+ " key, and thus cannot be parsed as an InternlaServerExcpetion.";
		Constants.LOGGER.error(message);
		throw new IllegalArgumentException(message);
	}

	private static String getPrimaryMessage(JSONArray messages) {
		JSONObject primary = messages.getJSONObject(messages.length() - 1);
		validateKey(primary, KEY_MESSAGE);
		return !primary.isNull(KEY_MESSAGE) ? primary.getString(KEY_MESSAGE)
				: Integer.toString(primary.getInt(KEY_CODE));

	}

	private static ISECause constructCause(JSONArray messages) {
		int index = messages.length() - 2;
		return constructCause(messages, index);
	}

	private static ISECause constructCause(JSONArray messages, int index) {
		if (index < 0)
			return null;
		return new ISECause(messages, index);
	}
}
