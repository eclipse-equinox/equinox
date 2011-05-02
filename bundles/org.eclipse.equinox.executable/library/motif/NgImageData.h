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

#ifndef __NG_IMAGEDATA_H
#define __NG_IMAGEDATA_H

/**
 * Type ng_bitmap_image_t (C version of SWT ImageData)
 * 
 * Unlike ImageData, ng_bitmap_image_t and all its api are 'internal'.
 * The api marked 'public' is in the sense that it can be used
 * by the rest of the native graphic library.
 */

#include "NgCommon.h"

typedef struct ng_bitmap_image_t ng_bitmap_image_t;

typedef struct {
	UBYTE1 blue;
	UBYTE1 green;
	UBYTE1 red;
} ng_color_map_entry_t;

/* ImageData in SWT expects RGB not BGR */
enum { RedOffset=0, GreenOffset=1, BlueOffset=2 };

struct ng_bitmap_image_t {
	/* Width in bytes of each row */
	UBYTE4 row_width;
	/* Number of bits per pixel (depth) 1, 2, 4, 8, 16 or 24 */
	UBYTE4 bit_count;
	UBYTE4 image_width;
	UBYTE4 image_height;
	/* image data
	 * 24-bit images, 3 bytes per pixel representing RGB values
	 * 32 bit images, 4 bytes per pixel representing RGB values + one wasted byte
	 * 16 bit images, 2 bytes per pixel
	 * rest (1, 2, 4, 8): index into color map
	 */
	UBYTE1 *image_data;
	/* alpha data (either NULL or of size image_width*image_height) */
	UBYTE1 *alpha_data;
	/* transparent pixel - default is -1 which means no transparent pixel */
	BYTE4 transparent_pixel;
	/* number of entries in color map */
	UBYTE4 color_count;
	ng_color_map_entry_t *color_map;
};

/************************************************
 * Public API ng_bitmap_image_t
 ************************************************/

/**
 * Init an image
 */
void NgBitmapImageInit (ng_bitmap_image_t *);

/**
 * Dispose the resources allocated by the image.
 */
void NgBitmapImageFree (ng_bitmap_image_t *);

/**
 * Access start of image data
 * return: a pointer to an array of UBYTE1 of size image_row * image_width
 * signature: UBYTE1 *NgBitmapImageImageData (ng_bitmap_image_t *image)
 */
#define NgBitmapImageImageData(image) ((image)->image_data)

/**
 * signature: UBYTE4 NgBitmapImageWidth (ng_bitmap_image_t *image)
 */
#define NgBitmapImageWidth(image) ((image)->image_width)

/**
 * signature: UBYTE4 NgBitmapImageHeight (ng_bitmap_image_t *image)
 */
#define NgBitmapImageHeight(image) ((image)->image_height)

/**
 * signature: UBYTE4 NgBitmapImageBitCount (ng_bitmap_image_t *image)
 */
#define NgBitmapImageBitCount(image) ((image)->bit_count)

/**
 * signature: UBYTE4 NgBitmapImageColorCount (ng_bitmap_image_t *image)
 */
#define NgBitmapImageColorCount(image) ((image)->color_count)

/**
 * Access a row of the image
 * row: a value which must be between 0 and image_height-1
 * return: a pointer to the desired row, which is an array of size row_width
 * signature: UBYTE1 *NgBitmapImageGetRow (ng_bitmap_image_t *image, UBYTE4 row)
 */
#define NgBitmapImageGetRow(image, row) (&image->image_data[row * image->row_width])

/**
 * signature: UBYTE4 NgBitmapImageBytesPerRow (ng_bitmap_image_t *image)
 */
#define NgBitmapImageBytesPerRow(image) ((image)->row_width)

/**
 * Retrieve an entry from the color map
 * index: a value which must be between 0 and color_count-1
 */
ng_color_map_entry_t *NgBitmapImageColorMap (ng_bitmap_image_t *, UBYTE4 index);

/**
 * Get the value of the transparent pixel
 * signature: BYTE4 NgBitmapImageGetTransparent (ng_bitmap_image_t *image)
 */
#define NgBitmapImageGetTransparent(image) ((image)->transparent_pixel)

/**
 * Get the alpha data
 * signature: UBYTE1 *NgBitmapImageGetAlpha (ng_bitmap_image_t* image)
 */
#define NgBitmapImageGetAlpha(image) ((image)->alpha_data)

void NgBitmapImageBlitDirectToDirect(
	UBYTE1 *srcData, BYTE4 srcStride,
	BYTE4 srcWidth, BYTE4 srcHeight,
	UBYTE1 *destData, BYTE4 destDepth, BYTE4 destStride, BYTE4 destOrder,
	UBYTE4 destRedMask, UBYTE4 destGreenMask, UBYTE4 destBlueMask);

/* Size of hash table used in NgBitmapImageBlitDirectToPalette */
#define RGBIndexTableSize 103

typedef struct {
	UBYTE1 isSet;
	UBYTE1 blue;
	UBYTE1 green;
	UBYTE1 red;
	UBYTE1 index;
} ng_palette_bucket_t;
	
void NgBitmapImageBlitDirectToPalette(
	UBYTE1 *srcData, BYTE4 srcStride,
	BYTE4 srcWidth, BYTE4 srcHeight,
	UBYTE1 *destData, BYTE4 destDepth, BYTE4 destStride, BYTE4 destOrder,
	UBYTE1 *destColors, int destNumColors);

/************************************************
 * Private API ng_bitmap_image_t
 ************************************************/

/* Number of bytes to round each row to */
#define RowRounding 4

void NgBitmapImageInitialize (ng_bitmap_image_t *);
void NgBitmapImageClearData (ng_bitmap_image_t *);

void NgBitmapImageSetSize(ng_bitmap_image_t *,
						   UBYTE4 color_count,
						   UBYTE4 bits,
						   UBYTE4 width,
						   UBYTE4 height);

#endif /* NG_IMAGEDATA_H */
