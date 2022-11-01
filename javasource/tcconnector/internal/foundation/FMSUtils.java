package tcconnector.internal.foundation;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.thirdparty.org.json.JSONObject;
import com.teamcenter.fms.servercache.FSCException;
import com.teamcenter.fms.servercache.proxy.CommonsFSCWholeFileIOImpl;

import tcconnector.proxies.TeamcenterConfiguration;

public class FMSUtils {

	/*
	 * Initialize FMS
	 */
	public static CommonsFSCWholeFileIOImpl initializeFMS(IContext context, String ConfigurationName)
			throws UnknownHostException, FSCException {
		CommonsFSCWholeFileIOImpl fscFileIOImpl = new CommonsFSCWholeFileIOImpl();
		String[] fmsURLs = retrieveFMSURLs(context, ConfigurationName);
		final InetAddress clientIP = InetAddress.getLocalHost();
		fscFileIOImpl.init(clientIP.getHostAddress(), fmsURLs, fmsURLs);
		return fscFileIOImpl;
	}

	/*
	 * retrieve FMS URL from active TC configuration
	 */
	public static String[] retrieveFMSURLs(IContext context, String ConfigurationName) {
		TeamcenterConfiguration config = tcconnector.proxies.microflows.Microflows
				.retrieveTeamcenterConifgurationByName(context, ConfigurationName);
		String FMSURL = config.getFMSURL(context);
		String[] bootstrapURLs = FMSURL.split(",");
		return bootstrapURLs;
	}

	/*
	 * Upload Files to FMS.
	 */
	public static java.util.List<IMendixObject> uploadFilestoFMS(IContext context, String ConfigurationName,
			JSONObject createTransientFileTicketsResponse, java.util.List<tcconnector.proxies.FileDocument> Files)
			throws UnknownHostException, FSCException, CoreException {
		java.util.List<IMendixObject> fileTickets = new ArrayList<IMendixObject>();
		CommonsFSCWholeFileIOImpl fscFileIOImpl = FMSUtils.initializeFMS(context, ConfigurationName);

		for (int i = 0; i < Files.size(); i++) {
			String ticket = getTicket(createTransientFileTicketsResponse, i);

			// open stream to upload file to FMS
			InputStream is = Core.getFileDocumentContent(context, Files.get(i).getMendixObject());

			// This API throws exception if file upload is unsuccessful.
			fscFileIOImpl.upload("TCM", null, ticket, is, Files.get(i).getSize());

			// Create fileTicket object and add it to list
			IMendixObject fileTicket = Core.instantiate(context, tcconnector.proxies.StringObject.entityName);
			fileTicket.setValue(context, "value", ticket);
			fileTickets.add(fileTicket);
		}
		return fileTickets;
	}

	/*
	 * retrieve ticket from createTransientFileTicketsResponse response
	 */
	private static String getTicket(JSONObject createTransientFileTicketsResponse, int count) {
		// ticket information
		String ticket = null;
		if (createTransientFileTicketsResponse.getJSONArray("transientFileTicketInfos").length() > 0) {
			JSONObject transientFileTicketInfo = (JSONObject) createTransientFileTicketsResponse
					.getJSONArray("transientFileTicketInfos").get(count);
			int length = transientFileTicketInfo.getString("ticket").length();
			if (length > 0) {
				ticket = transientFileTicketInfo.getString("ticket");
			}
		}
		return ticket;
	}
}
