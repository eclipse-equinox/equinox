package org.eclipse.equinox.api.internal;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class TracingOptions {
	public static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = new DebugOptionsListener() {
		@Override
		public void optionsChanged(DebugOptions options) {
			debug = options.getBooleanOption(APISupport.PI_COMMON + "/debug", false); //$NON-NLS-1$

			debugProgressMonitors = debug
					&& options.getBooleanOption(APISupport.PI_COMMON + "/progress_monitors", false); //$NON-NLS-1$
		}
	};

	public static boolean debug;
	public static boolean debugProgressMonitors;
}
