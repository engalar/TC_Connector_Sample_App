package tcconnector.internal.servicehelper;

import com.mendix.core.Core;
import com.mendix.core.objectmanagement.member.MendixObjectReference;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.IMendixObjectMember;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JPolicy;
import tcconnector.foundation.JServiceData;
import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.ServiceResponse;

/**
 * ServiceHelper class helps do common things required by Java actions
 * implemented using JSON based CallTeamcenterService method.
 */
public class ServiceHelper {
	private IContext context;
	private java.lang.String ServiceName;
	private IMendixObject InputArgument;
	private IMendixObject responseObject;
	private java.lang.String ReturnType;
	private java.lang.String BusinessObjectMapping;
	private BusinessObjectMappings boMappings;
	private JSONObject policyJson;

	private static final String RES_SERVICEDATA_KEY = "ServiceData";
	private static final String SERVICE_RESPONSE_RESPONSE_DATA_ASSOCIATION_NAME = "TcConnector.ResponseData";

	public ServiceHelper(IContext context, java.lang.String ServiceName, IMendixObject InputArgument,
			java.lang.String ReturnType, java.lang.String BusinessObjectMapping) {
		this.context = context;
		this.ServiceName = ServiceName;
		this.InputArgument = InputArgument;
		this.ReturnType = ReturnType;
		this.BusinessObjectMapping = BusinessObjectMapping;
		this.boMappings = null;
		this.policyJson = null;
		this.responseObject = null;
	}

	public JSONObject getPolicy() {
		return this.policyJson;
	}

	public BusinessObjectMappings getBOMapping() {
		return this.boMappings;
	}

	public IMendixObject getReponseEntity() {
		return this.responseObject;
	}

	private IMendixObject instantiateResponseEntity() {
		if (ReturnType == null)
			throw new IllegalArgumentException("The ReturnType cannot be null.");

		try {
			IMendixObject responseObj = Core.instantiate(context, ReturnType);
			if (Core.isSubClassOf(ServiceResponse.entityName, responseObj.getType()))
				return responseObj;
		} catch (Exception e) {
		}
		throw new IllegalArgumentException("'" + ReturnType + "' is not a valid response type.");
	}

	// Initialization function performs following tasks
	// 1. Validates inputs
	// 2. Instantiates Response entity
	// 3. Initializes BO Mapping
	// 4. Prepares property policy
	public void initialize() throws Exception {
		try {
			if (ServiceName == null || !ServiceName.contains("/"))
				throw new IllegalArgumentException("The ServiceName argument (" + ServiceName
						+ ") must have the syntax <service name>/<operation name> "
						+ ".i.e 'Core-2011-06-Session/login'");
			if (InputArgument == null)
				throw new IllegalArgumentException("The InputArgument cannot be null.");

			responseObject = instantiateResponseEntity();

			if (BusinessObjectMapping != null && !BusinessObjectMapping.isEmpty()) {
				String configurationName = "";
				boMappings = new BusinessObjectMappings(BusinessObjectMapping, configurationName);
				policyJson = new JPolicy(boMappings);
			}
		} catch (Exception e) {
			String message = "The input arguments for  the " + ServiceName + " service operation call are not valid. "
					+ e.getMessage();
			Constants.LOGGER.error(message);
			throw e;
		}
	}

	// Populates Response entity with ServiceData from the input JSON response
	public void populateResponseWithServiceData(JSONObject jsonResponseObj, String configurationName) throws Exception {
		if (jsonResponseObj == null)
			throw new Exception("Service call for " + ServiceName + "failed.");

		if (jsonResponseObj.has(RES_SERVICEDATA_KEY)) {
			JServiceData serviceData = (JServiceData) jsonResponseObj.getJSONObject(RES_SERVICEDATA_KEY);
			IMendixObject serviceDataEntity = serviceData.instantiateEntity(context, boMappings, configurationName);
			IMendixObjectMember<?> member = serviceDataEntity.getMember(context,
					SERVICE_RESPONSE_RESPONSE_DATA_ASSOCIATION_NAME);
			((MendixObjectReference) member).setValue(context, responseObject.getId());
		}
	}

	// Check SOA Response entity and if it is null, throw exception
	public static void checkResponseForExceptions(JSONObject jsonResponseObj, String message) throws Exception {
		if (jsonResponseObj == null)
			throw new Exception(message);
	}
}