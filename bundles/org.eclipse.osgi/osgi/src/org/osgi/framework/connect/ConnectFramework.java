package org.osgi.framework.connect;

import org.osgi.framework.launch.Framework;

/**
 * A framework created by a {@link ConnectFrameworkFactory} ... this is non
 * standard extension
 */
public interface ConnectFramework extends Framework {

	/**
	 * adds a {@link FrameworkUtilHelper} via this {@link ConnectFramework}
	 * 
	 * @param helper
	 */
	void addFrameworkUtilHelper(FrameworkUtilHelper helper);

	/**
	 * removes a {@link FrameworkUtilHelper} via this {@link ConnectFramework}
	 * 
	 * @param helper
	 */
	void removeFrameworkUtilHelper(FrameworkUtilHelper helper);

}
