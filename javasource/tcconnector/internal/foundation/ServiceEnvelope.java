// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.thirdparty.org.json.JSONObject;

public class ServiceEnvelope {

	private static final String CLIENT_NAME = "MxTcC";
	private static final String STATE_CLIENT_VERSION = "clientVersion";
	private static final String STATE_UNLOAD_OBJECTS = "unloadObjects";
	private static final String STATE_LOG_ID = "logCorrelationID";
	private static final String STATE_CLIENT_ID = "clientID";
	private static final String STATE_STATELESS = "stateless";
	private static final String STATE_ENABLE_HEADERS = "enableServerStateHeaders";

	private static final String KEY_STATE = "state";
	private static final String KEY_POLICY = "policy";
	private static final String KEY_HEADER = "header";
	private static final String KEY_BODY = "body";

	private static JSONObject DEFAULT_POLICY = null;
	private static boolean HAVE_DEFAULT_POLICY = false;
	private static final String PROPERTY_POLICY_FILE_NAME = "TeamcenterCommon\\DefaultPropertyPolicy.json";

	private JSONObject envelopeObj;

	public ServiceEnvelope(IContext context) {
		envelopeObj = new JSONObject();
		JSONObject header = new JSONObject();
		JSONObject state = new JSONObject();

		state.accumulate(STATE_CLIENT_VERSION, "MxTcC_" + tcconnector.proxies.constants.Constants.getVersion());
		state.accumulate(STATE_LOG_ID, LogCorrelationID.getId());
		state.accumulate(STATE_CLIENT_ID, CLIENT_NAME + "." + context.getSession().getId().hashCode());
		state.accumulate(STATE_STATELESS, true);
		state.accumulate(STATE_ENABLE_HEADERS, true);
		state.accumulate(STATE_UNLOAD_OBJECTS, true);

		header.accumulate(KEY_STATE, state);
		envelopeObj.accumulate(KEY_HEADER, header);

	}

	public void setBody(JSONObject body) {
		envelopeObj.accumulate(KEY_BODY, body);
	}

	public void setPolicy(JSONObject policy) {
		if (policy == null) {
			policy = getDefaultPolicy();
		}
		if (policy != null) {
			envelopeObj.getJSONObject(KEY_HEADER).accumulate(KEY_POLICY, policy);
		}
	}

	public JSONObject getJSONObject() {
		return envelopeObj;
	}

	private static String getFileContent(FileInputStream fis, String encoding) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(fis, encoding))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			return sb.toString();
		}
	}

	private static synchronized JSONObject getDefaultPolicy() {
		if (HAVE_DEFAULT_POLICY)
			return DEFAULT_POLICY;

		HAVE_DEFAULT_POLICY = true;
		String policyFilePath = Core.getConfiguration().getResourcesPath() + File.separator + PROPERTY_POLICY_FILE_NAME;
		File policyFile = new File(policyFilePath);
		try {
			FileInputStream fis = new FileInputStream(policyFile);
			String ootbPropertyPolicy = getFileContent(fis, "UTF-8");
			DEFAULT_POLICY = new JSONObject(ootbPropertyPolicy);
		} catch (IOException e) {
			Constants.LOGGER.warn("Failed to readed  the default Object Property Policy, " + PROPERTY_POLICY_FILE_NAME
					+ ". " + e.getMessage()
					+ " Will use server defined Default.xml policy for service operations that do not defined a policy.");
			DEFAULT_POLICY = null;
		}
		return DEFAULT_POLICY;
	}
}
