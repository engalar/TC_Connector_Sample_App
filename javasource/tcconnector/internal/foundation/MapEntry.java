// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.util.Set;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import tcconnector.foundation.TcMapping;
import tcconnector.foundation.TcMappings;

public class MapEntry {
	public static final String KEY = "key";
	public static final String VALUE = "value";

	private IMendixObject entryEntity;
	private Set<TcMapping> entryMappings;

	public static boolean isMapEntry(TcMappings mappings, IMendixObject tgtMxObj) {
		try {
			new MapEntry(mappings, tgtMxObj);
			return true;
		} catch (IllegalArgumentException e) {
		}
		return false;
	}

	public MapEntry(IContext context, TcMappings mappings, String entryType) {
		entryEntity = Core.instantiate(context, entryType);
		entryMappings = mappings.getMemberMappings(entryEntity);
		if (!isValidEntry()) {
			Constants.LOGGER
					.error(entryEntity.getMetaObject().getName() + " does not have a 'key' and/or 'value' Attribute.");
			throw new IllegalArgumentException(
					entryEntity.getMetaObject().getName() + " does not have a 'key' Attribute.");
		}
	}

	private MapEntry(TcMappings mappings, IMendixObject tgtMxObj) {
		entryEntity = tgtMxObj;
		entryMappings = mappings.getMemberMappings(entryEntity);
		if (!isValidEntry()) {
			throw new IllegalArgumentException(
					entryEntity.getMetaObject().getName() + " does not have a 'key' Attribute.");
		}
	}

	public IMendixObject getEntry() {
		return entryEntity;
	}

	public String getMemberKeyName() {
		for (TcMapping mapping : entryMappings) {
			if (mapping.getTcName().equals(KEY))
				return mapping.getMxName();
		}
		throw new IllegalArgumentException(entryEntity.getMetaObject().getName() + " does not have a 'key' Attribute.");
	}

	public String getMemberValueName() {
		for (TcMapping mapping : entryMappings) {
			if (mapping.getTcName().equals(VALUE))
				return mapping.getMxName();
		}
		throw new IllegalArgumentException(
				entryEntity.getMetaObject().getName() + " does not have a 'value' Attribute.");
	}

	private boolean isValidEntry() {
		try {
			getMemberKeyName();
			getMemberValueName();
			return true;
		} catch (IllegalArgumentException e) {
		}
		return false;
	}
}
