/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#ifndef __NG_WINBMPFILEFORMAT_H
#define __NG_WINBMPFILEFORMAT_H

/**
 * BMP Decoder
 */
#include "NgCommon.h"
#include "NgImageData.h"

/**
 * BmpDecoderReadImage
 * 
 * Decode the content of a bmp file. 
 *
 * in    : the input stream
 * image : a pointer to a ng_bitmap_image_t
 *
 * return: ERR_OK if the image was correctly built from the input stream
 *		   ERR_NG otherwise.
 */
ng_err_t NgBmpDecoderReadImage (ng_stream_t *in, ng_bitmap_image_t *image);

#endif /* __NG_WINBMPFILEFORMAT_H */
