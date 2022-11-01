package tcconnector.internal.servicehelper;

import java.util.List;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import tcconnector.foundation.JModelObject;
import tcconnector.foundation.exceptions.InvalidInputException;

/**
 * Prepares the request body for Core-2006-03-DataManagement/createRelations SOA
 */
public class PrepareCreateRelationReqBody {
	private IContext __context;
	private List<IMendixObject> __primaryObjs;
	private List<IMendixObject> __secondaryObjs;
	private List<IMendixObject> __userDatas;
	private String __relationName;

	public static final String REQ_RELATION_TYPE_KEY = "\"relationType\":";
	public static final String REQ_PRIMARY_OBJECT_KEY = "\"primaryObject\":";
	public static final String REQ_SECONDARY_OBJECT_KEY = "\"secondaryObject\":";
	public static final String REQ_USER_DATA_KEY = "\"userData\":";

	public PrepareCreateRelationReqBody(IContext context, java.lang.String relationName,
			List<IMendixObject> primaryObjs, List<IMendixObject> secondaryObjs, List<IMendixObject> userDatas) {
		this.__context = context;
		this.__relationName = relationName;
		this.__primaryObjs = primaryObjs;
		this.__userDatas = userDatas;
		this.__secondaryObjs = secondaryObjs;
	}

	/*
	 * Validate the input parameters. Following exceptions are thrown by this
	 * function.
	 * 
	 * InvalidInputException - If primaryObjects is null or empty or If
	 * secondaryObjects is null or empty or If the size of primaryObjects and
	 * secondaryObjects is not same. *
	 */
	private void validateInputs() {
		if (__primaryObjs == null || __primaryObjs.isEmpty()) {
			String message = "Property " + REQ_PRIMARY_OBJECT_KEY + " cannot be null or empty.";
			throw new InvalidInputException(message);
		}
		if (__secondaryObjs == null || __secondaryObjs.isEmpty()) {
			String message = "Property " + REQ_SECONDARY_OBJECT_KEY + " cannot be null or empty.";
			throw new InvalidInputException(message);
		}
		if (__primaryObjs.size() != __secondaryObjs.size()) {
			String message = "Number of objects specified for input property " + REQ_SECONDARY_OBJECT_KEY + " and "
					+ REQ_SECONDARY_OBJECT_KEY + " does not match.";
			throw new InvalidInputException(message);
		}
	}

	/**
	 * Creates the request body for createRelation SOA
	 * 
	 * @throws InvalidInputException If the input is invalid.
	 */
	public java.lang.String get() throws Exception {
		validateInputs();

		StringBuffer buffer = new StringBuffer();

		buffer.append("{");
		buffer.append("\"input\":[");

		for (int index = 0; index < __primaryObjs.size(); ++index) {
			IMendixObject primaryObj = __primaryObjs.get(index);
			JModelObject jmoPrimary = new JModelObject(__context, primaryObj);
			IMendixObject secondaryObj = __secondaryObjs.get(index);
			JModelObject jmoSecondary = new JModelObject(__context, secondaryObj);

			buffer.append("{");
			buffer.append("\"clientId\": \"CreateRelation\",");
			buffer.append(REQ_RELATION_TYPE_KEY);
			buffer.append("\"");
			if (__relationName != null && !__relationName.isEmpty()) {
				buffer.append(__relationName);
			}
			buffer.append("\",");
			buffer.append(REQ_PRIMARY_OBJECT_KEY);
			buffer.append(jmoPrimary.toString() + ",");
			buffer.append(REQ_SECONDARY_OBJECT_KEY);
			buffer.append(jmoSecondary.toString() + ",");
			buffer.append(REQ_USER_DATA_KEY);

			boolean isUserDataSupplied = false;
			if (__userDatas != null && __userDatas.size() > 0) {
				IMendixObject userDataObj = __userDatas.get(index);
				if (userDataObj != null) {
					isUserDataSupplied = true;
					JModelObject jmoUserData = new JModelObject(__context, userDataObj);
					buffer.append(jmoUserData.toString());
				}
			}

			// If no user data is supplied, specify empty bucket
			if (isUserDataSupplied == false) {
				buffer.append("{ \"uid\" = \"AAAAAAAAAA\" }");
			}

			buffer.append("}");

			if (index < __primaryObjs.size() - 1) {
				buffer.append(",");
			}
		}

		buffer.append("]");
		buffer.append("}");

		return buffer.toString();
	}
}
