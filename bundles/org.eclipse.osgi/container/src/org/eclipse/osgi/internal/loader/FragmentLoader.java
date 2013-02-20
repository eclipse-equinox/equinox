package org.eclipse.osgi.internal.loader;

import java.net.URL;
import java.util.*;
import org.eclipse.osgi.container.ModuleLoader;

public class FragmentLoader implements ModuleLoader {

	@Override
	public List<URL> findEntries(String path, String filePattern, int options) {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> listResources(String path, String filePattern, int options) {
		return Collections.emptyList();
	}

	@Override
	public ClassLoader getClassLoader() {
		return null;
	}

	@Override
	public boolean getAndSetTrigger() {
		// nothing to do here
		return false;
	}

	@Override
	public boolean isTriggerSet() {
		// nothing to do here
		return false;
	}

}
