/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.identity;

/**
 * Identify an image for further reuse in
 * <ul>
 * <li>EMF: org.eclipse.emf.edit.provider.ItemProvider</li>
 * <li>JFace: org.eclipse.jface.resource.ImageRegistry</li>
 * <li>other places where we need both image identifier and URL</li>
 * </ul>
 * 
 */
public interface ImageIdentity extends ContributionIdentity, ResourceUrl {
	// consider adding image specifics like size
}
