// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.util.HashSet;
import java.util.Set;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

public class JPolicy extends JSONObject {

	private static final String KEY_USE_REF_COUNT = "useRefCount";
	private static final String KEY_TYPES = "types";
	private static final String KEY_NAME = "name";
	private static final String KEY_PROPERTIES = "properties";
	private static final String KEY_MODIFIERS = "modifiers";
	private static final String KEY_VALUE = "Value";

	private static final String WITH_PROPERTIES = "withProperties";
	private static final String EXCLUDE_UI = "excludeUiValues";

	private IMetaObject rootMeta;
	private BusinessObjectMappings boMappings;
	private Set<String> processed;

	public JPolicy(IMetaObject meta, BusinessObjectMappings boMappings) {
		super();
		this.rootMeta = meta;
		this.boMappings = (boMappings == null) ? new BusinessObjectMappings() : boMappings;
		this.processed = new HashSet<>();
		createPolicy();
	}

	public JPolicy(BusinessObjectMappings boMappings) {
		super();
		this.rootMeta = null;
		this.boMappings = (boMappings == null) ? new BusinessObjectMappings() : boMappings;
		this.processed = new HashSet<>();
		createPolicy();
	}

	public JPolicy(IMetaObject meta) {
		super();
		this.rootMeta = meta;
		this.boMappings = new BusinessObjectMappings();
		this.processed = new HashSet<>();
		createPolicy();
	}

	public void removeProperty(String typeName, String propName) {
		JSONArray types = getJSONArray(KEY_TYPES);
		for (int i = 0; i < types.length(); i++) {
			JSONObject type = types.getJSONObject(i);
			if (type.getString(KEY_NAME).equals(typeName)) {
				removeProperty(type, propName);
				return;
			}
		}
	}

	public void removeProperty(JSONObject type, String propName) {
		JSONArray props = type.getJSONArray(KEY_PROPERTIES);
		for (int j = 0; j < props.length(); j++) {
			JSONObject prop = props.getJSONObject(j);
			if (prop.getString(KEY_NAME).equals(propName)) {
				props.remove(j);
				return;
			}
		}
	}

	private void createPolicy() {
		this.put(KEY_USE_REF_COUNT, false);
		JSONArray types = new JSONArray();
		for (String tcName : boMappings.getBusinessObjectNames()) {
			IMetaObject boMeta = boMappings.getMetaObject(tcName);
			Set<TcMapping> boMapping = TcModelObjectMappings.INSTANCE.getMemberMappings(boMeta);
			JSONObject type = createType(tcName, boMeta, boMapping, false);
			types.put(type);
		}
		if (rootMeta != null)
			traverseEntity(types, rootMeta, true);

		this.put(KEY_TYPES, types);
	}

	private void traverseEntity(JSONArray types, IMetaObject meta, boolean recurse) {
		boolean aModelObject = TcMappings.isModelObject(meta);
		Set<TcMapping> entityMapping = TcModelObjectMappings.INSTANCE.getMemberMappings(meta);

		if (aModelObject && !processed.contains(meta.getName())) {
			String tcType = meta.getName().split("\\.")[1];
			types.put(createType(tcType, meta, entityMapping, recurse));
		}
		if (aModelObject) {
			if (!recurse)
				return;
			recurse = false;
		}

		for (TcMapping mapping : entityMapping) {
			String memberName = mapping.getMxName();
			if (!memberName.contains("."))
				continue;
			IMetaAssociation association = Core.getMetaAssociation(memberName);
			IMetaObject childMeta = association.getParent();
			if (childMeta.equals(meta))
				continue;
			traverseEntity(types, childMeta, recurse);
		}
	}

	private JSONObject createType(String typeName, IMetaObject meta, Set<TcMapping> objMapping,
			boolean withProperties) {
		JSONObject type = new JSONObject();
		type.put(KEY_NAME, typeName);
		JSONArray properties = new JSONArray();
		processed.add(meta.getName());

		for (TcMapping mapping : objMapping) {
			JSONObject jProp;
			String tcPropName = mapping.getTcName();
			String memberName = mapping.getMxName();
			jProp = createPropertyFromAttribute(tcPropName, meta, memberName);
			if (jProp != null)
				properties.put(jProp);
			else {
				jProp = createPropertyFromAssociation(tcPropName, meta, memberName, withProperties);
				if (jProp != null)
					properties.put(jProp);
			}
		}

		type.put(KEY_PROPERTIES, properties);
		return type;
	}

	private JSONObject createPropertyFromAttribute(String propName, IMetaObject meta, String memberName) {
		IMetaPrimitive attributeType = meta.getMetaPrimitive(memberName);
		if (attributeType == null)
			return null;
		return creatProperty(propName);
	}

	private JSONObject createPropertyFromAssociation(String propName, IMetaObject meta, String memberName,
			boolean withProperties) {
		IMetaAssociation association = meta.getMetaAssociationChild(memberName);
		String otherSideEntity = null;
		if (association == null) {
			association = meta.getMetaAssociationParent(memberName);
			if (association == null)
				return null;
			else
				otherSideEntity = boMappings.getBusinessObjectName(association.getChild().getName());
		} else
			otherSideEntity = boMappings.getBusinessObjectName(association.getParent().getName());

		JSONObject jProp = creatProperty(propName);
		jProp.getJSONArray(KEY_MODIFIERS).put(createModifier(EXCLUDE_UI));
		if (withProperties || otherSideEntity != null)
			jProp.getJSONArray(KEY_MODIFIERS).put(createModifier(WITH_PROPERTIES));
		return jProp;
	}

	private JSONObject creatProperty(String propName) {
		JSONObject property = new JSONObject();
		property.put(KEY_NAME, propName);
		property.put(KEY_MODIFIERS, new JSONArray());
		return property;
	}

	private JSONObject createModifier(String name) {
		JSONObject modifier = new JSONObject();
		modifier.put(KEY_NAME, name);
		modifier.put(KEY_VALUE, "true");
		return modifier;
	}
}
