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

#include "NgCommon.h"
#include "NgWinBMPFileFormat.h"

#define BMPHeaderFixedSize 40

BYTE4 decompressRLE4Data(BYTE1 *src, BYTE4 numBytes, BYTE4 stride, BYTE1 *dest, BYTE4 destSize)
{
	BYTE4 sp = 0;
	BYTE4 se = numBytes;
	BYTE4 dp = 0;
	BYTE4 de = destSize;
	BYTE4 x = 0, y = 0;
	BYTE4 i;
	while (sp < se)
	{
		int len = src[sp] & 0xFF;
		sp++;
		if (len == 0) 
		{
			len = src[sp] & 0xFF;
			sp++;
			switch (len) 
			{
				case 0: /* end of line */
					y++;
					x = 0;
					dp = y * stride;
					if (dp >= de)
						return -1;
					break;
				case 1: /* end of bitmap */
					return 1;
				case 2: /* delta */
					x += src[sp] & 0xFF;
					sp++;
					y += src[sp] & 0xFF;
					sp++;
					dp = y * stride + x / 2;
					if (dp >= de)
						return -1;
					break;
				default: /* absolute mode run */
					if ((len & 1) != 0) /* odd run lengths not currently supported */
						return -1;
					x += len;
					len = len / 2;
					if (len > (se - sp))
						return -1;
					if (len > (de - dp))
						return -1;
					for (i = 0; i < len; i++) 
					{
						dest[dp] = src[sp];
						dp++;
						sp++;
					}
					if ((sp & 1) != 0)
						sp++; /* word align sp? */
					break;
			}
		} else 
		{
			BYTE1 theByte;
			if ((len & 1) != 0)
				return -1;
			x += len;
			len = len / 2;
			theByte = src[sp];
			sp++;
			if (len > (de - dp))
				return -1;
			for (i = 0; i < len; i++) 
			{
				dest[dp] = theByte;
				dp++;
			}
		}
	}
	return 1;
}

BYTE4 decompressRLE8Data(BYTE1 *src, BYTE4 numBytes, BYTE4 stride, BYTE1 *dest, BYTE4 destSize)
{
	BYTE4 sp = 0;
	BYTE4 se = numBytes;
	BYTE4 dp = 0;
	BYTE4 de = destSize;
	BYTE4 x = 0, y = 0;
	BYTE4 i;
	while (sp < se) {
		int len = src[sp] & 0xFF;
		sp++;
		if (len == 0) {
			len = src[sp] & 0xFF;
			sp++;
			switch (len)
			{
				case 0: /* end of line */
					y++;
					x = 0;
					dp = y * stride;
					if (dp >= de)
						return -1;
					break;
				case 1: /* end of bitmap */
					return 1;
				case 2: /* delta */
					x += src[sp] & 0xFF;
					sp++;
					y += src[sp] & 0xFF;
					sp++;
					dp = y * stride + x;
					if (dp >= de)
						return -1;
					break;
				default: /* absolute mode run */
					if (len > (se - sp))
						return -1;
					if (len > (de - dp))
						return -1;
					for (i = 0; i < len; i++)
					{
						dest[dp] = src[sp];
						dp++;
						sp++;
					}
					if ((sp & 1) != 0)
						sp++; /* word align sp? */
					x += len;
					break;
			}
		} else 
		{
			BYTE1 theByte = src[sp];
			sp++;
			if (len > (de - dp))
				return -1;
			for (i = 0; i < len; i++)
			{
				dest[dp] = theByte;
				dp++;
			}
			x += len;
		}
	}
	return 1;
}

ng_err_t decompressData (BYTE1 *src, BYTE4 numBytes, BYTE1 *dest, BYTE4 destSize, BYTE4 stride, BYTE4 cmp)
{
	if (cmp == 1)
	{
		/* BMP_RLE8_COMPRESSION */
		if (decompressRLE8Data (src, numBytes, stride, dest, destSize) <= 0)
			return NgError (ERR_NG, "Error decompressRLE8Data failed");
	} else if (cmp == 2)
	{
		/* BMP_RLE4_COMPRESSION */
		if (decompressRLE4Data (src, numBytes, stride, dest, destSize) <= 0)
			return NgError (ERR_NG, "Error decompressRLE4Data failed");
	} else 
	{
		return NgError (ERR_NG, "Error decompressData failed - unsupported compression");
	}
	return ERR_OK;
}

void flipScanLines(BYTE1 *data, BYTE4 numBytes, int stride, int height)
{
	BYTE4 i1 = 0;
	BYTE4 i2 = (height - 1) * stride;
	BYTE4 i, index;
	for (i = 0; i < height / 2; i++)
	{
		for (index = 0; index < stride; index++)
		{
			BYTE1 b = data[index + i1];
			data[index + i1] = data[index + i2];
			data[index + i2] = b;
		}
		i1 += stride;
		i2 -= stride;
	}
}

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
ng_err_t NgBmpDecoderReadImage (ng_stream_t *in, ng_bitmap_image_t *image)
{
	BYTE4 *fileHeader = (BYTE4*) NgMalloc (5 * sizeof(BYTE4));
	BYTE1 *infoHeader, *data;
	BYTE4 width, height, stride, dataSize, cmp, pos;	
	BYTE2 depth;
	BYTE2 d0;
	
	NgStreamRead (in, (char *) &d0, sizeof(BYTE2));
	fileHeader[0] = (BYTE4)LittleEndianToSystemUBYTE2(d0);
	NgStreamRead (in, (char *) &fileHeader[1], sizeof(BYTE4));
	fileHeader[1] = LittleEndianToSystemUBYTE4(fileHeader[1]);
	NgStreamRead (in, (char *) &d0, sizeof(BYTE2));
	fileHeader[2] = (BYTE4)LittleEndianToSystemUBYTE2(d0);
	NgStreamRead (in, (char *) &d0, sizeof(BYTE2));
	fileHeader[3] = (BYTE4)LittleEndianToSystemUBYTE2(d0);
	NgStreamRead (in, (char *) &fileHeader[4], sizeof(BYTE4));
	fileHeader[4] = LittleEndianToSystemUBYTE4(fileHeader[4]);
	
	if (NgStreamEof (in))
	{
		NgFree (fileHeader);
		return NgError (ERR_NG, "Error invalid header file");
	}
	if (fileHeader[0] != 0x4D42)
	{
		NgFree (fileHeader);
		return NgError (ERR_NG, "Error not a BMP file");
	}
	
	infoHeader = (BYTE1*) NgMalloc (BMPHeaderFixedSize * sizeof (BYTE1));	
	NgStreamRead (in, infoHeader, BMPHeaderFixedSize * sizeof (BYTE1));
	
	if (NgStreamEof (in))
	{
		NgFree (fileHeader);
		NgFree (infoHeader);
		return NgError (ERR_NG, "Error invalid info header");
	}
	
	NgMemCpy (&width, &infoHeader[4], sizeof (BYTE4));
	width = LittleEndianToSystemUBYTE4(width);

	NgMemCpy (&height, &infoHeader[8], sizeof (BYTE4));
	height = LittleEndianToSystemUBYTE4(height);
	
	NgMemCpy (&depth, &infoHeader[14], sizeof (BYTE2));
	depth = LittleEndianToSystemUBYTE2(depth);

	stride = (width * depth + 7) / 8;
	stride = (stride + 3) / 4 * 4; /* Round up to 4 byte multiple */

	if (depth <= 8)
	{
		BYTE4 i, index;
		BYTE1 *colors;
		BYTE4 numColors;
		NgMemCpy (&numColors, &infoHeader[32], sizeof (BYTE4));
		numColors = LittleEndianToSystemUBYTE4(numColors);
		if (numColors == 0)
		{
			BYTE2 value;
			NgMemCpy (&value, &infoHeader[14], sizeof (BYTE2));
			value = LittleEndianToSystemUBYTE2(value);
			numColors = 1 << value;
		} else
		{
			if (numColors > 256)
				numColors = 256;
		}
		colors = (BYTE1*) NgMalloc (numColors * 4);
		NgStreamRead (in, colors, numColors * 4);
		
		if (NgStreamEof (in))
		{
			NgFree (fileHeader);
			NgFree (infoHeader);
			NgFree (colors);
			return NgError (ERR_NG, "Error invalid palette info");
		} 
		
		index = 0;
		
		NgBitmapImageSetSize(image, (UBYTE4)numColors, (UBYTE4)depth,
			(UBYTE4)width, (UBYTE4)height);
			
		for (i = 0; i < numColors; i++)
		{
			ng_color_map_entry_t *color_map = NgBitmapImageColorMap (image, i);
			color_map->blue = colors[index++];
			color_map->green = colors[index++];
			color_map->red = colors[index++];
			index++;
		}
		
		NgFree (colors);
	} else
	{
		/* direct - 16 and 24 bits */
		NgBitmapImageSetSize(image, 0, (UBYTE4)depth,
			(UBYTE4)width, (UBYTE4)height);
	}
	
	pos = NgStreamGetPosition (in);
	if (pos < fileHeader[4])
	{
		NgStreamSkip (in, fileHeader[4] - pos);
	}
	
	dataSize = height * stride;
	
	data = (BYTE1*)NgBitmapImageImageData(image);
	NgMemCpy (&cmp, &infoHeader[16], sizeof (BYTE4));
	cmp = LittleEndianToSystemUBYTE4(cmp);
	if (cmp == 0)
	{
		/* BMP_NO_COMPRESSION */
		BYTE4 cnt;
		cnt = NgStreamRead (in, data, dataSize);
		if (cnt != dataSize)
		{
			NgFree (fileHeader);
			NgFree (infoHeader);
			return NgError (ERR_NG, "Error failed reading uncompressed data");
		}
	} else
	{
		BYTE4 compressedSize;
		BYTE1 *compressed;
		BYTE4 cnt;
		ng_err_t res;
		NgMemCpy (&compressedSize, &infoHeader[20], sizeof (BYTE4));
		compressedSize = LittleEndianToSystemUBYTE4(compressedSize);
		compressed = (BYTE1*) NgMalloc (compressedSize * sizeof (BYTE1));
		cnt = NgStreamRead (in, compressed, compressedSize);
		if (cnt != compressedSize)
		{
			NgFree (fileHeader);
			NgFree (infoHeader);
			NgFree (compressed);
			return NgError (ERR_NG, "Error failed reading compressed data");
		}
		res = decompressData (compressed, compressedSize, data, dataSize, stride, cmp);
		if (res != ERR_OK)
		{
			NgFree (fileHeader);
			NgFree (infoHeader);
			NgFree (compressed);
			return NgError (res, "Error failed data decompression");			
		}
				
		NgFree (compressed);
	}
		
	flipScanLines(data, dataSize, stride, height);
	
	NgFree (fileHeader);
	NgFree (infoHeader);
	return ERR_OK;
}
