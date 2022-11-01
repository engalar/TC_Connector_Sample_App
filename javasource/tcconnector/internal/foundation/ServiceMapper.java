package tcconnector.internal.foundation;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.BusinessObjectMappings;
import tcconnector.foundation.JPolicy;
import tcconnector.foundation.JServiceData;
import tcconnector.proxies.ServiceData;
import tcconnector.proxies.ServiceResponse;

/**
 * Maps the input and output of a service operation.
 *
 */
public class ServiceMapper {

	private String serviceName;
	private String operationMapping;
	private BusinessObjectMappings boMappings = null;
	private IContext context;
	private String configurationName;

	public ServiceMapper(IContext context, String serviceName, String operationMapping, String businessObjectMapping,
			String configurationName) throws Exception {
		this.context = context;
		this.serviceName = serviceName;
		this.operationMapping = operationMapping;
		this.configurationName = configurationName;
		validatArguments(businessObjectMapping, configurationName);
	}

	/**
	 * @return The BusinessObjectMappings for this service operation.
	 */
	public BusinessObjectMappings getBusinessObjectMappings() {
		return boMappings;
	}

	/**
	 * @return The Object Property Policy for this service operation (as defined by
	 *         the BusinessObjectMappings).
	 */
	public JPolicy getObjectPropertyPolicy() {
		return new JPolicy(boMappings);
	}

	/**
	 * Maps the source Mendix object to a JSONObject as defined by the
	 * operationMapping.
	 * 
	 * @param inputArgument The source input argument for the service operation.
	 * @return The JSONObject that can be used as input to the
	 *         TcConnection.callTeamcenterService
	 * @throws Exception If failed to map the service operation input data.
	 */
	public JSONObject mapInputData(IMendixObject inputArgument) throws Exception {
		try {
			Constants.LOGGER.trace("Mapping the input data");
			return OperationMapper.toJSONObject(context, inputArgument, operationMapping);
		} catch (Exception e) {
			String message = "Failed to map the input Entity to JSON for the " + serviceName + " service. "
					+ e.getMessage();
			Constants.LOGGER.error(message);
			throw e;
		}
	}

	/**
	 * Maps the source JSONObject to the Mendix object as defined by the
	 * operationMapping.
	 * 
	 * @param jsonResponseObj The source JSONObject returned from
	 *                        TcConnection.callTeamcenterService.
	 * @return The Mendix object
	 * @throws Exception If failed to map the service operation response data.
	 */
	public ServiceResponse mapOutputData(JSONObject jsonResponseObj, ServiceResponse responseObject) throws Exception {
		if (jsonResponseObj == null)
			throw new Exception("Service call for " + serviceName + "failed.");

		try {
			Constants.LOGGER.trace("Mapping the output data starts");
			OperationMapper.toDomainModelEntity(context, jsonResponseObj, responseObject.getMendixObject(),
					operationMapping, boMappings, configurationName);
			autoMapServiceData(jsonResponseObj, responseObject, configurationName);
			Constants.LOGGER.trace("Mapping the output data ends");
			return responseObject;
		} catch (Exception e) {
			String message = "Failed to map the response JSON to return Entity object for the " + serviceName
					+ " service. " + e.getMessage();
			Constants.LOGGER.error(message + e.getMessage());
			throw e;
		}
	}

	private void validatArguments(String businessObjectMapping, String configurationName) throws Exception {
		try {
			if (serviceName == null || !serviceName.contains("/"))
				throw new IllegalArgumentException("The ServiceName argument (" + serviceName
						+ ") must have the syntax <service name>/<operation name> "
						+ ".i.e 'Core-2011-06-Session/login'");

			OperationMappingCache.getMapping(operationMapping);
			if (businessObjectMapping != null && !businessObjectMapping.isEmpty()) {
				boMappings = new BusinessObjectMappings(businessObjectMapping, configurationName);
			}
		} catch (Exception e) {
			String message = "The input arguments for  the " + serviceName + " service operation call are not valid. "
					+ e.getMessage();
			Constants.LOGGER.error(message);
			throw e;
		}
	}

	private void autoMapServiceData(JSONObject jsonResponseObj, ServiceResponse responseObject,
			String configurationName) throws Exception {
		if (responseObject.getResponseData() != null)
			return;

		JServiceData jSD = (JServiceData) ModelObjectResolver.getServiceData(context, jsonResponseObj,
				configurationName);
		if (jSD == null)
			return;

		boMappings = OperationMapper.getBusinessObjectMappings(operationMapping, boMappings);
		ServiceData sd = jSD.instantiateServiceData(context, boMappings, configurationName);
		responseObject.setResponseData(sd);
	}
}
