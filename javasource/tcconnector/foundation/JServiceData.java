// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.internal.foundation.Constants;
import tcconnector.internal.foundation.LogCorrelationID;
import tcconnector.proxies.ErrorStack;
import tcconnector.proxies.ModelObject;
import tcconnector.proxies.ServiceData;

/**
 * Extends the JSONObject class to represent the ServiceData structure.
 */
public class JServiceData extends JSONObject {

	public static final String QNAME = "http://teamcenter.com/Schemas/Soa/2006-03/Base.ServiceData";
	public static final String PLAIN = "plain";
	public static final String UPDATED = "updated";
	public static final String CREATED = "created";
	public static final String DELETED = "deleted";
	public static final String PARTIAL_ERRORS = "partialErrors";

	// Not exposed outside of framework processing
	private static final String MODEL_OBJECTS = "modelObjects";
	private static final Set<String> VALID_KEYS;
	static {
		VALID_KEYS = new HashSet<>();
		Iterator<String> keys = Arrays.asList(
				new String[] { PLAIN, UPDATED, CREATED, DELETED, PARTIAL_ERRORS, MODEL_OBJECTS, Constants.KEY_QNAME })
				.iterator();
		keys.forEachRemaining(VALID_KEYS::add);
	}

	/**
	 * Construct an object from the following keys:
	 * 
	 * <pre>
	 * {
	 * 	"plain":	A List of plain business objects (JModelObject).
	 * 	"updated":	A List of updated business objects (JModelObject).
	 * 	"created":	A List of created business objects (JModelObject).
	 * 	"deleted":	A List of deleted business object UIDs.
	 * }
	 * </pre>
	 * 
	 * @param obj A JSONObject that represents a ServiceData.
	 * @throws IllegalArgumentException The JSONObject does not have the required
	 *                                  keys/fields that represent a ServiceData.
	 */
	public JServiceData(JSONObject obj) {
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
	public IMendixObject instantiateEntity(IContext context, BusinessObjectMappings boMappings,
			String configurationName) {
		ServiceData tgtSD = instantiateServiceData(context, boMappings, configurationName);
		return tgtSD.getMendixObject();
	}

	/**
	 * Create an Entity with property values from this JErrorStack.
	 * 
	 * @param context
	 * @return The Entity object created.
	 * 
	 */
	public ServiceData instantiateServiceData(IContext context, BusinessObjectMappings boMappings,
			String configurationName) {
		ServiceData tgtSD = new ServiceData(context);
		instantiateList(context, PLAIN, ModelObject.MemberNames.Plain, tgtSD, boMappings, configurationName);
		instantiateList(context, UPDATED, ModelObject.MemberNames.Updated, tgtSD, boMappings, configurationName);
		instantiateList(context, CREATED, ModelObject.MemberNames.Created, tgtSD, boMappings, configurationName);
		instantiateErrors(context, tgtSD);
		return tgtSD;
	}

	/** @return Get the unmodifiable list of plain business objects. */
	public List<JModelObject> getPlainObjects() {
		return getNamedList(PLAIN);
	}

	/** @return Get the unmodifiable list of updated business objects. */
	public List<JModelObject> getUpdatedObjects() {
		return getNamedList(UPDATED);
	}

	/** @return Get the unmodifiable list of created business objects. */
	public List<JModelObject> getCreatedObjects() {
		return getNamedList(CREATED);
	}

	/** @return Get the unmodifiable list of deleted business object UIDs. */
	public List<String> getDeletedUIDs() {
		List<String> objs = new ArrayList<>();
		if (has(DELETED) && !isNull(DELETED)) {
			JSONArray jArray = this.getJSONArray(DELETED);
			for (int i = 0; i < jArray.length(); i++) {
				objs.add(jArray.getString(i));
			}
		}
		return Collections.unmodifiableList(objs);
	}

	public List<JErrorStack> getPartialErrors() {
		List<JErrorStack> errors = new ArrayList<>();
		if (has(PARTIAL_ERRORS) && !isNull(PARTIAL_ERRORS)) {
			JSONArray jArray = getJSONArray(PARTIAL_ERRORS);
			for (int i = 0; i < jArray.length(); i++) {
				errors.add(new JErrorStack(jArray.getJSONObject(i)));
			}
		}
		return Collections.unmodifiableList(errors);
	}

	private void validateKeys(JSONObject obj) {
		Iterator<String> it = obj.keys();
		while (it.hasNext()) {
			String key = it.next();
			if (!VALID_KEYS.contains(key)) {
				Constants.LOGGER.error(LogCorrelationID.getId() + ": The JSONObject does not represent a ServiceData.\n"
						+ obj.toString());
				throw new IllegalArgumentException("The JSONObject does not represent a ServiceData.");
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

	private List<JModelObject> getNamedList(String key) {
		List<JModelObject> objs = new ArrayList<>();
		if (has(key) && !isNull(key)) {
			JSONArray jArray = this.getJSONArray(key);
			for (int i = 0; i < jArray.length(); i++) {
				objs.add((JModelObject) jArray.getJSONObject(i));
			}
		}
		return Collections.unmodifiableList(objs);
	}

	private void instantiateList(IContext context, String key, ModelObject.MemberNames memberName, ServiceData tgtSD,
			BusinessObjectMappings boMappings, String configurationName) {
		for (JModelObject jModelObj : getNamedList(key)) {
			IMendixObject plainObj = jModelObj.instantiateEntity(context, ModelObject.entityName, boMappings,
					configurationName);
			plainObj.setValue(context, memberName.toString(), tgtSD.getMendixObject().getId());
		}
	}

	private void instantiateErrors(IContext context, ServiceData tgtSD) {
		for (JErrorStack jError : getPartialErrors()) {
			for (int cnt = 0; cnt < jError.length(); ++cnt) {
				JSONArray errorValues = jError.getJSONArray("errorValues");
				for (int errorValuesCnt = 0; errorValuesCnt < errorValues.length(); ++errorValuesCnt) {
					JSONObject errorStackJSONObj = errorValues.getJSONObject(errorValuesCnt);
					ErrorStack errorStack = new ErrorStack(context);
					errorStack.setMessage(context, errorStackJSONObj.optString("message"));
					errorStack.setCode(context, Integer.parseInt(errorStackJSONObj.optString("code")));
					errorStack.setLevel(context, Integer.parseInt(errorStackJSONObj.optString("level")));
					errorStack.getMendixObject().setValue(context, ErrorStack.MemberNames.PartialErrors.toString(),
							tgtSD.getMendixObject().getId());
				}
			}
		}
	}
}
