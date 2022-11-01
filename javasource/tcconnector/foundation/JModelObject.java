// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.exceptions.NotLoadedExcpetion;
import tcconnector.foundation.exceptions.PropertyTypeExcpetion;
import tcconnector.internal.foundation.Constants;
import tcconnector.internal.foundation.LogCorrelationID;
import tcconnector.internal.foundation.ModelObjectMapper;
import tcconnector.internal.foundation.PropertyResolver;
import tcconnector.proxies.ModelObject;

/**
 * Extends the JSONObject class to represent a single Teamcenter ModelObject.
 * This class has convenience methods to access property values and other
 * elements defined on the ModelObject type.
 */
public class JModelObject extends JSONObject {

	public static final String UID = "uid";
	public static final String CLASS_NAME = "className";
	public static final String TYPE = "type";
	public static final String PROPS = "props";
	public static final String UI_VALUES = "uiValues";
	public static final String DB_VALUES = "dbValues";

	public static final String NULL_TAG_UID = "AAAAAAAAAAAAAA";
	private static final String DB_SUFFIX = "__DB";
	public static final JModelObject NULL_MODELOBJECT = new JModelObject(NULL_TAG_UID);

	/**
	 * Create list of Entities with property values from the JSONArray. The
	 * {@link TcModelObjectMappings} mapping is used to map Teamcenter defined
	 * property names (keys in JSONObject) to the Entity member (Attributes or
	 * Associations) names.
	 * 
	 * Entity member types must match the type of the Teamcenter property type.
	 * Members of type String are the display or localized value of the Teamcenter
	 * property, while Members of other types (Boolean, Localized Date and time,
	 * Decimal, and Integer), are the database value of the Teamcenter property. To
	 * map a database value of a Teamcenter String property the member name must be
	 * suffixed with '__DB', i.e. 'description__DB'
	 * 
	 * @param context
	 * @param modelObjects The array of JModelObjects.
	 * @param boMappings   The business object mappings
	 * @return The unmodified list of Entity objects created.
	 * 
	 * @throws PropertyTypeExcpetion    An Entity member is defined with a different
	 *                                  type than the Teamcenter property.
	 * @throws IllegalArgumentException There is not a Mendix Entity mapping to this
	 *                                  business object type. The JSONObject does
	 *                                  not represent a ModelObject.
	 */
	public static List<IMendixObject> instantiateEntities(IContext context, JSONArray modelObjects,
			BusinessObjectMappings boMappings, String configurationName) {
		List<IMendixObject> tgtEntities = new ArrayList<>();
		for (int i = 0; i < modelObjects.length(); i++) {
			JSONObject obj = modelObjects.getJSONObject(i);
			try {
				JModelObject jMo = (JModelObject) obj;
				IMendixObject instantiatedEntity = jMo.instantiateEntity(context, null, boMappings, configurationName);
				if (instantiatedEntity != null) {
					tgtEntities.add(instantiatedEntity);
				}
			} catch (ClassCastException e) {
				throw new IllegalArgumentException(
						"The JSON element does not represent a ModelObject. " + obj.toString());
			}
		}
		return tgtEntities;
	}

	/**
	 * Create list of Entities with property values from the list of JModelObjects.
	 * The {@link TcModelObjectMappings} mapping is used to map Teamcenter defined
	 * property names (keys in JSONObject) to the Entity member (Attributes or
	 * Associations) names.
	 * 
	 * Entity member types must match the type of the Teamcenter property type.
	 * Members of type String are the display or localized value of the Teamcenter
	 * property, while Members of other types (Boolean, Localized Date and time,
	 * Decimal, and Integer), are the database value of the Teamcenter property. To
	 * map a database value of a Teamcenter String property the member name must be
	 * suffixed with '__DB', i.e. 'description__DB'
	 * 
	 * @param context
	 * @param modelObjects The array of JModelObjects.
	 * @param boMappings   The business object mappings
	 * @return The unmodified list of Entity objects created.
	 * 
	 * @throws PropertyTypeExcpetion    An Entity member is defined with a different
	 *                                  type than the Teamcenter property.
	 * @throws IllegalArgumentException There is not a Mendix Entity mapping to this
	 *                                  business object type. The JSONObject does
	 *                                  not represent a ModelObject.
	 */
	public static List<IMendixObject> instantiateEntities(IContext context, List<JModelObject> modelObjects,
			BusinessObjectMappings boMappings, String configurationName) {
		List<IMendixObject> tgtEntities = new ArrayList<>();
		for (int i = 0; i < modelObjects.size(); i++) {
			JModelObject jMo = modelObjects.get(i);
			IMendixObject instantiatedEntity = jMo.instantiateEntity(context, null, boMappings, configurationName);
			if (instantiatedEntity != null) {
				tgtEntities.add(instantiatedEntity);
			}
		}
		return tgtEntities;
	}

	private IMendixObject mappedEntity;

	/**
	 * Construct an object from the following keys:
	 * 
	 * <pre>
	 * {
	 *     "uid":       The unique ID for the business object.
	 *     "type":      The business object's type name (as defined in BMIDE).
	 *     "className": The business object's class name (as defined in BMIDE).
	 *     "props:      (Optional) A list of property values (Key = property name, Value = JSONObject)
	 *                  Only property values returned from Teamcenter server, not the full list of 
	 *                  properties defined on the business object.
	 *     {
	 *         "uiValues": (Optional) A list of display or localized value(s) of the property.
	 *                     When the Object Property Policy flag excludeUiValues is enabled this array will 
	 *                     be empty. The uIValueOnly flag takes precedence over the excludeUiValues flag.
	 *         "dbValues": (Optional) A list of property database values.
	 *                     When the Object Property Policy flag uIValueOnly is enabled this array will 
	 *                     be empty. The content is always string data regardless of the Property type 
	 *                     (int,float,date,string .etc) The value is serialized to a string using the 
	 *                     XSD/XML standards: 
	 *                         boolean:         '1' or '0'
	 *                         date:             yyyy-MM-dd'T'HH:mm:ssZ
	 *                         double:           Scientific or Standard notation, always a US decimal point (never a decimal comma)
	 *                         business object:  UID string  
	 *        }
	 * }
	 * </pre>
	 * 
	 * @param obj A JSONObject that represents a ModelObject.
	 * @throws IllegalArgumentException The JSONObject does not have the required
	 *                                  keys/fields that represent a ModelObject.
	 */
	public JModelObject(JSONObject obj) {
		super();
		mappedEntity = null;
		validateKeys(obj);
		copyTopLevel(obj);
	}

	/**
	 * Construct an object from the given Entity. The JModelObject will be created
	 * with only the UID and Type information (no property values).
	 * 
	 * @param context The request context.
	 * @param entity  The source Entity to create the JModelObject from.
	 * @throws IllegalArgumentException The Entity does not extend from ModelObject.
	 */
	public JModelObject(IContext context, IMendixObject entity) {
		super();
		mappedEntity = null;
		ModelObjectMapper.validateTargetType(entity);
		ModelObject srcModelObj = ModelObject.initialize(context, entity);
		if (srcModelObj.getUID() == null) {
			put(UID, NULL_TAG_UID);
			put(TYPE, "UnKnown Type");
		} else {
			put(UID, srcModelObj.getUID());
			put(TYPE, srcModelObj.get_Type());
			put(CLASS_NAME, srcModelObj.getClassName());
		}
	}

	private JModelObject(String uid) {
		super();
		put(UID, uid);
	}

	/**
	 * Initialize the given Entity with property values from this JModelObject. The
	 * {@link TcModelObjectMappings} mapping is used to map the Teamcenter defined
	 * property names (keys in JSONObject) to the Entity member (Attributes or
	 * Associations) names.
	 * 
	 * Entity member types must match the type of the Teamcenter property type.
	 * Members of type String are the display or localized value of the Teamcenter
	 * property, while Members of other types (Boolean, Localized Date and time,
	 * Decimal, and Integer), are the database value of the Teamcenter property. To
	 * map a database value of a Teamcenter String property the member name must be
	 * suffixed with '__DB', i.e. 'description__DB'
	 * 
	 * @param context
	 * @param tgtEntity The target Entity object to initialize (must extend from
	 *                  ModelObject).
	 * 
	 * @throws IllegalArgumentException The tgtEntity does not extend from the
	 *                                  ModelObject Entity type.
	 * @throws PropertyTypeExcpetion    An Entity member is defined with a different
	 *                                  type than the Teamcenter property.
	 */
	public void initializeEntity(IContext context, IMendixObject tgtEntity, BusinessObjectMappings boMappings,
			String configurationName) {
		TcModelObjectMappings mappings = TcModelObjectMappings.INSTANCE;
		mappedEntity = tgtEntity;
		ModelObjectMapper.initializeEntity(context, this, tgtEntity, mappings, boMappings, configurationName);
	}

	/**
	 * Create an Entity with property values from this JModelObject. The
	 * {@link TcModelObjectMappings} mapping is used to map Teamcenter defined
	 * property names (keys in JSONObject) to the Entity member (Attributes or
	 * Associations) names.
	 * 
	 * Entity member types must match the type of the Teamcenter property type.
	 * Members of type String are the display or localized value of the Teamcenter
	 * property, while Members of other types (Boolean, Localized Date and time,
	 * Decimal, and Integer), are the database value of the Teamcenter property. To
	 * map a database value of a Teamcenter String property the member name must be
	 * suffixed with '__DB', i.e. 'description__DB'
	 * 
	 * @param context
	 * @param boMappings The set of Teamcenter business object names to Mendix
	 *                   Entity names.
	 * @return The Entity object created.
	 * 
	 * @throws PropertyTypeExcpetion An Entity member is defined with a different
	 *                               type than the Teamcenter property.
	 */
	public IMendixObject instantiateEntity(IContext context, String suggestedEntityName,
			BusinessObjectMappings boMappings, String configurationName) {
		String modelObjectType = boMappings.getEntityName(getType(), suggestedEntityName);
		IMendixObject tgtEntity = Core.instantiate(context, modelObjectType);
		initializeEntity(context, tgtEntity, boMappings, configurationName);
		return tgtEntity;
	}

	/** @return A string with the format: Type (UID) */
	public String toSimpleString() {
		return getString(TYPE) + " (" + getString(UID) + ")";
	}

	/** @return A string with the format: Type/propertyName (UID) */
	public String toSimpleString(String propertyName) {
		return getString(TYPE) + "/" + propertyName + " (" + getString(UID) + ")";
	}

	/** @return The business object's UID. */
	public String getUID() {
		return getString(UID);
	}

	/** @return The business object's type name (as defined in BMIDE). */
	public String getType() {
		return getString(TYPE);
	}

	/** @return The business object's class name (as defined in BMIDE). */
	public String getClassName() {
		return getString(CLASS_NAME);
	}

	/**
	 * @return The unmodifiable list of property names currently on this instance.
	 *         Downloaded from server vs. what properties are defined on this type.
	 */
	public List<String> getLoadedPropertyNames() {
		List<String> names = new ArrayList<>();
		if (has(PROPS) && !isNull(PROPS)) {
			JSONObject properties = getJSONObject(PROPS);
			Iterator<String> keys = properties.keys();
			keys.forEachRemaining(names::add);
		}
		return Collections.unmodifiableList(names);
	}

	/**
	 * Gets the display or localized value of a given property.
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion The property has not been returned from the
	 *                            Teamcenter server or was returned without a UI
	 *                            value.
	 */
	public String getPropertyValue(String name) {
		JSONArray values = getUiValues(name);
		return values.getString(0);
	}

	/**
	 * Gets the display or localized values of a given multi-valued property.
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion The property has not been returned from the
	 *                            Teamcenter server or was returned without a UI
	 *                            value.
	 */
	public List<String> getPropertyValues(String name) {
		JSONArray values = getUiValues(name);
		List<String> valueList = new ArrayList<>();
		for (int i = 0; i < values.length(); i++)
			valueList.add(values.getString(i));

		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property (the property is declared as of type
	 * String in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion The property has not been returned from the
	 *                            Teamcenter server or was returned without a DB
	 *                            value.
	 */
	public String getPropertyValueAsString(String name) {
		JSONArray dbValues = getDbValues(name);
		return PropertyResolver.parseString(dbValues, 0);
	}

	/**
	 * Gets the DB values of a given multi-valued property (the property is declared
	 * as of type String in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion The property has not been returned from the
	 *                            Teamcenter server or was returned without a DB
	 *                            value.
	 */
	public List<String> getPropertyValueAsStrings(String name) {
		JSONArray dbValues = getDbValues(name);
		List<String> valueList = new ArrayList<>();
		for (int i = 0; i < dbValues.length(); i++) {
			valueList.add(PropertyResolver.parseString(dbValues, i));
		}
		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property (the property is declared as of type
	 * Boolean in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion The property has not been returned from the
	 *                            Teamcenter server or was returned without a DB
	 *                            value.
	 */
	public Boolean getPropertyValueAsBoolean(String name) {
		JSONArray dbValues = getDbValues(name);
		return PropertyResolver.parseBoolean(dbValues, 0);
	}

	/**
	 * Gets the DB values of a given multi-valued property (the property is declared
	 * as of type Boolean in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion The property has not been returned from the
	 *                            Teamcenter server or was returned without a DB
	 *                            value.
	 */
	public List<Boolean> getPropertyValueAsBooleans(String name) {
		JSONArray dbValues = getDbValues(name);
		List<Boolean> valueList = new ArrayList<>();
		for (int i = 0; i < dbValues.length(); i++) {
			valueList.add(PropertyResolver.parseBoolean(dbValues, i));
		}
		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property (the property is declared as of type
	 * Date in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a date.
	 */
	public Date getPropertyValueAsDate(String name) {
		JSONArray dbValues = getDbValues(name);
		try {
			return PropertyResolver.parseDate(dbValues, 0);
		} catch (ParseException e) {
			throw createPropertyTypeExcpetion(name, "Date", dbValues, 0);
		}
	}

	/**
	 * Gets the DB values of a given multi-valued property (the property is declared
	 * as of type Date in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a date.
	 */
	public List<Date> getPropertyValueAsDates(String name) {
		JSONArray dbValues = getDbValues(name);
		List<Date> valueList = new ArrayList<>();
		for (int i = 0; i < dbValues.length(); i++) {
			try {
				valueList.add(PropertyResolver.parseDate(dbValues, i));
			} catch (ParseException e) {
				throw createPropertyTypeExcpetion(name, "Date", dbValues, i);
			}
		}
		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property (the property is declared as of type
	 * Double in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a double.
	 */
	public BigDecimal getPropertyValueAsDouble(String name) {
		JSONArray dbValues = getDbValues(name);
		try {
			Double doubleValue = PropertyResolver.parseDouble(dbValues, 0);
			if (doubleValue == null)
				return null;
			return new BigDecimal(doubleValue);
		} catch (NumberFormatException e) {
			throw createPropertyTypeExcpetion(name, "Double", dbValues, 0);
		}
	}

	/**
	 * Gets the DB values of a given multi-valued property (the property is declared
	 * as of type Double in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a double.
	 */
	public List<BigDecimal> getPropertyValueAsDoubles(String name) {
		JSONArray dbValues = getDbValues(name);
		List<BigDecimal> valueList = new ArrayList<>();
		for (int i = 0; i < dbValues.length(); i++) {
			try {
				valueList.add(new BigDecimal(PropertyResolver.parseDouble(dbValues, i)));
			} catch (NumberFormatException e) {
				throw createPropertyTypeExcpetion(name, "Double", dbValues, i);
			}
		}
		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property (the property is declared as of type
	 * Integer in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a integer.
	 */
	public Integer getPropertyValueAsInteger(String name) {
		JSONArray dbValues = getDbValues(name);
		try {
			return PropertyResolver.parseInteger(dbValues, 0);
		} catch (NumberFormatException e) {
			throw createPropertyTypeExcpetion(name, "Integer", dbValues, 0);
		}
	}

	/**
	 * Gets the DB values of a given multi-valued property (the property is declared
	 * as of type Integer in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as an integer.
	 */
	public List<Integer> getPropertyValueAsIntegers(String name) {
		JSONArray dbValues = getDbValues(name);
		List<Integer> valueList = new ArrayList<>();
		for (int i = 0; i < dbValues.length(); i++) {
			try {
				valueList.add(PropertyResolver.parseInteger(dbValues, i));
			} catch (NumberFormatException e) {
				throw createPropertyTypeExcpetion(name, "Integer", dbValues, i);
			}
		}
		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property (the property is declared as of type
	 * Business Object in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The value of the property.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a Business
	 *                               Object.
	 */
	public JModelObject getPropertyValueAsModelObject(String name) {
		JSONArray dbValues = getDbValues(name);
		try {
			return PropertyResolver.parseModelObject(dbValues, 0);
		} catch (IllegalArgumentException e) {
			throw createPropertyTypeExcpetion(name, "ModelObject", dbValues, 0);
		}
	}

	/**
	 * Gets the DB values of a given multi-valued property (the property is declared
	 * as of type Business Object in the Teamcenter Meta Model).
	 * 
	 * @param name The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as a Business
	 *                               Object.
	 */
	public List<JModelObject> getPropertyValueAsModelObjects(String name) {
		JSONArray dbValues = getDbValues(name);
		List<JModelObject> valueList = new ArrayList<>();
		for (int i = 0; i < dbValues.length(); i++) {
			try {
				JModelObject dbValue = PropertyResolver.parseModelObject(dbValues, i);
				if (dbValue != null) {
					valueList.add(dbValue);
				}
			} catch (IllegalArgumentException e) {
				throw createPropertyTypeExcpetion(name, "ModelObject", dbValues, i);
			}
		}
		return Collections.unmodifiableList(valueList);
	}

	/**
	 * Gets the DB value of a given property
	 * 
	 * @param primitive The desired primitive type for the property.
	 * @param name      The name of the desired property value.
	 * @return The property values.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as the desired
	 *                               type.
	 */
	public Object getPropertyValue(IMetaPrimitive primitive, String name) {
		switch (primitive.getType()) {
		case Boolean:
			return getPropertyValueAsBoolean(name);
		case DateTime:
			return getPropertyValueAsDate(name);
		case Decimal:
			return getPropertyValueAsDouble(name);
		case Integer:
			return getPropertyValueAsInteger(name);
		case String: {
			if (primitive.getName().endsWith(DB_SUFFIX))
				return getPropertyValueAsString(name);
			else
				return getPropertyValue(name);
		}
		default: {
			String message = "Teamcenter data mapping does not support the Entity member data type of "
					+ primitive.getType() + " (" + primitive.getName() + ").";
			Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
			throw new PropertyTypeExcpetion(message);
		}
		}
	}

	/**
	 * Gets the DB values of a given multi-valued property.
	 * 
	 * @param primitive The desired primitive type for the property.
	 * @param name      The name of the desired property value.
	 * @return The unmodifiable list of property values.
	 * 
	 * @throws NotLoadedExcpetion    The property has not been returned from the
	 *                               Teamcenter server or was returned without a DB
	 *                               value.
	 * @throws PropertyTypeExcpetion The value cannot be accessed as the desired
	 *                               type.
	 */
	public List<?> getPropertyValues(IMetaPrimitive primitive, String name) {
		switch (primitive.getType()) {
		case Boolean:
			return getPropertyValueAsBooleans(name);
		case DateTime:
			return getPropertyValueAsDates(name);
		case Decimal:
			return getPropertyValueAsDoubles(name);
		case Integer:
			return getPropertyValueAsIntegers(name);
		case String: {
			if (primitive.getName().endsWith(DB_SUFFIX))
				return getPropertyValueAsStrings(name);
			else
				return getPropertyValues(name);
		}
		default: {
			String message = "Teamcenter data mapping does not support the Entity member data type of "
					+ primitive.getType() + " (" + primitive.getName() + ").";
			Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
			throw new PropertyTypeExcpetion(message);
		}
		}
	}

	public IMendixObject getMappedEntity() {
		return mappedEntity;
	}

	public void setMappedEnitty(IMendixObject entity) {
		mappedEntity = entity;
	}

	private void validateKeys(JSONObject obj) {
		if (!TcModelObjectMappings.INSTANCE.isAModelObjectRef(obj)) {
			Constants.LOGGER.error(
					LogCorrelationID.getId() + ": The JSONObject does not represent a ModelObject.\n" + obj.toString());
			throw new IllegalArgumentException("The JSONObject does not represent a ModelObject.");
		}
	}

	private void copyTopLevel(JSONObject right) {
		Iterator<String> it = right.keys();
		while (it.hasNext()) {
			String key = it.next();
			put(key, right.get(key));
		}
	}

	private PropertyTypeExcpetion createPropertyTypeExcpetion(String propertyName, String expectedType,
			JSONArray dbValues, int index) {
		String myTypeAndProp = toSimpleString(propertyName);
		String actualValue = PropertyResolver.parseString(dbValues, index);
		String message = "The property value for " + myTypeAndProp + " is not a " + expectedType + " (" + actualValue
				+ ").";
		Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
		return new PropertyTypeExcpetion(message);
	}

	private JSONObject getProperty(String name) throws NotLoadedExcpetion {
		if (has(PROPS) && !isNull(PROPS)) {
			JSONObject properties = getJSONObject(PROPS);
			if (properties.has(name)) {
				return properties.getJSONObject(name);
			}
		}

		String message = toSimpleString() + " does not have a value for the property '" + name + "'. "
				+ "This property was not returned from the Teamcenter server and/or is not defined in the Meta Model. "
				+ "Valid properties for this object are " + getLoadedPropertyNames().toString() + ".";
		throw new NotLoadedExcpetion(message);
	}

	private JSONArray getUiValues(String name) throws NotLoadedExcpetion {
		JSONObject property = getProperty(name);
		if (property.has(UI_VALUES)) {
			return property.getJSONArray(UI_VALUES);
		}
		String message = toSimpleString() + " does not have a UI value(s) for the property '" + name + "'. "
				+ "This property was returned from the Teamcenter server without UI values.";
		throw new NotLoadedExcpetion(message);
	}

	private JSONArray getDbValues(String name) throws NotLoadedExcpetion {
		JSONObject property = getProperty(name);
		if (property.has(DB_VALUES)) {
			return property.getJSONArray(DB_VALUES);
		}
		String message = toSimpleString() + " does not have a DB value(s) for the property '" + name + "'. "
				+ "This property was returned from the Teamcenter server without DB values.";
		throw new NotLoadedExcpetion(message);
	}
}
