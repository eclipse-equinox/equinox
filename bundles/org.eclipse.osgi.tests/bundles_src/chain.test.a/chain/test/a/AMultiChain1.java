/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package chain.test.a;

import chain.test.b.BMultiChain1;
import chain.test.c.CMultipleChain1;

public interface AMultiChain1 extends AMultiChain2, CMultipleChain1, BMultiChain1 {
//
}
