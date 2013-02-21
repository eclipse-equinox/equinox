/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Silenio Quarti
 *******************************************************************************/

#ifndef ECLIPSE_SHM_H
#define ECLIPSE_SHM_H

/* Shared memory utilities */

/**
 * Creates and initializes a shared memory segment
 * with the specified size in bytes. The id for the
 * shared memory segment is stored in the id argument
 * and can be used from any process. It must be freed
 * with free().
 *
 * Returns 0 if success.
 */
extern int createSharedData(_TCHAR** id, int size);

/**
 * Destroy the shared memory segment specified by the
 * id argument. The id is the same as the one return
 * by createSharedData(). This function must be called
 * by the same process that created the segment.
 *
 * Returns 0 if success.
 */
extern int destroySharedData(_TCHAR* id);

/**
 * Gets a copy of the shared memory segment specified
 * by the id argument. The copy is stored in the data
 * argument as a null terminated string and must be
 * freed by free().
 *
 * Returns 0 if success.
 */
extern int getSharedData(_TCHAR* id, _TCHAR** data);

/**
 * Sets the shared memory segment specified by the id
 * argument with a null terminated string specified by
 * data.
 *
 * Returns 0 if sucess.
 */
extern int setSharedData(const _TCHAR* id, const _TCHAR* data);

#endif /* ECLIPSE_SHM_H */


