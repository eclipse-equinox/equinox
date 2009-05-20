/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.provisional.security.ui.AuthorizationManager;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This contribution item is used to create a visual cue that informs the
 * user of bundles disabled in the system for signature validation reasons.
 * 
 * It has the following functions:
 * - Two levels of severity, represented by two distinct graphical icons
 * - Visual notification when new user attention is required (e.g. throbbing)
 * - An informational message when the user hovers over the icon
 * - A right-click menu for contributing security related actions
 * 
 * @since 3.4
 */
public class SecurityStatusControl extends ControlContribution {

	private static final String IMAGE_PATH_OK = "/full/obj16/green.GIF"; //$NON-NLS-1$
	private static final String IMAGE_PATH_ERROR = "/full/obj16/red.GIF"; //$NON-NLS-1$
	private static final String IMAGE_PATH_DISABLED = "/full/obj16/red.GIF"; //$NON-NLS-1$
	private static final String IMAGE_PATH_UNKNOWN = "/full/obj16/red.GIF"; //$NON-NLS-1$

	/* the default id for this Item */
	private static final String ID = "org.eclipse.ui.securityStatus"; //$NON-NLS-1$

	private IWorkbenchWindow window;
	private CLabel label;

	private IconState currentState;

	/**
	 * Creates the contribution item.
	 * 
	 * @param window the window
	 */
	public SecurityStatusControl(IWorkbenchWindow window) {
		this(window, ID);
	}

	/**
	 * Creates the contribution item.
	 * 
	 * @param window the window
	 * @param id the id
	 */
	public SecurityStatusControl(IWorkbenchWindow window, String id) {
		super(id);
		Assert.isNotNull(window);
		this.window = window;
		this.currentState = getCurrentState();
	}

	private static IconState getCurrentState() {
		AuthorizationManager mgr = Activator.getAuthorizationManager();
		return new IconState(mgr.isEnabled(), mgr.getStatus(), mgr.needsAttention());
	}

	protected Control createControl(Composite parent) {

		label = new CLabel(parent, SWT.NONE);
		label.setImage(getIcon(currentState));
		label.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				//TODO: handleActionInvoked();
			}

			public void mouseDown(MouseEvent e) {
				//nothing yet
			}

			public void mouseUp(MouseEvent e) {
				//nothing yet
			}
		});

		Job updateJob = new Job(ID) {
			public IStatus run(IProgressMonitor monitor) {
				while (true) {
					IconState newState = getCurrentState();
					if (!currentState.equals(newState)) {
						final Display display = getDisplay(window);
						if (null != display)
							display.asyncExec(new Runnable() {
								public void run() {
									if (!label.isDisposed()) {
										Image oldIcon = label.getImage();
										label.setImage(getIcon(currentState));
										oldIcon.dispose();
									}
								}
							});
						currentState = newState;
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						break;
					}
				}
				return null;
			}
		};
		updateJob.setSystem(true);
		updateJob.setPriority(Job.DECORATE);
		updateJob.schedule();

		return label;
	}

	public void dispose() {
		Image currentImage = label.getImage();
		if (currentImage != null) {
			currentImage.dispose();
		}
		label.dispose();
	}

	protected static Image getIcon(IconState iconState) {
		Image returnValue = null;

		if (iconState.isEnabled()) {
			IStatus status = iconState.getStatus();
			ImageDescriptor imgDesc = null;
			switch (status.getSeverity()) {
				case IStatus.OK :
					imgDesc = Activator.getImageDescriptor(IMAGE_PATH_OK);
					break;

				case IStatus.ERROR :
					imgDesc = Activator.getImageDescriptor(IMAGE_PATH_ERROR);
					break;

				default :
					imgDesc = Activator.getImageDescriptor(IMAGE_PATH_UNKNOWN);
					break;
			}
			returnValue = imgDesc.createImage();
			//TODO: decorate for needsAttention
		} else {
			ImageDescriptor imgDesc = Activator.getImageDescriptor(IMAGE_PATH_DISABLED);
			returnValue = imgDesc.createImage();
		}
		return returnValue;
	}

	private static Display getDisplay(IWorkbenchWindow window) {
		if (null != window) {
			Shell shell = window.getShell();
			if (null != shell)
				return shell.getDisplay();
		}
		return null;
	}

	private static class IconState {
		boolean enabled;
		boolean needsAttention;
		IStatus status;

		IconState(boolean enabled, IStatus status, boolean needsAttention) {
			if (null == status)
				throw new IllegalArgumentException();

			this.enabled = enabled;
			this.status = status;
			this.needsAttention = needsAttention;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public IStatus getStatus() {
			return status;
		}

		public boolean equals(Object another) {
			boolean returnValue = false;
			if (another instanceof IconState) {
				if (enabled == ((IconState) another).enabled) {
					if (needsAttention == ((IconState) another).needsAttention) {
						if (status.equals(((IconState) another).getStatus())) {
							returnValue = true;
						}
					}
				}
			}
			return returnValue;
		}

		public int hashCode() {
			return Boolean.valueOf(enabled).hashCode() + status.hashCode() + Boolean.valueOf(needsAttention).hashCode();
		}
	}
}
