/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
