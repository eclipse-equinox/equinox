package org.eclipse.osgi.tests.container.dummys;

import java.util.Collection;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;

public class DummyResolverHookFactory implements ResolverHookFactory {
	private final ResolverHook hook;

	public DummyResolverHookFactory() {
		this(new DummyResolverHook());
	}

	public DummyResolverHookFactory(ResolverHook hook) {
		this.hook = hook;
	}

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return hook;
	}

	public ResolverHook getHook() {
		return hook;
	}
}
