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

import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

import tcconnector.internal.foundation.TcMappingImpl;

/**
 * The default mapping is used to map the Teamcenter structure element (keys in
 * JSONObject) names to the Entity member (Attributes or Associations) names.
 * The names must match one-to-one with the following caveats:
 * <ul>
 * <li>The Entity member name is prefixed with a '_', i.e. '_type'. In this case
 * the '_' is ignored, thus matching the Teamcenter name of 'type'. </lie>
 * <li>The Entity member name is suffixed with '__XXX', i.e. 'phone__Home'. In
 * this case the '__Home' is ignored, thus matching the Teamcenter name of
 * 'phone'.</li>
 * </ul>
 * 
 * This mapping may be applied to Teamcenter service operation data structures,
 * and the equivalent Mendix Entity objects. The Entity names are not used, it
 * is only the Associations between Entities that must match (or be mappable).
 */
public class TcDefaultMappings extends TcMappings {
	public static final TcDefaultMappings INSTANCE = new TcDefaultMappings();

	protected TcDefaultMappings() {
		super();
	}

	@Override
	protected Set<TcMapping> initializeMapping(IMetaObject meta) {
		Set<TcMapping> mappings = new HashSet<>();

		for (IMetaPrimitive primitive : meta.getMetaPrimitives()) {
			String memberName = primitive.getName();
			String tcName = memberName;

			tcName = trimPrefix(tcName);
			tcName = trimSuffix(tcName);

			TcMapping mapping = new TcMappingImpl(tcName, memberName);
			mappings.add(mapping);
		}

		// ThisEntity(*) -------- Association -----> (*)ChildEntity
		for (IMetaAssociation association : meta.getMetaAssociationsParent()) {
			String memberName = association.getName();
			String tcName = memberName.substring(memberName.indexOf('.') + 1);

			tcName = trimPrefix(tcName);
			tcName = trimSuffix(tcName);

			TcMapping mapping = new TcMappingImpl(tcName, memberName);
			mappings.add(mapping);
		}

		// ThisEntity(1) <-------- Association ----- (*)ChildEntity
		for (IMetaAssociation association : meta.getMetaAssociationsChild()) {
			String memberName = association.getName();

			String tcName = memberName.substring(memberName.indexOf('.') + 1);
			tcName = trimPrefix(tcName);
			tcName = trimSuffix(tcName);

			TcMapping mapping = new TcMappingImpl(tcName, memberName);
			mappings.add(mapping);

		}
		return mappings;
	}

	protected String trimPrefix(String name) {
		if (name.startsWith("_"))
			return name.substring(1);
		return name;
	}

	protected String trimSuffix(String name) {
		int suffix = name.lastIndexOf("__");
		if (suffix > 0)
			return name.substring(0, suffix);
		return name;
	}

}
