/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.PermissionStorage;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.osgi.framework.*;
import org.osgi.service.condpermadmin.*;
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

	private static final String ADMIN_IMPLIED_ACTIONS = AdminPermission.RESOURCE + ',' + AdminPermission.METADATA + ',' + AdminPermission.CLASS + ',' + AdminPermission.CONTEXT;
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
	private final PermissionStorage permissionStorage;
	private final Object lock = new Object();
	private final Framework framework;
	private final PermissionInfo[] impliedPermissionInfos;
	private final EquinoxSecurityManager supportedSecurityManager;

	private SecurityAdmin(EquinoxSecurityManager supportedSecurityManager, Framework framework, PermissionInfo[] impliedPermissionInfos, PermissionInfoCollection permAdminDefaults) {
		this.supportedSecurityManager = supportedSecurityManager;
		this.framework = framework;
		this.impliedPermissionInfos = impliedPermissionInfos;
		this.permAdminDefaults = permAdminDefaults;
		this.permissionStorage = null;
	}

	public SecurityAdmin(EquinoxSecurityManager supportedSecurityManager, Framework framework, PermissionStorage permissionStorage) throws IOException {
		this.supportedSecurityManager = supportedSecurityManager;
		this.framework = framework;
		this.permissionStorage = new SecurePermissionStorage(permissionStorage);
		this.impliedPermissionInfos = SecurityAdmin.getPermissionInfos(getClass().getResource(Constants.OSGI_BASE_IMPLIED_PERMISSIONS), framework);
		String[] encodedDefaultInfos = permissionStorage.getPermissionData(null);
		PermissionInfo[] defaultInfos = getPermissionInfos(encodedDefaultInfos);
		if (defaultInfos != null)
			permAdminDefaults = new PermissionInfoCollection(defaultInfos);
		String[] locations = permissionStorage.getLocations();
		if (locations != null) {
			for (int i = 0; i < locations.length; i++) {
				String[] encodedLocationInfos = permissionStorage.getPermissionData(locations[i]);
				if (encodedLocationInfos != null) {
					PermissionInfo[] locationInfos = getPermissionInfos(encodedLocationInfos);
					permAdminTable.setPermissions(locations[i], locationInfos);
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
			locationCollection = bundle instanceof AbstractBundle ? permAdminTable.getCollection(((AbstractBundle) bundle).getBundleData().getLocation()) : null;
			curCondAdminTable = condAdminTable;
			curPermAdminDefaults = permAdminDefaults;
		}
		if (locationCollection != null)
			return locationCollection.implies(permission);
		// if conditional admin table is empty the fall back to defaults
		if (curCondAdminTable.isEmpty())
			return curPermAdminDefaults != null ? curPermAdminDefaults.implies(permission) : DEFAULT_DEFAULT.implies(permission);
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

	public PermissionInfo[] getDefaultPermissions() {
		synchronized (lock) {
			if (permAdminDefaults == null)
				return null;
			return permAdminDefaults.getPermissionInfos();
		}
	}

	public String[] getLocations() {
		synchronized (lock) {
			String[] results = permAdminTable.getLocations();
			return results.length == 0 ? null : results;
		}
	}

	public PermissionInfo[] getPermissions(String location) {
		synchronized (lock) {
			return permAdminTable.getPermissions(location);
		}
	}

	public void setDefaultPermissions(PermissionInfo[] permissions) {
		checkAllPermission();
		synchronized (lock) {
			if (permissions == null)
				permAdminDefaults = null;
			else
				permAdminDefaults = new PermissionInfoCollection(permissions);
			try {
				permissionStorage.setPermissionData(null, getEncodedPermissionInfos(permissions));
			} catch (IOException e) {
				// log
				e.printStackTrace();
			}
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

	public void setPermissions(String location, PermissionInfo[] permissions) {
		checkAllPermission();
		synchronized (lock) {
			permAdminTable.setPermissions(location, permissions);
			try {
				permissionStorage.setPermissionData(location, getEncodedPermissionInfos(permissions));
			} catch (IOException e) {
				// TODO log
				e.printStackTrace();
			}
		}
	}

	void delete(SecurityRow securityRow, boolean firstTry) {
		ConditionalPermissionUpdate update = newConditionalPermissionUpdate();
		@SuppressWarnings("unchecked")
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
	public ConditionalPermissionInfo addConditionalPermissionInfo(ConditionInfo[] conds, PermissionInfo[] perms) {
		return setConditionalPermissionInfo(null, conds, perms, true);
	}

	public ConditionalPermissionInfo newConditionalPermissionInfo(String name, ConditionInfo[] conditions, PermissionInfo[] permissions, String decision) {
		return new SecurityRowSnapShot(name, conditions, permissions, decision);
	}

	public ConditionalPermissionInfo newConditionalPermissionInfo(String encoded) {
		return SecurityRow.createSecurityRowSnapShot(encoded);
	}

	public ConditionalPermissionUpdate newConditionalPermissionUpdate() {
		synchronized (lock) {
			return new SecurityTableUpdate(this, condAdminTable.getRows(), timeStamp);
		}
	}

	public AccessControlContext getAccessControlContext(String[] signers) {
		SecurityAdmin snapShot = getSnapShot();
		return new AccessControlContext(new ProtectionDomain[] {createProtectionDomain(createMockBundle(signers), snapShot)});
	}

	/**
	 * @deprecated
	 */
	public ConditionalPermissionInfo getConditionalPermissionInfo(String name) {
		synchronized (lock) {
			return condAdminTable.getRow(name);
		}
	}

	/**
	 * @deprecated
	 */
	public Enumeration<ConditionalPermissionInfo> getConditionalPermissionInfos() {
		// could implement our own Enumeration, but we don't care about performance here.  Just do something simple:
		synchronized (lock) {
			SecurityRow[] rows = condAdminTable.getRows();
			List<ConditionalPermissionInfo> vRows = new ArrayList<ConditionalPermissionInfo>(rows.length);
			for (int i = 0; i < rows.length; i++)
				vRows.add(rows[i]);
			return Collections.enumeration(vRows);
		}
	}

	/**
	 * @deprecated
	 */
	public ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conds, PermissionInfo[] perms) {
		return setConditionalPermissionInfo(name, conds, perms, true);
	}

	private SecurityAdmin getSnapShot() {
		SecurityAdmin sa;
		synchronized (lock) {
			sa = new SecurityAdmin(supportedSecurityManager, framework, impliedPermissionInfos, permAdminDefaults);
			SecurityRow[] rows = condAdminTable.getRows();
			SecurityRow[] rowsSnapShot = new SecurityRow[rows.length];
			for (int i = 0; i < rows.length; i++)
				rowsSnapShot[i] = new SecurityRow(sa, rows[i].getName(), rows[i].getConditionInfos(), rows[i].getPermissionInfos(), rows[i].getAccessDecision());
			sa.condAdminTable = new SecurityTable(sa, rowsSnapShot);
		}
		return sa;
	}

	private ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conds, PermissionInfo[] perms, boolean firstTry) {
		ConditionalPermissionUpdate update = newConditionalPermissionUpdate();
		@SuppressWarnings("unchecked")
		List<ConditionalPermissionInfo> rows = update.getConditionalPermissionInfos();
		ConditionalPermissionInfo newInfo = newConditionalPermissionInfo(name, conds, perms, ConditionalPermissionInfo.ALLOW);
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
			Collection<String> names = new ArrayList<String>();
			for (int i = 0; i < newRows.length; i++) {
				Object rowObj = rows.get(i);
				if (!(rowObj instanceof ConditionalPermissionInfo))
					throw new IllegalStateException("Invalid type \"" + rowObj.getClass().getName() + "\" at row: " + i); //$NON-NLS-1$//$NON-NLS-2$
				ConditionalPermissionInfo infoBaseRow = (ConditionalPermissionInfo) rowObj;
				String name = infoBaseRow.getName();
				if (name == null)
					name = generateName();
				if (names.contains(name))
					throw new IllegalStateException("Duplicate name \"" + name + "\" at row: " + i); //$NON-NLS-1$//$NON-NLS-2$
				names.add(name);
				newRows[i] = new SecurityRow(this, name, infoBaseRow.getConditionInfos(), infoBaseRow.getPermissionInfos(), infoBaseRow.getAccessDecision());
			}
			condAdminTable = new SecurityTable(this, newRows);
			try {
				permissionStorage.saveConditionalPermissionInfos(condAdminTable.getEncodedRows());
			} catch (IOException e) {
				// TODO log
				e.printStackTrace();
			}
			timeStamp += 1;
			return true;
		}
	}

	/* GuardedBy(lock) */
	private String generateName() {
		return "generated_" + Long.toString(nextID++); //$NON-NLS-1$;
	}

	public BundleProtectionDomain createProtectionDomain(Bundle bundle) {
		return createProtectionDomain(bundle, this);
	}

	private BundleProtectionDomain createProtectionDomain(Bundle bundle, SecurityAdmin sa) {
		PermissionInfoCollection impliedPermissions = getImpliedPermission(bundle);
		PermissionInfo[] restrictedInfos = getFileRelativeInfos(SecurityAdmin.getPermissionInfos(bundle.getEntry("OSGI-INF/permissions.perm"), framework), bundle); //$NON-NLS-1$
		PermissionInfoCollection restrictedPermissions = restrictedInfos == null ? null : new PermissionInfoCollection(restrictedInfos);
		BundlePermissions bundlePermissions = new BundlePermissions(bundle, sa, impliedPermissions, restrictedPermissions);
		return new BundleProtectionDomain(bundlePermissions, null, bundle);
	}

	private PermissionInfoCollection getImpliedPermission(Bundle bundle) {
		if (impliedPermissionInfos == null)
			return null;
		// create the implied AdminPermission actions for this bundle
		PermissionInfo impliedAdminPermission = new PermissionInfo(AdminPermission.class.getName(), "(id=" + bundle.getBundleId() + ")", ADMIN_IMPLIED_ACTIONS); //$NON-NLS-1$ //$NON-NLS-2$
		PermissionInfo[] bundleImpliedInfos = new PermissionInfo[impliedPermissionInfos.length + 1];
		System.arraycopy(impliedPermissionInfos, 0, bundleImpliedInfos, 0, impliedPermissionInfos.length);
		bundleImpliedInfos[impliedPermissionInfos.length] = impliedAdminPermission;
		return new PermissionInfoCollection(getFileRelativeInfos(bundleImpliedInfos, bundle));
	}

	private PermissionInfo[] getFileRelativeInfos(PermissionInfo[] permissionInfos, Bundle bundle) {
		if (permissionInfos == null || !(bundle instanceof AbstractBundle))
			return permissionInfos;
		PermissionInfo[] results = new PermissionInfo[permissionInfos.length];
		for (int i = 0; i < permissionInfos.length; i++) {
			results[i] = permissionInfos[i];
			if ("java.io.FilePermission".equals(permissionInfos[i].getType())) { //$NON-NLS-1$
				if (!"<<ALL FILES>>".equals(permissionInfos[i].getName())) { //$NON-NLS-1$
					File file = new File(permissionInfos[i].getName());
					if (!file.isAbsolute()) { // relative name
						File target = ((AbstractBundle) bundle).getBundleData().getDataFile(permissionInfos[i].getName());
						if (target != null)
							results[i] = new PermissionInfo(permissionInfos[i].getType(), target.getPath(), permissionInfos[i].getActions());
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
		for (int i = 0; i < permAdminCollections.length; i++)
			permAdminCollections[i].clearPermissionCache();
		for (int i = 0; i < condAdminRows.length; i++)
			condAdminRows[i].clearCaches();
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

	private static PermissionInfo[] getPermissionInfos(URL resource, Framework framework) {
		if (resource == null)
			return null;
		PermissionInfo[] info = EMPTY_PERM_INFO;
		DataInputStream in = null;
		try {
			in = new DataInputStream(resource.openStream());
			List<PermissionInfo> permissions = new ArrayList<PermissionInfo>();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(in, "UTF8")); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				reader = new BufferedReader(new InputStreamReader(in));
			}

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
					if (framework != null)
						framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.getBundle(0), iae);
				}
			}
			int size = permissions.size();
			if (size > 0)
				info = permissions.toArray(new PermissionInfo[size]);
		} catch (IOException e) {
			// do nothing
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException ee) {
				// do nothing
			}
		}
		return info;
	}

	private static Bundle createMockBundle(String[] signers) {
		Map<X509Certificate, List<X509Certificate>> signersMap = new HashMap<X509Certificate, List<X509Certificate>>();
		for (int i = 0; i < signers.length; i++) {
			List<String> chain = parseDNchain(signers[i]);
			List<X509Certificate> signersList = new ArrayList<X509Certificate>();
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

		public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
			return null;
		}

		public BundleContext getBundleContext() {
			return null;
		}

		public long getBundleId() {
			return -1;
		}

		public URL getEntry(String path) {
			return null;
		}

		public Enumeration<String> getEntryPaths(String path) {
			return null;
		}

		public Dictionary<String, String> getHeaders() {
			return new Hashtable<String, String>();
		}

		public Dictionary<String, String> getHeaders(String locale) {
			return getHeaders();
		}

		public long getLastModified() {
			return 0;
		}

		public String getLocation() {
			return ""; //$NON-NLS-1$
		}

		public ServiceReference<?>[] getRegisteredServices() {
			return null;
		}

		public URL getResource(String name) {
			return null;
		}

		/**
		 * @throws IOException  
		 */
		public Enumeration<URL> getResources(String name) throws IOException {
			return null;
		}

		public ServiceReference<?>[] getServicesInUse() {
			return null;
		}

		public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
			return new HashMap<X509Certificate, List<X509Certificate>>(signers);
		}

		public int getState() {
			return Bundle.UNINSTALLED;
		}

		public String getSymbolicName() {
			return null;
		}

		public Version getVersion() {
			return Version.emptyVersion;
		}

		public boolean hasPermission(Object permission) {
			return false;
		}

		/**
		 * @throws ClassNotFoundException  
		 */
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void start(int options) throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void start() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void stop(int options) throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void stop() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void uninstall() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void update() throws BundleException {
			throw new IllegalStateException();
		}

		/**
		 * @throws BundleException  
		 */
		public void update(InputStream in) throws BundleException {
			throw new IllegalStateException();
		}

		public int compareTo(Bundle o) {
			return 0;
		}

		public <A> A adapt(Class<A> type) {
			throw new IllegalStateException();
		}

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

		public Principal getSubjectDN() {
			return subject;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof MockX509Certificate)
				return subject.equals(((MockX509Certificate) obj).subject) && issuer.equals(((MockX509Certificate) obj).issuer);
			return false;
		}

		public int hashCode() {
			return subject.hashCode() + issuer.hashCode();
		}

		public String toString() {
			return subject.toString();
		}

		/**
		 * @throws CertificateExpiredException 
		 * @throws java.security.cert.CertificateNotYetValidException  
		 */
		public void checkValidity() throws CertificateExpiredException, java.security.cert.CertificateNotYetValidException {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws java.security.cert.CertificateExpiredException 
		 * @throws java.security.cert.CertificateNotYetValidException  
		 */
		public void checkValidity(Date var0) throws java.security.cert.CertificateExpiredException, java.security.cert.CertificateNotYetValidException {
			throw new UnsupportedOperationException();
		}

		public int getBasicConstraints() {
			throw new UnsupportedOperationException();
		}

		public Principal getIssuerDN() {
			return issuer;
		}

		public boolean[] getIssuerUniqueID() {
			throw new UnsupportedOperationException();
		}

		public boolean[] getKeyUsage() {
			throw new UnsupportedOperationException();
		}

		public Date getNotAfter() {
			throw new UnsupportedOperationException();
		}

		public Date getNotBefore() {
			throw new UnsupportedOperationException();
		}

		public BigInteger getSerialNumber() {
			throw new UnsupportedOperationException();
		}

		public String getSigAlgName() {
			throw new UnsupportedOperationException();
		}

		public String getSigAlgOID() {
			throw new UnsupportedOperationException();
		}

		public byte[] getSigAlgParams() {
			throw new UnsupportedOperationException();
		}

		public byte[] getSignature() {
			throw new UnsupportedOperationException();
		}

		public boolean[] getSubjectUniqueID() {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws CertificateEncodingException  
		 */
		public byte[] getTBSCertificate() throws CertificateEncodingException {
			throw new UnsupportedOperationException();
		}

		public int getVersion() {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws CertificateEncodingException  
		 */
		public byte[] getEncoded() throws CertificateEncodingException {
			throw new UnsupportedOperationException();
		}

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
		public void verify(PublicKey var0) throws java.security.InvalidKeyException, java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException, java.security.SignatureException, java.security.cert.CertificateException {
			throw new UnsupportedOperationException();
		}

		/**
		 * @throws InvalidKeyException 
		 * @throws NoSuchAlgorithmException 
		 * @throws NoSuchProviderException 
		 * @throws SignatureException 
		 * @throws CertificateException  
		 */
		public void verify(PublicKey var0, String var1) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, CertificateException {
			throw new UnsupportedOperationException();
		}

		public Set<String> getCriticalExtensionOIDs() {
			throw new UnsupportedOperationException();
		}

		public byte[] getExtensionValue(String var0) {
			throw new UnsupportedOperationException();
		}

		public Set<String> getNonCriticalExtensionOIDs() {
			throw new UnsupportedOperationException();
		}

		public boolean hasUnsupportedCriticalExtension() {
			throw new UnsupportedOperationException();
		}
	}

	private static class MockPrincipal implements Principal {
		private final String name;

		MockPrincipal(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof MockPrincipal) {
				return name.equals(((MockPrincipal) obj).name);
			}
			return false;
		}

		public int hashCode() {
			return name.hashCode();
		}

		public String toString() {
			return getName();
		}
	}

	private static List<String> parseDNchain(String dnChain) {
		if (dnChain == null) {
			throw new IllegalArgumentException("The DN chain must not be null."); //$NON-NLS-1$
		}
		List<String> parsed = new ArrayList<String>();
		int startIndex = 0;
		startIndex = skipSpaces(dnChain, startIndex);
		while (startIndex < dnChain.length()) {
			int endIndex = startIndex;
			boolean inQuote = false;
			out: while (endIndex < dnChain.length()) {
				char c = dnChain.charAt(endIndex);
				switch (c) {
					case '"' :
						inQuote = !inQuote;
						break;
					case '\\' :
						endIndex++; // skip the escaped char
						break;
					case ';' :
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
