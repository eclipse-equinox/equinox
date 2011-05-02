/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.security;

import java.security.*;
import org.eclipse.equinox.internal.util.pool.ObjectCreator;
import org.eclipse.equinox.internal.util.pool.ObjectPool;

/**
 * A simple wrapper for executing privileged actions.
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @version 1.0
 */

public final class PrivilegedRunner implements ObjectCreator {

	private static ObjectPool POOL;

	static {
		try {
			POOL = new ObjectPool(new PrivilegedRunner(), 5, 10);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* prevent instantiations */
	private PrivilegedRunner() {
	}

	/**
	 * Same as the longer doPrivileged method, but fills in the first parameter
	 * only. All other parameters are set to <code>null</code>.
	 * 
	 * @param context
	 *            the access context
	 * @param dispatcher
	 *            the dispatcher which should be called
	 * @param type
	 *            the type of the action - used in the dispatcher
	 * @param arg1
	 *            a parameter received by the dispatcher
	 * @see #doPrivileged(Object, PrivilegedDispatcher, int, Object)
	 * @return the object returned from the execution
	 * @throws Exception
	 *             if the dispatcher fails
	 */
	public static final Object doPrivileged(Object context, PrivilegedDispatcher dispatcher, int type, Object arg1) throws Exception {
		return doPrivileged(context, dispatcher, type, arg1, null, null, null);
	}

	/**
	 * Performs a privileged action. The method calls the dispatcher inside the
	 * privileged call passing it the same parameters that were passed to this
	 * method.
	 * 
	 * @param context
	 *            the access context
	 * @param dispatcher
	 *            the dispatcher which should be called
	 * @param type
	 *            the type of the action - used in the dispatcher
	 * @param arg1
	 *            a parameter received by the dispatcher
	 * @param arg2
	 *            a parameter received by the dispatcher
	 * @param arg3
	 *            a parameter received by the dispatcher
	 * @param arg4
	 *            a parameter received by the dispatcher
	 * @return the object returned from the execution
	 * @throws Exception
	 *             if the dispatcher fails
	 */
	public static final Object doPrivileged(Object context, PrivilegedDispatcher dispatcher, int type, Object arg1, Object arg2, Object arg3, Object arg4) throws Exception {
		/* init runner */
		PA runner = (PA) POOL.getObject();
		runner.dispatcher = dispatcher;
		runner.type = type;
		runner.arg1 = arg1;
		runner.arg2 = arg2;
		runner.arg3 = arg3;
		runner.arg4 = arg4;

		try {
			if (System.getSecurityManager() != null) {
				/*
				 * if security manager is set - then privileged execution is
				 * started
				 */
				return (context != null)
				// 
				? AccessController.doPrivileged(runner, (AccessControlContext) context)
						: AccessController.doPrivileged(runner);
			}
			/* if no security manager is set - simply run the action */
			return runner.run();
		} catch (PrivilegedActionException e) {
			throw e.getException();
		} finally {
			runner.recycle();
			POOL.releaseObject(runner);
		}
	}

	/**
	 * @see org.eclipse.equinox.internal.util.pool.ObjectCreator#getInstance()
	 */
	public Object getInstance() throws Exception {
		return new PA();
	}

	/**
	 * This dispatcher is the handler that is called within the privileged call.
	 * It should dispatch and perform the requested actions depending on the
	 * action type and using the given job parameters.
	 * 
	 * @author Valentin Valchev
	 * @version $Revision: 1.1 $
	 */
	public static interface PrivilegedDispatcher {

		/**
		 * @param type
		 *            the type of the action
		 * @param arg1
		 *            parameter 1 - depends on the action type
		 * @param arg2
		 *            parameter 2 - depends on the action type
		 * @param arg3
		 *            parameter 3 - depends on the action type
		 * @param arg4
		 *            parameter 4 - depends on the action type
		 * @return an object which should be returned from the
		 *         PrivilegedAction.run() method
		 * @throws Exception
		 *             on error
		 */
		Object dispatchPrivileged(int type, Object arg1, Object arg2, Object arg3, Object arg4) throws Exception;
	}

	static class PA implements PrivilegedExceptionAction {

		int type;
		Object arg1, arg2, arg3, arg4;
		PrivilegedDispatcher dispatcher;

		void recycle() {
			dispatcher = null;
			type = -1;
			arg1 = arg2 = arg3 = arg4 = null;
		}

		/**
		 * @see java.security.PrivilegedExceptionAction#run()
		 */
		public Object run() throws Exception {
			return dispatcher.dispatchPrivileged(type, arg1, arg2, arg3, arg4);
		}
	}

}
