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

#include "NgImageData.h"

static UBYTE4 RoundRow (UBYTE4 width)
{
  UBYTE4 result = (width + RowRounding - 1)
                      & ~(RowRounding - 1) ;
  return result ;
}

void NgBitmapImageInit (ng_bitmap_image_t *image)
{
	NgBitmapImageClearData (image);
}

void NgBitmapImageFree (ng_bitmap_image_t *image)
{
	NgFree (image->color_map);
	NgFree (image->image_data);
	NgFree (image->alpha_data);
}

void NgBitmapImageClearData (ng_bitmap_image_t *image)
{
	image->bit_count = 0;
	image->image_width = 0;
	image->image_height = 0;
	image->color_count = 0;
	image->color_map = NULL;
	image->image_data = NULL;
	image->alpha_data = NULL;
	image->transparent_pixel = -1;
}

void NgBitmapImageSetSize(ng_bitmap_image_t *image,
						   UBYTE4 color_count,
						   UBYTE4 bits,
						   UBYTE4 width,
						   UBYTE4 height)
{
	NgFree (image->color_map);
	NgFree (image->image_data);
	NgBitmapImageClearData (image);

	switch (bits)
	{
		case 1:
		case 2:
		case 4:
		case 8:
		{
			UBYTE4 bitsize;
			UBYTE4 bytecount;

			image->bit_count = bits;
			image->color_count = color_count;
			image->image_width = width;
			image->image_height = height;
	
			image->color_map = (ng_color_map_entry_t *) NgMalloc (sizeof(ng_color_map_entry_t) * image->color_count);
			NgMemSet (image->color_map, 0, sizeof (ng_color_map_entry_t) * image->color_count);
			bitsize = image->bit_count * image->image_width;
			image->row_width = RoundRow ((bitsize + 7)/8);
			bytecount = image->row_width * image->image_height;
			image->image_data = (UBYTE1 *) NgMalloc (bytecount);
			NgMemSet (image->image_data, 0, (BYTE4)bytecount);
		}
		break ;
		case 16:
		{
			image->bit_count = bits;
			image->color_count = color_count;
			image->image_width = width;
			image->image_height = height;
			image->row_width = RoundRow (2 * image->image_width);
			image->image_data = (UBYTE1 *) NgMalloc (image->row_width * image->image_height);
			NgMemSet (image->image_data, 0, image->row_width * image->image_height);
		}
		break;
		case 24:
		{
			image->bit_count = bits;
			image->color_count = color_count;
			image->image_width = width;
			image->image_height = height;
			image->row_width = RoundRow (3 * image->image_width);
			image->image_data = (UBYTE1 *) NgMalloc (image->row_width * image->image_height);
			NgMemSet (image->image_data, 0, image->row_width * image->image_height);
		}
		break;
		case 32:
		{
			image->bit_count = bits;
			image->color_count = color_count;
			image->image_width = width;
			image->image_height = height;
			image->row_width = RoundRow (4 * image->image_width);
			image->image_data = (UBYTE1 *) NgMalloc (image->row_width * image->image_height);
			NgMemSet (image->image_data, 0, image->row_width * image->image_height);
		}
		break ;
		default:
		NgError (ERR_INVALID_BIT_COUNT, NULL);
	}
}

ng_color_map_entry_t *NgBitmapImageColorMap (ng_bitmap_image_t *image, UBYTE4 index)
{
	if (index >= image->color_count)
	{
		NgError (ERR_SUBSCRIPT_OUT_OF_RANGE, "Error NgBitmapImageColorMap failed");
		return NULL;
	}

	return &image->color_map [index] ;
}

/* blit constants */
#define TYPE_INDEX_1_MSB 1
#define TYPE_INDEX_1_LSB 2
#define TYPE_INDEX_2 3
#define TYPE_INDEX_4 4
#define TYPE_INDEX_8 5
#define TYPE_GENERIC_24 6
#define TYPE_GENERIC_8 7
#define TYPE_GENERIC_16_MSB 8
#define TYPE_GENERIC_16_LSB 9
#define TYPE_GENERIC_32_MSB 10
#define TYPE_GENERIC_32_LSB 11

/**
 * Computes the required channel shift from a mask.
 */
UBYTE4 getChannelShift(UBYTE4 mask)
{
	UBYTE4 i;
	if (mask == 0) return 0;
	for (i = 0; ((mask & 1) == 0) && (i < 32); ++i)
	{
		mask >>= 1;
	}
	return i;
}

/**
 * Computes the required channel width (depth) from a mask.
 */
UBYTE4 getChannelWidth(UBYTE4 mask, UBYTE4 shift)
{
	UBYTE4 i;
	if (mask == 0) return 0;
	mask >>= shift;
	for (i = shift; ((mask & 1) != 0) && (i < 32); ++i) 
	{
		mask >>= 1;
	}
	return i - shift;
}

/**
 * Blits a direct palette image into a direct palette image.
 * 
 * srcData the source byte array containing image data
 * srcStride the source number of bytes per line
 * srcWidth the width of the source blit region
 * srcHeight the height of the source blit region
 * destData the destination byte array containing image data
 * destDepth the destination depth: one of 8, 16, 24, 32
 * destStride the destination number of bytes per line
 * destOrder the destination byte ordering: 0 for LSB, 1 otherwise
 *        ignored if destDepth is not 16 or 32
 * destRedMask the destination red channel mask
 * destGreenMask the destination green channel mask
 * destBlueMask the destination blue channel mask
 *
 * It is assumed that.
 * srcDepth: 24 - BGR ordering (BMP format)
 * no alpha
 * srcX: 0
 * srcY: 0
 * destX: 0
 * destY: 0
 * destWidth: same as srcWidth
 * destHeight: same as srcHeight 
 */
void NgBitmapImageBlitDirectToDirect(
	UBYTE1 *srcData, BYTE4 srcStride,
	BYTE4 srcWidth, BYTE4 srcHeight,
	UBYTE1 *destData, BYTE4 destDepth, BYTE4 destStride, BYTE4 destOrder,
	UBYTE4 destRedMask, UBYTE4 destGreenMask, UBYTE4 destBlueMask)
{
	BYTE4 srcX = 0, srcY = 0, destX = 0, destY = 0, destWidth = srcWidth, destHeight = srcHeight;
	
	BYTE4 sbpp, stype, spr, dbpp, dtype, dpr, dprxi, dpryi, dp, sp, dy, dx;
	BYTE4 destRedShift, destRedWidth;
	BYTE4 destRedPreShift, destGreenShift, destGreenWidth, destGreenPreShift;
	BYTE4 destBlueShift, destBlueWidth, destBluePreShift;
	UBYTE1 r, g, b;
	UBYTE4 data;
	
	/*** Prepare source-related data ***/
	sbpp = 3;
	stype = TYPE_GENERIC_24;

	spr = srcY * srcStride + srcX * sbpp;

	/*** Prepare destination-related data ***/
	switch (destDepth)
	{
		case 8:
			dbpp = 1;
			dtype = TYPE_GENERIC_8;
			break;
		case 16:
			dbpp = 2;
			dtype = (destOrder != 0) ? TYPE_GENERIC_16_MSB : TYPE_GENERIC_16_LSB;
			break;
		case 24:
			dbpp = 3;
			dtype = TYPE_GENERIC_24;
			break;
		case 32:
			dbpp = 4;
			dtype = (destOrder != 0) ? TYPE_GENERIC_32_MSB : TYPE_GENERIC_32_LSB;
			break;
		default:
			return;
	}			
	
	dpr = destY * destStride + destX * dbpp;
	dprxi = dbpp;
	dpryi = destStride;

	/*** Blit ***/
	dp = dpr;
	sp = spr;

	/*** Comprehensive blit (apply transformations) ***/
	destRedShift = getChannelShift(destRedMask);
	destRedWidth = getChannelWidth(destRedMask, destRedShift);
	destRedPreShift = 8 - destRedWidth;
	destGreenShift = getChannelShift(destGreenMask);
	destGreenWidth = getChannelWidth(destGreenMask, destGreenShift);
	destGreenPreShift = 8 - destGreenWidth;
	destBlueShift = getChannelShift(destBlueMask);
	destBlueWidth = getChannelWidth(destBlueMask, destBlueShift);
	destBluePreShift = 8 - destBlueWidth;

	r = 0; g = 0; b = 0;
	for (dy = destHeight; dy > 0; --dy, sp = spr += srcStride, dp = dpr += dpryi)
	{
		for (dx = destWidth; dx > 0; --dx, dp += dprxi) 
		{
			/*** READ NEXT PIXEL ASSUMING BGR ordering (BMP format) ***/
			b = srcData[sp];
			g = srcData[sp + 1];
			r = srcData[sp + 2];
			sp += 3;
			/*** WRITE NEXT PIXEL ***/
			data = 
				(r >> destRedPreShift << destRedShift) |
				(g >> destGreenPreShift << destGreenShift) |
				(b >> destBluePreShift << destBlueShift);
			switch (dtype)
			{
				case TYPE_GENERIC_8:
				{
					destData[dp] = (UBYTE1) data;
				} break;
				case TYPE_GENERIC_16_MSB:
				{
					destData[dp] = (UBYTE1) (data >> 8);
					destData[dp + 1] = (UBYTE1) (data & 0xff);
				} break;
				case TYPE_GENERIC_16_LSB: 
				{
					destData[dp] = (UBYTE1) (data & 0xff);
					destData[dp + 1] = (UBYTE1) (data >> 8);
				} break;
				case TYPE_GENERIC_24: 
				{
					destData[dp] = (UBYTE1) (data >> 16);
					destData[dp + 1] = (UBYTE1) (data >> 8);
					destData[dp + 2] = (UBYTE1) (data & 0xff);
				} break;
				case TYPE_GENERIC_32_MSB: 
				{
					destData[dp] = (UBYTE1) (data >> 24);
					destData[dp + 1] = (UBYTE1) (data >> 16);
					destData[dp + 2] = (UBYTE1) (data >> 8);
					destData[dp + 3] = (UBYTE1) (data & 0xff);
				} break;
				case TYPE_GENERIC_32_LSB: 
				{
					destData[dp] = (UBYTE1) (data & 0xff);
					destData[dp + 1] = (UBYTE1) (data >> 8);
					destData[dp + 2] = (UBYTE1) (data >> 16);
					destData[dp + 3] = (UBYTE1) (data >> 24);
				} break;
			}
		}
	}			
}

/**
 * Create a simple hash table used when converting direct colors to values in a palette
 * Each bucket stores the RGB codes and the corresponding palette index.
 * The key is made from the RGB values.
 * It is used as a cache. New entries colliding with older ones simply
 * replace them.
 */
ng_palette_bucket_t *NgRGBIndexCreate ()
{
	ng_palette_bucket_t *table = (ng_palette_bucket_t *)NgMalloc (RGBIndexTableSize * sizeof (ng_palette_bucket_t));
	NgMemSet (table, 0, RGBIndexTableSize * sizeof (ng_palette_bucket_t));
	return table;
}

void NgRGBIndexFree (ng_palette_bucket_t *table)
{
	NgFree (table);
}

void NgRGBIndexSet (ng_palette_bucket_t *table, UBYTE1 r, UBYTE1 g, UBYTE1 b, UBYTE1 index)
{
	int i = (r * g * b) % RGBIndexTableSize;
	table[i].blue = b;
	table[i].green = g;
	table[i].red = r;
	table[i].index = index;
	table[i].isSet = 1;
}

int NgRGBIndexGet (ng_palette_bucket_t *table, UBYTE1 r, UBYTE1 g, UBYTE1 b)
{
	int i = (r * g * b) % RGBIndexTableSize;
	if (table[i].isSet && table[i].blue == b && table[i].green == g && table[i].red == r)
		return table[i].index;
	return -1;
}

/**
 * Blits a direct palette image into an index palette image.
 * 
 * srcData the source byte array containing image data
 * srcStride the source number of bytes per line
 * srcX the top-left x-coord of the source blit region
 * srcY the top-left y-coord of the source blit region
 * srcWidth the width of the source blit region
 * srcHeight the height of the source blit region
 * destData the destination byte array containing image data
 * destDepth the destination depth: one of 1, 2, 4, 8
 * destStride the destination number of bytes per line
 * destOrder the destination byte ordering: 0 if LSB, 1 otherwise;
 *        ignored if destDepth is not 1
 * destX the top-left x-coord of the destination blit region
 * destY the top-left y-coord of the destination blit region
 * destWidth the width of the destination blit region
 * destHeight the height of the destination blit region
 * destColors the destination palette red green blue component intensities
 * destNumColors the number of colors in destColors
 * 
 * It is assumed that.
 * srcDepth: 24 - BGR ordering (BMP format)
 * no alpha
 * srcX: 0
 * srcY: 0
 * destX: 0
 * destY: 0
 * destWidth: same as srcWidth
 * destHeight: same as srcHeight
 */

void NgBitmapImageBlitDirectToPalette(
	UBYTE1 *srcData, BYTE4 srcStride,
	BYTE4 srcWidth, BYTE4 srcHeight,
	UBYTE1 *destData, BYTE4 destDepth, BYTE4 destStride, BYTE4 destOrder,
	UBYTE1 *destColors, int destNumColors)
{	
	BYTE4 srcX = 0, srcY = 0, destX = 0, destY = 0, destWidth = srcWidth, destHeight = srcHeight;
	BYTE4 sbpp, spr, dtype, dpr, dp, sp, destPaletteSize, dy, dx, j, dr, dg, db, distance, minDistance;

	UBYTE1 r = 0, g = 0, b = 0, index = 0;
	int storedIndex;
	ng_palette_bucket_t *RGBIndexTable;
		
	/*** Prepare source-related data ***/
	sbpp = 3;
	spr = srcY * srcStride + srcX * sbpp;

	/*** Prepare destination-related data ***/
	switch (destDepth)
	{
		case 8:
			dtype = TYPE_INDEX_8;
			break;
		case 4:
			destStride <<= 1;
			dtype = TYPE_INDEX_4;
			break;
		case 2:
			destStride <<= 2;
			dtype = TYPE_INDEX_2;
			break;
		case 1:
			destStride <<= 3;
			dtype = (destOrder != 0) ? TYPE_INDEX_1_MSB : TYPE_INDEX_1_LSB;
			break;
		default:
			return;
	}			
	dpr = destY * destStride + destX;

	dp = dpr;
	sp = spr;
	destPaletteSize = destNumColors;

	RGBIndexTable = NgRGBIndexCreate ();
	for (dy = destHeight; dy > 0; --dy, sp = spr += srcStride, dp = dpr += destStride)
		{
		for (dx = destWidth; dx > 0; --dx, dp += 1)
		{
			/*** READ NEXT PIXEL ASSUMING BGR ordering (BMP format) ***/
			b = srcData[sp];
			g = srcData[sp+1];
			r = srcData[sp+2];
			sp += 3;

			/*** MAP COLOR TO THE PALETTE ***/
			storedIndex = NgRGBIndexGet (RGBIndexTable, r, g, b);
			if (storedIndex >= 0)
			{
				index = (UBYTE1) storedIndex;
			} else
			{
				for (j = 0, minDistance = 0x7fffffff; j < destPaletteSize; ++j)
				{
					dr = (destColors[j*3] & 0xff) - r;
					dg = (destColors[j*3+1] & 0xff) - g;
					db = (destColors[j*3+2] & 0xff) - b;
					distance = dr * dr + dg * dg + db * db;
					if (distance < minDistance)
					{
						index = (UBYTE1)j;
						if (distance == 0) break;
						minDistance = distance;
					}
				}
				NgRGBIndexSet (RGBIndexTable, r, g, b, index);
			}

			/*** WRITE NEXT PIXEL ***/
			switch (dtype) {
				case TYPE_INDEX_8:
					destData[dp] = (UBYTE1) index;
					break;
				case TYPE_INDEX_4:
					if ((dp & 1) != 0) destData[dp >> 1] = ((destData[dp >> 1] & 0xf0) | index);
					else destData[dp >> 1] = ((destData[dp >> 1] & 0x0f) | (index << 4));
					break;
				case TYPE_INDEX_2: 
				{
					int shift = 6 - (dp & 3) * 2;
					destData[dp >> 2] = ((destData[dp >> 2] & ~(0x03 << shift)) | (index << shift));
				} break;					
				case TYPE_INDEX_1_MSB:
				{
					int shift = 7 - (dp & 7);
					destData[dp >> 3] = ((destData[dp >> 3] & ~(0x01 << shift)) | (index << shift));
				} break;
				case TYPE_INDEX_1_LSB: 
				{
					int shift = dp & 7;
					destData[dp >> 3] = ((destData[dp >> 3] & ~(0x01 << shift)) | (index << shift));
				} break;					
			}
		}
	}
	NgRGBIndexFree (RGBIndexTable);
}
