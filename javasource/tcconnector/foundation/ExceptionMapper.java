// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.foundation;

import com.mendix.thirdparty.org.json.JSONObject;

import tcconnector.foundation.exceptions.InternalServerException;
import tcconnector.foundation.exceptions.InvalidUserException;
import tcconnector.foundation.exceptions.ServiceException;
import tcconnector.internal.foundation.Constants;

public class ExceptionMapper {

	/**
	 * Constructs and throws an appropriate exception base on the content of the
	 * JSONObject. If the JSON does not map to a known exception, this method
	 * returns without error.
	 * 
	 * @param jsonObj The JSON to convert to an exception.
	 * @throws InternalServerException
	 * @throws InvalidUserException
	 * @throws ServiceException
	 */
	public static void throwExcpetion(JSONObject jsonObj)
			throws InternalServerException, InvalidUserException, ServiceException {
		if (!jsonObj.has(Constants.KEY_QNAME))
			return;

		String qName = jsonObj.optString(Constants.KEY_QNAME);
		if (qName.equals(Constants.QNAME_SERVER_EXP))
			throw new InternalServerException(jsonObj);
		if (qName.equals(Constants.QNAME_USER_EXP))
			throw new InvalidUserException(jsonObj);
		if (qName.equals(Constants.QNAME_SERVICE_EXP))
			throw new ServiceException(jsonObj);
	}
}
