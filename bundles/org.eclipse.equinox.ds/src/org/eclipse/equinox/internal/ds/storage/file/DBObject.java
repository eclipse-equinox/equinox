/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.storage.file;

import java.io.*;
import java.util.Vector;
import org.eclipse.equinox.internal.ds.model.ServiceComponent;

/**
 * Used for serialization of the DS components
 * 
 * @author Nina Ruseva
 * @author Pavlin Dobrev
 */

public class DBObject implements Serializable {
	private static final long serialVersionUID = 1L;

	public DBObject() {
		//
	}

	public Vector components;

	public DBObject(Vector components) {
		this.components = components;
	}

	public void writeObject(OutputStream out) throws Exception {
		DataOutputStream dataOut;
		if (out instanceof DataOutputStream) {
			dataOut = (DataOutputStream) out;
		} else {
			dataOut = new DataOutputStream(out);
		}
		dataOut.writeInt(components == null ? 0 : components.size());
		if (components != null && components.size() > 0) {
			for (int k = 0; k < components.size(); k++) {
				((ServiceComponent) components.elementAt(k)).writeObject(dataOut);
			}
		}
	}

	public void readObject(InputStream in) throws Exception {
		DataInputStream dataIn;
		if (in instanceof DataInputStream) {
			dataIn = (DataInputStream) in;
		} else {
			dataIn = new DataInputStream(in);
		}

		int size = dataIn.readInt();
		components = new Vector();
		for (int k = 0; k < size; k++) {
			ServiceComponent component = new ServiceComponent();
			component.readObject(dataIn);
			components.addElement(component);
		}

	}
}
