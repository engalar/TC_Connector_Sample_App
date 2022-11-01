package tcconnector.internal.servicehelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mendix.core.Core;
import com.mendix.core.objectmanagement.member.MendixObjectReference;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import tcconnector.foundation.exceptions.InvalidInputException;
import tcconnector.internal.foundation.PropertyResolver;

import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;

/*
 * Prepares the request body for Creating the Teamcenter Objects  
 */
public class PrepareCreateObjectsReqBody {
	private IMendixObject __createInput;
	private IContext __context;

	public static final String REQ_DATATOBERELATED_KEY = "\"dataToBeRelated\":";
	public static final String REQ_WORKFLOWDATA_KEY = "\"workflowData\":";
	public static final String REQ_TARGETOBJECT_KEY = "\"targetObject\":";
	public static final String REQ_PASTEPROP_KEY = "\"pasteProp\":";
	public static final String REQ_BONAME_PROP_KEY = "\"boName\":";
	public static final String REQ_PROPNAMEVALUES_KEY = "\"propertyNameValues\":";
	public static final String REQ_COMPOUND_CREATE_INPUT_REF_MEMBER_PROP_KEY = "compoundCreateInput";
	public static final String ENTITY_BONAME_PROP_KEY = "__boName";
	public static final String ENTITY_REF_PROP_KEY = "__referencePropName";

	public PrepareCreateObjectsReqBody(IContext context, IMendixObject createInput) {
		this.__createInput = createInput;
		this.__context = context;
	}

	/**
	 * Creates the request body for createRelateAndSubmitObjects2 SOA
	 * 
	 * @throws InvalidInputException If the input __boName property is empty.
	 * @throws InvalidInputException If the input __referencePropName property is
	 *                               empty.
	 */
	public java.lang.String get() throws Exception {
		List<Short> listOfProcessedEntities = new ArrayList<Short>();
		StringBuffer buffer = new StringBuffer();

		buffer.append("{");
		buffer.append("\"createInputs\":[{");
		buffer.append("\"clientId\": \"CreateObject\",");
		buffer.append("\"createData\":{");
		String createDataValueStr = getCreateDataJsonStr(__createInput, listOfProcessedEntities);
		buffer.append(createDataValueStr);
		buffer.append("},"); // createData
		// To be supported in future
		buffer.append(REQ_DATATOBERELATED_KEY);
		buffer.append("{},");

		// To be supported in future
		buffer.append(REQ_WORKFLOWDATA_KEY);
		buffer.append("{},");

		// To be supported in future
		buffer.append(REQ_TARGETOBJECT_KEY);
		buffer.append("\"\",");

		// To be supported in future
		buffer.append(REQ_PASTEPROP_KEY);
		buffer.append(" \"\"");

		buffer.append("}]}");

		// System.out.println(buffer.toString());
		return buffer.toString();
	}

	public java.lang.String getCreateDataJsonStr(IMendixObject createInput, List<Short> listOfProcessedEntities) {
		String returnVal = new String();
		try {
			List<? extends MendixObjectReference> references = createInput.getReferences(__context);
			Map<String, ? extends IMendixObjectMember<?>> members = createInput.getMembers(__context);
			Map<String, String> strProps = new HashMap<String, String>();
			String boName = new String();

			for (String key : members.keySet()) {
				IMendixObjectMember<?> m = members.get(key);

				// Ignore reference member properties as that will be handled separately.
				if (m instanceof com.mendix.core.objectmanagement.member.MendixObjectReference) {
					continue;
				}

				Object value = m.getValue(__context);
				String strVal = "";

				if (value != null) {
					strVal = value.toString();

					if (value.getClass().getName().contains("Date"))
						strVal = PropertyResolver.serializeDate((Date) value);

					// Read the BO Name but do not add it to object property bucket
					if (m.getName().equals(ENTITY_BONAME_PROP_KEY) == true) {
						boName = strVal;
						continue;
					}

					// Ignore CreateInput specific properties
					if (m.getName().equals(ENTITY_REF_PROP_KEY) == true) {
						continue;
					}

					strProps.put(m.getName(), strVal);
				}
			}

			if (boName.isEmpty()) {
				String message = "Property " + createInput.getMetaObject().getName() + "." + ENTITY_BONAME_PROP_KEY
						+ "is empty.";
				throw new InvalidInputException(message);
			}

			StringBuffer buffer = new StringBuffer();

			buffer.append(REQ_BONAME_PROP_KEY + "\"");
			buffer.append(boName);
			buffer.append("\",\n");
			buffer.append(REQ_PROPNAMEVALUES_KEY + " { \n");

			// Start loop over the properties
			int propIndex = 0;
			for (Map.Entry<String, String> entry : strProps.entrySet()) {
				++propIndex;
				String key = entry.getKey();
				String val = entry.getValue();
				buffer.append("\"" + key + "\":[");
				buffer.append("\"");
				if (val != null) {
					buffer.append(val);
				}
				buffer.append("\"]");
				if (propIndex < strProps.size()) {
					buffer.append(",");
				}
				buffer.append("\n");
			}

			buffer.append("}\n");
			listOfProcessedEntities.add(createInput.getId().getEntityId());

			// Process references
			for (int referenceIndex = 0; referenceIndex < references.size(); ++referenceIndex) {
				MendixObjectReference refMember = references.get(referenceIndex);
				String associationEntityName = refMember.getName();
				String associationName = associationEntityName.substring(associationEntityName.lastIndexOf(".") + 1);
				IMendixIdentifier identifier = refMember.getValue(__context);
				if (identifier != null) {
					IMendixObject referencedInput = Core.retrieveId(__context, identifier);
					if (associationName.contains(REQ_COMPOUND_CREATE_INPUT_REF_MEMBER_PROP_KEY)
							&& (referencedInput != null)) {
						Short entityId = referencedInput.getId().getEntityId();
						// Process only if the entity is not processed already
						if (!listOfProcessedEntities.contains(entityId)) {
							buffer.append(",\n");
							buffer.append("\"" + REQ_COMPOUND_CREATE_INPUT_REF_MEMBER_PROP_KEY + "\": {\n");
							String compoundPropName = referencedInput.getValue(__context, ENTITY_REF_PROP_KEY);
							if (compoundPropName.isEmpty()) {
								String message = "Property " + createInput.getMetaObject().getName() + "."
										+ "compoundCreateInput.CompndCreateInput." + ENTITY_REF_PROP_KEY + "is empty.";
								throw new InvalidInputException(message);
							}
							buffer.append("\"" + compoundPropName + "\": [{");
							String compoundInputStr = getCreateDataJsonStr(referencedInput, listOfProcessedEntities);
							buffer.append("" + compoundInputStr);
							buffer.append("}]"); // compoundPropName
							buffer.append("}"); // compoundCreateInput
						}
					}
				}
			}

			returnVal = buffer.toString();
		} catch (Exception e) {
			returnVal = "";
			e.printStackTrace();
		}

		return returnVal;
	}
}
