package org.eclipse.equinox.launcher;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.X509TrustManager;

class CollectionTrustManager implements X509TrustManager {

	/*package, for test*/ final List<X509TrustManager> trustManagers;

	public CollectionTrustManager(List<X509TrustManager> trustManagers) {
		this.trustManagers = trustManagers;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		CertificateException ce = null;
		for (X509TrustManager trustManager : this.trustManagers) {
			try {
				trustManager.checkClientTrusted(chain, authType);
				return;
			} catch (CertificateException e) {
				if (ce == null) {
					ce = e;
				} else {
					ce.addSuppressed(e);
				}
			}
		}
		if (ce != null) {
			throw ce;
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		CertificateException ce = null;
		for (X509TrustManager trustManager : this.trustManagers) {
			try {
				trustManager.checkServerTrusted(chain, authType);
				return;
			} catch (CertificateException e) {
				if (ce == null) {
					ce = e;
				} else {
					ce.addSuppressed(e);
				}
			}
		}
		if (ce != null) {
			throw ce;
		}
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return this.trustManagers.stream() //
				.map(X509TrustManager::getAcceptedIssuers) //
				.filter(Objects::nonNull).flatMap(Arrays::stream).toArray(X509Certificate[]::new);
	}
}