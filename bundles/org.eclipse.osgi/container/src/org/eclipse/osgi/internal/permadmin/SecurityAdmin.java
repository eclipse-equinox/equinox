/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
package org.eclipse.osgi.internal.permadmin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlContext;
import java.security.AllPermission;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.PermissionData;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

public final class SecurityAdmin implements PermissionAdmin, ConditionalPermissionAdmin {
	private static final PermissionCollection DEFAULT_DEFAULT;
	static {
		AllPermission allPerm = new AllPermission();
		DEFAULT_DEFAULT = allPerm.newPermissionCollection();
		if (DEFAULT_DEFAULT != null)
			DEFAULT_DEFAULT.add(allPerm);
	}

	// Base implied permissions for all bundles
	private static final String OSGI_BASE_IMPLIED_PERMISSIONS = "implied.permissions"; //$NON-NLS-1$

	private static final String ADMIN_IMPLIED_ACTIONS = AdminPermission.RESOURCE + ',' + AdminPermission.METADATA + ','
			+ AdminPermission.CLASS + ',' + AdminPermission.CONTEXT;
	private static final PermissionInfo[] EMPTY_PERM_INFO = new PermissionInfo[0];
	/* @GuardedBy(lock) */
	private final PermissionAdminTable permAdminTable = new PermissionAdminTable();
	/* @GuardedBy(lock) */
	private SecurityTable condAdminTable;
	/* @GuardedBy(lock) */
	private PermissionInfoCollection permAdminDefaults;
	/* @GuardedBy(lock) */
	private long timeStamp = 0;
	/* @GuardedBy(lock) */
	private long nextID = System.currentTimeMillis();
	/* @GuardedBy(lock) */
	private final PermissionData permissionStorage;
	private final Object lock = new Object();
	// private final EquinoxContainer container;
	private final PermissionInfo[] impliedPermissionInfos;
	private final EquinoxSecurityManager supportedSecurityManager;

	public SecurityAdmin(EquinoxSecurityManager supportedSecurityManager, PermissionData permissionStorage) {
		this.supportedSecurityManager = supportedSecurityManager;
		this.permissionStorage = permissionStorage;
		this.impliedPermissionInfos = SecurityAdmin
				.getPermissionInfos(getClass().getResource(OSGI_BASE_IMPLIED_PERMISSIONS));
		String[] encodedDefaultInfos = permissionStorage.getPermissionData(null);
		PermissionInfo[] defaultInfos = getPermissionInfos(encodedDefaultInfos);
		if (defaultInfos != null)
			permAdminDefaults = new PermissionInfoCollection(defaultInfos);
		String[] locations = permissionStorage.getLocations();
		if (locations != null) {
			for (String location : locations) {
				String[] encodedLocationInfos = permissionStorage.getPermissionData(location);
				if (encodedLocationInfos != null) {
					PermissionInfo[] locationInfos = getPermissionInfos(encodedLocationInfos);
					permAdminTable.setPermissions(location, locationInfos);
				}
			}
		}
		String[] encodedCondPermInfos = permissionStorage.getConditionalPermissionInfos();
		if (encodedCondPermInfos == null)
			condAdminTable = new SecurityTable(this, new SecurityRow[0]);
		else {
			SecurityRow[] rows = new SecurityRow[encodedCondPermInfos.length];
			try {
				for (int i = 0; i < rows.length; i++)
					rows[i] = SecurityRow.createSecurityRow(this, encodedCondPermInfos[i]);
			} catch (IllegalArgumentException e) {
				// TODO should log
				// bad format persisted in storage; start clean
				rows = new SecurityRow[0];
			}
			condAdminTable = new SecurityTable(this, rows);
		}
	}

	private static PermissionInfo[] getPermissionInfos(String[] encodedInfos) {
		if (encodedInfos == null)
			return null;
		PermissionInfo[] results = new PermissionInfo[encodedInfos.length];
		for (int i = 0; i < results.length; i++)
			results[i] = new PermissionInfo(encodedInfos[i]);
		return results;
	}

	boolean checkPermission(Permission permission, BundlePermissions bundlePermissions) {
		// check permissions by location
		PermissionInfoCollection locationCollection;
		SecurityTable curCondAdminTable;
		PermissionInfoCollection curPermAdminDefaults;
		// save off the current state of the world while holding the lock
		synchronized (lock) {
			// get location the hard way to avoid permission check
			Bundle bundle = bundlePermissions.getBundle();
			locationCollection = bundle instanceof EquinoxBundle
					? permAdminTable.getCollection(((EquinoxBundle) bundle).getModule().getLocation())
					: null;
			curCondAdminTable = condAdminTable;
			curPermAdminDefaults = permAdminDefaults;
		}
		if (locationCollection != null)
			return locationCollection.implies(bundlePermissions, permission);
		// if conditional admin table is empty the fall back to defaults
		if (curCondAdminTable.isEmpty())
			return curPermAdminDefaults != null ? curPermAdminDefaults.implies(permission)
					: DEFAULT_DEFAULT.implies(permission);
		// check the condition table
		int result = curCondAdminTable.evaluate(bundlePermissions, permission);
		if ((result & SecurityTable.GRANTED) != 0)
			return true;
		if ((result & SecurityTable.DENIED) != 0)
			return false;
		if ((result & SecurityTable.POSTPONED) != 0)
			return true;
		return false;
	}

	@Override
	public PermissionInfo[] getDefaultPermissions() {
		synchronized (lock) {
			if (permAdminDefaults == null)
				return null;
			return permAdminDefaults.getPermissionInfos();
		}
	}

	@Override
	public String[] getLocations() {
		synchronized (lock) {
			String[] results = permAdminTable.getLocations();
			return results.length == 0 ? null : results;
		}
	}

	@Override
	public PermissionInfo[] getPermissions(String location) {
		synchronized (lock) {
			return permAdminTable.getPermissions(location);
		}
	}

	@Override
	public void setDefaultPermissions(PermissionInfo[] permissions) {
		checkAllPermission();
		synchronized (lock) {
			if (permissions == null)
				permAdminDefaults = null;
			else
				permAdminDefaults = new PermissionInfoCollection(permissions);
			permissionStorage.setPermissionData(null, getEncodedPermissionInfos(permissions));
		}
	}

	private static void checkAllPermission() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AllPermission());
	}

	private static String[] getEncodedPermissionInfos(PermissionInfo[] permissions) {
		if (permissions == null)
			return null;
		String[] encoded = new String[permissions.length];
		for (int i = 0; i < encoded.length; i++)
			encoded[i] = permissions[i].getEncoded();
		return encoded;
	}

	@Override
	public void setPermissions(String location, PermissionInfo[] permissions) {
		checkAllPermission();
		synchronized (lock) {
			permAdminTable.setPermissions(location, permissions);
			permissionStorage.setPermissionData(location, getEncodedPermissionInfos(permissions));
		}
	}

	void delete(SecurityRow securityRow, boolean firstTry) {
		ConditionalPermissionUpdate update = newConditionalPermissionUpdate();
		List<ConditionalPermissionInfo> rows = update.getConditionalPermissionInfos();
		for (Iterator<ConditionalPermissionInfo> iRows = rows.iterator(); iRows.hasNext();) {
			ConditionalPermissionInfo info = iRows.next();
			if (securityRow.getName().equals(info.getName())) {
				iRows.remove();
				synchronized (lock) {
					if (!update.commit()) {
						if (firstTry)
							// try again
							delete(securityRow, false);
					}
				}
				break;
			}
		}
	}

	/**
	 * @deprecated
	 */
	@Override
	public ConditionalPermissionInfo addConditionalPermissionInfo(ConditionInfo[] conds, PermissionInfo[] perms) {
		return setConditionalPermissionInfo(null, conds, perms, true);
	}

	@Override
	public ConditionalPermissionInfo newConditionalPermissionInfo(String name, ConditionInfo[] conditions,
			PermissionInfo[] permissions, String decision) {
		return new SecurityRowSnapShot(name, conditions, permissions, decision);
	}

	@Override
	public ConditionalPermissionInfo newConditionalPermissionInfo(String encoded) {
		return SecurityRow.createSecurityRowSnapShot(encoded);
	}

	@Override
	public ConditionalPermissionUpdate newConditionalPermissionUpdate() {
		synchronized (lock) {
			return new SecurityTableUpdate(this, condAdminTable.getRows(), timeStamp);
		}
	}

	@Override
	public AccessControlContext getAccessControlContext(String[] signers) {
		return new AccessControlContext(
				new ProtectionDomain[] { createProtectionDomain(createMockBundle(signers), this) });
	}

	/**
	 * @deprecated
	 */
	@Override
	public ConditionalPermissionInfo getConditionalPermissionInfo(String name) {
		synchronized (lock) {
			return condAdminTable.getRow(name);
		}
	}

	/**
	 * @deprecated
	 */
	@Override
	public Enumeration<ConditionalPermissionInfo> getConditionalPermissionInfos() {
		// could implement our own Enumeration, but we don't care about performance
		// here. Just do something simple:
		synchronized (lock) {
			SecurityRow[] rows = condAdminTable.getRows();
			List<ConditionalPermissionInfo> vRows = new ArrayList<>(rows.length);
			Collections.addAll(vRows, rows);
			return Collections.enumeration(vRows);
		}
	}

	/**
	 * @deprecated
	 */
	@Override
	public ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conds,
			PermissionInfo[] perms) {
		return setConditionalPermissionInfo(name, conds, perms, true);
	}

	private ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conds,
			PermissionInfo[] perms, boolean firstTry) {
		ConditionalPermissionUpdate update = newConditionalPermissionUpdate();
		List<ConditionalPermissionInfo> rows = update.getConditionalPermissionInfos();
		ConditionalPermissionInfo newInfo = newConditionalPermissionInfo(name, conds, perms,
				ConditionalPermissionInfo.ALLOW);
		int index = -1;
		if (name != null) {
			for (int i = 0; i < rows.size() && index < 0; i++) {
				ConditionalPermissionInfo info = rows.get(i);
				if (name.equals(info.getName())) {
					index = i;
				}
			}
		}
		if (index < 0) {
			// must always add to the beginning (bug 303930)
			rows.add(0, newInfo);
			index = 0;
		} else {
			rows.set(index, newInfo);
		}
		synchronized (lock) {
			if (!update.commit()) {
				if (firstTry)
					// try again
					setConditionalPermissionInfo(name, conds, perms, false);
			}
			return condAdminTable.getRow(index);
		}
	}

	boolean commit(List<ConditionalPermissionInfo> rows, long updateStamp) {
		checkAllPermission();
		synchronized (lock) {
			if (updateStamp != timeStamp)
				return false;
			SecurityRow[] newRows = new SecurityRow[rows.size()];
			Collection<String> names = new ArrayList<>();
			for (int i = 0; i < newRows.length; i++) {
				Object rowObj = rows.get(i);
				if (!(rowObj instanceof ConditionalPermissionInfo))
					throw new IllegalStateException(
							"Invalid type \"" + rowObj.getClass().getName() + "\" at row: " + i); //$NON-NLS-1$//$NON-NLS-2$
				ConditionalPermissionInfo infoBaseRow = (ConditionalPermissionInfo) rowObj;
				String name = infoBaseRow.getName();
				if (name == null)
					name = generateName();
				if (names.contains(name))
					throw new IllegalStateException("Duplicate name \"" + name + "\" at row: " + i); //$NON-NLS-1$//$NON-NLS-2$
				names.add(name);
				newRows[i] = new SecurityRow(this, name, infoBaseRow.getConditionInfos(),
						infoBaseRow.getPermissionInfos(), infoBaseRow.getAccessDecision());
			}
			condAdminTable = new SecurityTable(this, newRows);
			permissionStorage.saveConditionalPermissionInfos(condAdminTable.getEncodedRows());
			timeStamp += 1;
			return true;
		}
	}

	/* GuardedBy(lock) */
	private String generateName() {
		return "generated_" + Long.toString(nextID++); //$NON-NLS-1$ ;
	}

	public ProtectionDomain createProtectionDomain(Bundle bundle) {
		return createProtectionDomain(bundle, this);
	}

	private ProtectionDomain createProtectionDomain(Bundle bundle, SecurityAdmin sa) {
		PermissionInfoCollection impliedPermissions = getImpliedPermission(bundle);
		URL permEntry = null;
		try {
			permEntry = bundle.getEntry("OSGI-INF/permissions.perm"); //$NON-NLS-1$
		} catch (IllegalStateException e) {
			// bundle may be uninstalled
		}
		PermissionInfo[] restrictedInfos = getFileRelativeInfos(SecurityAdmin.getPermissionInfos(permEntry), bundle);
		PermissionInfoCollection restrictedPermissions = restrictedInfos == null ? null
				: new PermissionInfoCollection(restrictedInfos);
		BundlePermissions bundlePermissions = new BundlePermissions(bundle, sa, impliedPermissions,
				restrictedPermissions);
		return new ProtectionDomain(null, bundlePermissions);
	}

	private PermissionInfoCollection getImpliedPermission(Bundle bundle) {
		if (impliedPermissionInfos == null)
			return null;
		// create the implied AdminPermission actions for this bundle
		PermissionInfo impliedAdminPermission = new PermissionInfo(AdminPermission.class.getName(),
				"(id=" + bundle.getBundleId() + ")", ADMIN_IMPLIED_ACTIONS); //$NON-NLS-1$ //$NON-NLS-2$
		PermissionInfo[] bundleImpliedInfos = new PermissionInfo[impliedPermissionInfos.length + 1];
		System.arraycopy(impliedPermissionInfos, 0, bundleImpliedInfos, 0, impliedPermissionInfos.length);
		bundleImpliedInfos[impliedPermissionInfos.length] = impliedAdminPermission;
		return new PermissionInfoCollection(getFileRelativeInfos(bundleImpliedInfos, bundle));
	}

	private PermissionInfo[] getFileRelativeInfos(PermissionInfo[] permissionInfos, Bundle bundle) {
		if (permissionInfos == null)
			return permissionInfos;
		PermissionInfo[] results = new PermissionInfo[permissionInfos.length];
		for (int i = 0; i < permissionInfos.length; i++) {
			results[i] = permissionInfos[i];
			if (PermissionInfoCollection.FILE_PERMISSION_NAME.equals(permissionInfos[i].getType())) {
				if (!PermissionInfoCollection.ALL_FILES.equals(permissionInfos[i].getName())) {
					File file = new File(permissionInfos[i].getName());
					if (!file.isAbsolute()) { // relative name
						try {
							File target = bundle.getDataFile(permissionInfos[i].getName());
							if (target != null)
								results[i] = new PermissionInfo(permissionInfos[i].getType(), target.getPath(),
										permissionInfos[i].getActions());
						} catch (IllegalStateException e) {
							// can happen if the bundle has been uninstalled;
							// we just keep the original permission in this case.
						}
					}
				}
			}
		}
		return results;
	}

	public void clearCaches() {
		PermissionInfoCollection[] permAdminCollections;
		SecurityRow[] condAdminRows;
		synchronized (lock) {
			permAdminCollections = permAdminTable.getCollections();
			condAdminRows = condAdminTable.getRows();
		}
		for (PermissionInfoCollection permAdminCollection : permAdminCollections) {
			permAdminCollection.clearPermissionCache();
		}
		for (SecurityRow condAdminRow : condAdminRows) {
			condAdminRow.clearCaches();
		}
		condAdminTable.clearEvaluationCache();
	}

	EquinoxSecurityManager getSupportedSecurityManager() {
		return supportedSecurityManager != null ? supportedSecurityManager : getSupportedSystemSecurityManager();
	}

	static private EquinoxSecurityManager getSupportedSystemSecurityManager() {
		try {
			EquinoxSecurityManager equinoxManager = (EquinoxSecurityManager) System.getSecurityManager();
			return equinoxManager != null && equinoxManager.inCheckPermission() ? equinoxManager : null;
		} catch (ClassCastException e) {
			return null;
		}
	}

	private static PermissionInfo[] getPermissionInfos(URL resource) {
		if (resource == null)
			return null;
		PermissionInfo[] info = EMPTY_PERM_INFO;
		List<PermissionInfo> permissions = new ArrayList<>();
		try (DataInputStream in = new DataInputStream(resource.openStream());
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

			while (true) {
				String line = reader.readLine();
				if (line == null) /* EOF */
					break;
				line = line.trim();
				if ((line.length() == 0) || line.startsWith("#") || line.startsWith("//")) /* comments *///$NON-NLS-1$ //$NON-NLS-2$
					continue;

				try {
					permissions.add(new PermissionInfo(line));
				} catch (IllegalArgumentException iae) {
					/* incorrectly encoded permission */
					// TODO used to publish an event here
				}
			}
			int size = permissions.size();
			if (size > 0)
				info = permissions.toArray(new PermissionInfo[size]);
		} catch (IOException e) {
			// do nothing
		}
		return info;
	}

	private static Bundle createMockBundle(String[] signers) {
		Map<X509Certificate, List<X509Certificate>> signersMap = new HashMap<>();
		for (String signer : signers) {
			List<String> chain = parseDNchain(signer);
			List<X509Certificate> signersList = new ArrayList<>();
			Principal subject = null, issuer = null;
			X509Certificate first = null;
			for (Iterator<String> iChain = chain.iterator(); iChain.hasNext();) {
				subject = issuer == null ? new MockPrincipal(iChain.next()) : issuer;
				issuer = iChain.hasNext() ? new MockPrincipal(iChain.next()) : subject;
				X509Certificate cert = new MockX509Certificate(subject, issuer);
				if (first == null)
					first = cert;
				signersList.add(cert);
			}
			if (subject != issuer)
				signersList.add(new MockX509Certificate(issuer, issuer));
			signersMap.put(first, signersList);
		}
		return new MockBundle(signersMap);
	}

	static class MockBundle implements Bundle {
		private final Map<X509Certificate, List<X509Certificate>> signers;

		MockBundle(Map<X509Certificate, List<X509Certificate>> signers) {
			this.signers = signers;
		}

		@Override
		public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
			return null;
		}

		@Override
		public BundleContext getBundleContext() {
			return null;
		}

		@Override
		public long getBundleId() {
			return -1;
		}

		@Override
		public URL getEntry(String path) {
			return null;
		}

		@Override
		public Enumeration<String> getEntryPaths(String path) {
			return null;
		}

		@Override
		public Dictionary<String, String> getHeaders() {
			return new Hashtable<>();
		}

		@Override
		public Dictionary<String, String> getHeaders(String locale) {
			return getHeaders();
		}

		@Override
		public long getLastModified() {
			return 0;
		}

		@Override
		public String getLocation() {
			return ""; //$NON-NLS-1$
		}

		@Override
		public ServiceReference<?>[] getRegisteredServices() {
			return null;
		}

		@Override
		public URL getResource(String name) {
			return null;
		}

		/**
		 * @throws IOException
		 */
		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return null;
		}

		@Override
		public ServiceReference<?>[] getServicesInUse() {
			return null;
		}

		@Override
		public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
			return new HashMap<>(signers);
		}

		@Override
		public int getState() {
			return Bundle.UNINSTALLED;
		}

		@Override
		public String getSymbolicName() {
			return null;
		}

		@Override
		public Version getVersion() {
			return Version.emptyVersion;
		}

		@Override
		public boolean hasPermission(Object permission) {
			return false;
		}

		/**
		 * @throws ClassNotFoundException
		 */
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void start(int options) throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void start() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void stop(int options) throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void stop() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void uninstall() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void update() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException
		 */
		@Override
		public void update(InputStream in) throws BundleException {
			throw new IllegalStateException();
		}

		@Override
		public int compareTo(Bundle o) {
			return 0;
		}

		@Override
		public <A> A adapt(Class<A> type) {
			throw new IllegalStateException();
		}

		@Override
		public File getDataFile(String filename) {
			return null;
		}
	}

	private static class MockX509Certificate extends X509Certificate {
		private final Principal subject;
		private final Principal issuer;

		MockX509Certificate(Principal subject, Principal issuer) {
			this.subject = subject;
			this.issuer = issuer;
		}

		@Override
		public Principal getSubjectDN() {
			return subject;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof MockX509Certificate)
				return subject.equals(((MockX509Certificate) obj).subject)
						&& issuer.equals(((MockX509Certificate) obj).issuer);
			return false;
		}

		@Override
		public int hashCode() {
			return subject.hashCode() + issuer.hashCode();
		}

		@Override
		public String toString() {
			return subject.toString();
		}

		/**
		 * @throws CertificateExpiredException
		 * @throws java.security.cert.CertificateNotYetValidException
		 */
		@Override
		public void checkValidity()
				throws CertificateExpiredException, java.security.cert.CertificateNotYetValidException {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws java.security.cert.CertificateExpiredException
		 * @throws java.security.cert.CertificateNotYetValidException
		 */
		@Override
		public void checkValidity(Date var0) throws java.security.cert.CertificateExpiredException,
				java.security.cert.CertificateNotYetValidException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getBasicConstraints() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Principal getIssuerDN() {
			return issuer;
		}

		@Override
		public boolean[] getIssuerUniqueID() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean[] getKeyUsage() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getNotAfter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getNotBefore() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BigInteger getSerialNumber() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSigAlgName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSigAlgOID() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getSigAlgParams() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getSignature() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean[] getSubjectUniqueID() {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws CertificateEncodingException
		 */
		@Override
		public byte[] getTBSCertificate() throws CertificateEncodingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getVersion() {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws CertificateEncodingException
		 */
		@Override
		public byte[] getEncoded() throws CertificateEncodingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public PublicKey getPublicKey() {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws java.security.InvalidKeyException
		 * @throws java.security.NoSuchAlgorithmException
		 * @throws java.security.NoSuchProviderException
		 * @throws java.security.SignatureException
		 * @throws java.security.cert.CertificateException
		 */
		@Override
		public void verify(PublicKey var0) throws java.security.InvalidKeyException,
				java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException,
				java.security.SignatureException, java.security.cert.CertificateException {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws InvalidKeyException
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchProviderException
		 * @throws SignatureException
		 * @throws CertificateException
		 */
		@Override
		public void verify(PublicKey var0, String var1) throws InvalidKeyException, NoSuchAlgorithmException,
				NoSuchProviderException, SignatureException, CertificateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getCriticalExtensionOIDs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getExtensionValue(String var0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getNonCriticalExtensionOIDs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasUnsupportedCriticalExtension() {
			throw new UnsupportedOperationException();
		}
	}

	private static class MockPrincipal implements Principal {
		private final String name;

		MockPrincipal(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof MockPrincipal) {
				return name.equals(((MockPrincipal) obj).name);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	private static List<String> parseDNchain(String dnChain) {
		if (dnChain == null) {
			throw new IllegalArgumentException("The DN chain must not be null."); //$NON-NLS-1$
		}
		List<String> parsed = new ArrayList<>();
		int startIndex = 0;
		startIndex = skipSpaces(dnChain, startIndex);
		while (startIndex < dnChain.length()) {
			int endIndex = startIndex;
			boolean inQuote = false;
			out: while (endIndex < dnChain.length()) {
				char c = dnChain.charAt(endIndex);
				switch (c) {
				case '"':
					inQuote = !inQuote;
					break;
				case '\\':
					endIndex++; // skip the escaped char
					break;
				case ';':
					if (!inQuote)
						break out;
				}
				endIndex++;
			}
			if (endIndex > dnChain.length()) {
				throw new IllegalArgumentException("unterminated escape"); //$NON-NLS-1$
			}
			parsed.add(dnChain.substring(startIndex, endIndex));
			startIndex = endIndex + 1;
			startIndex = skipSpaces(dnChain, startIndex);
		}
		return parsed;
	}

	private static int skipSpaces(String dnChain, int startIndex) {
		while (startIndex < dnChain.length() && dnChain.charAt(startIndex) == ' ') {
			startIndex++;
		}
		return startIndex;
	}
}
