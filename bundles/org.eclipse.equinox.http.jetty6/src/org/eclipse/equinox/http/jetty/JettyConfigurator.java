package org.eclipse.equinox.http.jetty;

import java.security.Permission;
import java.util.Dictionary;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationPermission;
import org.eclipse.equinox.http.jetty.internal.Activator;

/**
 * JettyConfigurator provides API level access for creating configured instances of a Jetty-based Http Service.
 * The created instances are not persistent across re-starts of the bundle.
 * Settings:
 * 		name="http.enabled" type="Boolean" (default: true)
 * 		name="http.port" type="Integer" (default: 0 -- first available port)
 * 		name="https.enabled" type="Boolean" (default: false)
 * 		name="https.port" type="Integer" (default: 0 -- first available port)
 * 		name="ssl.keystore" type="String"
 * 		name="ssl.password" type="String"
 * 		name="ssl.keypassword" type="String"
 * 		name="ssl.needclientauth" type="Boolean"
 * 		name="ssl.wantclientauth" type="Boolean"
 * 		name="ssl.protocol" type="String"
 * 		name="ssl.algorithm" type="String"
 * 		name="ssl.keystoretype" type="String"
 * 		name="context.path" type="String"
 * 		name="context.sessioninactiveinterval" type="Integer"
 * 		name="other.info" type="String"
 *
 */
public class JettyConfigurator {
	private static final String PID_PREFIX = "org.eclipse.equinox.http.jetty.JettyConfigurator."; //$NON-NLS-1$
	private static Permission configurationPermission = new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE); //$NON-NLS-1$

	public static void startServer(String id, Dictionary settings) throws Exception {
		checkConfigurationPermission();
		String pid = PID_PREFIX + id;
		settings.put(Constants.SERVICE_PID, pid);
		Activator.startServer(pid, settings);
	}
	
	public static void stopServer(String id) throws Exception {
		checkConfigurationPermission();
		Activator.stopServer(PID_PREFIX + id);		
	}
	
	private static void checkConfigurationPermission() throws SecurityException {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) 
			sm.checkPermission(configurationPermission );
	}
}
