package org.eclipse.osgi.service.datalocation;

import java.net.URL;

/**
 */
public interface Location {
	public boolean allowsDefault();	
	public Location getParentLocation();
	public URL getURL();
	public boolean isSet();
	public boolean isReadOnly();
	public void setURL(URL newLocation) throws IllegalStateException;
}
