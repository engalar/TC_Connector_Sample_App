// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Collection;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;

import tcconnector.foundation.exceptions.PropertyTypeExcpetion;

public class Primitives {
	/**
	 * Entities with a a single Primitive 'Value' Attribute are assumed to be an
	 * Array element.
	 * 
	 * @param typeName
	 * @return
	 */
	public static boolean isArrayElement(String typeName) {
		IMetaObject meta = Core.getMetaObject(typeName);
		return (getElementMember(meta) != null);
	}

	public static boolean isArrayElement(IMendixObject entity) {
		IMetaObject meta = entity.getMetaObject();
		return (getElementMember(meta) != null);
	}

	public static IMetaPrimitive getElementMember(String typeName) {
		IMetaObject meta = Core.getMetaObject(typeName);
		return getElementMember(meta);
	}

	public static IMetaPrimitive getElementMember(IMendixObject entity) {
		IMetaObject meta = entity.getMetaObject();
		return getElementMember(meta);
	}

	public static Object getValue(IMetaPrimitive primitive, Object srcJsonValue) {
		switch (primitive.getType()) {
		case Boolean:
			return (Boolean) srcJsonValue;
		case DateTime: {
			try {
				return PropertyResolver.parseDate((String) srcJsonValue);
			} catch (ParseException e) {
				String message = "Failed to parse the date value for " + primitive.getName() + ". " + e.getMessage();
				Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
				throw new IllegalArgumentException(message);
			}
		}
		case Decimal:
			return new BigDecimal((Double) srcJsonValue);
		case Integer:
			return (Integer) srcJsonValue;
		case String:
			return (String) srcJsonValue;
		default:
		}

		String message = "Teamcenter data mapping does not support the Entity member data type of "
				+ primitive.getType() + " (" + primitive.getName() + ").";
		Constants.LOGGER.error(LogCorrelationID.getId() + ": " + message);
		throw new PropertyTypeExcpetion(message);
	}

	private static IMetaPrimitive getElementMember(IMetaObject meta) {
		Collection<? extends IMetaPrimitive> elementMembers = meta.getMetaPrimitives();
		if (elementMembers.size() != 1)
			return null;
		IMetaPrimitive firstMember = elementMembers.toArray(new IMetaPrimitive[elementMembers.size()])[0];
		if (firstMember.getName().compareToIgnoreCase("value") != 0)
			return null;
		return firstMember;
	}
}
