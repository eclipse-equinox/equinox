package org.osgi.service.condpermadmin;

/**
 * This interface is used to implement Conditions that are bound to Permissions
 * using ConditionalPermissionCollection. The Permissions of the
 * ConditionalPermissionCollection can only be used if the associated Condition
 * is satisfied.
 */
public interface Condition {
	/**
	 * This method returns true if the Condition has already been evaluated, and
	 * its satisfiability can be determined from its internal state. In other
	 * words, isSatisfied() will return very quickly since no external sources,
	 * such as users, need to be consulted.
	 */
	boolean isEvaluated();

	/**
	 * This method returns true if the Condition is satisfied.
	 */
	boolean isSatisfied();

	/**
	 * This method returns true if the satisfiability may change.
	 */
	boolean isMutable();

	/**
	 * This method returns true if the set of Conditions are satisfied. Although
	 * this method is not static, it should be implemented as if it were static.
	 * All of the passed Conditions will have the same type and will correspond
	 * to the class type of the object on which this method is invoked.
	 */
	boolean isSatisfied(Condition conds[]);
}
