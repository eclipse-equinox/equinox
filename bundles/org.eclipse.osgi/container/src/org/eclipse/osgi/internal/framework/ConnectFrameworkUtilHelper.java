package org.eclipse.osgi.internal.framework;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.connect.FrameworkUtilHelper;

public class ConnectFrameworkUtilHelper implements FrameworkUtilHelper {
	static final Set<FrameworkUtilHelper> connectHelpers = ConcurrentHashMap.newKeySet();

	@Override
	public Optional<Bundle> getBundle(Class<?> classFromBundle) {
		return connectHelpers.stream().filter(Objects::nonNull)
				.flatMap(helper -> helper.getBundle(classFromBundle).stream()).findFirst();
	}

	public static void add(FrameworkUtilHelper moduleConnector) {
		connectHelpers.add(moduleConnector);
	}

	public static void remove(FrameworkUtilHelper moduleConnector) {
		connectHelpers.remove(moduleConnector);
	}
}
