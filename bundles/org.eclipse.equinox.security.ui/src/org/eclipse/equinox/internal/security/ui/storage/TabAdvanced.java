/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.storage;

import java.util.Iterator;
import java.util.Map;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.internal.security.storage.friends.IStorageConstants;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.internal.security.ui.nls.SecUIMessages;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.osgi.service.prefs.BackingStoreException;

public class TabAdvanced {

	private static final String PREFERENCES_PLUGIN = "org.eclipse.equinox.security"; //$NON-NLS-1$

	private Map availableCiphers = null;
	private Combo cipherSelector = null;

	private IEclipsePreferences eclipseNode = null;
	private String defaultCipherAlgorithm;

	public TabAdvanced(TabFolder folder, int index, final Shell shell) {

		TabItem tab = new TabItem(folder, SWT.NONE, index);
		tab.setText(SecUIMessages.tabAdvanced);
		Composite page = new Composite(folder, SWT.NONE);
		tab.setControl(page);

		Label cipherLabel = new Label(page, SWT.NONE);
		cipherLabel.setText(SecUIMessages.selectCipher);

		cipherSelector = new Combo(page, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridData gridDataSelector = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
		cipherSelector.setLayoutData(gridDataSelector);

		// initialize values
		eclipseNode = new ConfigurationScope().getNode(PREFERENCES_PLUGIN);
		defaultCipherAlgorithm = eclipseNode.get(IStorageConstants.CIPHER_KEY, IStorageConstants.DEFAULT_CIPHER);
		availableCiphers = InternalExchangeUtils.ciphersDetectAvailable();

		// fill cipher selector
		int position = 0;
		for (Iterator i = availableCiphers.keySet().iterator(); i.hasNext();) {
			String cipherAlgorithm = (String) i.next();
			cipherSelector.add(cipherAlgorithm, position);
			if (defaultCipherAlgorithm.equals(cipherAlgorithm))
				cipherSelector.select(position);
			position++;
		}

		GridLayoutFactory.fillDefaults().margins(LayoutConstants.getMargins()).numColumns(1).generateLayout(page);
	}

	public void performDefaults() {
		for (int i = 0; i < cipherSelector.getItemCount(); i++) {
			String item = cipherSelector.getItem(i);
			if (item.equals(IStorageConstants.DEFAULT_CIPHER))
				cipherSelector.select(i);
		}
	}

	public void performOk() {
		String selectedCipherAlgorithm = cipherSelector.getText();
		if (!defaultCipherAlgorithm.equals(selectedCipherAlgorithm)) {
			eclipseNode.put(IStorageConstants.CIPHER_KEY, selectedCipherAlgorithm);
			String keyFactory = (String) availableCiphers.get(selectedCipherAlgorithm);
			eclipseNode.put(IStorageConstants.KEY_FACTORY_KEY, keyFactory);
			defaultCipherAlgorithm = selectedCipherAlgorithm;
			try {
				eclipseNode.flush();
			} catch (BackingStoreException e) {
				// nothing can be done
			}
		}
	}

}
