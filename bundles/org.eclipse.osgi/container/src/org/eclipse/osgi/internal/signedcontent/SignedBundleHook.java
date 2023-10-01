/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.ActivatorHookFactory;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.service.security.KeyStoreTrustEngine;
import org.eclipse.osgi.internal.signedcontent.SignedContentFromBundleFile.BaseSignerInfo;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Implements signed bundle hook support for the framework
 */
public class SignedBundleHook implements ActivatorHookFactory, HookConfigurator, SignedContentFactory {
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	// TODO: comes from configuration!;
	private final static String CACERTS_PATH = System.getProperty("java.home") + File.separatorChar + "lib" //$NON-NLS-1$//$NON-NLS-2$
			+ File.separatorChar + "security" + File.separatorChar + "cacerts"; //$NON-NLS-1$//$NON-NLS-2$
	private final static String CACERTS_TYPE = "JKS"; //$NON-NLS-1$
	private final static String OSGI_KEYSTORE = "osgi.framework.keystore"; //$NON-NLS-1$
	private int supportSignedBundles;
	TrustEngineListener trustEngineListener;
	private String trustEngineNameProp;
	private ServiceRegistration<?> signedContentFactoryReg;
	private ServiceRegistration<?> systemTrustEngineReg;
	private List<ServiceRegistration<?>> osgiTrustEngineReg;
	private ServiceTracker<TrustEngine, TrustEngine> trustEngineTracker;
	private BundleContext context;
	private EquinoxContainer container;

	@Override
	public BundleActivator createActivator() {
		return new BundleActivator() {

			@Override
			public void start(BundleContext bc) throws Exception {
				frameworkStart(bc);
			}

			@Override
			public void stop(BundleContext bc) throws Exception {
				frameworkStop(bc);
			}
		};
	}

	BundleContext getContext() {
		return context;
	}

	void frameworkStart(BundleContext bc) {
		this.context = bc;
		if ((supportSignedBundles & EquinoxConfiguration.SIGNED_CONTENT_VERIFY_TRUST) != 0)
			// initialize the trust engine listener only if trust is being established with
			// a trust engine
			trustEngineListener = new TrustEngineListener(context, this);
		// always register the trust engine
		Dictionary<String, Object> trustEngineProps = new Hashtable<>(7);
		trustEngineProps.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		trustEngineProps.put(SignedContentConstants.TRUST_ENGINE, SignedContentConstants.DEFAULT_TRUST_ENGINE);
		KeyStoreTrustEngine systemTrustEngine = new KeyStoreTrustEngine(CACERTS_PATH, CACERTS_TYPE, null, "System", //$NON-NLS-1$
				this);
		systemTrustEngineReg = context.registerService(TrustEngine.class.getName(), systemTrustEngine,
				trustEngineProps);
		String osgiTrustPath = context.getProperty(OSGI_KEYSTORE);
		if (osgiTrustPath != null) {
			try {
				URL url = new URL(osgiTrustPath);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					trustEngineProps.put(SignedContentConstants.TRUST_ENGINE, OSGI_KEYSTORE);
					String path = url.getPath();
					osgiTrustEngineReg = new ArrayList<>(1);
					osgiTrustEngineReg.add(context.registerService(TrustEngine.class.getName(),
							new KeyStoreTrustEngine(path, CACERTS_TYPE, null, OSGI_KEYSTORE, this), trustEngineProps));
				}
			} catch (MalformedURLException e) {
				log("Invalid setting for " + OSGI_KEYSTORE, FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
			}
		} else {
			String osgiTrustRepoPaths = context.getProperty(Constants.FRAMEWORK_TRUST_REPOSITORIES);
			if (osgiTrustRepoPaths != null) {
				trustEngineProps.put(SignedContentConstants.TRUST_ENGINE, Constants.FRAMEWORK_TRUST_REPOSITORIES);
				StringTokenizer st = new StringTokenizer(osgiTrustRepoPaths, File.pathSeparator);
				osgiTrustEngineReg = new ArrayList<>(1);
				while (st.hasMoreTokens()) {
					String trustRepoPath = st.nextToken();
					osgiTrustEngineReg.add(context.registerService(TrustEngine.class.getName(),
							new KeyStoreTrustEngine(trustRepoPath, CACERTS_TYPE, null, OSGI_KEYSTORE, this),
							trustEngineProps));
				}
			}
		}
		// always register the signed content factory
		signedContentFactoryReg = context.registerService(SignedContentFactory.class.getName(), this, null);
	}

	void frameworkStop(BundleContext bc) {
		if (signedContentFactoryReg != null) {
			signedContentFactoryReg.unregister();
			signedContentFactoryReg = null;
		}
		if (systemTrustEngineReg != null) {
			systemTrustEngineReg.unregister();
			systemTrustEngineReg = null;
		}
		if (osgiTrustEngineReg != null) {
			for (ServiceRegistration<?> serviceRegistration : osgiTrustEngineReg)
				serviceRegistration.unregister();
			osgiTrustEngineReg = null;
		}
		if (trustEngineTracker != null) {
			trustEngineTracker.close();
			trustEngineTracker = null;
		}
	}

	@Override
	public void addHooks(HookRegistry hookRegistry) {
		container = hookRegistry.getContainer();
		hookRegistry.addActivatorHookFactory(this);
		supportSignedBundles = hookRegistry.getConfiguration().supportSignedBundles;
		trustEngineNameProp = hookRegistry.getConfiguration().getConfiguration(SignedContentConstants.TRUST_ENGINE);
	}

	@Override
	public SignedContent getSignedContent(File content) throws IOException, InvalidKeyException, SignatureException,
			CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		SignedContentFromBundleFile signedContent = new SignedContentFromBundleFile(content,
				container.getConfiguration().getDebug());
		determineTrust(signedContent, EquinoxConfiguration.SIGNED_CONTENT_VERIFY_TRUST);
		return signedContent;
	}

	@Override
	public SignedContent getSignedContent(Bundle bundle) throws IOException, InvalidKeyException, SignatureException,
			CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		Generation generation = (Generation) ((EquinoxBundle) bundle).getModule().getCurrentRevision()
				.getRevisionInfo();
		SignedContentFromBundleFile signedContent = new SignedContentFromBundleFile(generation.getBundleFile());
		determineTrust(signedContent, EquinoxConfiguration.SIGNED_CONTENT_VERIFY_TRUST);
		return signedContent;
	}

	public void log(String msg, int severity, Throwable t) {
		container.getLogServices().log(EquinoxContainer.NAME, severity, msg, t);
	}

	private TrustEngine[] getTrustEngines() {
		// find all the trust engines available
		if (context == null)
			return new TrustEngine[0];
		if (trustEngineTracker == null) {
			// read the trust provider security property
			Filter filter = null;
			if (trustEngineNameProp != null)
				try {
					filter = context.createFilter("(&(" + Constants.OBJECTCLASS + "=" + TrustEngine.class.getName() //$NON-NLS-1$ //$NON-NLS-2$
							+ ")(" + SignedContentConstants.TRUST_ENGINE + "=" + trustEngineNameProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				} catch (InvalidSyntaxException e) {
					log("Invalid trust engine filter", FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
				}
			if (filter != null) {
				trustEngineTracker = new ServiceTracker<>(context, filter, new TrustEngineCustomizer());
			} else
				trustEngineTracker = new ServiceTracker<>(context, TrustEngine.class.getName(),
						new TrustEngineCustomizer());
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

	class TrustEngineCustomizer implements ServiceTrackerCustomizer<TrustEngine, TrustEngine> {

		@Override
		public TrustEngine addingService(ServiceReference<TrustEngine> reference) {
			TrustEngine engine = getContext().getService(reference);
			if (engine != null) {
				try {
					Field trustEngineListenerField = TrustEngine.class.getDeclaredField("trustEngineListener"); //$NON-NLS-1$
					trustEngineListenerField.setAccessible(true);
					trustEngineListenerField.set(engine, SignedBundleHook.this.trustEngineListener);
				} catch (Exception e) {
					log("Unable to set the trust engine listener.", FrameworkLogEntry.ERROR, e); //$NON-NLS-1$
				}

			}
			return engine;
		}

		@Override
		public void modifiedService(ServiceReference<TrustEngine> reference, TrustEngine service) {
			// nothing
		}

		@Override
		public void removedService(ServiceReference<TrustEngine> reference, TrustEngine service) {
			// nothing
		}

	}

	void determineTrust(SignedContentFromBundleFile trustedContent, int supportFlags) {
		TrustEngine[] engines = null;
		SignerInfo[] signers = trustedContent.getSignerInfos();
		for (SignerInfo signer : signers) {
			// first check if we need to find an anchor
			if (signer.getTrustAnchor() == null) {
				// no anchor set ask the trust engines
				if (engines == null)
					engines = getTrustEngines();
				// check trust of singer certs
				Certificate[] signerCerts = signer.getCertificateChain();
				((BaseSignerInfo) signer).setTrustAnchor(findTrustAnchor(signerCerts, engines, supportFlags));
				// if signer has a tsa check trust of tsa certs
				SignerInfo tsaSignerInfo = trustedContent.getTSASignerInfo(signer);
				if (tsaSignerInfo != null) {
					Certificate[] tsaCerts = tsaSignerInfo.getCertificateChain();
					((BaseSignerInfo) tsaSignerInfo).setTrustAnchor(findTrustAnchor(tsaCerts, engines, supportFlags));
				}
			}
		}
	}

	private Certificate findTrustAnchor(Certificate[] certs, TrustEngine[] engines, int supportFlags) {
		if ((supportFlags & EquinoxConfiguration.SIGNED_CONTENT_VERIFY_TRUST) == 0)
			// we are not searching the engines; in this case we just assume the root cert
			// is trusted
			return certs != null && certs.length > 0 ? certs[certs.length - 1] : null;
		for (TrustEngine engine : engines) {
			try {
				Certificate anchor = engine.findTrustAnchor(certs);
				if (anchor != null)
					// found an anchor
					return anchor;
			} catch (IOException e) {
				// log the exception and continue
				log("TrustEngine failure: " + engine.getName(), FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
			}
		}
		return null;
	}
}
