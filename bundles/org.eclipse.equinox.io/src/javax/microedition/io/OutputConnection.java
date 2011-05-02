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
package javax.microedition.io;

public abstract interface OutputConnection extends javax.microedition.io.Connection {

	public abstract java.io.DataOutputStream openDataOutputStream() throws java.io.IOException;

	public abstract java.io.OutputStream openOutputStream() throws java.io.IOException;

}
