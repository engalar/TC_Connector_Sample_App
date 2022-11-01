// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JModelObject;
import tcconnector.foundation.TcModelObjectMappings;

/**
 * Maps Teamcenter service operation Request or Response to/from Mendix Entity.
 * The Mapping file contains a JSON template for the given service operation
 * Request and Response with developer defined mappings. Values in the JSON
 * template are either hard coded values, or use a $Input or $Response
 * substitution keyword.
 * 
 * The substitution keys have the following syntax
 * 
 * <pre>
 * $Input[/Association]&lt;/Attribute&gt;[;Instruction]
 * $Response[/Association]&lt;/Attribute&gt;
 * 
 * Where:
 *      Association    Optional Association name on the given Entity type. 
 *                     Multiple Associations can be sequenced, each separated by a '/'
 *      Attribute      Optional Attribute name on the given Entity type.
 *      Instruction    Optional instruction to be applied to the substitution.
 *                     Multiple instruction can be used, each separated with a ';'
 *                     Supported instructions are:
 *                          AttributeAsArray    Create single valued JSONArray for each Attribute value.
 *                          DateFormat=Format   Use the custom date 'Format' for serializing Date Attributes.
 *
 * Examples:
 *      $Input                             The full Entity will be mapped.
 *      $Input/TcConnector.itemRev         The Entity referenced by the 'TcConnector.itemRev' Association will be mapped.
 *      $Input/TcConnector.user/person     The 'person' Attribute on the referenced Entity ('TcConnector.user' Association) will be mapped.
 *      $Input;DateFormat=MM/dd/yyyy       The full Entity will be mapped, with any Date Attributes serialized as 'MM/dd/yyyy'
 * </pre>
 *
 * The Entity Member (Attribute or Association) names mapped one-to-one with the
 * Teamcenter service operation data structure with the following caveats:
 * <ul>
 * <li>The Entity Member name may be prefixed with a '_', i.e. '_type'. In this
 * case the '_' is ignored, thus matching the Teamcenter name of 'type'.</li>
 * <li>The Entity Member name may be suffixed with '__XXX', i.e. 'phone__Home'.
 * In this case the '__Home' is ignored, thus matching the Teamcenter name of
 * 'phone'.</li>
 * </ul>
 */
public class OperationMapper {
	public static final String KEY_SERVICE_OPERATION = "ServiceOperation";
	public static final String KEY_INPUT_TYPE = "InputType";
	public static final String KEY_RESPONSE_TYPE = "ResponseType";
	public static final String KEY_OBJECT_MAPPING = "ObjectMapping";
	public static final String KEY_OPERATION_INPUT = "OperationInput";
	public static final String KEY_OPERATION_RESPONSE = "OperationResponse";

	private static final String INPUT_KEY = "$Input";
	private static final String REPONSE_KEY = "$Response";
	private static final String GET_ID = "getId()";
	private static String configurationName;

	/**
	 * Maps a Mendix Entity to the Teamcenter service operation input JSONObject.
	 * 
	 * @param context     The Request context.
	 * @param srcEntity   The source Mendix Entity service operation input.
	 * @param mappingFile The path to the operation mapping file.
	 * @return The service operation input JSONObject.
	 */
	public static JSONObject toJSONObject(IContext context, IMendixObject srcEntity, String mappingFile) {
		if (mappingFile == null)
			return TcDataMapper.toJSONObject(context, srcEntity);

		OperationMapper mapper = new OperationMapper(context, mappingFile);
		return mapper.resolveInput(srcEntity);
	}

	/**
	 * Maps a Teamcenter service operation response JSONObject to a Mendix Entity.
	 * 
	 * @param context     The Request context.
	 * @param srcJsonObj  The source JSON response object.
	 * @param tgtEntity   The target Mendix Entity response object.
	 * @param mappingFile The path to the operation mapping file.
	 * @param boMappings  The business object mappings.
	 */
	public static void toDomainModelEntity(IContext context, JSONObject srcJsonObj, IMendixObject tgtEntity,
			String mappingFile, BusinessObjectMappings boMappings, String configurationName) {
		if (mappingFile == null)
			TcDataMapper.toDomainModelEntity(context, srcJsonObj, tgtEntity);

		OperationMapper mapper = new OperationMapper(context, mappingFile, boMappings, configurationName);
		mapper.resolveResponse(srcJsonObj, tgtEntity);
	}

	public static BusinessObjectMappings getBusinessObjectMappings(String mappingFile,
			BusinessObjectMappings boMappings) {
		if (boMappings == null) {
			JSONObject opMapping = OperationMappingCache.getMapping(mappingFile);
			boMappings = new BusinessObjectMappings(opMapping.getString(KEY_OBJECT_MAPPING), configurationName);
		}
		return boMappings;
	}

	private IContext context;
	private JSONObject operationMapObj;
	private String replacePrefix;
	private BusinessObjectMappings boMapping;

	private OperationMapper(IContext context, String mappingFile) {
		this.context = context;
		this.operationMapObj = OperationMappingCache.getMapping(mappingFile);
		this.replacePrefix = "";
		this.boMapping = null;
	}

	private OperationMapper(IContext context, String mappingFile, BusinessObjectMappings boMapping,
			String configurationName) {
		this.context = context;
		this.operationMapObj = OperationMappingCache.getMapping(mappingFile);
		this.replacePrefix = "";
		this.boMapping = boMapping;
		OperationMapper.configurationName = configurationName;
	}

	private JSONObject resolveInput(IMendixObject srcEntity) {
		Constants.LOGGER.trace("Resolving the input");
		JSONObject inputObjTemplate = operationMapObj.getJSONObject(KEY_OPERATION_INPUT);
		if (inputObjTemplate.toString().contains(INPUT_KEY)) {
			if (srcEntity == null)
				throw new IllegalArgumentException("The InputArgument cannot be null.");
		} else
			return inputObjTemplate;

		replacePrefix = INPUT_KEY;
		JSONObject mappedInputObj = resolveObject(inputObjTemplate, srcEntity);
		return mappedInputObj;
	}

	private void resolveResponse(JSONObject srcJsonObj, IMendixObject tgtEntity) {
		replacePrefix = REPONSE_KEY;
		if (boMapping == null)
			boMapping = new BusinessObjectMappings(operationMapObj.getString(KEY_OBJECT_MAPPING), configurationName);

		if (operationMapObj.isJSONObject(KEY_OPERATION_RESPONSE)) {
			JSONObject inputObjTemplate = operationMapObj.getJSONObject(KEY_OPERATION_RESPONSE);
			resolveObject(inputObjTemplate, srcJsonObj, tgtEntity);
		} else {
			String templateValue = operationMapObj.getString(KEY_OPERATION_RESPONSE);
			resolveValue(templateValue, srcJsonObj, tgtEntity);
		}
	}

	// ==========================================================================
	// Entity to JSON
	// ==========================================================================
	private JSONObject resolveObject(JSONObject templateObj, IMendixObject srcEntity) {
		Constants.LOGGER.trace("Resolving the json object: \n" + templateObj);
		JSONObject tgtObj = new JSONObject();
		Iterator<String> keys = templateObj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Object srcValue = templateObj.get(key);
			Object tgtValue = resolveAny(srcValue, srcEntity);
			if (key.startsWith(replacePrefix)) {
				key = (String) resolveValue(key, srcEntity);
			}
			tgtObj.put(key, tgtValue);
		}
		return tgtObj;
	}

	private JSONArray resolveArray(JSONArray templateArray, IMendixObject srcEntity) {
		Constants.LOGGER.trace("Resolving : \n" + templateArray);
		if (templateArray.length() == 0)
			return new JSONArray();

		Object templateValue = templateArray.get(0);
		if (templateValue instanceof String) {
			String templateSValue = (String) templateValue;
			if (templateSValue.startsWith(replacePrefix) && !templateSValue.equals(replacePrefix)) {
				return resolveArrayValue(templateSValue, srcEntity);
			}
		}
		JSONArray tgtArray = new JSONArray();
		for (int i = 0; i < templateArray.length(); i++) {
			Object srcValue = templateArray.get(i);
			Object tgtValue = resolveAny(srcValue, srcEntity);
			tgtArray.put(tgtValue);
		}
		return tgtArray;
	}

	private Object resolveAny(Object templateAny, IMendixObject srcEntity) {
		Constants.LOGGER.trace("Resolving : \n" + templateAny);
		if (templateAny instanceof JSONObject) {
			return resolveObject((JSONObject) templateAny, srcEntity);
		}
		if (templateAny instanceof JSONArray) {
			return resolveArray((JSONArray) templateAny, srcEntity);
		}
		if (templateAny instanceof String) {
			String templateValue = (String) templateAny;
			if (templateValue.startsWith(replacePrefix)) {
				return resolveValue(templateValue, srcEntity);
			}
		}
		return templateAny;
	}

	private Object resolveValue(String replacement, IMendixObject srcEntity) {
		Constants.LOGGER.trace("Resolving value for " + replacement);
		try {
			OperationSubstitution opSubs = new OperationSubstitution(context, replacement, srcEntity);
			List<IMendixObject> leafEntity = opSubs.getLeafEntity();
			String attributeName = opSubs.getAttributeName();
			Map<String, Object> instructions = opSubs.getInstructions();

			if (attributeName.length() == 0) {
				// The replacement is the $Input, so srceEntity and leafEntity are the same
				if (TcModelObjectMappings.INSTANCE.isAModelObjectEntity(leafEntity.get(0)))
					return new JModelObject(context, leafEntity.get(0));
				return TcDataMapper.toJSONObject(context, srcEntity, instructions);
			}
			if (attributeName.equals(GET_ID)) {
				return Long.toString(leafEntity.get(0).getId().toLong());
			}
			return TcDataMapper.toJSONValue(context, leafEntity, attributeName, instructions);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
			throw new IllegalArgumentException(
					"Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
		}
	}

	private JSONArray resolveArrayValue(String replacement, IMendixObject srcEntity) {
		try {
			OperationSubstitution opSubs = new OperationSubstitution(context, replacement, srcEntity);
			String associationName = opSubs.getAssociationName();
			IMendixObject leafParentEntity = opSubs.createLeafParentEntity();
			Map<String, Object> instructions = opSubs.getInstructions();

			List<IMendixObject> leafParentEntities = new ArrayList<IMendixObject>();
			leafParentEntities.add(leafParentEntity);
			return (JSONArray) TcDataMapper.toJSONValue(context, leafParentEntities, associationName, instructions);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
			throw new IllegalArgumentException(
					"Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
		}
	}

	// ==========================================================================
	// JSON to Entity
	// ==========================================================================
	private void resolveObject(JSONObject templateObj, JSONObject srcObj, IMendixObject tgtEntity) {
		Iterator<String> keys = templateObj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Object templateValue = templateObj.get(key);

			if (!srcObj.has(key))
				continue;
			Object srcValue = srcObj.get(key);
			if (srcValue == null)
				continue;

			resolveAny(templateValue, srcValue, tgtEntity);
		}
	}

	private void resolveArray(JSONArray templateArray, JSONArray srcArray, IMendixObject tgtEntity) {
		Object templateValue = templateArray.get(0);

		if (templateValue instanceof String) {
			String templateSValue = (String) templateValue;
			if (templateSValue.startsWith(replacePrefix) && !templateSValue.equals(replacePrefix)) {
				resolveArrayValue(templateSValue, srcArray, tgtEntity);
				return;
			}
		}
		for (int i = 0; i < srcArray.length(); i++) {
			Object srcValue = srcArray.get(i);
			resolveAny(templateValue, srcValue, tgtEntity);
		}
	}

	private void resolveAny(Object templateAny, Object srcAny, IMendixObject tgtEntity) {
		if (templateAny instanceof JSONObject) {
			resolveObject((JSONObject) templateAny, (JSONObject) srcAny, tgtEntity);
		}
		if (templateAny instanceof JSONArray) {
			resolveArray((JSONArray) templateAny, (JSONArray) srcAny, tgtEntity);
		}
		if (templateAny instanceof String) {
			String templateValue = (String) templateAny;
			if (templateValue.startsWith(replacePrefix))
				resolveValue(templateValue, srcAny, tgtEntity);
		}
	}

	private void resolveValue(String replacement, Object srcAny, IMendixObject tgtEntity) {
		try {
			OperationSubstitution opSubs = new OperationSubstitution(context, replacement, tgtEntity);
			String memberName = opSubs.getAssociationName();
			IMendixObject leafParentEntity = opSubs.createLeafParentEntity();

			String[] elements = replacement.split("/");
			int eleIndex = 0;
			for (eleIndex = 0; eleIndex < elements.length; eleIndex++) {
				if (elements[eleIndex].equals(memberName))
					break;
			}
			String traverseName = null;
			if (eleIndex > 1)
				traverseName = elements[eleIndex - 1];

			if (memberName.length() == 0) {
				// Just the $Response was given for the replacement, so tgtEntity and leafParent
				// are the same
				if (srcAny instanceof JModelObject) {
					((JModelObject) srcAny).initializeEntity(context, leafParentEntity, boMapping, configurationName);
				} else {
					TcDataMapper.toDomainModelEntity(context, (JSONObject) srcAny, leafParentEntity, boMapping,
							configurationName);
				}
				return;
			}
			TcDataMapper.setDomainModelMemberValue(context, leafParentEntity, memberName, srcAny, boMapping,
					configurationName);
			if (traverseName != null) {
				if (leafParentEntity.hasMember(traverseName))
					leafParentEntity.setValue(context, traverseName, tgtEntity.getId());
				else
					tgtEntity.setValue(context, traverseName, leafParentEntity.getId());
			}
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
			throw new IllegalArgumentException(
					"Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
		}
	}

	private void resolveArrayValue(String replacement, Object srcAny, IMendixObject tgtEntity) {
		try {
			OperationSubstitution opSbus = new OperationSubstitution(context, replacement, tgtEntity);
			String associationName = opSbus.getAssociationName();
			IMendixObject leafParentEntity = opSbus.createLeafParentEntity();

			TcDataMapper.setDomainModelMemberValue(context, leafParentEntity, associationName, srcAny, boMapping,
					configurationName);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
			throw new IllegalArgumentException(
					"Failed to to resolve the value for '" + replacement + "'. " + e.getMessage());
		}
	}
}
