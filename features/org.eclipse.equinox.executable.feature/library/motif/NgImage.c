/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#include "NgCommon.h"
#include "NgImageData.h"
#include "NgImage.h"
#include "NgWinBMPFileFormat.h"
#include <dlfcn.h>

struct NG_PTRS ng;

#define FN_TABLE_ENTRY(fn) { (void**)&ng.fn, #fn } 
typedef struct {
	void ** fnPtr;
	char * fnName;
} FN_TABLE;
static FN_TABLE x11Functions[] = {	FN_TABLE_ENTRY(XCreateGC),
									FN_TABLE_ENTRY(XCreateImage),
									FN_TABLE_ENTRY(XCreatePixmap),		        
									FN_TABLE_ENTRY(XDefaultColormap),
									FN_TABLE_ENTRY(XDefaultDepthOfScreen),
									FN_TABLE_ENTRY(XDefaultRootWindow),
									FN_TABLE_ENTRY(XDefaultScreen),
									FN_TABLE_ENTRY(XDefaultScreenOfDisplay),
									FN_TABLE_ENTRY(XDefaultVisual),
									FN_TABLE_ENTRY(XFreeGC),
									FN_TABLE_ENTRY(XFreePixmap),
									FN_TABLE_ENTRY(XPutImage),
									FN_TABLE_ENTRY(XQueryColor),
									{ NULL, NULL }
								};

static FN_TABLE xtFunctions[] = {	FN_TABLE_ENTRY(XtMalloc), {NULL, NULL} };

int NGImageInit() {
	int i = 0;
	void * fn;
#ifdef AIX
	void * x11Lib = dlopen(X11_LIB, RTLD_LAZY | RTLD_MEMBER);
	void * xtLib = dlopen(XT_LIB, RTLD_LAZY | RTLD_MEMBER);
#else
	void * x11Lib = dlopen(X11_LIB, RTLD_LAZY);
	void * xtLib = dlopen(XT_LIB, RTLD_LAZY);
#endif		
	/* initialize ptr struct to 0's */
	memset(&ng, 0, sizeof(struct NG_PTRS));
	

	if (x11Lib == NULL || xtLib == NULL)
		return -1;
	
	for (i = 0; x11Functions[i].fnName != NULL; i++) {
		fn = dlsym(x11Lib, x11Functions[i].fnName);
		if (fn != 0)
			*(x11Functions[i].fnPtr) = fn;
		else
			return -1;
	}
	
	for (i = 0; xtFunctions[i].fnName != NULL; i++) {
		fn = dlsym(xtLib, xtFunctions[i].fnName);
		if (fn != 0)
			*(xtFunctions[i].fnPtr) = fn;
		else
			return -1;
	}
	return 0;
}
/**
 * Return the nbr of entries in the default color palette
 */
int getNbrColorsXPalette(Display *xDisplay)
{
	Visual *visual = ng.XDefaultVisual (xDisplay, ng.XDefaultScreen(xDisplay));
	return visual->map_entries;
}

/**
 * Return the RGB codes of the default palette
 * palette: buffer of size numColors * 3, holding the RGB values
 */
ng_err_t getXPalette (Display *xDisplay, int numColors, char* palette)
{
	XColor color;
	int i;
	int index = 0;
	int colormap = ng.XDefaultColormap (xDisplay, ng.XDefaultScreen(xDisplay));
	for (i = 0; i < numColors; i++)
	{
		color.pixel = i;
		ng.XQueryColor (xDisplay, colormap, &color);
		palette[index++] = ((color.red >> 8) & 0xFF);
		palette[index++] = ((color.green >> 8) & 0xFF);
		palette[index++] = ((color.blue >> 8) & 0xFF);
	}
	return ERR_OK;
}

/**
 * Put a device-independent image of any depth into a drawable of the same size, 
 */
ng_err_t putImage(ng_bitmap_image_t *image, int srcX, int srcY, int srcWidth, int srcHeight, int destX, int destY, 
	Display *xDisplay, Visual *visual, int screenDepth, 
	int drawable) 
{

	XImage *xImagePtr;
	int bufSize;
	int destRedMask = 0, destGreenMask = 0, destBlueMask = 0;
	BYTE1 screenDirect;
	UBYTE1 *srcData = NgBitmapImageImageData(image);
	UBYTE4 srcDepth = NgBitmapImageBitCount(image);
	BYTE4 sbpp, dbpp;
	GC tempGC;
	int numColors = 0;
	
	/* We only support image depth of 24 bits */
	if (srcDepth != 24) return NgError (ERR_NG, "Error unsupported depth - only support 24 bit");
	if (screenDepth <= 8) 
	{
		numColors = getNbrColorsXPalette (xDisplay);
		if (numColors == 0)
			return NgError (ERR_NG, "Error pseudo-color mode detected, no colors available");
		numColors = 1 << ng.XDefaultDepthOfScreen (ng.XDefaultScreenOfDisplay (xDisplay));
		screenDirect = 0;
	} else
	{
		destRedMask = visual->red_mask;
		destGreenMask = visual->green_mask;
		destBlueMask = visual->blue_mask;
		screenDirect = 1;
	}
	
	xImagePtr = ng.XCreateImage(xDisplay, visual, screenDepth, ZPixmap, 0, 0, srcWidth, srcHeight, 32, 0);
	if (xImagePtr == NULL) return NgError (ERR_NG, "Error XCreateImage failed");
	bufSize = xImagePtr->bytes_per_line * srcHeight;

	xImagePtr->data = (char*) ng.XtMalloc (bufSize);
	sbpp = NgBitmapImageBytesPerRow(image);
	dbpp = xImagePtr->bytes_per_line;
	
	if (screenDirect)
	{
		/* 24 bit source to direct screen destination */
		NgBitmapImageBlitDirectToDirect(srcData, sbpp, srcWidth, srcHeight,
			(UBYTE1*)xImagePtr->data, xImagePtr->bits_per_pixel, dbpp, xImagePtr->byte_order,
			destRedMask, destGreenMask, destBlueMask);
	} else
	{
		/* 24 bit source to palette screen destination */
		char *palette = (char*) NgMalloc (numColors * 3);
		getXPalette (xDisplay, numColors, palette);
		NgBitmapImageBlitDirectToPalette(srcData, sbpp, srcWidth, srcHeight,
			(UBYTE1*)xImagePtr->data, xImagePtr->bits_per_pixel, dbpp, xImagePtr->byte_order,
			(UBYTE1*)palette, numColors);
		NgFree (palette);
	}
	
	tempGC = ng.XCreateGC (xDisplay, drawable, 0, NULL);	
	ng.XPutImage(xDisplay, drawable, tempGC, xImagePtr, 0, 0, 0, 0, srcWidth, srcHeight);
	
	XDestroyImage (xImagePtr);
	ng.XFreeGC (xDisplay, tempGC);
	return ERR_OK;
}

ng_err_t init(ng_bitmap_image_t *image, Display *xDisplay, int screenDepth, int drawable, Pixmap *pixmap) 
{
	ng_err_t err;
	int width = (int)NgBitmapImageWidth(image);
	int height = (int)NgBitmapImageHeight(image);
	
	Visual *visual = ng.XDefaultVisual(xDisplay, ng.XDefaultScreen(xDisplay));
	*pixmap = ng.XCreatePixmap(xDisplay, drawable, width, height, screenDepth);
	if (*pixmap == 0) 
	{
		return NgError (ERR_NG, "Error XCreatePixmap failed");
	}
	err = putImage(image, 0, 0, width, height, 0, 0, xDisplay, visual, screenDepth, *pixmap);
	if (err != ERR_OK) 
	{
		ng.XFreePixmap (xDisplay, *pixmap);
		return NgError (err, "Error putImage failed");
	}
	
	return ERR_OK;
}

/**
 * loadBMPImage
 * Create a pixmap representing the given BMP file, for the specified display and screen.
 *
 * display: connection to X server
 * screen: the screen to create the pixmap for
 * bmpPathname: absolute path and name to the bmp file
 *
 * returned value: the pixmap newly created if successful. 0 otherwise.
 */
Pixmap loadBMPImage (Display *display, Screen *screen, char *bmpPathname) {
	Window drawable;
	ng_stream_t in;
	ng_bitmap_image_t image;
	ng_err_t err = ERR_OK;
	int screenDepth;
	Pixmap pixmap;

	/* this must be called before any X functions are used */
	NGImageInit();
	NgInit();
	
	drawable = ng.XDefaultRootWindow (display);
	screenDepth = ng.XDefaultDepthOfScreen (screen);
	
	if (NgStreamInit (&in, bmpPathname) != ERR_OK)
	{
		NgError (ERR_NG, "Error can't open BMP file");
		return 0;
	}	
	NgBitmapImageInit (&image);
	err = NgBmpDecoderReadImage (&in, &image);
	NgStreamClose (&in);
	
	if (err != ERR_OK)
	{
		NgBitmapImageFree (&image);
		return 0;
	}
		
	err = init (&image, display, screenDepth, drawable, &pixmap);
	NgBitmapImageFree (&image);
	
	return err == ERR_OK ? pixmap : 0;
}

const char *getBMPErrorMessage ()
{
	return NgGetLastErrorMsg ();
}
