package org.eclipse.osgi.service.environment;

public interface DebugOptions {
	public abstract boolean getBooleanOption(String option, boolean defaultValue);
	public abstract String getOption(String option);
	public abstract int getIntegerOption(String option, int defaultValue);
	public abstract void setOption(String option, String value);
}