/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.preferences;

import org.eclipse.equinox.internal.security.ui.Activator;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationEngine;
import org.eclipse.osgi.internal.service.security.DefaultAuthorizationEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PolicyPage extends PreferencePage implements IWorkbenchPreferencePage {

	Button anyButton;
	Button anysignedButton;
	Button onlytrustedButton;
	Button expiredButton;
	//TabFolder folder;
	private int selectedPolicy;
	private static final int BIT_TRUST_EXPIRED = DefaultAuthorizationEngine.ENFORCE_VALIDITY | DefaultAuthorizationEngine.ENFORCE_TRUSTED | DefaultAuthorizationEngine.ENFORCE_SIGNED;
	private static final int BIT_TRUST = DefaultAuthorizationEngine.ENFORCE_TRUSTED | DefaultAuthorizationEngine.ENFORCE_SIGNED;

	protected Control createContents(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.marginWidth = 0;
		compositeLayout.marginHeight = 0;
		composite.setLayout(compositeLayout);
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		//Group gives nice box around the radio buttons
		Group buttonGroup = new Group(composite, SWT.LEFT);
		GridLayout buttonLayout = new GridLayout();
		buttonGroup.setLayout(buttonLayout);
		GridData compositeData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		buttonGroup.setLayoutData(compositeData);
		buttonGroup.setText(SecurityUIMsg.POLPAGE_LABEL_DESC);

		anyButton = new Button(buttonGroup, SWT.RADIO);
		anysignedButton = new Button(buttonGroup, SWT.RADIO);
		onlytrustedButton = new Button(buttonGroup, SWT.RADIO);

		expiredButton = new Button(buttonGroup, SWT.CHECK);
		expiredButton.setEnabled(true); //since onlytrustedButton is default on

		GridData data = new GridData();
		data.horizontalIndent = 20;
		expiredButton.setLayoutData(data);

		expiredButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				if (expiredButton.getSelection())
					persistPolicySetting(DefaultAuthorizationEngine.ENFORCE_VALIDITY | DefaultAuthorizationEngine.ENFORCE_TRUSTED | DefaultAuthorizationEngine.ENFORCE_SIGNED);
				else
					persistPolicySetting(DefaultAuthorizationEngine.ENFORCE_TRUSTED | DefaultAuthorizationEngine.ENFORCE_SIGNED);
			}

		});

		// select the default authorization engine
		AuthorizationEngine authEngine = Activator.getAuthorizationEngine();
		// check if osgi.signedcontent.support property is enable
		if (System.getProperty("osgi.signedcontent.support") != null && authEngine instanceof DefaultAuthorizationEngine) {
			DefaultAuthorizationEngine defaultAuthEngine = (DefaultAuthorizationEngine) authEngine;
			selectedPolicy = defaultAuthEngine.getLoadPolicy();

			if ((selectedPolicy & BIT_TRUST_EXPIRED) == BIT_TRUST_EXPIRED) {
				onlytrustedButton.setSelection(true);
				expiredButton.setSelection(true);
				expiredButton.setEnabled(true);
			} else if ((selectedPolicy & BIT_TRUST) == BIT_TRUST) {
				onlytrustedButton.setSelection(true);
				expiredButton.setEnabled(true);
			} else if ((selectedPolicy & DefaultAuthorizationEngine.ENFORCE_SIGNED) == DefaultAuthorizationEngine.ENFORCE_SIGNED)
				anysignedButton.setSelection(true);
			else
				anyButton.setSelection(true);
		} else {
			anyButton.setSelection(true);
		}

		anyButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_ANY);
		anyButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				expiredButton.setEnabled(false);
				expiredButton.setSelection(false);
				persistPolicySetting(DefaultAuthorizationEngine.ENFORCE_NONE);
			}
		});

		anysignedButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_ANY_SIGNED);
		anysignedButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				expiredButton.setSelection(false);
				expiredButton.setEnabled(false);
				persistPolicySetting(DefaultAuthorizationEngine.ENFORCE_SIGNED);
			}
		});

		onlytrustedButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_ONLY_TRUSTED);
		onlytrustedButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				expiredButton.setEnabled(true);
				persistPolicySetting(DefaultAuthorizationEngine.ENFORCE_TRUSTED | DefaultAuthorizationEngine.ENFORCE_SIGNED);
			}

		});

		/*
				whitelistButton.setText(STR_whiteList);
				whitelistButton.addSelectionListener(new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent event) {
						//do nothing
					}

					public void widgetSelected(SelectionEvent event) {
						whitelistTable.setEnabled(true);
						promptButton.setEnabled(true);
					}

				});
				data = new FormData();
				data.top = new FormAttachment(anytrustedButton, 2);
				data.left = new FormAttachment(0, 5);
				whitelistButton.setLayoutData(data);

				Composite whitelistArea = new Composite(loadArea, SWT.NONE);
				whitelistArea.setLayout(new FormLayout());

				data = new FormData();
				data.top = new FormAttachment(whitelistButton, 2);
				data.left = new FormAttachment(0, 25);
				data.bottom = new FormAttachment(promptButton, -2);
				data.right = new FormAttachment(100, 0);
				whitelistArea.setLayoutData(data);

				Button addButton = new Button(whitelistArea, SWT.PUSH);
				addButton.setText("Add...");

				data = new FormData();
				data.top = new FormAttachment(0, 0);
				data.right = new FormAttachment(100, -5);
				data.width = 100;
				data.height = 25;
				addButton.setLayoutData(data);

				Button removeButton = new Button(whitelistArea, SWT.PUSH);
				removeButton.setText("Remove");

				data = new FormData();
				data.top = new FormAttachment(addButton, 5);
				data.right = new FormAttachment(100, -5);
				data.width = 100;
				data.height = 25;
				removeButton.setLayoutData(data);

				whitelistTable = new Table(whitelistArea, SWT.BORDER | SWT.V_SCROLL);
				whitelistTable.setEnabled(false);
				data = new FormData();
				data.top = new FormAttachment(0, 0);
				data.left = new FormAttachment(0, 0);
				data.bottom = new FormAttachment(100, 0);
				data.right = new FormAttachment(addButton, -5);
				data.height = whitelistTable.getItemHeight() * 5;
				whitelistTable.setLayoutData(data);

				promptButton.setText(STR_promptUntrusted);
				promptButton.setEnabled(false);
				data = new FormData();
				//data.top = new FormAttachment(whitelistTable, 10);
				data.bottom = new FormAttachment(expiredButton, 0);
				data.left = new FormAttachment(0, 5);
				promptButton.setLayoutData(data);
		*/
		expiredButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_EXPIRED);

		//		onlytrustedButton.setSelection(true);
		//		expiredButton.setEnabled(true);

		return composite;
	}

	//protected void enableSecurityWidgets() {
	//	folder.setEnabled(true);
	//}

	//protected void disableSecurityWidgets() {
	//	folder.setEnabled(false);
	//}

	public boolean performOk() {
		// update the policy iff the page is dirty
		AuthorizationEngine authEngine = Activator.getAuthorizationEngine();
		if (authEngine instanceof DefaultAuthorizationEngine) {
			DefaultAuthorizationEngine defaultAuthEngine = (DefaultAuthorizationEngine) authEngine;
			defaultAuthEngine.setLoadPolicy(selectedPolicy);
		} else {
			// log the error
		}

		return super.performOk();
	}

	void persistPolicySetting(int policy) {
		selectedPolicy = policy;
	}

	public void init(IWorkbench workbench) {
		this.noDefaultAndApplyButton();
	}

}
