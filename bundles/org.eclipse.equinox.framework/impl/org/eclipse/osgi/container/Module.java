package org.eclipse.osgi.container;

import org.osgi.framework.BundleReference;

public abstract class Module implements BundleReference {
	private volatile ModuleRevisions revisions;

	protected final ModuleRevisions getRevisions() {
		ModuleRevisions current = revisions;
		if (current == null)
			throw new IllegalStateException("Module installation is not complete.");
		return current;
	}

	final void setRevisions(ModuleRevisions revisions) {
		this.revisions = revisions;
	}
}
