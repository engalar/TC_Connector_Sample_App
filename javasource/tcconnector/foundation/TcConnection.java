// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONObject;

import system.proxies.HttpResponse;
import tcconnector.foundation.exceptions.BaseServiceException;
import tcconnector.foundation.exceptions.BaseServiceException.Severity;
import tcconnector.foundation.exceptions.InternalServerException;
import tcconnector.foundation.exceptions.InvalidUserException;
import tcconnector.internal.foundation.Constants;
import tcconnector.internal.foundation.CookieManager;
import tcconnector.internal.foundation.LogCorrelationID;
import tcconnector.internal.foundation.ModelObjectResolver;
import tcconnector.internal.foundation.ServiceEnvelope;
import tcconnector.internal.foundation.ServiceMapper;
import tcconnector.internal.foundation.SessionManager;
import tcconnector.proxies.ServiceResponse;
import tcconnector.proxies.SoaServiceRequest;
import tcconnector.proxies.TcSession;

public class TcConnection {
	private static final String REST_SERVLET_PATH = "/JsonRestServices/";
	private static final Object COUNT_LOCK = new Object();
	private static final String XML_SERVICEDATA = "<ns1:ServiceData xmlns:ns1=\"http://teamcenter.com/Schemas/Soa/2006-03/Base\"/>";
	private static final String JSON_SERVICEDATA = new JSONObject().put(".QName", JServiceData.QNAME).toString();
	private static final JSONObject NOT_LOGGED_IN;

	static {
		NOT_LOGGED_IN = new JSONObject();
		NOT_LOGGED_IN.put(Constants.KEY_QNAME, Constants.QNAME_USER_EXP);
		NOT_LOGGED_IN.put(InvalidUserException.KEY_CODE, 0);
		NOT_LOGGED_IN.put(InvalidUserException.KEY_LEVEL, Severity.ERROR.ordinal());
		NOT_LOGGED_IN.put(InvalidUserException.KEY_MESSAGE,
				"Please login to Teamcenter before calling other service operations.");
	}

	/**
	 * Execute a Teamcenter Service Operation.
	 * 
	 * @param context           The Mendix request context.
	 * @param serviceName       The name of the service/operation
	 *                          (Core-2011-06-Session/login)
	 * @param jsonInputObj      The input arguments of the service operation.
	 * @param jsonPropPolicyObj The Object Property Policy for the service
	 *                          operation.
	 * @param tcSession
	 * @return The returned data structure of the service operation. All business
	 *         object references (UIDs) throughout the response are replaced with
	 *         the full ModelObject instance from the ServiceData. The JSONObject
	 *         for these instance may be cast to JModelObject, which offers
	 *         convenience methods for accessing property values.
	 * @throws Exception Any non-service operation error encountered while
	 *                   processing the service requests are caught by this method
	 *                   and displayed in a pop-up message dialog. The exception is
	 *                   re-thrown here.
	 */
	public static JSONObject callTeamcenterService(IContext context, String serviceName, JSONObject jsonInputObj,
			JSONObject jsonPropPolicyObj, String configurationName) throws Exception {
		TcConnection connection = new TcConnection(context, serviceName, jsonInputObj, jsonPropPolicyObj,
				configurationName);
		return connection.sendRequest();
	}

	/**
	 * Execute a Teamcenter Service Operation.
	 * 
	 * @param context                The Mendix request context.
	 * @param serviceName            The name of the service/operation
	 *                               (Core-2011-06-Session/login)
	 * @param inputArgument          The input arguments of the service operation.
	 * @param responseObject         The returned object. If null an instance of
	 *                               ServiceResponse will be created for the return.
	 * @param operationMapping       Path (relative to resources/OperationMappings)
	 *                               for the operation mapping definition. The
	 *                               inputArgument and responseObject are mapped to
	 *                               the service operation input/output based on the
	 *                               definition in the mapping file.
	 * @param businessObjectMappings A semicolon separated list of Teamcenter
	 *                               business object names and Entity names
	 *                               (BOMLine=TcConnector.BOMLine;ItemRevision=TcConnector.ItemRevision)
	 * 
	 * @return The returned data structure of the service operation.
	 * 
	 * @throws Exception Any non-service operation error encountered while
	 *                   processing the service requests are caught by this method
	 *                   and displayed in a pop-up message dialog. The exception is
	 *                   re-thrown here.
	 */
	public static ServiceResponse callTeamcenterService(IContext context, String serviceName,
			IMendixObject inputArgument, ServiceResponse responseObject, String operationMapping,
			String businessObjectMappings, String configurationName) throws Exception {
		if (responseObject == null)
			responseObject = new ServiceResponse(context);

		if (configurationName == null || configurationName.equals("") || configurationName.isEmpty()) {
			configurationName = tcconnector.proxies.microflows.Microflows
					.retrieveConfigNameFromSingleActiveConfiguration(context);
			if (configurationName.isEmpty())
				return null;
		}

		ServiceMapper serviceMapper = new ServiceMapper(context, serviceName, operationMapping, businessObjectMappings,
				configurationName);
		JSONObject jsonInputObj = serviceMapper.mapInputData(inputArgument);
		JSONObject jsonPolicy = serviceMapper.getObjectPropertyPolicy();
		JSONObject jsonResponseObj = TcConnection.callTeamcenterService(context, serviceName, jsonInputObj, jsonPolicy,
				configurationName);
		return serviceMapper.mapOutputData(jsonResponseObj, responseObject);
	}

	/**
	 * Execute a Teamcenter Service Operation.
	 * 
	 * @param context           The Mendix request context.
	 * @param serviceName       The name of the service/operation
	 *                          (Core-2011-06-Session/login)
	 * @param jsonInput         The input arguments of the service operation.
	 * @param jsonPropPolicyObj The Object Property Policy for the service
	 *                          operation.
	 * @return The returned data structure of the service operation. All business
	 *         object references (UIDs) throughout the response are replaced with
	 *         the full ModelObject instance from the ServiceData. The JSONObject
	 *         for these instance may be cast to JModelObject, which offers
	 *         convenience methods for accessing property values.
	 * @throws Exception Any non-service operation error encountered while
	 *                   processing the service requests are caught by this method
	 *                   and displayed in a pop-up message dialog. The exception is
	 *                   re-thrown here.
	 */
	public static JSONObject callTeamcenterService(IContext context, String serviceName, String jsonInput,
			JSONObject jsonPropPolicyObj, String configurationName) throws Exception {
		JSONObject jsonInputObj = new JSONObject(jsonInput);
		return callTeamcenterService(context, serviceName, jsonInputObj, jsonPropPolicyObj, configurationName);
	}

	private IContext context;
	private String serviceName;
	private JSONObject jsonInputObj;
	private JSONObject jsonPropPolicyObj;
	private JSONObject jsonResponseObj;
	private String hostAddress;
	private String configurationName;

	private TcConnection(IContext context, String serviceName, JSONObject jsonInputObj, JSONObject jsonPropPolicyObj,
			String configurationName) {
		this.context = context;
		this.serviceName = serviceName;
		this.jsonInputObj = jsonInputObj;
		this.jsonPropPolicyObj = jsonPropPolicyObj;
		this.jsonResponseObj = null;
		this.hostAddress = "";
		this.configurationName = configurationName;
	}

	private JSONObject sendRequest() throws Exception {
		try {
			TcSession tcSession = null;

			if (configurationName == null || configurationName.equals("") || configurationName.isEmpty()) {
				configurationName = tcconnector.proxies.microflows.Microflows
						.retrieveConfigNameFromSingleActiveConfiguration(context);
				if (configurationName.isEmpty())
					return null;
			}

			IContext ct = context.getSession().createContext();// Core.createSystemContext();
			ct.startTransaction();
			tcSession = tcconnector.proxies.microflows.Microflows.retrieveTcSessionBasedOnConfigName(ct,
					configurationName);
			ct.endTransaction();

			hostAddress = tcSession.getHostAddress();

			if (hostAddress == null) // Host Address is set in ShowLoginPage microflow, if null, user has not logged
										// in yet
				throw new InvalidUserException(NOT_LOGGED_IN);

			String serviceURL = hostAddress + REST_SERVLET_PATH + serviceName;

			pushLogCorrelationIDRequestCount(tcSession);
			logRequest();

			ServiceEnvelope envelope = new ServiceEnvelope(context);
			envelope.setBody(jsonInputObj);
			envelope.setPolicy(this.jsonPropPolicyObj);

			String jsonRequstDoc = envelope.getJSONObject().toString();

			SoaServiceRequest soaServiceRequest = new SoaServiceRequest(context);
			soaServiceRequest.setServiceURL(serviceURL);
			soaServiceRequest.setLogCorrelationID(LogCorrelationID.getId());
			soaServiceRequest.setCookies(CookieManager.getCookieHeaderValue(context, tcSession));
			soaServiceRequest.setBody(jsonRequstDoc);

			long start = System.currentTimeMillis();
			HttpResponse httpResponse = tcconnector.proxies.microflows.Microflows.sendService(context,
					soaServiceRequest);
			long end = System.currentTimeMillis();
			if (httpResponse == null)
				throw new RuntimeException("Failed to get an HTTP response.");

			CookieManager.presistCookies(context, tcSession, httpResponse);
			String jsonResponseDoc = getBody(httpResponse);
			logResponse((end - start), jsonRequstDoc.length(), jsonResponseDoc);

			jsonResponseObj = new JSONObject(jsonResponseDoc);
			ExceptionMapper.throwExcpetion(jsonResponseObj);

			jsonResponseObj = ModelObjectResolver.resolve(context, jsonResponseObj, configurationName);

			SessionManager.cacheSessionState(tcSession, jsonInputObj, jsonResponseObj);
			return jsonResponseObj;
		} catch (InvalidUserException | InternalServerException e) {
			logServiceExcpetion((BaseServiceException) e);
			return null;
		} catch (RuntimeException e) {
			logAnyExcpetion(e);
			return null;
		} finally {
			LogCorrelationID.pop();
			LogCorrelationID.pop();
		}
	}

	private String getBody(HttpResponse httpResponse) {
		String responseBody = httpResponse.getContent();
		// Logout from an invalid session gives this hard coded response
		if (responseBody.equals(XML_SERVICEDATA))
			return JSON_SERVICEDATA;
		return responseBody;
	}

	private void logRequest() {
		if (Constants.LOGGER.isTraceEnabled() && !serviceName.endsWith("login") && !serviceName.endsWith("loginSSO"))
			Constants.LOGGER.trace(
					LogCorrelationID.getId() + ": Service Request  - " + serviceName + "\n" + jsonInputObj.toString());
		else if (Constants.LOGGER.isDebugEnabled())
			Constants.LOGGER.debug(LogCorrelationID.getId() + ": Service Request  - " + serviceName);
		else
			Constants.LOGGER.info(LogCorrelationID.getId() + ": Service Request  - " + serviceName);
	}

	private void logResponse(long duration, int bytesOut, String jsonResponseDoc) {
		if (!Constants.LOGGER.isDebugEnabled())
			return;

		String idAndName = LogCorrelationID.getId() + ": Service Response - " + serviceName;
		String text = String.format("%-120s (Time:%9dms, Request:%9dbytes, Response:%9dbytes)", idAndName, duration,
				bytesOut, jsonResponseDoc.length());

		if (Constants.LOGGER.isTraceEnabled()) {
			Constants.LOGGER.trace(text + "\n" + jsonResponseDoc);
		} else if (Constants.LOGGER.isDebugEnabled()) {
			Constants.LOGGER.debug(text);
		}
	}

	private void pushLogCorrelationIDRequestCount(TcSession tcSession) throws Exception {
		synchronized (COUNT_LOCK) {
			long requestCount = tcSession.getRequestCount();
			if (requestCount == Long.MAX_VALUE)
				requestCount = 1;
			LogCorrelationID.push(Integer.toString(Math.abs(context.getSession().getId().hashCode())));
			LogCorrelationID.push(String.format("%05d", requestCount++));
			tcSession.setRequestCount(requestCount);

			// In Mendix, tcSession.commit doesn't actually do commit to DB. In Mendix DB
			// commit seems
			// to happen after a Microflow is complete.
			// So we need to explicitely do a commit and release the lock from object from
			// DB.
			//
			// If there are more than one threads call SOA, then first thread who comes in
			// synchronized
			// block does not finish the block and release the COUNT_LOCK object.
			// It continue to hold lock on tcSession object in DB due to recursive microflow
			// & mutliple
			// SOA calls in connector.
			// This does not happen if SOAs are called sequentially.
			try {
				/*
				 * IContext ct = context.getSession().createContext(); ct.startTransaction();
				 * tcSession.commit(ct); ct.endTransaction();
				 */
				tcSession.commit();
			} catch (CoreException e) {
				logAnyExcpetion(e);
			}
		}
	}

	private void logServiceExcpetion(BaseServiceException se) throws BaseServiceException {
		String message = "Failed to execute the service operation " + serviceName + ". ";
		String logMessage = message;
		message += se.getMessage();
		logMessage += se.getClass().getSimpleName() + " (" + se.getCode() + "): " + se.getMessage();
		while (se.getCause() != null) {
			se = (BaseServiceException) se.getCause();
			logMessage += "\nCaused by - " + se.getClass().getSimpleName() + " (" + se.getCode() + "): "
					+ se.getMessage();
		}
		Constants.LOGGER.error(logMessage);
		if (se instanceof InternalServerException) {
			message += " Please contact your Teamcenter server administrator for further assistance.";
		}
		throw se;
	}

	private void logAnyExcpetion(Exception e) throws Exception {
		String message = "Failed to send the service operation " + serviceName + ".";
		String logMessage = message + " " + e.getClass().getSimpleName() + ": " + e.getMessage(); // This message is
																									// from the REST
																									// action and is not
																									// helpful to an end
																									// user

		Throwable t = (Throwable) e;
		while (t.getCause() != null) {
			t = t.getCause();
			logMessage += "\nCaused by - " + t.getClass().getSimpleName() + ": " + t.getMessage();
		}
		logMessage += "\nPlease ensure the Teamcenter server is running on the address " + hostAddress + ".";
		message += "\nPlease ensure the Teamcenter server is running on the address " + hostAddress + ".";
		Constants.LOGGER.error(logMessage);
		throw e;
	}
}
