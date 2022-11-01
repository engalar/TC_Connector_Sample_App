// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package tcconnector.actions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.mendix.core.Core;
import com.mendix.core.objectmanagement.member.MendixDateTime;
import com.mendix.core.objectmanagement.member.MendixObjectReference;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONArray;
import com.mendix.thirdparty.org.json.JSONObject;
import com.mendix.webui.CustomJavaAction;
import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JModelObject;
import tcconnector.foundation.JPolicy;
import tcconnector.foundation.JServiceData;
import tcconnector.foundation.TcConnection;
import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.ServiceResponse;

/**
 * SOA URL:
 * Core-2010-09-DataManagement/setProperties
 * 
 * Description:
 * Updates Teamcenter objects corresponding to input model object entities.
 * Updated properties will be retrive from inut entiies , updated properties list not required.
 * 
 * Returns:
 * An entity of type TcConnector.ServiceResponse. Partial errors can be retrieved using association TcConnector.ResponseData/TcConnector.PartialErrors. List of modified model objects can be retrieved using TcConnector.ResponseData/TcConnector.Updated association.
 */
public class SetProperties extends CustomJavaAction<IMendixObject>
{
	private java.util.List<IMendixObject> __modelObjects;
	private java.util.List<tcconnector.proxies.ModelObject> modelObjects;
	private java.lang.String businessObjectMapping;
	private java.lang.String ConfigurationName;

	public SetProperties(IContext context, java.util.List<IMendixObject> modelObjects, java.lang.String businessObjectMapping, java.lang.String ConfigurationName)
	{
		super(context);
		this.__modelObjects = modelObjects;
		this.businessObjectMapping = businessObjectMapping;
		this.ConfigurationName = ConfigurationName;
	}

	@java.lang.Override
	public IMendixObject executeAction() throws Exception
	{
		this.modelObjects = new java.util.ArrayList<tcconnector.proxies.ModelObject>();
		if (__modelObjects != null)
			for (IMendixObject __modelObjectsElement : __modelObjects)
				this.modelObjects.add(tcconnector.proxies.ModelObject.initialize(getContext(), __modelObjectsElement));

		// BEGIN USER CODE

		boolean before = true;
		try {
			JSONObject setPropertyBody = new JSONObject();
			JSONArray info = new JSONArray();
			for (int modelObjCount = 0; modelObjCount < __modelObjects.size(); ++modelObjCount) {
				IMendixObject modelObject = __modelObjects.get(modelObjCount);
				JSONObject infoElement = new JSONObject();
				JModelObject jModelObj = new JModelObject(getContext(), modelObject);
				infoElement.put("object", jModelObj);

				List<IMendixObject> changedMembersList = Core.retrieveByPath(getContext(), modelObject,
						OBJECT_SETPROPS_KEY);
				JSONArray NameValVec = new JSONArray();

				for (int cnt = 0; cnt < changedMembersList.size(); ++cnt) {
					IMendixObject changedMember = changedMembersList.get(cnt);
					String propName = changedMember.getMember(getContext(), NAME_KEY).getValue(getContext()).toString();
					List<IMendixObject> changedValuesList = Core.retrieveByPath(getContext(), changedMember,
							VALUES_KEY);

					for (IMendixObject changedValue : changedValuesList) {
						Object value = changedValue.getMember(getContext(), VALUE_KEY).getValue(getContext());
						JSONObject name = new JSONObject();
						JSONArray values = new JSONArray();
						name.put("name", propName);
						if (value != null && !(changedMember instanceof MendixObjectReference)) {
							if (modelObject.getMember(getContext(), propName) instanceof MendixDateTime) {
								SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
								java.util.Date date = (Date) value;
								value = format.format(date);
							}
							values.put(value.toString());
						}
						name.put("values", values);
						NameValVec.put(name);
					}

				}
				infoElement.put("vecNameVal", NameValVec);
				infoElement.put("timestamp", "");
				info.put(infoElement);
			}

			setPropertyBody.put("info", info);
			JSONArray options = new JSONArray();
			setPropertyBody.put("options", options);

			BusinessObjectMappings boMappings = new BusinessObjectMappings(businessObjectMapping, ConfigurationName);
			JPolicy policy = new JPolicy(boMappings);
			JSONObject response = TcConnection.callTeamcenterService(getContext(), Constants.OPERATION_SETPROPERTIES,
					setPropertyBody, policy, ConfigurationName);

			before = false;
			ServiceResponse responseObj = new ServiceResponse(getContext());

			JServiceData serviceData = (JServiceData) response.getJSONObject(RES_SERVICEDATA_KEY);
			responseObj
					.setResponseData(serviceData.instantiateServiceData(getContext(), boMappings, ConfigurationName));
			return responseObj.getMendixObject();
		} catch (Exception e) {
			String message = (before)
					? "Failed to marshall the the service operation " + Constants.OPERATION_SETPROPERTIES
							+ " input argument."
					: "Failed to marshall the the service operation " + Constants.OPERATION_SETPROPERTIES
							+ " response data.";
			Constants.LOGGER.error(message + e.getMessage());
			message += "Please contact your system administrator for further assistance.";
			throw e;
		}
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "SetProperties";
	}

	// BEGIN EXTRA CODE
	private static final String RES_SERVICEDATA_KEY = "ServiceData";
	private static final String OBJECT_SETPROPS_KEY = "TcConnector.objectsSetProperties";
	private static final String VALUES_KEY = "TcConnector.values";
	private static final String NAME_KEY = "name";
	private static final String VALUE_KEY = "value";
	// END EXTRA CODE
}