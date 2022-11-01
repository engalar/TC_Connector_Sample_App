// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.util.ArrayList;
import java.util.List;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.HttpHeader;
import system.proxies.HttpResponse;

public class HttpUtil {
	public static List<HttpHeader> getHeaders(IContext context, HttpResponse httpResponse, String headerName) {
		List<IMendixObject> mxHeaders = com.mendix.core.Core.retrieveByPath(context, httpResponse.getMendixObject(),
				HttpHeader.MemberNames.HttpHeaders.toString(), true);
		List<HttpHeader> headers = new ArrayList<>();

		for (IMendixObject mxHeader : mxHeaders) {
			HttpHeader header = HttpHeader.initialize(context, mxHeader);

			if (header.getKey().equalsIgnoreCase(headerName)) {
				headers.add(header);
			}
		}
		return headers;
	}

	public static HttpHeader getHeader(IContext context, HttpResponse httpResponse, String headerName) {
		List<IMendixObject> mxHeaders = com.mendix.core.Core.retrieveByPath(context, httpResponse.getMendixObject(),
				HttpHeader.MemberNames.HttpHeaders.toString(), true);

		for (IMendixObject mxHeader : mxHeaders) {
			HttpHeader header = HttpHeader.initialize(context, mxHeader);
			if (header.getKey().equalsIgnoreCase(headerName)) {
				return header;
			}
		}
		return null;
	}
}
