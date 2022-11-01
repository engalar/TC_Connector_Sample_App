package tcconnector.actions;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.externalinterface.connector.RequestHandler;
import com.mendix.m2ee.api.IMxRuntimeRequest;
import com.mendix.m2ee.api.IMxRuntimeResponse;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.ISession;
import com.teamcenter.ss.SSOAuthorizationException;
import com.teamcenter.ss.SSOConfigurationException;
import com.teamcenter.ss.SSOException;
import com.teamcenter.ss.SSOInvalidSessionException;
import com.teamcenter.ss.SSOSystemException;
import com.teamcenter.ss.server.SSOAppSession;
import com.teamcenter.ss.server.SSOAppSessionFactory;

import tcconnector.internal.foundation.Constants;
import tcconnector.proxies.Credentials;
import tcconnector.proxies.TeamcenterConfiguration;
import tcconnector.proxies.microflows.Microflows;

public class SSORedirectRequestHandler extends RequestHandler {
	private static final String XAS_SESSION_ID = "XASSESSIONID";
	private static final String XAS_ID = "XASID";
	private static final String OriginURI = "originURI";
	private static final String OriginURIValue = "index.html";
	public static final int SECONDS_PER_YEAR = 60 * 60 * 24 * 365;

	public static String INDEX_PAGE = "/index.html";
	@SuppressWarnings("unused")
	private String contextPath;

	public SSORedirectRequestHandler(String contextPath) {
		this.contextPath = contextPath;
	}

	@Override
	public void processRequest(IMxRuntimeRequest request, IMxRuntimeResponse response, String path) throws Exception {

		HttpServletRequest servletRequest = request.getHttpServletRequest();

		Constants.LOGGER.trace("Received process request event");

		try {
			Constants.LOGGER.debug("Request URI: " + servletRequest.getRequestURI());
			processCallback(request, response);
		} catch (Exception ex) {
			Constants.LOGGER.error("Exception occurred while processing request " + ex);
			response.sendError("Exception occurred while processing request");
		}
	}

	private void processCallback(IMxRuntimeRequest request, IMxRuntimeResponse response)
			throws CoreException, SSOInvalidSessionException, SSOAuthorizationException, SSOSystemException,
			SSOConfigurationException, SSOException {
		// Extract parameters from the request
		String appUserId = request.getParameter("TCSSO_APP_USER_ID");
		String ssoKey = request.getParameter("TCSSO_SESSION_KEY");
		String sessionID = request.getParameter("sessionid");
		String tcConfigName = request.getParameter("TC_CONFIG_NAME");
		String ssoKey2 = "";
		String handle = "";

		// Retrieve the session
		ISession session = Core.getSessionById(UUID.fromString(sessionID));
		IContext context = session.createContext();

		TeamcenterConfiguration config = tcconnector.proxies.microflows.Microflows
				.retrieveTeamcenterConifgurationByName(context, tcConfigName);
		String mendixAppID = config.getSSOMendixAppId(context);
		String tcAppID = config.getSSOTCAppId(context);
		String identityURL = config.getSSOIdentityURL(context);

		SSOAppSession ssoAppSession;
		try {
			ssoAppSession = SSOAppSessionFactory.createSSOAppSession(SSOAppSessionFactory.XMLRPC_TYPE, mendixAppID,
					handle, identityURL);
		} catch (Exception e) {
			ssoAppSession = SSOAppSessionFactory.createSSOAppSession(SSOAppSessionFactory.JSON_TYPE, mendixAppID,
					handle, identityURL);
		}

		handle = ssoAppSession.getSessionHandle();
		ssoAppSession.validateAppToken(appUserId, ssoKey);
		ssoKey2 = ssoAppSession.generateSSOAppToken(tcAppID).getSSOSessionKey();

		context.startTransaction();
		// Add relevant cookies to the response
		response.addCookie(XAS_SESSION_ID, session.getId().toString(), "/", "", -1);
		response.addCookie(XAS_ID, "0." + Core.getXASId(), "/", "", -1);
		response.addCookie(OriginURI, OriginURIValue, "/", "", SECONDS_PER_YEAR);

		// Create credentials object needed as parameter for the login microflow
		Credentials creds = new Credentials(context);
		creds.setuser(appUserId);
		creds.setpassword(ssoKey2);

		// Login to Teamcenter
		executeTeamcenterLogin(context, creds, config);
		context.endTransaction();

		// Redirect to home page post login
		String indexpage = "/index.html";
		// Mendix code to redirect to home pagex
		response.setStatus(HttpServletResponse.SC_SEE_OTHER);
		response.addHeader("location", indexpage);
	}

	private void executeTeamcenterLogin(IContext context, Credentials credentials, TeamcenterConfiguration config) {
		try {
			Microflows.executeLogin(context, credentials, config);
		} catch (Exception e) {
			Constants.LOGGER.error(e.getMessage());
		}
	}
}