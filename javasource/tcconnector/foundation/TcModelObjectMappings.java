// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation.AssociationOwner;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation.AssociationType;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

import tcconnector.internal.foundation.TcMappingImpl;
import tcconnector.proxies.ModelObject;

/**
 * Extends the {@link TcDefaultMappings} to exclude Member names defined
 * directly on the ModelObject Entity (UID, _Type...). These are mapped
 * explicitly and this mapping only looks at Attributes defined on child types
 * of ModelObject ('last_mod_date' Attribute defined on the 'WorkspaceObject'
 * Entity) This class also maps business object type names to Entity names,
 * using the same naming conventions as Member names.
 */
public class TcModelObjectMappings extends TcDefaultMappings {
	public static final TcModelObjectMappings INSTANCE = new TcModelObjectMappings();

	private Set<String> excludeNames = new HashSet<>();
	private Map<String, List<String>> allModelObjNameMappings = new Hashtable<>();

	protected TcModelObjectMappings() {
		super();
		Set<TcMapping> baseMapping = initializeMapping(Core.getMetaObject(ModelObject.entityName));
		baseMapping.iterator().forEachRemaining(m -> {
			excludeNames.add(m.getMxName());
		});

		findAllModelObjectEntities();
	}

	@Override
	protected Set<TcMapping> initializeMapping(IMetaObject meta) {
		Set<TcMapping> mappings = new HashSet<>();
		for (IMetaPrimitive primitive : meta.getMetaPrimitives()) {
			String memberName = primitive.getName();
			if (excludeNames.contains(memberName))
				continue;

			String tcName = memberName;
			tcName = trimPrefix(tcName);
			tcName = trimSuffix(tcName);

			TcMapping mapping = new TcMappingImpl(tcName, memberName);
			mappings.add(mapping);
		}

		// ThisEntity(*) -------- Association -----> (*)ChildEntity
		for (IMetaAssociation association : meta.getMetaAssociationsParent()) {
			String memberName = association.getName();
			if (excludeNames.contains(memberName))
				continue;

			// ThisEntity(1) -------- Association -----> (1)ChildEntity
			if (association.getType().equals(AssociationType.REFERENCE)
					&& association.getType().equals(AssociationOwner.BOTH))
				continue;

			if (!isAModelObjectEntity(association.getChild()))
				continue;

			String tcName = memberName.substring(memberName.indexOf('.') + 1);
			tcName = trimPrefix(tcName);
			tcName = trimSuffix(tcName);

			TcMapping mapping = new TcMappingImpl(tcName, memberName);
			mappings.add(mapping);
		}

		// ThisEntity(1) <-------- Association ----- (*)ChildEntity
		for (IMetaAssociation association : meta.getMetaAssociationsChild()) {
			String memberName = association.getName();
			if (excludeNames.contains(memberName))
				continue;

			String tcName = memberName.substring(memberName.indexOf('.') + 1);
			tcName = trimPrefix(tcName);
			tcName = trimSuffix(tcName);

			TcMapping mapping = new TcMappingImpl(tcName, memberName);
			mappings.add(mapping);
		}
		return mappings;
	}

	private void findAllModelObjectEntities() {
		Iterable<IMetaObject> mendixEntityList = Core.getMetaObjects();
		for (IMetaObject entityObj : mendixEntityList) {
			String entityQName = entityObj.getName();
			String entityModule = entityQName.substring(0, entityQName.indexOf('.') - 1);
			String entityName = entityQName.substring(entityQName.indexOf('.') + 1);
			if (entityModule.equals("System"))
				continue;

			String tcName;
			tcName = trimPrefix(entityName);
			tcName = trimSuffix(tcName);

			if (isAModelObjectEntity(entityObj)) {
				List<String> entityQNames = allModelObjNameMappings.get(tcName);
				if (entityQNames == null) {
					entityQNames = new ArrayList<>();
					allModelObjNameMappings.put(tcName, entityQNames);
				}
				if (entityName.equals(tcName))
					entityQNames.add(0, entityQName);
				else
					entityQNames.add(entityQName);
			}
		}
	}
}
