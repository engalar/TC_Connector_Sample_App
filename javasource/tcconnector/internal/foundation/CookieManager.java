// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.util.List;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.HttpHeader;
import system.proxies.HttpResponse;
import tcconnector.proxies.Cookie;
import tcconnector.proxies.TcSession;

public class CookieManager {
	private static final String HEADER_SET_COOKIE = "Set-Cookie";
	private static final String PATH_TOKEN = "path=";
	private static final String SECURE_TOKEN = "Secure";

	public static void presistCookies(IContext context, TcSession tcSession, HttpResponse httpResponse) {
		CookieManager cm = new CookieManager(context, tcSession);
		cm.presistCookies(httpResponse);
	}

	public static String getCookieHeaderValue(IContext context, TcSession tcSession) {
		CookieManager cm = new CookieManager(context, tcSession);
		return cm.getCookieHeaderValue();
	}

	public static void deleteCookies(IContext context, TcSession tcSession) {
		CookieManager cm = new CookieManager(context, tcSession);
		cm.deleteCookies();
	}

	private IContext context;
	private TcSession tcSession;

	private CookieManager(IContext context, TcSession tcSession) {
		this.context = context;
		this.tcSession = tcSession;
	}

	private void presistCookies(HttpResponse httpResponse) {
		List<IMendixObject> mxCookies = com.mendix.core.Core.retrieveByPath(context, tcSession.getMendixObject(),
				Cookie.MemberNames.Cookies.toString(), true);
		List<HttpHeader> headers = HttpUtil.getHeaders(context, httpResponse, HEADER_SET_COOKIE);

		for (HttpHeader header : headers) {
			String fullCookie = header.getValue();
			String[] parameters = fullCookie.split(";");
			Cookie cookie = findOrUpdate(mxCookies, parameters[0].trim());
			for (int i = 1; i < parameters.length; i++) {
				String parameter = parameters[i].trim();
				setPath(cookie, parameter);
				setSecure(cookie, parameter);
			}
			try {
				cookie.setCookies(tcSession);
				cookie.commit();
				/*
				 * IContext ct = context.getSession().createContext(); ct.startTransaction();
				 * tcSession.commit(ct); ct.endTransaction();
				 */
				tcSession.commit();
			} catch (CoreException e) {
			}
		}
	}

	private void deleteCookies() {
		List<IMendixObject> mxCookies = com.mendix.core.Core.retrieveByPath(context, tcSession.getMendixObject(),
				Cookie.MemberNames.Cookies.toString(), true);
		boolean deleted = com.mendix.core.Core.delete(context, mxCookies);
		if (!deleted) {
			Constants.LOGGER
					.warn("Failed to delete  the cookies from " + tcSession.getUserName() + " Teamcenter session.");
		}
	}

	private String getCookieHeaderValue() {
		String cookieHeaderValue = "";
		List<IMendixObject> mxCookies = com.mendix.core.Core.retrieveByPath(context, tcSession.getMendixObject(),
				Cookie.MemberNames.Cookies.toString(), true);
		String targetPath = tcSession.getHostAddress();
		boolean isHttps = targetPath.startsWith("https");
		targetPath = targetPath.substring(8);
		targetPath = targetPath.substring(targetPath.indexOf("/"));

		for (IMendixObject mxCookie : mxCookies) {
			Cookie cookie = Cookie.initialize(context, mxCookie);
			if (!targetPath.startsWith(cookie.getPath()))
				continue;
			if (cookie.getSecure() && !isHttps)
				continue;
			if (cookieHeaderValue.length() > 0)
				cookieHeaderValue += "; ";
			cookieHeaderValue += cookie.getName() + "=" + cookie.getValue();
		}
		return cookieHeaderValue;
	}

	private Cookie findOrUpdate(List<IMendixObject> mxCookies, String nameValue) {
		String[] nameAndValue = nameValue.split("=");
		Cookie cookie = findExistingCookie(mxCookies, nameAndValue[0]);
		cookie.setValue(nameAndValue[1]);
		return cookie;
	}

	private Cookie findExistingCookie(List<IMendixObject> mxCookies, String name) {
		for (IMendixObject mxCookie : mxCookies) {
			Cookie cookie = Cookie.initialize(context, mxCookie);
			if (cookie.getName().equals(name))
				return cookie;
		}
		Cookie cookie = new Cookie(context);
		cookie.setName(name);
		return cookie;
	}

	private void setPath(Cookie cookie, String parameter) {
		if (parameter.startsWith(PATH_TOKEN)) {
			String path = parameter.substring(5);
			cookie.setPath(path);
		}
	}

	private void setSecure(Cookie cookie, String parameter) {
		if (parameter.equals(SECURE_TOKEN)) {
			cookie.setSecure(true);
		}
	}
}
