/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.preferences;

import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PolicyPage extends PreferencePage implements IWorkbenchPreferencePage {

	Button anyButton;
	Button anysignedButton;
	Button onlytrustedButton;
	Button expiredButton;

	protected Control createContents(Composite parent) {

		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new FormLayout());

		Label titleLabel = new Label(page, SWT.NONE);
		titleLabel.setText(SecurityUIMsg.POLPAGE_LABEL_TITLE);
		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.left = new FormAttachment(0, 0);
		titleLabel.setLayoutData(data);

		TabFolder folder = new TabFolder(page, SWT.NONE);
		folder.setLayout(new FormLayout());
		data = new FormData();
		data.top = new FormAttachment(titleLabel, 10);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
		folder.setLayoutData(data);

		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(SecurityUIMsg.POLPAGE_LABEL_SECTION);

		Composite loadArea = new Composite(folder, SWT.NONE);
		loadArea.setLayout(new FormLayout());

		item.setControl(loadArea);

		data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
		loadArea.setLayoutData(data);

		anyButton = new Button(loadArea, SWT.RADIO);
		anysignedButton = new Button(loadArea, SWT.RADIO);
		onlytrustedButton = new Button(loadArea, SWT.RADIO);
		expiredButton = new Button(loadArea, SWT.CHECK);

		anyButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_ANY);
		anyButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				expiredButton.setEnabled(false);
			}
		});
		data = new FormData();
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(0, 5);
		anyButton.setLayoutData(data);

		anysignedButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_ANY_SIGNED);
		anysignedButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				expiredButton.setEnabled(true);
			}
		});
		data = new FormData();
		data.top = new FormAttachment(anyButton, 2);
		data.left = new FormAttachment(0, 5);
		anysignedButton.setLayoutData(data);

		onlytrustedButton.setText(SecurityUIMsg.POLPAGE_BUTTON_ALLOW_ONLY_TRUSTED);
		onlytrustedButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent event) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent event) {
				expiredButton.setEnabled(true);
			}

		});
		data = new FormData();
		data.top = new FormAttachment(anysignedButton, 2);
		data.left = new FormAttachment(0, 5);
		onlytrustedButton.setLayoutData(data);
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
		data = new FormData();
		data.top = new FormAttachment(onlytrustedButton, 5);
		data.left = new FormAttachment(0, 5);
		//data.bottom = new FormAttachment(100, -5);
		expiredButton.setLayoutData(data);

		onlytrustedButton.setSelection(true);
		expiredButton.setEnabled(true);

		return page;
	}

	public void init(IWorkbench workbench) {
		this.noDefaultAndApplyButton();
	}

}
