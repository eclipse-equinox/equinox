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

#ifndef __NG_IMAGE_H
#define __NG_IMAGE_H

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xos.h>
#include <X11/Intrinsic.h>

struct NG_PTRS {
	GC 			(*XCreateGC)			(Display*, Drawable, unsigned long, XGCValues*);
	XImage *	(*XCreateImage)			(Display*, Visual*, unsigned int, int, int, char*, unsigned int, unsigned int, int, int);
	Pixmap 		(*XCreatePixmap)		(Display*, Drawable, unsigned int, unsigned int, unsigned int);		        
	Colormap 	(*XDefaultColormap)		(Display*, int);
	int 		(*XDefaultDepthOfScreen)(Screen*);
	Window 		(*XDefaultRootWindow)	(Display*);
	int 		(*XDefaultScreen)		(Display*);
	Screen *	(*XDefaultScreenOfDisplay)(Display*);
	Visual *	(*XDefaultVisual)		(Display*, int);
	int 		(*XFreeGC)				(Display*, GC);
	int 		(*XFreePixmap)			(Display*, Pixmap);
	int 		(*XPutImage)			(Display*, Drawable, GC, XImage*, int, int, int, int, unsigned int, unsigned int);
	int 		(*XQueryColor)			(Display*, Colormap, XColor*);
	char *		(*XtMalloc)				(Cardinal);
};

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
Pixmap loadBMPImage (Display *display, Screen *screen, char *bmpPathname);

/**
 * Return error message describing why the BMP file could not be displayed
 */
const char *getBMPErrorMessage();

#endif /* NG_IMAGE_H */
