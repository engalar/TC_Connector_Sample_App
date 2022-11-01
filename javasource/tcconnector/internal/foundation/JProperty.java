package tcconnector.internal.foundation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

public class JProperty extends JSONObject {
	public static final String UI_VALUES = "uiValues";
	public static final String DB_VALUES = "dbValues";

	/* Optional Property keys that could be exposed later */
	/*
	 * True if a property value is null. This array is filled only when one or more
	 * values is actually null. If not present all values are assumed to be NOT
	 * null.
	 */
	private static final String IS_NULLS = "isNulls";

	/*
	 * The modifiable flag overrides the modifiable flag on the corresponding
	 * Property Descriptor for this single instance of the Property. This flag will
	 * only be set when the Object Property Policy flag includeIsModifiable is
	 * enabled.
	 */
	private static final String MODIFIABLE = "modifiable";

	private static final Set<String> VALID_KEYS;
	static {
		VALID_KEYS = new HashSet<>();
		Iterator<String> keys = Arrays.asList(new String[] { UI_VALUES, DB_VALUES, IS_NULLS, MODIFIABLE }).iterator();
		keys.forEachRemaining(VALID_KEYS::add);
	}

	private JSONArray unresolvedDbValues;

	/**
	 * Construct an object from the following keys:
	 * 
	 * <pre>
	 * {
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
	 * }
	 * </pre>
	 * 
	 * @param obj A JSONObject that represents a ModelObject.
	 * @throws IllegalArgumentException The JSONObject does not have the required
	 *                                  keys/fields that represent a ModelObject.
	 */
	public JProperty(JSONObject obj, JSONArray resolvedDbValues) {
		super();
		validateKeys(obj);
		copyTopLevel(obj);
		unresolvedDbValues = (JSONArray) remove(DB_VALUES);
		put(DB_VALUES, resolvedDbValues);
	}

	@Override
	public String toString() {
		JSONArray resolvedDBValues = (JSONArray) remove(DB_VALUES);
		put(DB_VALUES, unresolvedDbValues);
		String jsonDoc = super.toString();
		remove(DB_VALUES);
		put(DB_VALUES, resolvedDBValues);
		return jsonDoc;
	}

	private void validateKeys(JSONObject obj) {
		Iterator<String> it = obj.keys();
		while (it.hasNext()) {
			String key = it.next();
			if (!VALID_KEYS.contains(key)) {
				Constants.LOGGER.error(LogCorrelationID.getId() + ": The JSONObject does not represent a Property.\n"
						+ obj.toString());
				throw new IllegalArgumentException("The Property does not represent a Property.");
			}
		}
	}

	private void copyTopLevel(JSONObject right) {
		Iterator<String> it = right.keys();
		while (it.hasNext()) {
			String key = it.next();
			put(key, right.get(key));
		}
	}
}
