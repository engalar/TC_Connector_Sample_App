// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package tcconnector.actions;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import tcconnector.foundation.TcConnection;
import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.ExpandGRMResponse;
import com.mendix.systemwideinterfaces.core.IMendixObject;

/**
 * SOA URL:
 * Core-2007-09-DataManagement/expandGRMRelationsForPrimary
 * 
 * Description:
 * This action returns the secondary objects that are related to the input primary objects.
 * 
 * Returns:
 * An entity of type ExpandGRMResponse, relationshipObject can be retrieved using association TcConnector.relationshipObjects. Partial errors can be retrieved using association TcConnector.ResponseData/TcConnector.PartialErrors.
 */
public class ExpandGRMRelationsForPrimary extends CustomJavaAction<IMendixObject>
{
	private IMendixObject __InputEntity;
	private tcconnector.proxies.ExpandGRMInput InputEntity;
	private java.lang.String BusinessObjectMappings;
	private java.lang.String ConfigurationName;

	public ExpandGRMRelationsForPrimary(IContext context, IMendixObject InputEntity, java.lang.String BusinessObjectMappings, java.lang.String ConfigurationName)
	{
		super(context);
		this.__InputEntity = InputEntity;
		this.BusinessObjectMappings = BusinessObjectMappings;
		this.ConfigurationName = ConfigurationName;
	}

	@java.lang.Override
	public IMendixObject executeAction() throws Exception
	{
		this.InputEntity = __InputEntity == null ? null : tcconnector.proxies.ExpandGRMInput.initialize(getContext(), __InputEntity);

		// BEGIN USER CODE
		ExpandGRMResponse response = new ExpandGRMResponse(getContext());
		response = (ExpandGRMResponse) TcConnection.callTeamcenterService(getContext(),
				Constants.OPERATION_EXPAND_GRM_RELATIONS_PRIMARY, InputEntity.getMendixObject(), response,
				SERVICE_OPERATION_MAP, BusinessObjectMappings, ConfigurationName);
		return response.getMendixObject();
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "ExpandGRMRelationsForPrimary";
	}

	// BEGIN EXTRA CODE
	private static final String SERVICE_OPERATION_MAP = "OperationMapping/Core/2007-09/DataManagement/expandGRMRelationsForPrimary.json";
	// END EXTRA CODE
}
