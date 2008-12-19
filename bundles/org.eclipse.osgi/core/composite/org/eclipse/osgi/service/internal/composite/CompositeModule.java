package org.eclipse.osgi.service.internal.composite;

import java.io.InputStream;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

/**
 * An internal interface only used by the composite implementation
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface CompositeModule {
	public void updateContent(InputStream content) throws BundleException;

	public void refreshContent(boolean synchronously);

	public boolean resolveContent();

	public BundleDescription getCompositeDescription();

	public ClassLoaderDelegate getDelegate();

	public void started(CompositeModule compositeBundle);

	public void stopped(CompositeModule compositeBundle);
}
