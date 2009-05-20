/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.preferences;

import java.security.Provider;
import java.security.Security;
import java.util.*;
import java.util.List;
import org.eclipse.equinox.internal.security.ui.SecurityUIMsg;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;

public class AdvancedPage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String ALG_ALIAS = "Alg.Alias."; //$NON-NLS-1$
	private static final String PROVIDER = "Provider."; //$NON-NLS-1$

	TreeViewer providerViewer;
	Combo providerCombo;
	Label versionText;
	Label descriptionText;

	public AdvancedPage() {
		//empty
	}

	public void init(IWorkbench workbench) {
		this.noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent) {

		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new FormLayout());

		Label providerLabel = new Label(area, SWT.NONE);
		providerLabel.setText(SecurityUIMsg.ADVPAGE_LABEL_PROVIDER);
		FormData data = new FormData();
		data.top = new FormAttachment(0, 0);
		providerLabel.setData(data);

		providerCombo = new Combo(area, SWT.DROP_DOWN | SWT.READ_ONLY);
		data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.left = new FormAttachment(providerLabel, 0);
		//data.right = new FormAttachment(100, 0);
		//data.height = 5 * providerCombo.getItemHeight();
		data.width = 100;
		providerCombo.setLayoutData(data);

		Provider[] providers = Security.getProviders();
		for (int i = 0; i < providers.length; i++) {
			providerCombo.add(i + ": " + providers[i].getName()); //$NON-NLS-1$
		}
		providerCombo.setVisibleItemCount(providers.length);
		providerCombo.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Provider provider = Security.getProviders()[providerCombo.getSelectionIndex()];
				providerViewer.setInput(getContent(provider));
				versionText.setText(String.valueOf(provider.getVersion()));
				descriptionText.setText(provider.getInfo());
			}
		});

		/*
		Link link = new Link(area, SWT.NONE);
		link.setText(SecurityUIMsg.ADVPAGE_LABEL_LINK);
		link.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				//todo
			}
		});
		 
		data = new FormData();
		data.top = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		link.setLayoutData(data);
		*/

		Label versionLabel = new Label(area, SWT.NONE);
		versionLabel.setText(SecurityUIMsg.ADVPAGE_LABEL_VERSION);
		data = new FormData();
		data.top = new FormAttachment(providerCombo, 0);
		data.left = new FormAttachment(0, 0);
		versionLabel.setLayoutData(data);

		versionText = new Label(area, SWT.NONE);
		data = new FormData();
		data.top = new FormAttachment(providerCombo, 0);
		data.left = new FormAttachment(versionLabel, 0);
		data.right = new FormAttachment(100, 0);
		versionText.setLayoutData(data);

		Label descriptionLabel = new Label(area, SWT.NONE);
		descriptionLabel.setText(SecurityUIMsg.ADVPAGE_LABEL_DESCRIPTION);
		data = new FormData();
		data.top = new FormAttachment(versionLabel, 0);
		data.left = new FormAttachment(0, 0);
		descriptionLabel.setLayoutData(data);

		descriptionText = new Label(area, SWT.NONE);
		data = new FormData();
		data.top = new FormAttachment(versionText, 0);
		data.left = new FormAttachment(versionLabel, 0);
		data.right = new FormAttachment(100, 0);
		data.width = 250;
		descriptionText.setLayoutData(data);

		Group serviceGroup = new Group(area, SWT.NONE);
		serviceGroup.setText(SecurityUIMsg.ADVPAGE_LABEL_SERVICES);
		data = new FormData();
		data.top = new FormAttachment(descriptionLabel, 0);
		data.bottom = new FormAttachment(100, 0);
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		serviceGroup.setLayoutData(data);

		serviceGroup.setLayout(new FormLayout());

		providerViewer = new TreeViewer(serviceGroup);
		providerViewer.setContentProvider(new ProviderContentProvider());
		providerViewer.setLabelProvider(new ProviderLabelProvider());
		Tree tree = (Tree) providerViewer.getControl();

		data = new FormData();
		data.top = new FormAttachment(0, 5);
		data.left = new FormAttachment(0, 5);
		data.right = new FormAttachment(100, -5);
		data.bottom = new FormAttachment(100, -5);
		data.height = (10 * tree.getItemHeight()) + tree.getHeaderHeight();
		providerViewer.getControl().setLayoutData(data);

		providerCombo.select(0);
		Provider provider = Security.getProviders()[0];
		providerViewer.setInput(getContent(provider));
		versionText.setText(String.valueOf(provider.getVersion()));
		descriptionText.setText(provider.getInfo());

		return area;
	}

	Object[] getContent(Provider provider) {

		Set providerKeys = provider.keySet();
		Hashtable serviceList = new Hashtable();
		Hashtable attributeMap = new Hashtable(); // "type" => "Hashtable of (attribute,value) pairs"
		Hashtable aliasMap = new Hashtable(); // "type" => "Arraylist of aliases"
		for (Iterator it = providerKeys.iterator(); it.hasNext();) {
			String key = (String) it.next();

			// this is provider info, available off the Provider API
			if (key.startsWith(PROVIDER)) {
				continue;
			}

			// this is an alias
			if (key.startsWith(ALG_ALIAS)) {
				String value = key.substring(key.indexOf(ALG_ALIAS) + ALG_ALIAS.length(), key.length());
				String type = (String) provider.get(key);
				String algo = value.substring(0, value.indexOf('.'));
				String alias = value.substring(value.indexOf('.') + 1, value.length());
				ArrayList aliasList = (ArrayList) aliasMap.get(type + '.' + algo);
				if (aliasList == null) {
					aliasList = new ArrayList();
					aliasList.add(alias);
					aliasMap.put(type, aliasList);
				} else {
					aliasList.add(alias);
				}
			}

			// this is an attribute
			else if (key.indexOf(' ') > -1) {
				String type = key.substring(0, key.indexOf('.'));
				String algorithm = key.substring(key.indexOf('.') + 1, key.indexOf(' '));
				String attribute = key.substring(key.indexOf(' ') + 1, key.length());
				String value = (String) provider.get(key);
				Hashtable attributeTable = (Hashtable) attributeMap.get(type + '.' + algorithm);
				if (attributeTable == null) {
					attributeTable = new Hashtable();
					attributeTable.put(attribute, value);
					attributeMap.put(type + '.' + algorithm, attributeTable);
				} else {
					attributeTable.put(attribute, value);
				}
			}

			// else this is a service
			else {
				serviceList.put(key, provider.get(key));
			}
		}

		ProviderService[] serviceArray = new ProviderService[serviceList.size()];
		Set serviceKeys = serviceList.keySet();
		int serviceCount = 0;
		for (Iterator it = serviceKeys.iterator(); it.hasNext();) {
			String key = (String) it.next();
			String type = key.substring(0, key.indexOf('.'));
			String algo = key.substring(key.indexOf('.') + 1, key.length());
			String className = (String) serviceList.get(key);
			List aliases = (List) aliasMap.get(algo);
			Map attributes = (Map) attributeMap.get(key);

			serviceArray[serviceCount] = new ProviderService(type, algo, className, aliases, attributes, null);
			serviceCount++;
		}

		// sort the provider services
		Arrays.sort(serviceArray, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				ProviderService s0 = (ProviderService) arg0;
				ProviderService s1 = (ProviderService) arg1;
				return s0.getType().compareTo(s1.getType());
			}
		});

		return serviceArray;
	}

	class ProviderContentProvider implements ITreeContentProvider {

		public Object[] getElements(Object inputElement) {
			Object[] returnValue = null;
			if (inputElement instanceof ProviderService[]) {
				returnValue = (Object[]) inputElement;
			}
			return returnValue;
		}

		public Object getParent(Object element) {
			Object returnValue = null;
			if (element instanceof ProviderServiceDetail) {
				returnValue = ((ProviderServiceDetail) element).getParent();
			}
			return returnValue;
		}

		public Object[] getChildren(Object parentElement) {
			Object[] returnValue = null;
			if (parentElement instanceof ProviderService) {
				ProviderService service = (ProviderService) parentElement;
				ArrayList detailList = new ArrayList();
				detailList.add(new ProviderServiceDetail(service, TYPE_CLASSNAME, service.getClassName()));
				if (service.getAliases() != null) {
					detailList.add(new ProviderServiceDetail(service, TYPE_ALIASES, service.getAliases()));
				}
				if (service.getAttributes() != null) {
					detailList.add(new ProviderServiceDetail(service, TYPE_ATTRIBUTES, service.getAttributes()));
				}
				returnValue = detailList.toArray(new ProviderServiceDetail[] {});
			} else if (parentElement instanceof ProviderServiceDetail) {
				returnValue = ((ProviderServiceDetail) parentElement).getChildren();
			}
			return returnValue;
		}

		public boolean hasChildren(Object element) {
			boolean returnValue = false;
			if (element instanceof ProviderService) {
				returnValue = true;
			} else if (element instanceof ProviderServiceDetail) {
				returnValue = ((ProviderServiceDetail) element).hasChildren();
			}
			return returnValue;
		}

		public void dispose() {
			//nothing to do
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//empty
		}
	}

	class ProviderLabelProvider implements ILabelProvider {

		public Image getImage(Object element) {
			return null;
		}

		public String getText(Object element) {
			String returnValue = null;
			if (element instanceof String) {
				returnValue = (String) element;
			} else if (element instanceof ProviderService) {
				ProviderService service = (ProviderService) element;
				returnValue = service.getType() + ": " + service.getAlgorithm(); //$NON-NLS-1$
			} else if (element instanceof ProviderServiceDetail) {
				ProviderServiceDetail detail = (ProviderServiceDetail) element;
				returnValue = detail.toString();
			} else if (element instanceof ProviderServiceAttribute) {
				ProviderServiceAttribute attribute = (ProviderServiceAttribute) element;
				returnValue = attribute.toString();
			}
			return returnValue;
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void addListener(ILabelProviderListener listener) {
			//empty
		}

		public void removeListener(ILabelProviderListener listener) {
			//empty
		}

		public void dispose() {
			//empty
		}
	}

	private class ProviderService {
		private final String type;
		private final String algorithm;
		private final String className;
		private final List aliases;
		private final Map attributes;

		public ProviderService(String type, String algorithm, String className, List aliases, Map attributes, Bundle providingBundle) {
			this.type = type;
			this.algorithm = algorithm;
			this.className = className;
			this.aliases = aliases;
			this.attributes = attributes;
		}

		public String getType() {
			return type;
		}

		public String getAlgorithm() {
			return algorithm;
		}

		public String getClassName() {
			return className;
		}

		public List getAliases() {
			return aliases;
		}

		public Map getAttributes() {
			return attributes;
		}
	}

	//	private static final int TYPE_ALGORITHM = 0;
	//	private static final int TYPE_TYPE = 1;
	//	private static final int TYPE_BUNDLEID = 2;
	private static final int TYPE_CLASSNAME = 3;
	private static final int TYPE_ALIASES = 4;
	private static final int TYPE_ATTRIBUTES = 5;

	private class ProviderServiceDetail {

		ProviderService parent;
		int type;
		Object data;

		ProviderServiceDetail(ProviderService parent, int type, Object data) {
			this.parent = parent;
			this.type = type;
			this.data = data;
		}

		int getType() {
			return type;
		}

		Object getData() {
			return data;
		}

		ProviderService getParent() {
			return parent;
		}

		public String toString() {
			String returnValue = null;
			switch (getType()) {
				case TYPE_CLASSNAME :
					returnValue = SecurityUIMsg.ADVPAGE_LABEL_CLASS + (String) getData();
					break;

				case TYPE_ALIASES :
					StringBuffer buffer = new StringBuffer();
					buffer.append(SecurityUIMsg.ADVPAGE_LABEL_ALIASES);
					String[] aliases = (String[]) ((List) getData()).toArray(new String[] {});
					for (int i = 0; i < aliases.length; i++) {
						buffer.append(aliases[i]);
						if (i < aliases.length - 1) {
							buffer.append(", "); //$NON-NLS-1$
						}
					}
					returnValue = buffer.toString();
					break;

				case TYPE_ATTRIBUTES :
					returnValue = SecurityUIMsg.ADVPAGE_LABEL_ATTRIBUTES;
					break;
			}
			return returnValue;
		}

		boolean hasChildren() {
			boolean returnValue = false;
			if (getType() == TYPE_ATTRIBUTES) {
				returnValue = true;
			}
			return returnValue;
		}

		Object[] getChildren() {
			Object[] returnValue = null;
			if (getType() == TYPE_ATTRIBUTES) {
				Map attributeMap = (Map) getData();
				ArrayList attributeList = new ArrayList();
				for (Iterator it = attributeMap.keySet().iterator(); it.hasNext();) {
					String key = (String) it.next();
					String value = (String) attributeMap.get(key);
					attributeList.add(new ProviderServiceAttribute(key, value));
				}
				returnValue = attributeList.toArray(new ProviderServiceAttribute[] {});
			}
			return returnValue;
		}
	}

	private class ProviderServiceAttribute {

		String key;
		String value;

		public ProviderServiceAttribute(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String toString() {
			return key + ": " + value; //$NON-NLS-1$
		}
	}
}
