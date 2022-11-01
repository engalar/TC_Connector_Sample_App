// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.JModelObject;
import tcconnector.foundation.JServiceData;
import tcconnector.foundation.TcMappings;
import tcconnector.foundation.TcModelObjectMappings;

public class ModelObjectResolver {

	private static final String KEY_SERVICE_DATA = "ServiceData";
	private static final String KEY_sERVICE_DATA = "serviceData";
	private static final String KEY_MODEL_OBJECTS = "modelObjects";

	/**
	 * Replace all ModelObject references (UIDs) with the actual ModelObject
	 * instance from the ServiceData.modelObjects map.
	 * 
	 * @param responseObj The service operation response, may be a ServiceData or
	 *                    custom data structure.
	 */
	public static JSONObject resolve(IContext context, JSONObject responseObj, String configurationName) {
		ModelObjectResolver resolver = new ModelObjectResolver(context, responseObj, TcModelObjectMappings.INSTANCE,
				configurationName);
		return resolver.resolve();
	}

	public static JSONObject getServiceData(IContext context, JSONObject responseObj, String configurationName) {
		ModelObjectResolver resolver = new ModelObjectResolver(context, responseObj, TcModelObjectMappings.INSTANCE,
				configurationName);
		return resolver.findServiceData();
	}

	private IContext context;
	private JSONObject serviceData;
	private JSONObject modelObjects;
	private JSONObject rootObject;
	private TcMappings tcMappings;
	private String configurationName;

	private ModelObjectResolver(IContext context, JSONObject root, TcMappings mappings, String configurationName) {
		this.context = context;
		this.rootObject = root;
		this.serviceData = null;
		this.modelObjects = null;
		this.tcMappings = mappings;
		this.configurationName = configurationName;
	}

	private JSONObject resolve() {
		// Not all service operations return a ServiceData
		serviceData = findServiceData();
		if (serviceData != null) {
			// Not all ServiceDatas have a modelObject map (the ServiceData is empty)
			modelObjects = findModelObjectsMap(serviceData);
			if (modelObjects != null) {
				resolveServiceData();
			}
			resolveCustomStruct(rootObject);
		}
		return rootObject;
	}

	private JSONObject findServiceData() {
		// Is it the Root object
		if (rootObject.has(Constants.KEY_QNAME) && !rootObject.isNull(Constants.KEY_QNAME)
				&& rootObject.getString(Constants.KEY_QNAME).equals(JServiceData.QNAME)) {
			rootObject = new JServiceData(rootObject);
			return rootObject;
		}

		// Child of the Root
		if (rootObject.has(KEY_SERVICE_DATA) && !rootObject.isNull(KEY_SERVICE_DATA)) {
			JSONObject sd = rootObject.getJSONObject(KEY_SERVICE_DATA);
			JServiceData jSd = new JServiceData(sd);
			rootObject.remove(KEY_SERVICE_DATA);
			rootObject.put(KEY_SERVICE_DATA, jSd);
			return jSd;
		}
		// Sometimes it has a lower case 's'
		if (rootObject.has(KEY_sERVICE_DATA) && !rootObject.isNull(KEY_sERVICE_DATA)) {
			JSONObject sd = rootObject.getJSONObject(KEY_sERVICE_DATA);
			JServiceData jSd = new JServiceData(sd);
			rootObject.remove(KEY_sERVICE_DATA);
			rootObject.put(KEY_sERVICE_DATA, jSd);
			return jSd;
		}
		return null;
	}

	private JSONObject findModelObjectsMap(JSONObject responseObj) {
		if (responseObj.has(KEY_MODEL_OBJECTS) && !responseObj.isNull(KEY_MODEL_OBJECTS)) {
			return responseObj.getJSONObject(KEY_MODEL_OBJECTS);
		}
		return null;
	}

	private void resolveServiceData() {
		resolvePropertyValues();
		resolveServiceDataArray(JServiceData.PLAIN);
		resolveServiceDataArray(JServiceData.UPDATED);
		resolveServiceDataArray(JServiceData.CREATED);
		serviceData.remove(KEY_MODEL_OBJECTS);
	}

	private void resolveCustomStruct(JSONObject structObj) {
		Iterator<String> keys = structObj.keys();
		List<String> keyList = new ArrayList<>();
		keys.forEachRemaining(keyList::add);

		for (String key : keyList) {
			resolveChildObject(structObj, key);
			resolveChildArray(structObj, key);
		}
	}

	private Object getModelObjectInstance(String uid) {
		if (uid.equals(JModelObject.NULL_TAG_UID) || modelObjects == null || !modelObjects.has(uid))
			return JSONObject.NULL;

		return modelObjects.getJSONObject(uid);
	}

	private void resolveChildObject(JSONObject parentStruct, String key) {
		if (!parentStruct.isJSONObject(key))
			return;

		JSONObject childStruct = parentStruct.getJSONObject(key);
		if (childStruct == serviceData)
			return;

		if (tcMappings.isAModelObjectRef(childStruct)) {
			Object mo = getModelObjectInstance(childStruct.getString(JModelObject.UID));
			if (mo == JSONObject.NULL) {
				parentStruct.remove(key);
				parentStruct.put(key, new JModelObject(childStruct));
			} else {
				parentStruct.remove(key);
				parentStruct.put(key, mo);
			}
		} else {
			resolveCustomStruct(childStruct);
		}
	}

	private void resolveChildArray(JSONObject parentStruct, String key) {
		if (!parentStruct.isJSONArray(key))
			return;

		JSONArray childArray = parentStruct.getJSONArray(key);
		JSONArray resolvedArray = resolveArray(childArray);

		parentStruct.remove(key);
		parentStruct.put(key, resolvedArray);
	}

	private JSONArray resolveArray(JSONArray tgtArray) {
		if (tgtArray.length() == 0)
			return tgtArray;

		Object firsElement = tgtArray.get(0);
		if (!((firsElement instanceof JSONObject) || (firsElement instanceof JSONArray)))
			return tgtArray;

		JSONArray resolvedArray = new JSONArray();

		if (firsElement instanceof JSONObject) {
			for (int i = 0; i < tgtArray.length(); i++) {
				JSONObject childElement = tgtArray.getJSONObject(i);
				if (tcMappings.isAModelObjectRef(childElement)) {
					Object mo = getModelObjectInstance(childElement.getString(JModelObject.UID));
					if (mo == JSONObject.NULL) {
						resolvedArray.put(new JModelObject(childElement));
					} else {
						resolvedArray.put(mo);
					}
				} else {
					resolveCustomStruct(childElement);
					resolvedArray.put(childElement);
				}
			}
		} else if (firsElement instanceof JSONArray) {
			for (int i = 0; i < tgtArray.length(); i++) {
				JSONArray childElement = tgtArray.getJSONArray(i);
				JSONArray resolvedElement = resolveArray(childElement);
				resolvedArray.put(resolvedElement);
			}
		}
		return resolvedArray;
	}

	private void resolveServiceDataArray(String key) {
		if (serviceData.has(key) && !serviceData.isNull(key)) {
			JSONArray uids = serviceData.getJSONArray(key);
			JSONArray mos = new JSONArray();
			for (int i = 0; i < uids.length(); i++) {
				String uid = uids.getString(i);
				Object mo = getModelObjectInstance(uid);
				mos.put(mo);
			}
			serviceData.remove(key);
			serviceData.put(key, mos);
		}
	}

	private void resolvePropertyValues() {
		Set<String> typeNames = new HashSet<>();
		JSONObject resolvedModelObjects = new JSONObject();
		Iterator<String> uids = modelObjects.keys();
		while (uids.hasNext()) {
			String uid = uids.next();
			JModelObject resolvedObj = parseModelObject(uid);
			resolvedModelObjects.put(uid, resolvedObj);
			typeNames.add(resolvedObj.getType());
		}
		modelObjects = resolvedModelObjects;

		if (context != null)
			ClientMetaModel.ensureTypesAreLoaded(context, typeNames, configurationName);
	}

	private JModelObject parseModelObject(String uid) {
		JSONObject mo = modelObjects.getJSONObject(uid);
		if (mo.has(JModelObject.PROPS)) {
			JSONObject props = mo.getJSONObject(JModelObject.PROPS);
			Iterator<String> keys = props.keys();
			List<String> names = new ArrayList<>();
			keys.forEachRemaining(names::add);

			for (String key : names) {
				parseProperty(props, key);
			}
		}
		return new JModelObject(mo);
	}

	private void parseProperty(JSONObject props, String name) {
		JSONObject property = props.getJSONObject(name);
		if (!property.has(JModelObject.DB_VALUES) || !(property.getJSONArray(JModelObject.DB_VALUES).length() > 0))
			return;

		JSONArray dbValues = property.getJSONArray(JModelObject.DB_VALUES);
		String possibleUID = dbValues.getString(0);
		if (!(modelObjects.has(possibleUID) || possibleUID.equals(JModelObject.NULL_TAG_UID)))
			return;

		JSONArray resolvedArray = new JSONArray();
		for (int i = 0; i < dbValues.length(); i++) {
			possibleUID = dbValues.getString(i);
			if (modelObjects.has(possibleUID)) {
				resolvedArray.put(modelObjects.getJSONObject(possibleUID));
			} else if (possibleUID.equals(JModelObject.NULL_TAG_UID)) {
				resolvedArray.put(JSONObject.NULL);
			} else {
				// Either they are all UIDs, or they are not
				return;
			}
		}
		JProperty jProperty = new JProperty(property, resolvedArray);
		props.remove(name);
		props.put(name, jProperty);
	}
}
