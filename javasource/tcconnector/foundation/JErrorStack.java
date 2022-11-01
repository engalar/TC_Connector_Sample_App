// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.internal.foundation.Constants;
import tcconnector.internal.foundation.LogCorrelationID;
import tcconnector.proxies.ErrorStack;

/**
 * A single error returned by a service operation. Since most service operations
 * are set based, the operation may return a different JErrorStack for each
 * element in the set that failed. The JErrorStack may also have an optional
 * UID, Client ID, or Index to help identify the source element that caused this
 * error.
 */
public class JErrorStack extends JSONObject {

	public static final String UID = "uid";
	public static final String CLIENT_ID = "clientId";
	public static final String CLIENT_INDEX = "clienetIndex";
	public static final String ERROR_VALUES = "errorValues";
	public static final String CODE = "code";
	public static final String LEVEL = "level";
	public static final String MESSAGE = "message";

	private static final Set<String> VALID_STACK_KEYS;
	private static final Set<String> VALID_VALUES_KEYS;
	static {
		VALID_STACK_KEYS = new HashSet<>();
		Iterator<String> keys = Arrays.asList(new String[] { UID, CLIENT_ID, CLIENT_INDEX, ERROR_VALUES }).iterator();
		keys.forEachRemaining(VALID_STACK_KEYS::add);

		VALID_VALUES_KEYS = new HashSet<>();
		keys = Arrays.asList(new String[] { CODE, LEVEL, MESSAGE }).iterator();
		keys.forEachRemaining(VALID_VALUES_KEYS::add);
	}

	private int primaryMessageIndex = 0;

	/**
	 * Construct an object from the following keys:
	 * 
	 * <pre>
	 * {
	 * 	"uid":			ModelObject(JModelObject) associated with this error.
	 * 	"clientId":		A client provided string associated with this error.
	 * 	"clientIndex":	An array index associated with this error.
	 * 	"errorValues":	A list of error values.
	 * }
	 * </pre>
	 * 
	 * @param obj A JSONObject that represents a ServiceData.
	 * @throws IllegalArgumentException The JSONObject does not have the required
	 *                                  keys/fields that represent a ServiceData.
	 */
	public JErrorStack(JSONObject obj) {
		super();
		validateKeys(obj);
		copyTopLevel(obj);
	}

	/**
	 * Create an Entity with property values from this JErrorStack.
	 * 
	 * @param context
	 * @return The Entity object created.
	 * 
	 */
	public IMendixObject instantiateEntity(IContext context) {
		ErrorStack tgtStack = new ErrorStack(context);
		tgtStack.setClientID(getClientID());
		tgtStack.setClientIndex(getClientIndex());
		tgtStack.setCode(getCode());
		tgtStack.setLevel(getLevel());
		tgtStack.setMessage(getMessage());

		return tgtStack.getMendixObject();
	}

	/** @return Get the ModelObject associated with this error (may be null). */
	public JModelObject getModelObject() {
		if (has(UID))
			return (JModelObject) getJSONObject(UID);
		return null;
	}

	/** @return Get the client ID associated with this error (may be null). */
	public String getClientID() {
		if (has(CLIENT_ID))
			return getString(CLIENT_ID);
		return null;
	}

	/** @return Get the client ID associated with this error (may be null). */
	public Integer getClientIndex() {
		if (has(CLIENT_INDEX))
			return getInt(CLIENT_INDEX);
		return null;
	}

	public String getMessage() {
		return getJSONArray(ERROR_VALUES).getJSONObject(primaryMessageIndex).getString(MESSAGE);
	}

	public Integer getCode() {
		return getJSONArray(ERROR_VALUES).getJSONObject(primaryMessageIndex).getInt(CODE);
	}

	public Integer getLevel() {
		return getJSONArray(ERROR_VALUES).getJSONObject(primaryMessageIndex).getInt(LEVEL);
	}

	private void validateKeys(JSONObject obj) {
		Iterator<String> it = obj.keys();
		while (it.hasNext()) {
			String key = it.next();
			if (!VALID_STACK_KEYS.contains(key)) {
				Constants.LOGGER.error(LogCorrelationID.getId() + ": The JSONObject does not represent a ErrorStack.\n"
						+ obj.toString());
				throw new IllegalArgumentException("The JSONObject does not represent a ErrorStack.");
			}
		}
		if (!obj.has(ERROR_VALUES)) {
			Constants.LOGGER.error(
					LogCorrelationID.getId() + ": The JSONObject does not represent a ErrorStack.\n" + obj.toString());
			throw new IllegalArgumentException("The JSONObject does not represent a ErrorStack.");
		}
		validateErrorKeys(obj.getJSONArray(ERROR_VALUES));
	}

	private void validateErrorKeys(JSONArray array) {
		primaryMessageIndex = array.length() - 1;
		for (int i = 0; i < primaryMessageIndex; i++) {
			JSONObject value = array.getJSONObject(i);
			Iterator<String> it = value.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (!VALID_VALUES_KEYS.contains(key)) {
					Constants.LOGGER.error(LogCorrelationID.getId()
							+ ": The JSONObject does not represent a ErrorValue.\n" + value.toString());
					throw new IllegalArgumentException("The JSONObject does not represent a ErrorValue.");
				}
			}
		}
	}

	private void copyTopLevel(JSONObject right) {
		Iterator<String> it = right.keys();
		while (it.hasNext()) {
			String key = it.next();
			put(key, right.get(key));
		}
	}
}
