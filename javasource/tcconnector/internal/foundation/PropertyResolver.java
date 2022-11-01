// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.JModelObject;

public class PropertyResolver {
	private static final Locale US_LOCALE = new Locale("en", "US");
	private static final SimpleDateFormat XSD_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private static final String NULL_DATE_STRING = "0001-01-01T00:00:00+00:00";
	private static final String NULL_DATE_STRING_NO_TZ = "0001-01-01T00:00:00";

	public static String parseString(JSONArray values, int index) {
		if (index > values.length() || values.isNull(index))
			return null;
		Object value = values.get(index);
		if (value instanceof JSONObject) {
			return ((JSONObject) value).getString(JModelObject.UID);
		}
		return (String) value;
	}

	public static Boolean parseBoolean(JSONArray values, int index) {
		String value = parseString(values, index);
		return (value != null && (value.equals("1") || value.equalsIgnoreCase("true")));
	}

	public static Date parseDate(JSONArray values, int index) throws ParseException {
		String value = parseString(values, index);
		if (value == null)
			return null;
		return parseDate(value);
	}

	public static Date parseDate(String value) throws ParseException {
		if (value == null || value.length() == 0 || value.startsWith(NULL_DATE_STRING_NO_TZ)) {
			return null;
		}

		// SimpleDateFormat does not like the : in the time zone
		// 2006-03-15T14:20:45-07:00
		// get rid of the last :
		int hhColon = value.indexOf(':');
		int mmColon = value.indexOf(':', hhColon + 1);
		int zzColon = value.indexOf(':', mmColon + 1);

		if (zzColon != -1) {
			value = value.substring(0, zzColon) + value.substring(zzColon + 1);
		}

		GregorianCalendar local = new GregorianCalendar(US_LOCALE);
		local.setTime(XSD_FORMAT.parse(value));
		return local.getTime();
	}

	public static String serializeDate(Date local) {
		if (local == null) {
			return NULL_DATE_STRING;
		}

		String xsdString = serializeDate(local, XSD_FORMAT);

		// Add a : to the time zone offset
		xsdString = xsdString.substring(0, xsdString.length() - 2) + ":" + xsdString.substring(xsdString.length() - 2);

		return xsdString;
	}

	public static String serializeDate(Date local, SimpleDateFormat format) {
		String xsdString = format.format(local.getTime());
		return xsdString;
	}

	public static Double parseDouble(JSONArray values, int index) throws NumberFormatException {
		String value = parseString(values, index);
		return (value == null || value.equals("")) ? null : Double.parseDouble(value);
	}

	public static Integer parseInteger(JSONArray values, int index) throws NumberFormatException {
		String value = parseString(values, index);
		if (value != null && !value.isEmpty()) {
			return Integer.parseInt(value);
		}
		return null;
	}

	public static JModelObject parseModelObject(JSONArray values, int index) throws IllegalArgumentException {
		if (index > values.length() || values.isNull(index))
			return null;
		Object value = values.get(index);

		if (value.toString().isEmpty()) {
			return null;
		}

		if (value instanceof JSONObject) {
			return new JModelObject((JSONObject) value);
		}
		throw new IllegalArgumentException("Property value is not a ModelObject");
	}
}
