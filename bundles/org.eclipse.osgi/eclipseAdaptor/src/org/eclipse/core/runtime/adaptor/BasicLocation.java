package org.eclipse.core.runtime.adaptor;

import java.net.URL;
import org.eclipse.osgi.service.datalocation.Location;

public class BasicLocation implements Location {
	private boolean isReadOnly;
	private URL location = null;
	private Location parent;
	private URL defaultValue;
	private String property;
	
	public BasicLocation(String property, URL defaultValue, boolean isReadOnly) {
		super();
		this.property = property;
		this.defaultValue = defaultValue;
		this.isReadOnly = isReadOnly;
	}
	public boolean allowsDefault() {
		return defaultValue != null;
	}
	public Location getParentLocation() {
		return parent;
	}
	public URL getURL() {
		if (location == null && defaultValue != null) 
			setURL(defaultValue);
		return location;
	}
	public boolean isSet() {
		return location != null;
	}
	public boolean isReadOnly() {
		return isReadOnly;
	}
	public void setURL(URL value) throws IllegalStateException {
		if (location != null)
			throw new IllegalStateException("Cannot change the location once it is set");
		location = value;
		System.getProperties().put(property, location);
	}
	
	public void setParent(Location value) {
		parent = value;
	}
}
