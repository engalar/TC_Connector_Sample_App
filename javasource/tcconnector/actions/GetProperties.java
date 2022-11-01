// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package tcconnector.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JModelObject;
import tcconnector.foundation.JPolicy;
import tcconnector.foundation.JServiceData;
import tcconnector.foundation.TcConnection;
import tcconnector.foundation.TcModelObjectMappings;
import tcconnector.internal.foundation.Constants;
import tcconnector.internal.foundation.Messages;
import tcconnector.internal.foundation.ModelObjectMapper;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;

/**
 * SOA URL: 
 * Core-2006-03-DataManagement/getProperties
 * 
 * Description:
 * This service operation is provided to get property values of Model Objects,
 * Input entities will be updated with new property values.
 * 
 * Returns:
 * Action returns True or False in case of success and failure respectively.
 * 
 */
public class GetProperties extends CustomJavaAction<java.lang.Boolean>
{
	private java.util.List<IMendixObject> __InputObjects;
	private java.util.List<tcconnector.proxies.ModelObject> InputObjects;
	private java.lang.String BusinessObjectMappings;
	private java.lang.String ConfigurationName;

	public GetProperties(IContext context, java.util.List<IMendixObject> InputObjects, java.lang.String BusinessObjectMappings, java.lang.String ConfigurationName)
	{
		super(context);
		this.__InputObjects = InputObjects;
		this.BusinessObjectMappings = BusinessObjectMappings;
		this.ConfigurationName = ConfigurationName;
	}

	@java.lang.Override
	public java.lang.Boolean executeAction() throws Exception
	{
		this.InputObjects = new java.util.ArrayList<tcconnector.proxies.ModelObject>();
		if (__InputObjects != null)
			for (IMendixObject __InputObjectsElement : __InputObjects)
				this.InputObjects.add(tcconnector.proxies.ModelObject.initialize(getContext(), __InputObjectsElement));

		// BEGIN USER CODE
		boolean isGetPropertySuccess = true;
		try {
			JSONObject getPropertyBody = new JSONObject();
			JSONArray objects = new JSONArray();
			JSONArray attributes = new JSONArray();
			for (int cnt = 0; cnt < InputObjects.size(); ++cnt) {
				JSONObject object = new JSONObject();
				object.put("uid", InputObjects.get(cnt).getUID());
				objects.put(object);
			}
			getPropertyBody.put("objects", objects);
			getPropertyBody.put("attributes", attributes);

			BusinessObjectMappings boMappings = new BusinessObjectMappings(BusinessObjectMappings, ConfigurationName);
			JPolicy policy = new JPolicy(boMappings);

			JSONObject response = TcConnection.callTeamcenterService(getContext(), Constants.OPERATION_GETPROPERTIES,
					getPropertyBody, policy, ConfigurationName);
			JServiceData object = (JServiceData) response;
			List<JModelObject> plainObjectsList = object.getPlainObjects();
			Map<String, JModelObject> plainObjectsMap = new HashMap<String, JModelObject>();
			IMendixObject inputEntity;
			if (!plainObjectsList.isEmpty()) {
				for (int cnt = 0; cnt < plainObjectsList.size(); ++cnt) {
					String jsonObjUid = plainObjectsList.get(cnt).getUID();
					plainObjectsMap.put(jsonObjUid, plainObjectsList.get(cnt));
				}

				for (int inputCnt = 0; inputCnt < InputObjects.size(); ++inputCnt) {
					inputEntity = InputObjects.get(inputCnt).getMendixObject();
					IMendixObjectMember<?> member = inputEntity.getMembers(getContext()).get("UID");
					Object value = member.getValue(getContext());
					String inputObjUid = "";

					if (value != null) {
						inputObjUid = value.toString();
					}

					JModelObject plainObject = plainObjectsMap.get(inputObjUid);

					if (plainObject != null) {
						ModelObjectMapper.initializeEntity(getContext(), plainObject, inputEntity,
								TcModelObjectMappings.INSTANCE, boMappings, ConfigurationName);
					}
				}
			}
		} catch (Exception e) {
			Constants.LOGGER.error(Messages.GetPropertyErrorMessage.GetPropertyError + e.getMessage());
			isGetPropertySuccess = false;
			throw e;
		}
		return isGetPropertySuccess;
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "GetProperties";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}