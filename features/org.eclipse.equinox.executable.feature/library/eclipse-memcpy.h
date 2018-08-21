/*******************************************************************************
 * Copyright (c) 2017 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at 
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation
 *******************************************************************************/
#ifndef ECLIPSE_MEMMOVE_H
#define ECLIPSE_MEMMOVE_H

/* Bug 517013: Replace memcpy() by memmove() on Linux x86_64 */
/* Must be included after memcpy / memmove system includes.  */
#if defined(LINUX) && defined(__x86_64__)
#  if defined(memcpy)
#    undef memcpy
#  endif
#  define memcpy memmove
#endif

#endif
