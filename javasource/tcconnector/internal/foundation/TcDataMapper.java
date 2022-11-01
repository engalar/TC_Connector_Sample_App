// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.core.objectmanagement.member.MendixObjectReference;
import com.mendix.core.objectmanagement.member.MendixObjectReferenceSet;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JModelObject;
import tcconnector.foundation.JServiceData;
import tcconnector.foundation.TcDefaultMappings;
import tcconnector.foundation.TcMapping;
import tcconnector.foundation.TcMappings;

/**
 * Maps data between Teamcenter defined data structures (service operation input
 * and output data types), and Mendix Entities.
 *
 */
public class TcDataMapper {
	public static final String INSTRUCTION_ATTRIBUTE_ARRAY = "AttributeAsArray";
	public static final String INSTRUCTION_DATE = "DateFormat";
	public static final String INSTRUCTION_IGNORE_NULL = "ignoreNull";

	/**
	 * Maps a Teamcenter data structure (service operation response object) to the
	 * Mendix Entity. The {@link TcDefaultMappings} is used to map the Teamcenter
	 * structure element (keys in JSONObject) names to the Entity member (Attributes
	 * or Associations) names.
	 * 
	 * @param context    The Request context.
	 * @param srcJsonObj The source Teamcenter data structure.
	 * @param tgtEntity  The target Mendix Entity.
	 */
	public static void toDomainModelEntity(IContext context, JSONObject srcJsonObj, IMendixObject tgtEntity) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE);
		dm.toEntity(srcJsonObj, tgtEntity);
	}

	public static void toDomainModelEntity(IContext context, JSONObject srcJsonObj, IMendixObject tgtEntity,
			BusinessObjectMappings boMappings, String configurationName) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE, boMappings, configurationName);
		dm.toEntity(srcJsonObj, tgtEntity);
	}

	public static void setDomainModelMemberValue(IContext context, IMendixObject tgtEntity, String memberName,
			Object srcValue, BusinessObjectMappings boMappings, String configurationName) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE, boMappings, configurationName);
		dm.setMemberValue(tgtEntity, memberName, srcValue);
	}

	/**
	 * Maps a Mendix Entity to the Teamcenter data structure (service operation
	 * input object). The {@link TcDefaultMappings} is used to map the Entity member
	 * (Attributes or Associations) names to the Teamcenter structure element (keys
	 * in JSONObject) names.
	 * 
	 * @param context   The Request context.
	 * @param srcEntity The source Mendix Entity.
	 * @return The equivalent JSONObject.
	 */
	public static JSONObject toJSONObject(IContext context, IMendixObject srcEntity) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE);
		return dm.toJSON(srcEntity);
	}

	public static JSONObject toJSONObject(IContext context, IMendixObject srcEntity, Map<String, Object> instructions) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE, instructions);
		return dm.toJSON(srcEntity);
	}

	public static Object toJSONValue(IContext context, IMendixObject srcEntity, String memberName) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE);
		List<IMendixObject> srcEntities = new ArrayList<IMendixObject>();
		srcEntities.add(srcEntity);
		return dm.toJSONValue(srcEntities, memberName);
	}

	public static Object toJSONValue(IContext context, List<IMendixObject> srcEntity, String memberName,
			Map<String, Object> instructions) {
		TcDataMapper dm = new TcDataMapper(context, TcDefaultMappings.INSTANCE, instructions);
		return dm.toJSONValue(srcEntity, memberName);
	}

	private IContext context;
	private TcMappings mappings;
	private Set<IMendixIdentifier> processed = new HashSet<>();
	private Map<String, Object> instructions = new HashMap<>();
	private BusinessObjectMappings boMappings = new BusinessObjectMappings();
	private String associatedMemberName = null;
	private String configurationName;

	private TcDataMapper(IContext context, TcMappings mappings) {
		this.context = context;
		this.mappings = mappings;
	}

	private TcDataMapper(IContext context, TcMappings mappings, BusinessObjectMappings boMappings,
			String configurationName) {
		this.context = context;
		this.mappings = mappings;
		this.boMappings = boMappings;
		this.configurationName = configurationName;
	}

	private TcDataMapper(IContext context, TcMappings mappings, Map<String, Object> instructions) {
		this.context = context;
		this.mappings = mappings;
		this.instructions = instructions;
	}
	// ==========================================================================
	// JSON to Entity
	// ==========================================================================

	private void toEntity(JSONObject srcJsonObj, IMendixObject tgtEntity) {
		Set<TcMapping> jsonToMembers = mappings.getMemberMappings(tgtEntity);
		for (TcMapping mapping : jsonToMembers) {
			String jsonKey = mapping.getTcName();
			String memberName = mapping.getMxName();

			if (!srcJsonObj.has(jsonKey) || srcJsonObj.isNull(jsonKey))
				continue;

			Object jsonValue = srcJsonObj.get(jsonKey);
			setMemberValue(tgtEntity, memberName, jsonValue);
		}
	}

	private void setMemberValue(IMendixObject tgtEntity, String memberName, Object srcJsonValue) {
		if (tgtEntity.hasMember(memberName)) {
			IMendixObjectMember<?> member = tgtEntity.getMember(context, memberName);
			if (member instanceof MendixObjectReference) {
				// Association 1-to-1
				setMemberChildValue(tgtEntity, (MendixObjectReference) member, (JSONObject) srcJsonValue);
			} else if (member instanceof MendixObjectReferenceSet) {
				// Association *-to-1
				if (srcJsonValue instanceof JSONArray) {
					setMemberChildValues(tgtEntity, (MendixObjectReferenceSet) member, (JSONArray) srcJsonValue);
				} else if (srcJsonValue instanceof JSONObject) {
					setMemberChildMap(tgtEntity, memberName, (MendixObjectReferenceSet) member,
							(JSONObject) srcJsonValue);
				}
			} else {
				// Attribute
				IMetaPrimitive primitive = tgtEntity.getMetaObject().getMetaPrimitive(memberName);
				Object tgtMxValue = Primitives.getValue(primitive, srcJsonValue);
				tgtEntity.setValue(context, memberName, tgtMxValue);
			}
		} else {
			// Association 1-to-*
			if (srcJsonValue instanceof JSONArray) {
				setMemberParentValues(tgtEntity, memberName, (JSONArray) srcJsonValue);
			} else if (srcJsonValue instanceof JSONObject) {
				setMemberParentMap(tgtEntity, memberName, (JSONObject) srcJsonValue);
			}
		}
	}

	// tgtEntity(1) ---memberName--- (1)childType
	// Use tgtEntity.get<MemberName>() method to get childType
	private void setMemberChildValue(IMendixObject tgtEntity, MendixObjectReference member, JSONObject srcJsonValue) {
		String childType = member.referenceType();

		if (srcJsonValue instanceof JModelObject) {
			JModelObject jModelObj = (JModelObject) srcJsonValue;
			IMendixObject childEntity = jModelObj.instantiateEntity(context, childType, boMappings, configurationName);
			member.setValue(context, childEntity.getId());
		} else if (srcJsonValue instanceof JServiceData) {
			JServiceData jSD = (JServiceData) srcJsonValue;
			IMendixObject childEntity = jSD.instantiateEntity(context, boMappings, configurationName);
			member.setValue(context, childEntity.getId());
		} else {
			IMendixObject childEntity = Core.instantiate(context, childType);
			toEntity(srcJsonValue, childEntity);
			member.setValue(context, childEntity.getId());
		}
	}

	// tgtEntity(*) ---memberName---> (*)childType
	// Use tgtEntity.get<MemberName>() method to get List<childType>
	private void setMemberChildValues(IMendixObject tgtEntity, MendixObjectReferenceSet member,
			JSONArray srcJsonArray) {
		if (srcJsonArray.length() == 0)
			return;

		String childType = member.referenceType();
		List<IMendixIdentifier> childList = new ArrayList<IMendixIdentifier>();
		boolean isPrimitiveArray = Primitives.isArrayElement(childType);
		IMetaPrimitive valueMember = Primitives.getElementMember(childType);

		for (int i = 0; i < srcJsonArray.length(); i++) {
			Object jsonElement = srcJsonArray.get(i);
			if (jsonElement instanceof JSONObject) {
				if (jsonElement instanceof JModelObject) {
					JModelObject jModelObj = (JModelObject) jsonElement;
					IMendixObject elmentEntity = jModelObj.instantiateEntity(context, childType, boMappings,
							configurationName);
					childList.add(elmentEntity.getId());
				} else {
					IMendixObject elementEntity = Core.instantiate(context, childType);
					toEntity((JSONObject) jsonElement, elementEntity);
					childList.add(elementEntity.getId());
				}
			} else if (isPrimitiveArray) {
				Object elementValue = Primitives.getValue(valueMember, jsonElement);
				IMendixObject elementEntity = Core.instantiate(context, childType);
				elementEntity.setValue(context, valueMember.getName(), elementValue);
				childList.add(elementEntity.getId());
			}
		}
		member.setValue(context, childList);
	}

	// tgtEntity(1) <---memberName--- (*)childType
	// Use Core.retrieveByPath( tgtEntity, memberName ) to get List<childType>
	private void setMemberParentValues(IMendixObject tgtEntity, String memberName, JSONArray srcJsonArray) {
		if (srcJsonArray.length() == 0)
			return;

		IMetaAssociation association = Core.getMetaAssociation(memberName);
		if (association == null) {
			String message = memberName + " is not a valid Association name.";
			Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
			throw new IllegalArgumentException(message);
		}
		String childType = association.getParent().getName();
		boolean isPrimitiveArray = Primitives.isArrayElement(childType);
		IMetaPrimitive valueMember = Primitives.getElementMember(childType);

		for (int i = 0; i < srcJsonArray.length(); i++) {
			Object jsonElement = srcJsonArray.get(i);
			if (jsonElement instanceof JModelObject) {
				JModelObject jModelObj = (JModelObject) jsonElement;
				IMendixObject elmentEntity = jModelObj.instantiateEntity(context, childType, boMappings,
						configurationName);
				((MendixObjectReference) elmentEntity.getMember(context, memberName)).setValue(context,
						tgtEntity.getId());
			} else if (jsonElement instanceof JSONObject) {
				IMendixObject elmentEntity = Core.instantiate(context, childType);
				toEntity((JSONObject) jsonElement, elmentEntity);
				((MendixObjectReference) elmentEntity.getMember(context, memberName)).setValue(context,
						tgtEntity.getId());
			} else if (isPrimitiveArray) {
				Object elementValue = Primitives.getValue(valueMember, jsonElement);
				IMendixObject elmentEntity = Core.instantiate(context, childType);
				elmentEntity.setValue(context, valueMember.getName(), elementValue);
				((MendixObjectReference) elmentEntity.getMember(context, memberName)).setValue(context,
						tgtEntity.getId());
			} else if (jsonElement instanceof String || jsonElement instanceof Integer) {
				IMendixObject elmentEntity = Core.instantiate(context, childType);
				List<? extends IMendixObjectMember<?>> primitives = elmentEntity.getPrimitives(context);

				if (primitives.size() > 1)
					throw new IllegalArgumentException("Failed to to resolve the value for '" + memberName);

				elmentEntity.setValue(context, primitives.get(0).getName().toString(), jsonElement);
				((MendixObjectReference) elmentEntity.getMember(context, memberName)).setValue(context,
						tgtEntity.getId());
			}
		}
	}

	// tgtEntity(*) ---memberName---> (*)MapEntryType
	// Use tgtEntity.get<MemberName>() method to get List<MapEntryType>
	private void setMemberChildMap(IMendixObject tgtEntity, String memberName, MendixObjectReferenceSet member,
			JSONObject srcJsonObj) {
		String mapEntryType = member.referenceType();
		List<IMendixIdentifier> mapEntryIds = new ArrayList<IMendixIdentifier>();

		Iterator<String> keys = srcJsonObj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = srcJsonObj.get(key);

			MapEntry entry = new MapEntry(context, mappings, mapEntryType);
			setMemberValue(entry.getEntry(), entry.getMemberKeyName(), key);
			setMemberValue(entry.getEntry(), entry.getMemberValueName(), value);

			mapEntryIds.add(entry.getEntry().getId());
		}
		member.setValue(context, mapEntryIds);
	}

	// tgtEntity(1) <---memberName--- (*)MapEntry
	// Use Core.retrieveByPath( tgtEntity, memberName ) to get List<MapEntry>
	private void setMemberParentMap(IMendixObject tgtEntity, String memberName, JSONObject srcJsonObj) {
		if (srcJsonObj.length() == 0)
			return;

		IMetaAssociation association = Core.getMetaAssociation(memberName);
		String mapEntryType = association.getParent().getName();

		Iterator<String> keys = srcJsonObj.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = srcJsonObj.get(key);

			MapEntry entry = new MapEntry(context, mappings, mapEntryType);
			setMemberValue(entry.getEntry(), entry.getMemberKeyName(), key);
			setMemberValue(entry.getEntry(), entry.getMemberValueName(), value);

			((MendixObjectReference) entry.getEntry().getMember(context, memberName)).setValue(context,
					tgtEntity.getId());
		}
	}

	// ==========================================================================
	// JSON to Entity
	// ==========================================================================

	private JSONObject toJSON(IMendixObject srcEntity) {
		Constants.LOGGER.trace("Converting " + srcEntity.getType() + " entity to json");
		JSONObject tgtJsonObj = new JSONObject();
		processed.add(srcEntity.getId());

		Set<TcMapping> jsonToMembers = mappings.getMemberMappings(srcEntity);
		for (TcMapping mapping : jsonToMembers) {
			String jsonKey = mapping.getTcName();
			String memberName = mapping.getMxName();

			if (associatedMemberName != null && associatedMemberName.equals(memberName))
				continue;

			List<IMendixObject> srcEntities = new ArrayList<IMendixObject>();
			srcEntities.add(srcEntity);
			Object jsonValue = toJSONValue(srcEntities, memberName);

			if (jsonValue != null)
				tgtJsonObj.put(jsonKey, jsonValue);
		}
		return tgtJsonObj;
	}

	private Object toJSONValue(List<IMendixObject> srcEntity, String memberName) {
		JSONArray mxValues = new JSONArray();
		IMendixObjectMember<?> member = null;
		boolean isMember = false;
		for (IMendixObject leaf : srcEntity) {
			if (leaf.hasMember(memberName)) {
				member = leaf.getMember(context, memberName);
				Object mxValue = member.getValue(context);

				if ((mxValue instanceof IMendixIdentifier && processed.contains((IMendixIdentifier) mxValue))) {
					return null;
				}

				if (mxValue == null) {
					if (!instructions.containsKey(INSTRUCTION_IGNORE_NULL)) {
						if (leaf.getMetaObject().getMetaPrimitive(memberName) != null)
							return "";
						else if (leaf.getMetaObject().getMetaAssociationParent(memberName) != null)
							if (leaf.getMetaObject().getMetaAssociationParent(memberName).getOwner().name()
									.equals("BOTH")) {
								return "";
							} else {
								return new ArrayList<>();
							}
						else if (leaf.getMetaObject().getMetaAssociationChild(memberName) != null)
							if (leaf.getMetaObject().getMetaAssociationChild(memberName).getOwner().name()
									.equals("BOTH")) {
								return "";
							} else {
								return new ArrayList<>();
							}
					}
					return null;
				}

				isMember = true;
				mxValues.put(mxValue);
			}
		}
		// Attributes, and 1-to-1, or *-to-* Associations
		if (isMember)
			return getMemberValue(member, mxValues);

		// 1-to-* Associations
		return getMemberParentValues(srcEntity.get(0), memberName);
	}

	@SuppressWarnings("unchecked")
	// tgtEntity(*) ---memberName---> (*)childType
	// tgtEntity(1) ---memberName---> (1)childType
	private Object getMemberValue(IMendixObjectMember<?> member, Object srcMxValue) {
		try {
			if (member instanceof MendixObjectReference) {
				if (srcMxValue instanceof JSONArray)
					srcMxValue = ((JSONArray) srcMxValue).get(0);
				IMendixObject childEntity = Core.retrieveId(context, (IMendixIdentifier) srcMxValue);
				return toJSONValue(childEntity);
			}

			if (member instanceof MendixObjectReferenceSet) {
				if (srcMxValue instanceof JSONArray)
					srcMxValue = ((JSONArray) srcMxValue).get(0);
				List<IMendixIdentifier> childIds = (List<IMendixIdentifier>) srcMxValue;

				if (childIds.size() == 0)
					return null;

				boolean isMap = MapEntry.isMapEntry(mappings, Core.retrieveId(context, childIds.get(0)));
				if (isMap) {
					return getMemberChildMap(childIds);
				} else {
					return getMemberChildArray(childIds);
				}
			}
			return getAttributeValue(srcMxValue);
		} catch (CoreException e) {
			String message = "Failed to get the child Mendix Entity";
			Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
			throw new IllegalArgumentException(message);
		}
	}

	private Object getAttributeValue(Object srcAttributeValue) {
		Object tgtValue = srcAttributeValue;

		if (srcAttributeValue instanceof JSONArray)
			tgtValue = ((JSONArray) srcAttributeValue).get(0);

		// Wrap up the value in an JSONArray
		if (instructions.containsKey(INSTRUCTION_ATTRIBUTE_ARRAY)) {
			JSONArray tgtArray = new JSONArray();
			if (srcAttributeValue instanceof JSONArray) {
				for (int index = 0; index < ((JSONArray) srcAttributeValue).length(); index++) {
					Object tgtVal = parseDateAndDecimal(((JSONArray) srcAttributeValue).get(index));
					tgtArray.put(tgtVal);
				}
			} else
				tgtArray.put(tgtValue);

			tgtValue = tgtArray;
		} else
			tgtValue = parseDateAndDecimal(tgtValue);

		return tgtValue;
	}

	private Object parseDateAndDecimal(Object tgtValue) {
		// Date is converted to String, other primitives can be added directly to the
		// JSONObject
		if (tgtValue instanceof BigDecimal) {
			tgtValue = new Double(((BigDecimal) tgtValue).doubleValue());
		}
		if (tgtValue instanceof Date) {
			// Use caller supplied date format
			if (instructions.containsKey(INSTRUCTION_DATE))
				tgtValue = PropertyResolver.serializeDate((Date) tgtValue,
						(SimpleDateFormat) instructions.get(INSTRUCTION_DATE));
			else
				tgtValue = PropertyResolver.serializeDate((Date) tgtValue);
		}
		return tgtValue;
	}

	// tgtEntity(1) <---memberName--- (*)childType
	private Object getMemberParentValues(IMendixObject srcMxObject, String memberName) {
		List<IMendixObject> childEntities = Core.retrieveByPath(context, srcMxObject, memberName, true);
		if (childEntities.size() == 0)
			return null;
		boolean isMap = MapEntry.isMapEntry(mappings, childEntities.get(0));
		associatedMemberName = memberName;
		if (isMap) {
			return getMemberParentMap(childEntities);
		} else {
			return getMemberParentArray(childEntities);
		}
	}

	private Object getMemberParentMap(List<IMendixObject> srcEntries) {
		JSONObject tgtMap = new JSONObject();
		for (IMendixObject srcEntry : srcEntries) {
			if (srcEntry == null || processed.contains(srcEntry.getId()))
				continue;

			JSONObject tgtEntry = toJSON(srcEntry);
			tgtMap.put(tgtEntry.getString(MapEntry.KEY), tgtEntry.get(MapEntry.VALUE));
		}
		return (tgtMap.length() > 0) ? tgtMap : null;
	}

	private Object getMemberChildMap(List<IMendixIdentifier> srcEntryIds) throws CoreException {
		JSONObject tgtMap = new JSONObject();
		for (IMendixIdentifier srcEntryId : srcEntryIds) {
			IMendixObject srcEntry = Core.retrieveId(context, srcEntryId);
			JSONObject tgtEntry = toJSON(srcEntry);
			tgtMap.put(tgtEntry.getString(MapEntry.KEY), tgtEntry.get(MapEntry.VALUE));
		}
		return tgtMap;
	}

	private Object getMemberParentArray(List<IMendixObject> srcElements) {
		JSONArray tgtArray = new JSONArray();
		for (IMendixObject srcElement : srcElements) {
			if (srcElement == null || processed.contains(srcElement.getId()))
				continue;

			Object tgtElment = toJSONValue(srcElement);
			tgtArray.put(tgtElment);
		}
		return (tgtArray.length() > 0) ? tgtArray : null;
	}

	private Object getMemberChildArray(List<IMendixIdentifier> srcElementIds) throws CoreException {
		JSONArray tgtArray = new JSONArray();
		for (IMendixIdentifier srcElementId : srcElementIds) {
			IMendixObject srcElement = Core.retrieveId(context, srcElementId);
			Object tgtElment = toJSONValue(srcElement);
			tgtArray.put(tgtElment);
		}
		return tgtArray;
	}

	private Object toJSONValue(IMendixObject srcChildEntity) {
		if (mappings.isAModelObjectEntity(srcChildEntity))
			return new JModelObject(context, srcChildEntity);
		else if (Primitives.isArrayElement(srcChildEntity)) {
			IMetaPrimitive member = Primitives.getElementMember(srcChildEntity);
			return srcChildEntity.getMember(context, member.getName()).getValue(context);
		} else
			return toJSON(srcChildEntity);
	}
}
