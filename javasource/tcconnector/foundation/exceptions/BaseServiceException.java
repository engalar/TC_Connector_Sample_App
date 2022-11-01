// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation.exceptions;

public abstract class BaseServiceException extends Exception {
	public enum Severity {
		NONE, INFORMATION, WARNING, ERROR, USER_ERROR
	}

	private static final long serialVersionUID = 1L;
	protected int code = 0;
	protected Severity level = Severity.ERROR;

	protected BaseServiceException(String message) {
		super(message);
	}

	protected BaseServiceException(String message, Throwable t) {
		super(message, t);
	}

	public int getCode() {
		return code;
	}

	public Severity getLevel() {
		return level;
	}

}
