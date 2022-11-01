// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.proxies.ModelObject;

/**
 * Defines mappings between Teamcenter structure element (keys in JSONObject)
 * names to the Entity member (Attributes or Associations) names.
 */
public abstract class TcMappings {

	private Map<String, Set<TcMapping>> allTypeMapppings = new Hashtable<>();
	private Map<String, Boolean> allModelObjectMapppings = new Hashtable<>();

	/**
	 * Gets the Teamcenter to Mendix mappings for a given Entity
	 * 
	 * @param entity The target Entity.
	 * @return Teamcenter/JSON key name to Entity member name
	 */
	public Set<TcMapping> getMemberMappings(IMendixObject entity) {
		IMetaObject meta = entity.getMetaObject();
		return getCachedMap(meta);
	}

	/**
	 * Gets the Teamcenter to Mendix mappings for a given Entity
	 * 
	 * @param entity The target Entity.
	 * @return Teamcenter/JSON key name to Entity member name
	 */
	public Set<TcMapping> getMemberMappings(IMetaObject meta) {
		return getCachedMap(meta);
	}

	/** @return true if the Entity extends from ModelObject. */
	public boolean isAModelObjectEntity(IMendixObject entity) {
		IMetaObject meta = entity.getMetaObject();
		return isAModelObjectEntity(meta);
	}

	/** @return true if the Entity extends from ModelObject. */
	public synchronized boolean isAModelObjectEntity(IMetaObject meta) {
		String myName = meta.getName();
		Boolean isModelObj = allModelObjectMapppings.get(myName);
		if (isModelObj == null) {
			isModelObj = isModelObject(meta);
			allModelObjectMapppings.put(myName, isModelObj);
		}
		return isModelObj.booleanValue();
	}

	/**
	 * @return true if the JSONObject represents a reference to a ModelObject (it
	 *         has the keys 'uid', 'className', and 'type').
	 */
	public boolean isAModelObjectRef(JSONObject jsonObj) {
		if (jsonObj.length() < 3)
			return false;
		if (!jsonObj.has(JModelObject.UID) || jsonObj.isNull(JModelObject.UID))
			return false;
		if (!jsonObj.has(JModelObject.CLASS_NAME) || jsonObj.isNull(JModelObject.CLASS_NAME))
			return false;
		if (!jsonObj.has(JModelObject.TYPE) || jsonObj.isNull(JModelObject.TYPE))
			return false;
		return true;
	}

	protected abstract Set<TcMapping> initializeMapping(IMetaObject meta);

	private synchronized Set<TcMapping> getCachedMap(IMetaObject meta) {
		String myName = meta.getName();
		Set<TcMapping> mappings = allTypeMapppings.get(myName);
		if (mappings == null) {
			mappings = initializeMapping(meta);
			allTypeMapppings.put(myName, mappings);
		}
		return mappings;
	}

	public static Boolean isModelObject(IMetaObject meta) {
		if (ModelObject.entityName.equals(meta.getName()))
			return Boolean.TRUE;

		// ModelObject should be the Root (last) parent in the list, but check them all.
		List<? extends IMetaObject> parents = meta.getSuperObjects();
		for (int i = parents.size() - 1; i >= 0; i--) {
			String parentName = parents.get(i).getName();
			if (ModelObject.entityName.equals(parentName)) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

}
