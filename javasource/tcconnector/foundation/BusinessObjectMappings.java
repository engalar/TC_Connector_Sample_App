// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mendix.core.Core;
import com.mendix.core.CoreRuntimeException;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;

import tcconnector.internal.foundation.ClientMetaModel;
import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.ModelObject;

/**
 * Contains the mapping between Teamcenter business object type names and Mendix
 * Entity names.
 *
 */
public class BusinessObjectMappings {
	private Map<String, String> mappings = new Hashtable<>();
	private String ConfigurationName;

	public BusinessObjectMappings() {
	}

	/**
	 * @param allMapings A semicolon (;) separated list of Teamcenter business
	 *                   object names to Domain Model Entity names.
	 *                   (BOMLine=TcConnector.BOMLine;ItemRevision=TcConnector.ItemRevision)
	 * 
	 * @throws IllegalArgumentException If one of the Domain Model names is not
	 *                                  valid or does not extend from ModelObject
	 */
	public BusinessObjectMappings(String allMapings, String ConfigurationName) {
		if (allMapings == null || allMapings.isEmpty())
			return;
		String[] maps = allMapings.split(";");
		for (String map : maps) {
			String[] tcToMx = map.split("=");
			String tcName = tcToMx[0].trim();
			String mxName = tcToMx[1].trim();
			validMendixName(mxName);

			mappings.put(tcName, mxName);
		}
		this.ConfigurationName = ConfigurationName;
	}

	/**
	 * Get a list of all Teamcenter business objects names in this mapping.
	 */
	public List<String> getBusinessObjectNames() {
		Set<String> keys = mappings.keySet();
		List<String> tcNames = new ArrayList<>();
		keys.forEach(m -> tcNames.add(m));
		return Collections.unmodifiableList(tcNames);
	}

	/**
	 * Get the Teamcenter business object name for the given Entity name; If the
	 * Entity is not in the mapping, a null is returned.
	 */
	public String getBusinessObjectName(String entityName) {
		Set<String> tcNames = mappings.keySet();
		for (String tcName : tcNames) {
			if (mappings.get(tcName).equals(entityName))
				return tcName;
			else {
				IMetaObject meta = Core.getMetaObject(entityName);
				List<? extends IMetaObject> subTypes = meta.getSubObjects();
				for (IMetaObject subType : subTypes) {
					String name = subType.getName();
					if (mappings.get(tcName).equals(name))
						return tcName;
				}
			}
		}
		return null;
	}

	/**
	 * Get the Entity name for the given Teamcenter name.
	 * 
	 * @param tcName              The business object name.
	 * @param suggestedEntityName A suggested Entity name. This is used only if the
	 *                            tcName does NOT have a mapped Entity name.
	 * @return The Entity name.
	 * @throws IllegalArgumentException If there is not a valid mapping for the
	 *                                  tcName, and the suggestedEntityName is not
	 *                                  valid or does not extend from ModelObject
	 */
	public String getEntityName(String tcName, String suggestedEntityName) {
		String entityName = findNearestParent(tcName);
		if (entityName == null) {
			if (suggestedEntityName != null) {
				validMendixName(suggestedEntityName);
				entityName = suggestedEntityName;
			} else
				entityName = ModelObject.entityName;
		}
		return entityName;
	}

	public IMetaObject getMetaObject(String tcName) {
		return Core.getMetaObject(getEntityName(tcName, null));
	}

	@Override
	public String toString() {
		return mappings.toString();
	}

	private String findNearestParent(String tcName) {
		List<String> typeParents = ClientMetaModel.getTypeHierarchy(tcName, ConfigurationName);
		for (String typeName : typeParents) {
			String entityName = mappings.get(typeName);
			if (entityName != null)
				return entityName;
		}
		return null;
	}

	private void validMendixName(String mxName) {
		try {
			IMetaObject meta = Core.getMetaObject(mxName);
			if (!TcMappings.isModelObject(meta)) {
				throw new IllegalArgumentException(
						"'" + mxName + "' does not extend from '" + ModelObject.entityName + "'.");
			}
		} catch (CoreRuntimeException e) {
			Constants.LOGGER.error(e.getMessage());
			throw new IllegalArgumentException(e.getMessage());
		}
	}
}
