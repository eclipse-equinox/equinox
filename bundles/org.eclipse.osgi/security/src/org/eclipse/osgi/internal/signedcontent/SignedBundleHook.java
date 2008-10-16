/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Hashtable;
import java.util.Properties;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationEngine;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifierFactory;
import org.eclipse.osgi.internal.service.security.DefaultAuthorizationEngine;
import org.eclipse.osgi.internal.service.security.KeyStoreTrustEngine;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implements signed bundle hook support for the framework
 */
public class SignedBundleHook implements AdaptorHook, BundleFileWrapperFactoryHook, HookConfigurator, SignedContentFactory {
	static final int VERIFY_CERTIFICATE = 0x01;
	static final int VERIFY_TRUST = 0x02;
	static final int VERIFY_RUNTIME = 0x04;
	static final int VERIFY_AUTHORITY = 0x08;
	static final int VERIFY_ALL = VERIFY_CERTIFICATE | VERIFY_TRUST | VERIFY_RUNTIME | VERIFY_AUTHORITY;
	private static String SUPPORT_CERTIFICATE = "certificate"; //$NON-NLS-1$
	private static String SUPPORT_TRUST = "trust"; //$NON-NLS-1$
	private static String SUPPORT_RUNTIME = "runtime"; //$NON-NLS-1$
	private static String SUPPORT_AUTHORITY = "authority"; //$NON-NLS-1$
	private static String SUPPORT_ALL = "all"; //$NON-NLS-1$
	private static String SUPPORT_TRUE = "true"; //$NON-NLS-1$

	//TODO: comes from configuration!;
	private static String CACERTS_PATH = System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security" + File.separatorChar + "cacerts"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
	private static String CACERTS_TYPE = "JKS"; //$NON-NLS-1$
	private static ServiceTracker trustEngineTracker;
	private static BaseAdaptor ADAPTOR;
	private static String SIGNED_BUNDLE_SUPPORT = "osgi.support.signature.verify"; //$NON-NLS-1$
	private static String SIGNED_CONTENT_SUPPORT = "osgi.signedcontent.support"; //$NON-NLS-1$
	private static String OSGI_KEYSTORE = "osgi.framework.keystore"; //$NON-NLS-1$
	private static int supportSignedBundles;
	private TrustEngineListener trustEngineListener;
	private BundleInstallListener installListener;
	private ServiceRegistration signedContentFactoryReg;
	private ServiceRegistration systemTrustEngineReg;
	private ServiceRegistration defaultAuthEngineReg;
	private ServiceRegistration osgiTrustEngineReg;
	private ServiceRegistration legacyFactoryReg;

	public boolean matchDNChain(String pattern, String dnChain[]) {
		boolean satisfied = false;
		if (dnChain != null) {
			for (int i = 0; i < dnChain.length; i++)
				if (DNChainMatching.match(dnChain[i], pattern)) {
					satisfied = true;
					break;
				}
		}
		return satisfied;
	}

	public void initialize(BaseAdaptor adaptor) {
		SignedBundleHook.ADAPTOR = adaptor;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		// check if load time authority is enabled
		if ((supportSignedBundles & VERIFY_AUTHORITY) != 0) {
			// install the default bundle install listener
			installListener = new BundleInstallListener();
			context.addBundleListener(installListener);
			// register the default authorization engine
			Hashtable properties = new Hashtable(7);
			properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
			properties.put(SignedContentConstants.AUTHORIZATION_ENGINE, SignedContentConstants.DEFAULT_AUTHORIZATION_ENGINE);
			defaultAuthEngineReg = context.registerService(AuthorizationEngine.class.getName(), new DefaultAuthorizationEngine(context, ADAPTOR.getState()), properties);
		}

		// always register the trust engine
		Hashtable properties = new Hashtable(7);
		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
		properties.put(SignedContentConstants.TRUST_ENGINE, SignedContentConstants.DEFAULT_TRUST_ENGINE);
		KeyStoreTrustEngine systemTrustEngine = new KeyStoreTrustEngine(CACERTS_PATH, CACERTS_TYPE, null, "System"); //$NON-NLS-1$
		systemTrustEngineReg = context.registerService(TrustEngine.class.getName(), systemTrustEngine, properties);
		String osgiTrustPath = context.getProperty(OSGI_KEYSTORE);
		if (osgiTrustPath != null) {
			try {
				URL url = new URL(osgiTrustPath);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					String path = url.getPath();
					osgiTrustEngineReg = context.registerService(TrustEngine.class.getName(), new KeyStoreTrustEngine(path, CACERTS_TYPE, null, OSGI_KEYSTORE), null);
				}
			} catch (MalformedURLException e) {
				SignedBundleHook.log("Invalid setting for " + OSGI_KEYSTORE, FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
			}
		} else {
			osgiTrustPath = context.getProperty(Constants.FRAMEWORK_TRUST_REPOSITORIES);
			if (osgiTrustPath != null) {
				osgiTrustEngineReg = context.registerService(TrustEngine.class.getName(), new KeyStoreTrustEngine(osgiTrustPath, CACERTS_TYPE, null, OSGI_KEYSTORE), null);
			}
		}
		if ((supportSignedBundles & VERIFY_TRUST) != 0)
			// initialize the trust engine listener only if trust is being established with a trust engine
			trustEngineListener = new TrustEngineListener(context);
		// always register the signed content factory
		signedContentFactoryReg = context.registerService(SignedContentFactory.class.getName(), this, null);
		legacyFactoryReg = context.registerService(CertificateVerifierFactory.class.getName(), new LegacyVerifierFactory(this), null);
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		if (legacyFactoryReg != null) {
			legacyFactoryReg.unregister();
			legacyFactoryReg = null;
		}
		if (signedContentFactoryReg != null) {
			signedContentFactoryReg.unregister();
			signedContentFactoryReg = null;
		}
		if (systemTrustEngineReg != null) {
			systemTrustEngineReg.unregister();
			systemTrustEngineReg = null;
		}
		if (osgiTrustEngineReg != null) {
			osgiTrustEngineReg.unregister();
			osgiTrustEngineReg = null;
		}
		if (defaultAuthEngineReg != null) {
			defaultAuthEngineReg.unregister();
			defaultAuthEngineReg = null;
		}
		if (trustEngineListener != null) {
			trustEngineListener.stopTrustEngineListener();
			trustEngineListener = null;
		}
		if (installListener != null) {
			context.removeBundleListener(installListener);
			installListener = null;
		}
		if (trustEngineTracker != null) {
			trustEngineTracker.close();
			trustEngineTracker = null;
		}
	}

	public void frameworkStopping(BundleContext context) {
		// do nothing
	}

	public void addProperties(Properties properties) {
		// do nothing
	}

	public URLConnection mapLocationToURLConnection(String location) throws IOException {
		return null;
	}

	public void handleRuntimeError(Throwable error) {
		// do nothing
	}

	public FrameworkLog createFrameworkLog() {
		return null;
	}

	public BundleFile wrapBundleFile(BundleFile bundleFile, Object content, BaseData data, boolean base) {
		try {
			if (bundleFile != null) {
				SignedStorageHook hook = (SignedStorageHook) data.getStorageHook(SignedStorageHook.KEY);
				SignedBundleFile signedBaseFile;
				if (base && hook != null) {
					signedBaseFile = new SignedBundleFile(hook.signedContent, supportSignedBundles);
					if (hook.signedContent == null) {
						signedBaseFile.setBundleFile(bundleFile);
						SignedContentImpl signedContent = signedBaseFile.getSignedContent();
						hook.signedContent = signedContent != null && signedContent.isSigned() ? signedContent : null;
					}
				} else
					signedBaseFile = new SignedBundleFile(null, supportSignedBundles);
				signedBaseFile.setBundleFile(bundleFile);
				SignedContentImpl signedContent = signedBaseFile.getSignedContent();
				if (signedContent != null && signedContent.isSigned()) {
					// only use the signed file if there are certs
					signedContent.setContent(signedBaseFile);
					bundleFile = signedBaseFile;
				}
			}
		} catch (IOException e) {
			SignedBundleHook.log("Bad bundle file: " + bundleFile.getBaseFile(), FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
		} catch (GeneralSecurityException e) {
			SignedBundleHook.log("Bad bundle file: " + bundleFile.getBaseFile(), FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
		}
		return bundleFile;
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
		String[] support = ManifestElement.getArrayFromList(FrameworkProperties.getProperty(SIGNED_CONTENT_SUPPORT, FrameworkProperties.getProperty(SIGNED_BUNDLE_SUPPORT)), ","); //$NON-NLS-1$
		for (int i = 0; i < support.length; i++) {
			if (SUPPORT_CERTIFICATE.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE;
			else if (SUPPORT_TRUST.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_TRUST;
			else if (SUPPORT_RUNTIME.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_RUNTIME;
			else if (SUPPORT_AUTHORITY.equals(support[i]))
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_TRUST | VERIFY_AUTHORITY;
			else if (SUPPORT_TRUE.equals(support[i]) || SUPPORT_ALL.equals(support[i]))
				supportSignedBundles |= VERIFY_ALL;
		}
		if ((supportSignedBundles & VERIFY_CERTIFICATE) != 0) {
			hookRegistry.addStorageHook(new SignedStorageHook());
			hookRegistry.addBundleFileWrapperFactoryHook(this);
		}
	}

	public SignedContent getSignedContent(File content) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		if (content == null)
			throw new IllegalArgumentException("null content"); //$NON-NLS-1$
		BundleFile contentBundleFile;
		if (content.isDirectory())
			contentBundleFile = new DirBundleFile(content);
		else
			contentBundleFile = new ZipBundleFile(content, null);
		SignedBundleFile result = new SignedBundleFile(null, VERIFY_ALL);
		result.setBundleFile(contentBundleFile);
		return new SignedContentFile(result.getSignedContent());
	}

	public SignedContent getSignedContent(Bundle bundle) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IllegalArgumentException {
		BundleData data = ((AbstractBundle) bundle).getBundleData();
		if (!(data instanceof BaseData))
			throw new IllegalArgumentException("Invalid bundle object.  No BaseData found."); //$NON-NLS-1$
		SignedStorageHook hook = (SignedStorageHook) ((BaseData) data).getStorageHook(SignedStorageHook.KEY);
		SignedContent result = hook != null ? hook.signedContent : null;
		if (result != null)
			return result; // just reuse the signed content the storage hook
		return getSignedContent(((BaseData) data).getBundleFile().getBaseFile()); // must create a new signed content using the raw file
	}

	public static void log(String msg, int severity, Throwable t) {
		if (SignedBundleHook.ADAPTOR == null) {
			System.err.println(msg);
			t.printStackTrace();
			return;
		}
		FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, severity, 0, msg, 0, t, null);
		SignedBundleHook.ADAPTOR.getFrameworkLog().log(entry);
	}

	static BundleContext getContext() {
		if (ADAPTOR == null)
			return null;
		return ADAPTOR.getContext();
	}

	static TrustEngine[] getTrustEngines() {
		// find all the trust engines available
		BundleContext context = SignedBundleHook.getContext();
		if (context == null)
			return new TrustEngine[0];
		if (trustEngineTracker == null) {
			// read the trust provider security property
			String trustEngineProp = FrameworkProperties.getProperty(SignedContentConstants.TRUST_ENGINE);
			Filter filter = null;
			if (trustEngineProp != null)
				try {
					filter = FrameworkUtil.createFilter("(&(" + Constants.OBJECTCLASS + "=" + TrustEngine.class.getName() + ")(" + SignedContentConstants.TRUST_ENGINE + "=" + trustEngineProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				} catch (InvalidSyntaxException e) {
					SignedBundleHook.log("Invalid trust engine filter", FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
				}
			if (filter != null) {
				trustEngineTracker = new ServiceTracker(context, filter, null);
			} else
				trustEngineTracker = new ServiceTracker(context, TrustEngine.class.getName(), null);
			trustEngineTracker.open();
		}
		Object[] services = trustEngineTracker.getServices();
		if (services != null) {
			TrustEngine[] engines = new TrustEngine[services.length];
			System.arraycopy(services, 0, engines, 0, services.length);
			return engines;
		}
		return new TrustEngine[0];
	}
}
