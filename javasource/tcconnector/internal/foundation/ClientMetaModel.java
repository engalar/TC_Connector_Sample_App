package tcconnector.internal.foundation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.TcConnection;

public class ClientMetaModel {
	private static final String KEY_TYPE_NAMES = "typeNames";
	private static final String KEY_OPTIONS = "options";
	private static final String KEY_PROPERTY_EXCLUSIONS = "PropertyExclusions";
	private static final String KEY_TYPE_EXCLUSIONS = "TypeExclusions";
	private static final String EXCLUDE_LOV_REFERENCES = "LovReferences";
	private static final String EXCLUDE_NAMING_RULES = "NamingRules";
	private static final String EXCLUDE_RENDERER_REFERENCES = "RendererReferences";
	private static final String EXCLUDE_DIRECT_CHILD_TYPE_INFO = "DirectChildTypesInfo";
	private static final String EXCLUDE_REVISION_NAMING_RULES = "RevisionNamingRules";
	private static final String EXCLUDE_TOOL_INFO = "ToolInfo";

	private static final String KEY_TYPES = "types";
	private static final String KEY_TYPE_NAME = "name";
	private static final String KEY_TYPE_HIERARCHY = "typeHierarchy";

	private static Map<String, Map<String, List<String>>> tcTypeHierarchyMap = new HashMap<>();
	private static List<String> availableServices = new ArrayList<>();

	public static List<String> getAvailableServices(IContext context, String configurationName) throws Exception {
		List<String> serviceNameList = new ArrayList<>();
		JSONObject jsonResponse = TcConnection.callTeamcenterService(context,
				Constants.OPERATION_GET_AVAILABLE_SERVICES, new JSONObject(), new JSONObject(), configurationName);
		JSONArray serviceNames = jsonResponse.getJSONArray("serviceNames");
		for (int i = 0; i < serviceNames.length(); ++i) {
			String serviceName = (String) serviceNames.get(i);
			serviceName = serviceName.replace(".", "/");
			serviceNameList.add(serviceName);
		}
		return serviceNameList;
	}

	/**
	 * Get the given business object's type hierarchy. The 0 element in the list is
	 * the type itself, with successive entries being the next parent type.
	 * 
	 * @param typeName The business object type name.
	 */
	public static List<String> getTypeHierarchy(String typeName, String configurationName) {
		// Get the information from the cache.
		synchronized (tcTypeHierarchyMap) {
			if (configurationName != null && tcTypeHierarchyMap.containsKey(configurationName)) {
				Map<String, List<String>> tempTypeHierarchyMap = tcTypeHierarchyMap.get(configurationName);
				if (tempTypeHierarchyMap.containsKey(typeName)) {
					return tempTypeHierarchyMap.get(typeName);
				}
			}
		}
		// At a minimum the type itself is the hierarchy
		List<String> hierarchy = new ArrayList<>();
		hierarchy.add(typeName);
		return hierarchy;
	}

	/**
	 * Ensure the named types exist in the Client Meta Model cache.
	 * 
	 * @param context
	 * @param typeNames
	 */
	public static void ensureTypesAreLoaded(IContext context, Set<String> typeNames, String configurationName) {
		String getTypeHierarchyService = Constants.OPERATION_GETTYPEDESCRIPTION;

		if (availableServices.isEmpty()) {
			try {
				availableServices.addAll(getAvailableServices(context, configurationName));
			} catch (Exception e) {
			}
		}

		JSONArray typeNamesInput = getListOfUnknownTypes(typeNames, configurationName);
		if (typeNamesInput.length() == 0)
			return;

		JSONObject jsonInputObj = new JSONObject();
		jsonInputObj.put(KEY_TYPE_NAMES, typeNamesInput);
		// Service Constants.OPERATION_GETTYPEDESCRIPTION2 performs better so use it if
		// available.

		// Service Constants.OPERATION_GETTYPEDESCRIPTION2 is available from TC10.6
		// onwards
		// Service Constants.OPERATION_GETTYPEDESCRIPTION is available from TC9.0
		// onwards
		if (availableServices.contains(Constants.OPERATION_GETTYPEDESCRIPTION2)) {
			getTypeHierarchyService = Constants.OPERATION_GETTYPEDESCRIPTION2;
			jsonInputObj.put(KEY_OPTIONS, getOptions());
		}
		try {
			JSONObject jsonResponse = TcConnection.callTeamcenterService(context, getTypeHierarchyService, jsonInputObj,
					new JSONObject(), configurationName);
			cacheTypeHierarchy(jsonResponse, configurationName);
		} catch (Exception e) {
		}
	}

	private static JSONArray getListOfUnknownTypes(Set<String> typeNames, String configurationName) {
		JSONArray typeNamesInput = new JSONArray();
		synchronized (tcTypeHierarchyMap) {
			if (tcTypeHierarchyMap.containsKey(configurationName)) {
				Map<String, List<String>> tempTypeHierarchyMap = tcTypeHierarchyMap.get(configurationName);

				for (String typeName : typeNames) {
					if (!tempTypeHierarchyMap.containsKey(typeName)) {
						typeNamesInput.put(typeName);
					}
				}
			} else {
				for (String typeName : typeNames) {
					typeNamesInput.put(typeName);
				}
			}
		}
		return typeNamesInput;
	}

	private static JSONObject getOptions() {
		JSONArray propertyExclusions = new JSONArray();
		propertyExclusions.put(EXCLUDE_LOV_REFERENCES);
		propertyExclusions.put(EXCLUDE_NAMING_RULES);
		propertyExclusions.put(EXCLUDE_RENDERER_REFERENCES);

		JSONArray typeExclusions = new JSONArray();
		typeExclusions.put(EXCLUDE_DIRECT_CHILD_TYPE_INFO);
		typeExclusions.put(EXCLUDE_REVISION_NAMING_RULES);
		typeExclusions.put(EXCLUDE_TOOL_INFO);

		JSONObject options = new JSONObject();
		options.put(KEY_PROPERTY_EXCLUSIONS, propertyExclusions);
		options.put(KEY_TYPE_EXCLUSIONS, typeExclusions);

		return options;
	}

	private static void cacheTypeHierarchy(JSONObject jsonResponse, String configurationName) {
		JSONArray typesArray = jsonResponse.getJSONArray(KEY_TYPES);
		synchronized (tcTypeHierarchyMap) {
			for (int index = 0; index < typesArray.length(); index++) {
				List<String> typeHierarchyValues = getTypeHierarchy(typesArray.getJSONObject(index));
				Map<String, List<String>> tempTypeHierarchyMap = new Hashtable<>();
				if (tcTypeHierarchyMap.containsKey(configurationName)) {
					tempTypeHierarchyMap = tcTypeHierarchyMap.get(configurationName);
					tempTypeHierarchyMap.put(typesArray.getJSONObject(index).getString(KEY_TYPE_NAME),
							typeHierarchyValues);
					tcTypeHierarchyMap.put(configurationName, tempTypeHierarchyMap);
				} else {
					tempTypeHierarchyMap.put(typesArray.getJSONObject(index).getString(KEY_TYPE_NAME),
							typeHierarchyValues);
					tcTypeHierarchyMap.put(configurationName, tempTypeHierarchyMap);
				}
			}
		}

	}

	private static List<String> getTypeHierarchy(JSONObject type) {
		List<String> typeHierarchyValues = new ArrayList<>();
		String typeHierarchy = type.getString(KEY_TYPE_HIERARCHY);

		for (String typeValue : typeHierarchy.split(","))
			typeHierarchyValues.add(typeValue.trim());

		return typeHierarchyValues;
	}
}
