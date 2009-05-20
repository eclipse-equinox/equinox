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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import org.eclipse.equinox.internal.provisional.security.ui.X500PrincipalHelper;
import org.eclipse.equinox.internal.provisional.security.ui.X509CertificateViewDialog;
import org.eclipse.equinox.internal.security.ui.*;
import org.eclipse.equinox.internal.security.ui.wizard.CertificateImportWizard;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.security.TrustEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

//potential enhancements
// 
public class CertificatesPage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int VIEW_ISSUE_TO_COLUMN_INDEX = 0;
	private static final int VIEW_ISSUE_BY_COLUMN_INDEX = 1;
	private static final int VIEW_PROVIDER_COLUMN_INDEX = 2;

	TrustEngine[] activeTrustEngines;
	TableViewer tableViewer;
	Table tableCert;
	CertRowEntry currentSelection;
	Button removeBtn;
	Button viewButton;

	private class CertTableSorter implements Listener {

		final int columnSelected;
		private final CertRowEntry[] certRowEntry;

		CertTableSorter(final int columnSelected, final CertRowEntry[] certRowEntry) {
			this.columnSelected = columnSelected;
			this.certRowEntry = certRowEntry;
		}

		public void handleEvent(Event e) {

			// get the sort column and figure out the direction
			TableColumn sortColumn = tableCert.getSortColumn();
			TableColumn currentColumn = (TableColumn) e.widget;
			int dir = tableCert.getSortDirection();

			if (sortColumn == currentColumn) {
				dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
			} else {
				tableCert.setSortColumn(currentColumn);
				dir = SWT.UP;
			}

			final int direction = dir;
			Arrays.sort(certRowEntry, new Comparator() {
				public int compare(Object arg0, Object arg1) {

					if (columnSelected == VIEW_ISSUE_TO_COLUMN_INDEX) {
						String issueTo1 = getSubjectCommonName(((CertRowEntry) arg0).cert);
						String issueTo2 = getSubjectCommonName(((CertRowEntry) arg1).cert);
						if (direction == SWT.UP)
							return issueTo1.compareTo(issueTo2);
						return issueTo2.compareTo(issueTo1);

					} else if (columnSelected == VIEW_ISSUE_BY_COLUMN_INDEX) {
						String issueOrg1 = getIssuerOrg(((CertRowEntry) arg0).cert);
						String issueOrg2 = getIssuerOrg(((CertRowEntry) arg1).cert);
						if (direction == SWT.UP)
							return issueOrg1.compareTo(issueOrg2);
						return issueOrg2.compareTo(issueOrg1);
					} else {
						String provider1 = activeTrustEngines[((CertRowEntry) arg0).trustEngineIndex].getName();
						String provider2 = activeTrustEngines[((CertRowEntry) arg1).trustEngineIndex].getName();
						if (direction == SWT.UP)
							return provider1.compareTo(provider2);
						return provider2.compareTo(provider1);
					}
				}
			});
			// update data displayed in table
			tableCert.setSortDirection(dir);
			tableCert.clearAll();
			tableViewer.setInput(certRowEntry);
		}
	}

	public CertificatesPage() {
		//empty
	}

	public void init(IWorkbench workbench) {
		this.noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent) {
		initTrustEngines();

		Composite page = new Composite(parent, SWT.NONE);
		FormLayout layout = new FormLayout();
		page.setLayout(layout);

		Label titleLabel = new Label(page, SWT.NONE);
		titleLabel.setText(SecurityUIMsg.CERTPAGE_LABEL_TITLE);

		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		//data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		//data.right = new FormAttachment(100, 0);
		//data.width = 100;
		titleLabel.setLayoutData(data);

		Link link = new Link(page, SWT.NONE);
		link.setText(SecurityUIMsg.CERTPAGE_LABEL_LINK);
		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				//todo
			}
		});

		data = new FormData();
		data.top = new FormAttachment(0, 0);
		//data.bottom = new FormAttachment(100, 0);
		//data.left = new FormAttachment(tableArea, 0);
		data.right = new FormAttachment(100, 0);
		//data.width = 100;
		link.setLayoutData(data);

		Label tableLabel = new Label(page, SWT.NONE);
		tableLabel.setText(SecurityUIMsg.CERTPAGE_TABLE_LABEL);

		data = new FormData();
		data.top = new FormAttachment(titleLabel, 10);
		//data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		//data.right = new FormAttachment(100, 0);
		//data.width = 100;
		tableLabel.setLayoutData(data);

		Composite tableArea = new Composite(page, SWT.NONE);
		FormLayout tableLayout = new FormLayout();
		tableArea.setLayout(tableLayout);

		Composite buttonArea = new Composite(page, SWT.NONE);
		FormLayout buttonLayout = new FormLayout();
		buttonArea.setLayout(buttonLayout);

		data = new FormData();
		data.top = new FormAttachment(tableLabel, 5);
		data.bottom = new FormAttachment(100, 0);
		//data.left = new FormAttachment(tableArea, 0);
		data.right = new FormAttachment(100, 0);
		//data.width = 100;
		buttonArea.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(tableLabel, 5);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(buttonArea, 0);
		tableArea.setLayoutData(data);

		tableViewer = new TableViewer(tableArea, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableCert = (Table) tableViewer.getControl();
		tableCert.setHeaderVisible(true);
		data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.height = (10 * tableCert.getItemHeight());
		tableCert.setLayoutData(data);

		TableColumn column1 = new TableColumn(tableCert, SWT.NULL);
		column1.setText(SecurityUIMsg.CERTPAGE_TABLE_HEADER_ISSUEDTO);
		column1.setWidth(200);
		column1.addListener(SWT.Selection, new CertTableSorter(VIEW_ISSUE_TO_COLUMN_INDEX, getCertificates()));

		TableColumn column2 = new TableColumn(tableCert, SWT.NULL);
		column2.setText(SecurityUIMsg.CERTPAGE_TABLE_HEADER_ISSUEDBY);
		column2.setWidth(200);
		column2.addListener(SWT.Selection, new CertTableSorter(VIEW_ISSUE_BY_COLUMN_INDEX, getCertificates()));

		TableColumn column3 = new TableColumn(tableCert, SWT.NULL);
		column3.setText(SecurityUIMsg.CERTPAGE_TABLE_HEADER_PROVIDER);
		column3.setWidth(200);
		column3.addListener(SWT.Selection, new CertTableSorter(VIEW_PROVIDER_COLUMN_INDEX, getCertificates()));

		Button button1 = new Button(buttonArea, SWT.PUSH);
		button1.setText(SecurityUIMsg.CERTPAGE_BUTTON_IMPORT);
		button1.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				//do nothing
			}

			public void widgetSelected(SelectionEvent e) {
				openImportWizard();
			}

		});

		data = new FormData();
		data.top = new FormAttachment(0, 0);
		//data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 5);
		data.right = new FormAttachment(100, 0);
		data.width = 100;
		data.height = 25;
		button1.setLayoutData(data);

		Button button2 = new Button(buttonArea, SWT.PUSH);
		button2.setText(SecurityUIMsg.CERTPAGE_BUTTON_EXPORT);
		data = new FormData();
		data.top = new FormAttachment(button1, 5);
		//data.bottom = new FormAttachment(100, 0);null
		data.left = new FormAttachment(0, 5);
		//data.right = new FormAttachment(100, 0);
		data.width = 100;
		data.height = 25;
		button2.setLayoutData(data);

		viewButton = new Button(buttonArea, SWT.PUSH);
		viewButton.setText(SecurityUIMsg.CERTPAGE_BUTTON_VIEW);
		viewButton.setEnabled(false);
		data = new FormData();
		data.top = new FormAttachment(button2, 5);
		//data.bottom = new FormAttachment(100, 0);null
		data.left = new FormAttachment(0, 5);
		//data.right = new FormAttachment(100, 0);
		data.width = 100;
		data.height = 25;
		viewButton.setLayoutData(data);

		viewButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			public void widgetSelected(SelectionEvent e) {
				X509CertificateViewDialog certViewer = new X509CertificateViewDialog(tableViewer.getTable().getShell(), (X509Certificate) currentSelection.cert);
				certViewer.open();
			}
		});

		removeBtn = new Button(buttonArea, SWT.PUSH);
		removeBtn.setEnabled(false);
		removeBtn.setText(SecurityUIMsg.CERTPAGE_BUTTON_REMOVE);
		data = new FormData();
		data.top = new FormAttachment(viewButton, 5);
		//data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 5);
		data.right = new FormAttachment(100, 0);
		data.width = 100;
		data.height = 25;
		removeBtn.setLayoutData(data);
		removeBtn.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
			}

			public void widgetSelected(SelectionEvent e) {
				removeSelected();
			}
		});

		initTrustEngines();
		tableViewer.setContentProvider(new SystemCertificatesContentProvider());
		tableViewer.setLabelProvider(new SystemCertificatesLabelProvider(activeTrustEngines));
		tableViewer.setInput(getCertificates());
		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					viewButton.setEnabled(true);
					currentSelection = (CertRowEntry) ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (null != currentSelection && !activeTrustEngines[currentSelection.trustEngineIndex].isReadOnly()) {
						removeBtn.setEnabled(true);

					}
				}

			}
		});

		return page;
	}

	protected void openImportWizard() {
		CertificateImportWizard wizard = new CertificateImportWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		if (dialog.open() == 0) {
			// reload the table viewer
			tableViewer.setInput(getCertificates());
		}
	}

	void removeSelected() {
		try {
			// get the confirmation first
			ConfirmationDialog confirmationDilaog = new ConfirmationDialog(tableViewer.getTable().getShell(), currentSelection.cert);
			if (confirmationDilaog.open() == ConfirmationDialog.YES) {
				activeTrustEngines[currentSelection.trustEngineIndex].removeTrustAnchor(currentSelection.cert);
				tableViewer.setInput(getCertificates());
				removeBtn.setEnabled(false);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private CertRowEntry[] getCertificates() {
		ArrayList certs = new ArrayList();
		try {
			for (int i = 0; i < activeTrustEngines.length; i++) {
				String[] aliases = activeTrustEngines[i].getAliases();
				for (int j = 0; j < aliases.length; j++) {
					CertRowEntry certRowEntry = new CertRowEntry(activeTrustEngines[i].getTrustAnchor(aliases[j]), i);
					certs.add(certRowEntry);
				}
			}
			return (CertRowEntry[]) certs.toArray(new CertRowEntry[] {});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void initTrustEngines() {
		if (activeTrustEngines == null) {
			activeTrustEngines = Activator.getTrustEngines();
		}
	}

	private class CertRowEntry {
		public Certificate cert;
		public int trustEngineIndex;

		public CertRowEntry(Certificate cert, int trustIndex) {
			this.cert = cert;
			this.trustEngineIndex = trustIndex;
		}
	}

	class SystemCertificatesContentProvider implements IStructuredContentProvider {

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
		}

		public Object[] getElements(Object inputElement) {

			if (inputElement instanceof CertRowEntry[]) {
				return (Object[]) inputElement;
			}
			throw new IllegalArgumentException();
		}

		public void dispose() {
			//nothing to dispose
		}
	}

	class SystemCertificatesLabelProvider extends LabelProvider implements ITableLabelProvider {

		private TrustEngine[] tEngines;

		public SystemCertificatesLabelProvider(TrustEngine[] engines) {
			this.tEngines = engines;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			String label = null;
			CertRowEntry certRowEntry = (CertRowEntry) element;
			switch (columnIndex) {
				case 0 :
					label = getSubjectCommonName(certRowEntry.cert);
					break;

				case 1 :
					label = getIssuerOrg(certRowEntry.cert);
					break;

				case 2 :
					label = tEngines[certRowEntry.trustEngineIndex].getName();
					break;

				default :
					break;
			}
			return label;
		}
	}

	static String getSubjectCommonName(Certificate cert) {
		if (cert instanceof X509Certificate) {
			X500PrincipalHelper cnHelper = new X500PrincipalHelper(((X509Certificate) cert).getSubjectX500Principal());
			//If there isn't a CN attribute, return the OU instead
			return (cnHelper.getCN() != null ? cnHelper.getCN() : cnHelper.getOU());
		}
		return SecurityUIMsg.CERTPAGE_ERROR_UNKNOWN_FORMAT;
	}

	static String getIssuerOrg(Certificate cert) {
		if (cert instanceof X509Certificate) {
			X500PrincipalHelper cnHelper = new X500PrincipalHelper(((X509Certificate) cert).getIssuerX500Principal());
			String retOrg = (cnHelper.getO() != null ? cnHelper.getO() : cnHelper.getOU());
			return retOrg;
		}
		return SecurityUIMsg.CERTPAGE_ERROR_UNKNOWN_FORMAT;
	}

}
