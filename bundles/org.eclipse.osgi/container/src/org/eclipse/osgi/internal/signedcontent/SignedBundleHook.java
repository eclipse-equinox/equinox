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
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.zip.ZipFile;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.*;
import org.eclipse.osgi.internal.service.security.KeyStoreTrustEngine;
import org.eclipse.osgi.internal.signedcontent.SignedStorageHook.StorageHookImpl;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.osgi.signedcontent.*;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Implements signed bundle hook support for the framework
 */
public class SignedBundleHook implements ActivatorHookFactory, BundleFileWrapperFactoryHook, HookConfigurator, SignedContentFactory {
	static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());
	static final int VERIFY_CERTIFICATE = 0x01;
	static final int VERIFY_TRUST = 0x02;
	static final int VERIFY_RUNTIME = 0x04;
	static final int VERIFY_ALL = VERIFY_CERTIFICATE | VERIFY_TRUST | VERIFY_RUNTIME;
	private final static String SUPPORT_CERTIFICATE = "certificate"; //$NON-NLS-1$
	private final static String SUPPORT_TRUST = "trust"; //$NON-NLS-1$
	private final static String SUPPORT_RUNTIME = "runtime"; //$NON-NLS-1$
	private final static String SUPPORT_ALL = "all"; //$NON-NLS-1$
	private final static String SUPPORT_TRUE = "true"; //$NON-NLS-1$

	//TODO: comes from configuration!;
	private final static String CACERTS_PATH = System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security" + File.separatorChar + "cacerts"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
	private final static String CACERTS_TYPE = "JKS"; //$NON-NLS-1$
	private final static String SIGNED_BUNDLE_SUPPORT = "osgi.support.signature.verify"; //$NON-NLS-1$
	private final static String SIGNED_CONTENT_SUPPORT = "osgi.signedcontent.support"; //$NON-NLS-1$
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
		if ((supportSignedBundles & VERIFY_TRUST) != 0)
			// initialize the trust engine listener only if trust is being established with a trust engine
			trustEngineListener = new TrustEngineListener(context, this);
		// always register the trust engine
		Dictionary<String, Object> trustEngineProps = new Hashtable<>(7);
		trustEngineProps.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MIN_VALUE));
		trustEngineProps.put(SignedContentConstants.TRUST_ENGINE, SignedContentConstants.DEFAULT_TRUST_ENGINE);
		KeyStoreTrustEngine systemTrustEngine = new KeyStoreTrustEngine(CACERTS_PATH, CACERTS_TYPE, null, "System", this); //$NON-NLS-1$
		systemTrustEngineReg = context.registerService(TrustEngine.class.getName(), systemTrustEngine, trustEngineProps);
		String osgiTrustPath = context.getProperty(OSGI_KEYSTORE);
		if (osgiTrustPath != null) {
			try {
				URL url = new URL(osgiTrustPath);
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					trustEngineProps.put(SignedContentConstants.TRUST_ENGINE, OSGI_KEYSTORE);
					String path = url.getPath();
					osgiTrustEngineReg = new ArrayList<>(1);
					osgiTrustEngineReg.add(context.registerService(TrustEngine.class.getName(), new KeyStoreTrustEngine(path, CACERTS_TYPE, null, OSGI_KEYSTORE, this), trustEngineProps));
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
					osgiTrustEngineReg.add(context.registerService(TrustEngine.class.getName(), new KeyStoreTrustEngine(trustRepoPath, CACERTS_TYPE, null, OSGI_KEYSTORE, this), trustEngineProps));
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
			for (Iterator<ServiceRegistration<?>> it = osgiTrustEngineReg.iterator(); it.hasNext();)
				it.next().unregister();
			osgiTrustEngineReg = null;
		}
		if (trustEngineTracker != null) {
			trustEngineTracker.close();
			trustEngineTracker = null;
		}
	}

	@Override
	public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
		try {
			if (bundleFile != null) {
				StorageHookImpl hook = generation.getStorageHook(SignedStorageHook.class);
				SignedBundleFile signedBaseFile;
				if (base && hook != null) {
					signedBaseFile = new SignedBundleFile(bundleFile, hook.signedContent, supportSignedBundles, this);
					if (hook.signedContent == null) {
						signedBaseFile.initializeSignedContent();
						SignedContentImpl signedContent = signedBaseFile.getSignedContent();
						hook.signedContent = signedContent != null && signedContent.isSigned() ? signedContent : null;
					}
				} else
					signedBaseFile = new SignedBundleFile(bundleFile, null, supportSignedBundles, this);
				signedBaseFile.initializeSignedContent();
				SignedContentImpl signedContent = signedBaseFile.getSignedContent();
				if (signedContent != null && signedContent.isSigned()) {
					// only use the signed file if there are certs
					signedContent.setContent(signedBaseFile);
					return new BundleFileWrapper(signedBaseFile);
				}
			}
		} catch (IOException | GeneralSecurityException e) {
			log("Bad bundle file: " + bundleFile.getBaseFile(), FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void addHooks(HookRegistry hookRegistry) {
		container = hookRegistry.getContainer();
		hookRegistry.addActivatorHookFactory(this);
		String[] supportOptions = ManifestElement.getArrayFromList(hookRegistry.getConfiguration().getConfiguration(SIGNED_CONTENT_SUPPORT, hookRegistry.getConfiguration().getConfiguration(SIGNED_BUNDLE_SUPPORT)), ","); //$NON-NLS-1$
		for (String supportOption : supportOptions) {
			if (SUPPORT_CERTIFICATE.equals(supportOption)) {
				supportSignedBundles |= VERIFY_CERTIFICATE;
			} else if (SUPPORT_TRUST.equals(supportOption)) {
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_TRUST;
			} else if (SUPPORT_RUNTIME.equals(supportOption)) {
				supportSignedBundles |= VERIFY_CERTIFICATE | VERIFY_RUNTIME;
			} else if (SUPPORT_TRUE.equals(supportOption) || SUPPORT_ALL.equals(supportOption)) {
				supportSignedBundles |= VERIFY_ALL;
			}
		}
		trustEngineNameProp = hookRegistry.getConfiguration().getConfiguration(SignedContentConstants.TRUST_ENGINE);

		if ((supportSignedBundles & VERIFY_CERTIFICATE) != 0) {
			hookRegistry.addStorageHookFactory(new SignedStorageHook());
			hookRegistry.addBundleFileWrapperFactoryHook(this);
		}
	}

	@Override
	public SignedContent getSignedContent(File content) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		if (content == null)
			throw new IllegalArgumentException("null content"); //$NON-NLS-1$
		BundleFile contentBundleFile;
		if (content.isDirectory()) {
			contentBundleFile = new DirBundleFile(content, false);
		} else {
			// Make sure we have a ZipFile first, this will throw an IOException if not valid.
			// Use SecureAction because it gives better errors about the path on exceptions
			ZipFile temp = secureAction.getZipFile(content);
			temp.close();
			contentBundleFile = new ZipBundleFile(content, null, null, container.getConfiguration().getDebug());
		}
		SignedBundleFile result = new SignedBundleFile(contentBundleFile, null, VERIFY_ALL, this);
		try {
			result.initializeSignedContent();
		} catch (InvalidKeyException e) {
			throw new InvalidKeyException(NLS.bind(SignedContentMessages.Factory_SignedContent_Error, content), e);
		} catch (SignatureException e) {
			throw new SignatureException(NLS.bind(SignedContentMessages.Factory_SignedContent_Error, content), e);
		} catch (CertificateException e) {
			throw new CertificateException(NLS.bind(SignedContentMessages.Factory_SignedContent_Error, content), e);
		} catch (NoSuchAlgorithmException e) {
			throw new NoSuchAlgorithmException(NLS.bind(SignedContentMessages.Factory_SignedContent_Error, content), e);
		} catch (NoSuchProviderException e) {
			throw (NoSuchProviderException) new NoSuchProviderException(NLS.bind(SignedContentMessages.Factory_SignedContent_Error, content)).initCause(e);
		}
		return new SignedContentFile(result.getSignedContent());
	}

	@Override
	public SignedContent getSignedContent(Bundle bundle) throws IOException, InvalidKeyException, SignatureException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, IllegalArgumentException {
		final Generation generation = (Generation) ((EquinoxBundle) bundle).getModule().getCurrentRevision().getRevisionInfo();
		StorageHookImpl hook = generation.getStorageHook(SignedStorageHook.class);
		SignedContent result = hook != null ? hook.signedContent : null;
		if (result != null)
			return result; // just reuse the signed content the storage hook
		// must create a new signed content using the raw file
		if (System.getSecurityManager() == null)
			return getSignedContent(generation.getBundleFile().getBaseFile());
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<SignedContent>() {
		    @Override
				public SignedContent run() throws Exception {
					return getSignedContent(generation.getBundleFile().getBaseFile());
				}
			});
		} catch (PrivilegedActionException e) {
			if (e.getException() instanceof IOException)
				throw (IOException) e.getException();
			if (e.getException() instanceof InvalidKeyException)
				throw (InvalidKeyException) e.getException();
			if (e.getException() instanceof SignatureException)
				throw (SignatureException) e.getException();
			if (e.getException() instanceof CertificateException)
				throw (CertificateException) e.getException();
			if (e.getException() instanceof NoSuchAlgorithmException)
				throw (NoSuchAlgorithmException) e.getException();
			if (e.getException() instanceof NoSuchProviderException)
				throw (NoSuchProviderException) e.getException();
			throw new RuntimeException("Unknown error.", e.getException()); //$NON-NLS-1$
		}
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
					filter = context.createFilter("(&(" + Constants.OBJECTCLASS + "=" + TrustEngine.class.getName() + ")(" + SignedContentConstants.TRUST_ENGINE + "=" + trustEngineNameProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
				} catch (InvalidSyntaxException e) {
					log("Invalid trust engine filter", FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
				}
			if (filter != null) {
				trustEngineTracker = new ServiceTracker<>(context, filter, new TrustEngineCustomizer());
			} else
				trustEngineTracker = new ServiceTracker<>(context, TrustEngine.class.getName(), new TrustEngineCustomizer());
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

	void determineTrust(SignedContentImpl trustedContent, int supportFlags) {
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
				((SignerInfoImpl) signer).setTrustAnchor(findTrustAnchor(signerCerts, engines, supportFlags));
				// if signer has a tsa check trust of tsa certs
				SignerInfo tsaSignerInfo = trustedContent.getTSASignerInfo(signer);
				if (tsaSignerInfo != null) {
					Certificate[] tsaCerts = tsaSignerInfo.getCertificateChain();
					((SignerInfoImpl) tsaSignerInfo).setTrustAnchor(findTrustAnchor(tsaCerts, engines, supportFlags));
				}
			}
		}
	}

	private Certificate findTrustAnchor(Certificate[] certs, TrustEngine[] engines, int supportFlags) {
		if ((supportFlags & SignedBundleHook.VERIFY_TRUST) == 0)
			// we are not searching the engines; in this case we just assume the root cert is trusted
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
