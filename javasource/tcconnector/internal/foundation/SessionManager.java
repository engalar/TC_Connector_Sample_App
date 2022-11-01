// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import com.mendix.core.CoreException;
import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.proxies.TcSession;

public class SessionManager {
	public static void cacheSessionState(TcSession tcSession, JSONObject jsonInputObj, JSONObject jsonResponseObj) {
		if (jsonResponseObj.optString(Constants.KEY_QNAME, "").equals(Constants.QNAME_LOGIN_RESPONSE)) {
			String userName = jsonInputObj.getJSONObject(Constants.KEY_CREDENTIALS).getString(Constants.KEY_USER);
			tcSession.setUserName(userName);
			try {
				/*
				 * IContext ct = tcSession.getContext().getSession().createContext();
				 * ct.startTransaction(); tcSession.commit(ct); ct.endTransaction();
				 */
				tcSession.commit();
			} catch (CoreException e) {
			}

		}
	}
}
