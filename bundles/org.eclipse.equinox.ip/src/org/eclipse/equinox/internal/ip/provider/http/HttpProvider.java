/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip.provider.http;

import java.util.*;
import javax.servlet.http.*;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.eclipse.equinox.internal.ip.provider.BaseProvider;
import org.osgi.framework.*;
import org.osgi.service.http.HttpService;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.service.useradmin.*;

/**
 * Implements ProvisioningInfoProvider. Allows remote data loading through HTTP.
 * Acts as dynamic provider. On <I>load(Dictionary)</I> it just get reference
 * to provisioning data.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class HttpProvider extends BaseProvider implements ProvisioningInfoProvider, ServiceListener {

	/** Alias this servlet will be registered if remote configuration is allowed. */
	public static final String ALIAS = "/rminit";
	/**
	 * This is a key for property that points if remote setting of info is
	 * allowed.
	 */
	public static final String HTTP_SUPPORT = "equinox.provisioning.http.provider.allowed";
	/**
	 * This system property is used to be determined if to accepts only
	 * HttpServletRequest-s with scheme "https".
	 */
	public static final String SECURE = "equinox.provisioning.http.provider.secure";
	/** This system property determines if to requires authentication. */
	public static final String REQUIRE_AUTH = "equinox.provisioning.require.auth";

	/** If to accepts only HttpServletRequests with scheme "https". */
	static boolean secure;
	/** Reference to provisioning service. */
	ProvisioningService prvSrv;
	/** Bundle context reference. */
	private BundleContext bc;
	/** HTTP service that is used for registration of servlet. */
	private HttpService http;
	/** Servlet that is registered. */
	private HttpServlet servlet = new HttpServletImpl();

	/**
	 * @see org.eclipse.equinox.internal.ip.impl.provider.BaseProvider#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bc) throws Exception {
		boolean httpsupport = true;
		if (bc.getProperty(HTTP_SUPPORT) != null)
			if (bc.getProperty(HTTP_SUPPORT).equals("false"))
				httpsupport = false;
		if (!httpsupport) {
			Log.debug(this + " is not an allowed provider.");
			return;
		}

		this.bc = bc;
		super.start(bc);
	}

	/**
	 * Stops provider. Unregister servlet.
	 * 
	 * @see org.eclipse.equinox.internal.ip.impl.provider.BaseProvider#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) {
		super.stop(bc);
		if (http != null) {
			try {
				http.unregister(ALIAS);
			} catch (Exception e) {
				// Not fatal
			}
			http = null;
		}
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningInfoProvider#init(org.osgi.service.provisioning.ProvisioningService)
	 */
	public Dictionary init(ProvisioningService prvSrv) {
		this.prvSrv = prvSrv;
		secure = false;
		if (ProvisioningAgent.bc.getProperty(SECURE) != null)
			if (ProvisioningAgent.bc.getProperty(SECURE).equals("true"))
				secure = true;
		try {
			bc.addServiceListener(this, '(' + "objectClass" + '=' + HttpService.class.getName() + ')');
		} catch (Exception e) {
			Log.debug(e);
		}

		synchronized (this) {
			if (http == null) {
				ServiceReference sref = bc.getServiceReference(HttpService.class.getName());
				if (sref != null) {
					http = (HttpService) bc.getService(sref);
					registerServlet();
				}
			}
		}
		return null; // Do not loads
	}

	/**
	 * @see org.eclipse.equinox.internal.ip.ProvisioningInfoProvider#get(java.lang.Object)
	 */
	public Object get(Object key) {
		return null;
	}

	/**
	 * Manages servlet (un)registration.
	 * 
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public synchronized void serviceChanged(ServiceEvent se) {
		if (se.getType() == ServiceEvent.REGISTERED) {
			if (http == null) {
				http = (HttpService) bc.getService(se.getServiceReference());
				registerServlet();
			}
		} else if (se.getType() == ServiceEvent.UNREGISTERING) {
			if (http != null) {
				try {
					http.unregister(ALIAS);
				} catch (Exception e) {
					// Not fatal
				}
				http = null;
				ServiceReference sref = bc.getServiceReference(HttpService.class.getName());
				if (sref != null) {
					http = (HttpService) bc.getService(sref);
					registerServlet();
				}
			}
		}
	}

	// Registers servlet with ALIAS.
	private void registerServlet() {
		try {
			http.registerServlet(ALIAS, servlet, null, Context.getInstance());
			Log.debug("Servlet \"" + ALIAS + "\" registered.");
		} catch (Exception e) {
			// Servlet won't be registered
			Log.debug("Error registering HTTP provider servlet!", e);
		}
	}

	/**
	 * Return name of provider.
	 * 
	 * @return name.
	 */
	public String toString() {
		return "Http";
	}

	/**
	 * Class implements servlet which enables (un)secure remote configuration.
	 */
	class HttpServletImpl extends HttpServlet {

		private static final long serialVersionUID = 1L;

		// If supports remote configuration
		/**
		 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
		 *      javax.servlet.http.HttpServletResponse)
		 */
		public void doGet(HttpServletRequest request, HttpServletResponse response) {
			Log.debug("doGet ...");
			if (request.getHeader("Get-Id") == null) {
				Log.debug("Redirect to doPost.");
				doPost(request, response);
			} else {
				Log.debug("Request for spid.");
				if (secure && !request.getScheme().equals("https")) {
					Log.debug("Request to secure HttpLoader must be via https!");
					response.setHeader("error", "Request to secure HttpLoader must be via https!");
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
				String spid = (String) prvSrv.getInformation().get(ProvisioningService.PROVISIONING_SPID);
				if (spid != null && spid.length() != 0) {
					response.setHeader("Gw-Id", spid);
				}
				response.setStatus(HttpServletResponse.SC_OK);
			}
		}

		// If supports remote configuration
		/**
		 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
		 *      javax.servlet.http.HttpServletResponse)
		 */
		public void doPost(HttpServletRequest request, HttpServletResponse response) {
			Log.debug("doPost ...");
			if (secure && !request.getScheme().equals("https")) {
				Log.debug("Request to secure HttpLoader must be via https!");
				response.setHeader("error", "Request to secure HttpLoader must be via https!");
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}

			String user = request.getParameter("user");
			String pass = request.getParameter("pass");

			boolean req_auth = false;
			if (ProvisioningAgent.bc.getProperty(REQUIRE_AUTH) != null)
				if (ProvisioningAgent.bc.getProperty(REQUIRE_AUTH).equals("true"))
					req_auth = true;

			if (!req_auth && !checkAccount(user, pass)) {
				Log.debug("Incorrect Account!");
				response.setHeader("error", "Incorrect account!");
				return;
			}

			Dictionary info = new Hashtable();
			Log.debug("HttpLoader loads:");
			for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
				String name = (String) e.nextElement();
				if (!"user".equals(name) && !"pass".equals(name)) {
					String param = request.getParameter(name);
					if (ProvisioningInfoProvider.GM_HOST.equals(name) && "no".equals(param)) {
						continue;
					}
					Log.debug("  " + name + '=' + param);
					info.put(name, param);
				}
			}

			prvSrv.addInformation(info);
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	/*
	 * Returns if account is correct.
	 * 
	 * TODO: add user admin to tracked services
	 */
	boolean checkAccount(String user, String pass) {
		if (user == null || pass == null) {
			return false;
		}

		ServiceReference sref = bc.getServiceReference(UserAdmin.class.getName());
		if (sref == null) {
			return false;
		}
		UserAdmin userAdmin = (UserAdmin) bc.getService(sref);
		if (userAdmin != null) {
			try {
				User userRole = (User) userAdmin.getRole(user);
				if (userRole != null && userRole.hasCredential("password", pass)) {
					Authorization authorization = userAdmin.getAuthorization(userRole);
					return authorization.hasRole("administration");
				}
			} catch (Exception e) {
				Log.debug(e);
			}
		}
		return false;
	}

}
