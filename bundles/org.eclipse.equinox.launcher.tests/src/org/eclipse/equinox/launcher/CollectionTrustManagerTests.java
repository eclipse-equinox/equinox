package org.eclipse.equinox.launcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Test;

public class CollectionTrustManagerTests {

	@Test
	public void testAcceptedIssuers() throws Exception {
		X509Certificate[] acceptedIssuers1 = { mock(X509Certificate.class), mock(X509Certificate.class) };
		X509Certificate[] acceptedIssuers2 = { mock(X509Certificate.class), mock(X509Certificate.class) };
		X509TrustManager mock1 = mock(X509TrustManager.class);
		X509TrustManager mock2 = mock(X509TrustManager.class);
		when(mock1.getAcceptedIssuers()).thenReturn(acceptedIssuers1);
		when(mock2.getAcceptedIssuers()).thenReturn(acceptedIssuers2);
		CollectionTrustManager collectionTrustManager = new CollectionTrustManager(List.of(mock1, mock2));

		X509Certificate[] allAcceptedIssuers = collectionTrustManager.getAcceptedIssuers();

		assertThat(allAcceptedIssuers,
				arrayContaining(acceptedIssuers1[0], acceptedIssuers1[1], acceptedIssuers2[0], acceptedIssuers2[1]));
	}

	@Test
	public void testCheckClientTrusted() throws Exception {
		X509Certificate[] chainTrustedBy1 = { mock(X509Certificate.class) };
		X509Certificate[] chainTrustedBy2 = { mock(X509Certificate.class) };
		X509Certificate[] chainTrustedByNone = { mock(X509Certificate.class) };
		X509Certificate[] chainTrustedByBoth = { mock(X509Certificate.class) };
		String authType = "testAuthType";

		CertificateException exceptionFrom1 = new CertificateException("exception from 1");
		CertificateException exceptionFrom2 = new CertificateException("exception from 2");

		X509TrustManager mock1 = mock(X509TrustManager.class);
		doThrow(IllegalStateException.class).when(mock1).checkClientTrusted(any(), any());
		doNothing().when(mock1).checkClientTrusted(eq(chainTrustedBy1), eq(authType));
		doNothing().when(mock1).checkClientTrusted(eq(chainTrustedByBoth), eq(authType));
		doThrow(exceptionFrom1).when(mock1).checkClientTrusted(eq(chainTrustedBy2), eq(authType));
		doThrow(exceptionFrom1).when(mock1).checkClientTrusted(eq(chainTrustedByNone), eq(authType));

		X509TrustManager mock2 = mock(X509TrustManager.class);
		doThrow(IllegalStateException.class).when(mock2).checkClientTrusted(any(), any());
		doNothing().when(mock2).checkClientTrusted(eq(chainTrustedBy2), eq(authType));
		doNothing().when(mock2).checkClientTrusted(eq(chainTrustedByBoth), eq(authType));
		doThrow(exceptionFrom2).when(mock2).checkClientTrusted(eq(chainTrustedBy1), eq(authType));
		doThrow(exceptionFrom2).when(mock2).checkClientTrusted(eq(chainTrustedByNone), eq(authType));

		CollectionTrustManager collectionTrustManager = new CollectionTrustManager(List.of(mock1, mock2));

		collectionTrustManager.checkClientTrusted(chainTrustedBy1, authType);
		collectionTrustManager.checkClientTrusted(chainTrustedBy2, authType);
		collectionTrustManager.checkClientTrusted(chainTrustedByBoth, authType);

		CertificateException exception = assertThrows(CertificateException.class, () -> {
			collectionTrustManager.checkClientTrusted(chainTrustedByNone, authType);
		});
		assertThat(exception, sameInstance(exceptionFrom1)); // first in the list
		assertThat(exception.getSuppressed(), arrayContaining(sameInstance(exceptionFrom2))); // second, suppressed
	}

	@Test
	public void testCheckServerTrusted() throws Exception {
		X509Certificate[] chainTrustedBy1 = { mock(X509Certificate.class) };
		X509Certificate[] chainTrustedBy2 = { mock(X509Certificate.class) };
		X509Certificate[] chainTrustedByNone = { mock(X509Certificate.class) };
		X509Certificate[] chainTrustedByBoth = { mock(X509Certificate.class) };
		String authType = "testAuthType";

		CertificateException exceptionFrom1 = new CertificateException("exception from 1");
		CertificateException exceptionFrom2 = new CertificateException("exception from 2");

		X509TrustManager mock1 = mock(X509TrustManager.class);
		doThrow(IllegalStateException.class).when(mock1).checkServerTrusted(any(), any());
		doNothing().when(mock1).checkServerTrusted(eq(chainTrustedBy1), eq(authType));
		doNothing().when(mock1).checkServerTrusted(eq(chainTrustedByBoth), eq(authType));
		doThrow(exceptionFrom1).when(mock1).checkServerTrusted(eq(chainTrustedBy2), eq(authType));
		doThrow(exceptionFrom1).when(mock1).checkServerTrusted(eq(chainTrustedByNone), eq(authType));

		X509TrustManager mock2 = mock(X509TrustManager.class);
		doThrow(IllegalStateException.class).when(mock2).checkServerTrusted(any(), any());
		doNothing().when(mock2).checkServerTrusted(eq(chainTrustedBy2), eq(authType));
		doNothing().when(mock2).checkServerTrusted(eq(chainTrustedByBoth), eq(authType));
		doThrow(exceptionFrom2).when(mock2).checkServerTrusted(eq(chainTrustedBy1), eq(authType));
		doThrow(exceptionFrom2).when(mock2).checkServerTrusted(eq(chainTrustedByNone), eq(authType));

		CollectionTrustManager collectionTrustManager = new CollectionTrustManager(List.of(mock1, mock2));

		collectionTrustManager.checkServerTrusted(chainTrustedBy1, authType);
		collectionTrustManager.checkServerTrusted(chainTrustedBy2, authType);
		collectionTrustManager.checkServerTrusted(chainTrustedByBoth, authType);

		CertificateException exception = assertThrows(CertificateException.class, () -> {
			collectionTrustManager.checkServerTrusted(chainTrustedByNone, authType);
		});
		assertThat(exception, sameInstance(exceptionFrom1)); // first in the list
		assertThat(exception.getSuppressed(), arrayContaining(sameInstance(exceptionFrom2))); // second, suppressed
	}
}
