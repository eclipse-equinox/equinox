package org.eclipse.osgi.service.resolver;

public interface StateDelta {
	/**
	 * Returns an array of all the bundle deltas in this delta regardless of type.
	 * @return an array of bundle deltas
	 */
	public BundleDelta[] getChanges();
	
	/**
	 * Returns an array of all the members
	 * of this delta which match the given flags.  If an exact match is requested 
	 * then only delta members whose type exactly matches the given mask are
	 * included.  Otherwise, all bundle deltas whose type's bit-wise and with the
	 * mask is non-zero are included. 
	 * 
	 * @param mask
	 * @param exact
	 * @return an array of bundle deltas matching the given match criteria.
	 */
	public BundleDelta[] getChanges(int mask, boolean exact);
	
	/**
	 * Returns the state whose changes are represented by this delta.
	 * @return
	 */
	public State getState();
}
