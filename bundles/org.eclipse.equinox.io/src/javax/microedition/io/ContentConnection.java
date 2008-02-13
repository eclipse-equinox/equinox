/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
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

public abstract interface ContentConnection extends javax.microedition.io.StreamConnection {

	public abstract java.lang.String getEncoding();

	public abstract long getLength();

	public abstract java.lang.String getType();

}
