// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation.exceptions;

public abstract class AuthException extends BaseServiceException {
	private static final long serialVersionUID = 1L;
	protected String ssoUrl = "";
	protected String appId = "";

	protected AuthException(String message) {
		super(message);
	}

	public String getSsoUrl() {
		return ssoUrl;
	}

	public String getAppId() {
		return appId;
	}
}
