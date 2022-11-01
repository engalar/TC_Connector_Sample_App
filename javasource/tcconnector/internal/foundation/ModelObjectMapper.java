package tcconnector.internal.foundation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
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
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JModelObject;
import tcconnector.foundation.TcMapping;
import tcconnector.foundation.TcModelObjectMappings;
import tcconnector.foundation.exceptions.NotLoadedExcpetion;
import tcconnector.proxies.ModelObject;

public class ModelObjectMapper {
	public static void initializeEntity(IContext context, JModelObject srcObj, IMendixObject tgtEntity,
			TcModelObjectMappings mappings, BusinessObjectMappings boMappings, String configurationName) {
		ModelObjectMapper mapper = new ModelObjectMapper(context, mappings, boMappings, configurationName);
		mapper.initializeEntity(srcObj, tgtEntity);
	}

	public static void validateTargetType(IMendixObject tgtEntity) {
		if (TcModelObjectMappings.INSTANCE.isAModelObjectEntity(tgtEntity))
			return;

		String myName = tgtEntity.getMetaObject().getName();
		List<? extends IMetaObject> parents = tgtEntity.getMetaObject().getSuperObjects();
		List<String> parentNames = new ArrayList<>();
		Iterator<? extends IMetaObject> pIt = parents.iterator();
		pIt.forEachRemaining(m -> {
			parentNames.add(m.getName());
		});

		Constants.LOGGER
				.error(LogCorrelationID.getId() + ": The Entity type " + myName + " is not a " + ModelObject.entityName
						+ " and does not a extend from " + ModelObject.entityName + " " + parentNames.toString() + ".");
		throw new IllegalArgumentException("The Entity type " + myName + " is not a " + ModelObject.entityName + ".");
	}

	private Set<String> processed;
	private Map<String, IMendixIdentifier> processedUIDToEntityIDMap;
	private IContext context;
	TcModelObjectMappings mappings;
	BusinessObjectMappings boMappings;
	private String configurationName;

	private ModelObjectMapper(IContext context, TcModelObjectMappings mappings, BusinessObjectMappings boMappings,
			String configurationName) {
		this.processed = new HashSet<>();
		this.processedUIDToEntityIDMap = new HashMap<String, IMendixIdentifier>();
		this.context = context;
		this.mappings = mappings;
		this.boMappings = boMappings;
		this.configurationName = configurationName;
	}

	private void initializeEntity(JModelObject srcObj, IMendixObject tgtEntity) {
		srcObj.setMappedEnitty(tgtEntity);

		initializeModelObjectEntity(srcObj, tgtEntity);
		initializeTypedModelObjectEntity(srcObj, tgtEntity);
	}

	private void initializeModelObjectEntity(JModelObject srcObj, IMendixObject tgtEntity) {
		validateTargetType(tgtEntity);

		ModelObject tgtModelObj = ModelObject.initialize(context, tgtEntity);
		tgtModelObj.setUID(srcObj.getUID());
		tgtModelObj.set_Type(srcObj.getType());
		tgtModelObj.setClassName(srcObj.getClassName());

		if (configurationName == null || configurationName.equals("") || configurationName.isEmpty()) {
			configurationName = tcconnector.proxies.microflows.Microflows
					.retrieveConfigNameFromSingleActiveConfiguration(context);
			if (configurationName.isEmpty())
				return;
		}
	}

	public void initializeTypedModelObjectEntity(JModelObject srcObj, IMendixObject tgtEntity) {
		String myName = tgtEntity.getMetaObject().getName();
		if (!srcObj.has(JModelObject.PROPS) || srcObj.isNull(JModelObject.PROPS)
				|| ModelObject.entityName.equals(myName))
			return;

		processed.add(srcObj.getUID());
		processedUIDToEntityIDMap.put(srcObj.getUID(), tgtEntity.getId());
		Set<TcMapping> jsonToMembers = mappings.getMemberMappings(tgtEntity);
		for (TcMapping mapping : jsonToMembers) {
			String jsonKey = mapping.getTcName();
			String memberName = mapping.getMxName();

			if (tgtEntity.hasMember(memberName)) {
				IMendixObjectMember<?> member = tgtEntity.getMember(context, memberName);
				if (member instanceof MendixObjectReferenceSet) {
					// tgtEntity(*) ---memberName---> (*)childType
					// Use tgtEntity.get<MemberName>() method to get List<childType>
					setReferencedChildValues(srcObj, tgtEntity, (MendixObjectReferenceSet) member, jsonKey);
				} else if (member instanceof MendixObjectReference) {
					setRefernecedValue(srcObj, tgtEntity, (MendixObjectReference) member, jsonKey);
				} else if (tgtEntity.hasMember(memberName)) {
					setEntityMemberPrimitiveValue(srcObj, tgtEntity, memberName, jsonKey);
				}
			} else {
				// tgtEntity(1) <---memberName--- (*)childType
				// Use Core.retrieveByPath( tgtEntity, memberName ) to get List<childType>
				IMetaAssociation association = Core.getMetaAssociation(memberName);
				String childType = association.getParent().getName();
				setReferencedParentValues(srcObj, tgtEntity, memberName, childType, jsonKey);
			}
		}
	}

	private void setRefernecedValue(JModelObject srcObj, IMendixObject tgtEntity, MendixObjectReference member,
			String jsonKey) {
		try {
			String referenceType = member.referenceType();
			JModelObject childObj = srcObj.getPropertyValueAsModelObject(jsonKey);
			if (childObj == null)
				return;

			if (boMappings != null) {
				String type = childObj.getType();
				String entityName = boMappings.getEntityName(type, null);
				List<? extends IMetaObject> superObjs = Core.getMetaObject(entityName).getSuperObjects();

				for (IMetaObject object : superObjs) {
					if (referenceType.equals(object.getName())) {
						referenceType = entityName;
						break;
					}
				}
			}

			IMendixObject childEntity;
			if (!processed.contains(childObj.getUID()))
				childEntity = initializReferencedEntity(referenceType, childObj);
			else {
				IMendixIdentifier childEntityIdentifier = processedUIDToEntityIDMap.get(childObj.getUID());
				childEntity = Core.retrieveId(context, childEntityIdentifier);
			}
			tgtEntity.setValue(context, member.getName(), childEntity.getId());
		} catch (NotLoadedExcpetion | CoreException e) {
		} // Quietly ignore this jsonKey
	}

	// tgtEntity(*) ---memberName---> (*)childType
	// Use tgtEntity.get<MemberName>() method to get List<childType>
	private void setReferencedChildValues(JModelObject srcObj, IMendixObject tgtEntity, MendixObjectReferenceSet member,
			String jsonKey) {
		try {
			String childType = member.referenceType();
			if (mappings.isAModelObjectEntity(Core.getMetaObject(childType))) {

				List<JModelObject> childObjs = srcObj.getPropertyValueAsModelObjects(jsonKey);
				List<IMendixIdentifier> tgtList = new ArrayList<IMendixIdentifier>();
				for (JModelObject childObj : childObjs) {
					IMendixObject childEntity = initializReferencedEntity(childType, childObj);
					tgtList.add(childEntity.getId());
				}
				member.setValue(context, tgtList);
			} else {
				Collection<? extends IMetaPrimitive> elementMembers = Core.getMetaObject(childType).getMetaPrimitives();
				if (elementMembers.size() != 1)
					return;
				IMetaPrimitive firstMember = elementMembers.toArray(new IMetaPrimitive[elementMembers.size()])[0];

				List<?> childObjs = srcObj.getPropertyValues(firstMember, jsonKey);
				setEntityMemberPrimitiveList(tgtEntity, member, firstMember, childObjs);
			}
		} catch (NotLoadedExcpetion e) {
		} // Quietly ignore this jsonKey
	}

	// tgtEntity(1) <---memberName--- (*)childType
	// Use Core.retrieveByPath( tgtEntity, memberName ) to get List<childType>
	private void setReferencedParentValues(JModelObject srcObj, IMendixObject tgtEntity, String memberName,
			String childType, String jsonKey) {
		try {
			if (mappings.isAModelObjectEntity(Core.getMetaObject(childType))) {
				List<IMendixObject> memberList = Core.retrieveByPath(context, tgtEntity, memberName);
				List<String> uidList = new ArrayList<String>();

				Map<String, IMendixObject> memeberUidMap = new HashMap<String, IMendixObject>();
				for (int cnt = 0; cnt < memberList.size(); ++cnt) {
					IMendixObjectMember<?> member = memberList.get(cnt).getMembers(context).get("UID");

					Object value = member.getValue(context);
					String memberUid = "";
					if (value != null) {
						memberUid = value.toString();
					}
					uidList.add(memberUid);
					memeberUidMap.put(memberUid, memberList.get(cnt));
				}

				List<JModelObject> childObjs = srcObj.getPropertyValueAsModelObjects(jsonKey);
				for (JModelObject childObj : childObjs) {
					if (!memeberUidMap.containsKey(childObj.getUID())) {
						IMendixObject childEntity = initializReferencedEntity(childType, childObj);
						if (childEntity.getMember(context, memberName) instanceof MendixObjectReferenceSet)
							setReferencedChildValues(srcObj, tgtEntity,
									(MendixObjectReferenceSet) childEntity.getMember(context, memberName), jsonKey);
						else if (childEntity.getMember(context, memberName) instanceof MendixObjectReference)
							((MendixObjectReference) childEntity.getMember(context, memberName)).setValue(context,
									tgtEntity.getId());
					} else {
						memeberUidMap.remove(childObj.getUID());
					}
				}
				for (Map.Entry<String, IMendixObject> entry : memeberUidMap.entrySet()) {
					((MendixObjectReference) entry.getValue().getMember(context, memberName)).setValue(context, null);
				}
			} else {
				Collection<? extends IMetaPrimitive> elementMembers = Core.getMetaObject(childType).getMetaPrimitives();
				if (elementMembers.size() != 1)
					return;
				IMetaPrimitive firstMember = elementMembers.toArray(new IMetaPrimitive[elementMembers.size()])[0];
				List<?> childObjs = srcObj.getPropertyValues(firstMember, jsonKey);
				for (Object childObj : childObjs) {
					IMendixObject childEntity = Core.instantiate(context, childType);
					if (childObj instanceof Double)
						childEntity.setValue(context, firstMember.getName(), new BigDecimal((Double) childObj));
					else
						childEntity.setValue(context, firstMember.getName(), childObj);

					if (childEntity.getMember(context, memberName) instanceof MendixObjectReferenceSet)
						setReferencedChildValues(srcObj, tgtEntity,
								(MendixObjectReferenceSet) childEntity.getMember(context, memberName), jsonKey);
					else if (childEntity.getMember(context, memberName) instanceof MendixObjectReference)
						((MendixObjectReference) childEntity.getMember(context, memberName)).setValue(context,
								tgtEntity.getId());
				}
			}
		} catch (NotLoadedExcpetion e) {
		} // Quietly ignore this jsonKey
	}

	private IMendixObject initializReferencedEntity(String referenceType, JModelObject childObj) {
		IMendixObject childEntity = childObj.getMappedEntity();
		if (childEntity == null) {
			childEntity = Core.instantiate(context, referenceType);
			initializeEntity(childObj, childEntity);
		}
		return childEntity;
	}

	private <T> void setEntityMemberPrimitiveList(IMendixObject tgtEntity, MendixObjectReferenceSet member,
			IMetaPrimitive childMember, List<T> tcList) {
		List<IMendixIdentifier> tgtList = new ArrayList<IMendixIdentifier>();
		for (T childObj : tcList) {
			IMendixObject childEntity = Core.instantiate(context, member.referenceType());
			if (childObj instanceof Double)
				childEntity.setValue(context, childMember.getName(), new BigDecimal((Double) childObj));
			else
				childEntity.setValue(context, childMember.getName(), childObj);
			tgtList.add(childEntity.getId());
		}
		member.setValue(context, tgtList);
	}

	private void setEntityMemberPrimitiveValue(JModelObject srcObj, IMendixObject tgtEntity, String memberName,
			String jsonKey) {
		try {
			IMetaPrimitive primitive = tgtEntity.getMetaObject().getMetaPrimitive(memberName);
			tgtEntity.setValue(context, memberName, srcObj.getPropertyValue(primitive, jsonKey));
		} catch (NotLoadedExcpetion e) {
		} // Quietly ignore this one
	}

}
