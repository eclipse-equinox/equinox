/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#ifndef ECLIPSE_MOTIF_H
#define ECLIPSE_MOTIF_H

#include <Xm/XmAll.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/IntrinsicP.h>
#include <X11/Intrinsic.h>
#include <X11/Shell.h>

#ifndef NO_XINERAMA_EXTENSIONS
#include <X11/extensions/Xinerama.h>
#endif

struct MOTIF_PTRS {
#ifndef NO_XINERAMA_EXTENSIONS
	Bool 		(*XineramaIsActive)		(Display*);
	XineramaScreenInfo*  (*XineramaQueryScreens) (Display*, int*);
#endif
	Widget 		(*XmCreateDrawingArea)	(Widget, String, ArgList, Cardinal);
	Widget 		(*XmCreateLabelGadget)	(Widget, char *, Arg *, Cardinal);
	Widget 		(*XmCreateMainWindow)	(Widget, char *, ArgList, Cardinal);
	Widget		(*XmCreateMessageDialog)(Widget, String, ArgList, Cardinal);
	Widget		(*XmMessageBoxGetChild)	(Widget, unsigned char);
	void 		(*XmStringFree)			(XmString);
	XmString 	(*XmStringGenerate) 	(XtPointer, XmStringTag, XmTextType, XmStringTag);
	
	void 		(*XtAddCallback)		(Widget, String, XtCallbackProc, XtPointer);
	Widget 		(*XtAppCreateShell)		(String, String, WidgetClass, Display*, ArgList, Cardinal);
	void		(*XtAppNextEvent)		(XtAppContext, XEvent*);
	XtInputMask (*XtAppPending)			(XtAppContext);
	void 		(*XtAppProcessEvent)	(XtAppContext, XtInputMask);
	void		(*XtDestroyWidget)		(Widget);
	Boolean		(*XtDispatchEvent)		(XEvent*);
	void		(*XtGetValues)			(Widget, ArgList, Cardinal);
	Widget		(*XtInitialize)			(String, String, XrmOptionDescRec*, Cardinal, int*, char**);
#ifdef AIX
	Widget		(*eclipseXtInitialize)	(String, String, XrmOptionDescRec*, Cardinal, int*, char**);
#endif
	Boolean		(*XtIsManaged)			(Widget);
	void 		(*XtManageChild)		(Widget);
	int			(*XtMapWidget)			(Widget);
	void 		(*XtPopup)				(Widget, XtGrabKind);
	void		(*XtRealizeWidget)		(Widget);
	Widget		(*XtSetLanguageProc)	(XtAppContext, XtLanguageProc, XtPointer);
	void 		(*XtSetMappedWhenManaged)(Widget, Boolean);
	void		(*XtSetValues)			(Widget, ArgList, Cardinal);
	void		(*XtUnmanageChild)		(Widget);
	XtAppContext (*XtWidgetToApplicationContext) (Widget);
	Window		(*XtWindowOfObject)		(Widget);
	
	Screen * 	(*XDefaultScreenOfDisplay)(Display*);
	int			(*XFree)				(void*);
	int			(*XFlush)				(Display*);
	Status 		(*XGetGeometry)			(Display*, Drawable, Window*, int*, int*, unsigned int*, unsigned int*, unsigned int*, unsigned int*);
	int 		(*XMapWindow)			(Display*, Window);
	
	char * 		_XmStrings;
	char * 		XtShellStrings;
	char * 		XtStrings;
	WidgetClass *applicationShellWidgetClass;
};

extern struct MOTIF_PTRS motif;

#define motif_XtDisplay 			XtDisplay
#define motif_XtSetArg				XtSetArg
#define motif_XtWindow				XtWindow
#define motif_XtIsTopLevelShell 	XtIsTopLevelShell
#define motif_XtIsRealized(object) (motif.XtWindowOfObject(object) != None)
#define motif_XtMapWidget(widget)  motif.XMapWindow(XtDisplay(widget), XtWindow(widget))

#define _XmStrings 				motif._XmStrings
#define XtShellStrings 			motif.XtShellStrings
#define XtStrings 				motif.XtStrings

/* macro resolves to { (void**)&motif.foo, "foo" }, use it to initialize FN_TABLEs */
#define FN_TABLE_ENTRY(fn) { (void**)&motif.fn, #fn } 
typedef struct {
	void ** fnPtr;
	char * fnName;
} FN_TABLE;

extern int loadMotif();
#endif
