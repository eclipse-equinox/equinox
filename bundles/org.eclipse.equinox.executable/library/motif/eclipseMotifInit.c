/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#include "eclipseMotif.h"
#include "eclipseCommon.h"
#include <dlfcn.h>
#include <stdlib.h>

struct MOTIF_PTRS motif;

/* need to undef these so the FN_TABLE works ok */
#undef _XmStrings
#undef XtShellStrings
#undef XtStrings

/* functions from libXm */
static FN_TABLE xmFunctions[] = {	FN_TABLE_ENTRY(XmCreateDrawingArea),
									FN_TABLE_ENTRY(XmCreateLabelGadget),
									FN_TABLE_ENTRY(XmCreateMainWindow),
									FN_TABLE_ENTRY(XmCreateMessageDialog),
									FN_TABLE_ENTRY(XmMessageBoxGetChild),
									FN_TABLE_ENTRY(XmStringFree),
									FN_TABLE_ENTRY(XmStringGenerate), 
									FN_TABLE_ENTRY(_XmStrings),		/* not a function */
									{ NULL, NULL } 
						  		 };

/* functions from libXt */
static FN_TABLE xtFunctions[] = {	FN_TABLE_ENTRY(XtAddCallback),
									FN_TABLE_ENTRY(XtAppCreateShell),
									FN_TABLE_ENTRY(XtAppNextEvent),
									FN_TABLE_ENTRY(XtAppPending),
									FN_TABLE_ENTRY(XtAppProcessEvent),
									FN_TABLE_ENTRY(XtDestroyWidget),
									FN_TABLE_ENTRY(XtDispatchEvent),
									FN_TABLE_ENTRY(XtGetValues),
#ifndef AIX
									FN_TABLE_ENTRY(XtInitialize),
#endif
									FN_TABLE_ENTRY(XtIsManaged),
									FN_TABLE_ENTRY(XtManageChild),
									FN_TABLE_ENTRY(XtMapWidget),
									FN_TABLE_ENTRY(XtPopup),
									FN_TABLE_ENTRY(XtRealizeWidget),
									FN_TABLE_ENTRY(XtSetLanguageProc),
									FN_TABLE_ENTRY(XtSetMappedWhenManaged),
									FN_TABLE_ENTRY(XtSetValues),
									FN_TABLE_ENTRY(XtUnmanageChild),
									FN_TABLE_ENTRY(XtWidgetToApplicationContext),
									FN_TABLE_ENTRY(XtWindowOfObject),
									FN_TABLE_ENTRY(XtShellStrings),				 /* not a function */
									FN_TABLE_ENTRY(XtStrings),					 /* not a function */
									FN_TABLE_ENTRY(applicationShellWidgetClass), /* not a function */
									{ NULL, NULL } 
								};

#ifdef AIX
static FN_TABLE shimFunctions[] = { FN_TABLE_ENTRY(eclipseXtInitialize), {NULL, NULL} };
#endif

/* functions from libX11 */
static FN_TABLE x11Functions[] = {	FN_TABLE_ENTRY(XDefaultScreenOfDisplay),
									FN_TABLE_ENTRY(XFree),
									FN_TABLE_ENTRY(XFlush),
									FN_TABLE_ENTRY(XGetGeometry),
									FN_TABLE_ENTRY(XMapWindow),
									{ NULL, NULL } 
								};

#ifndef NO_XINERAMA_EXTENSIONS
static FN_TABLE xinFunctions[] = {	FN_TABLE_ENTRY(XineramaIsActive),
									FN_TABLE_ENTRY(XineramaQueryScreens),
									{ NULL, NULL }
								 };
#endif

static int loadMotifSymbols( void * library, FN_TABLE * table) {
	int i = 0;
	void * fn;
	for (i = 0; table[i].fnName != NULL; i++) {
		fn = findSymbol(library, table[i].fnName);
		if (fn != 0) {
			*(table[i].fnPtr) = fn;
		} else {
			*(table[i].fnPtr) = 0;
			return -1;
		}
	}
	return 0;
}

#ifdef AIX
void * loadMotifShimLibrary() {
	if (eclipseLibrary != NULL) {
		/* library is the normal eclipse_<ver>.so, look for libeclipse-motif.so beside it */
		_TCHAR* eclipseMotifLib = _T_ECLIPSE("libeclipse-motif.so");
		_TCHAR* path = strdup(eclipseLibrary);
		_TCHAR* c = strrchr(path, '/');
		if (c == NULL) {
			free(path);
			return NULL;
		}

		*c = 0;
		c = malloc((strlen(path) + 2 + strlen(eclipseMotifLib)) * sizeof(char));
		_stprintf(c, _T_ECLIPSE("%s/%s"), path, eclipseMotifLib);
		free(path);
		return dlopen(c, RTLD_LAZY);
	}
	return 0;
}
#endif

int loadMotif() {
	void * xmLib = NULL, *xtLib = NULL, *x11Lib = NULL, *xinLib = NULL;
#ifdef AIX
	void * motifShim = NULL;
#endif
	char * path = getProgramDir();
	int dlFlags = RTLD_LAZY;
	
	/* initialize ptr struct to 0's */
	memset(&motif, 0, sizeof(struct MOTIF_PTRS));

#ifndef AIX	
	if (path != NULL) {
		/* look for libXm first in the root of eclipse */
		char * lib = malloc((strlen(path) + strlen(_T_ECLIPSE(XM_LIB)) + 2) * sizeof(char));
		sprintf( lib, "%s%c%s", path, dirSeparator, XM_LIB);
		xmLib = dlopen(lib, dlFlags);
		free(lib);
	}
#else
	dlFlags |= RTLD_MEMBER;
	motifShim = loadMotifShimLibrary();
	if (motifShim == NULL)
		return -1;
#endif

	if (xmLib == NULL) {
		xmLib = dlopen(XM_LIB, dlFlags);
	}
	
	if (xmLib == NULL) {
		/* bail now, don't load the others, libXm must be loaded first, so leave things for
		 * swt to do later */
		return -1;
	}
	
	xtLib = dlopen(XT_LIB, dlFlags);
	x11Lib = dlopen(X11_LIB, dlFlags);
			
	/* printf("XmLib: %s: %x\nXtLib: %s: %x\nX11Lib:%s, %x\n", XM_LIB, xmLib, XT_LIB, xtLib, X11_LIB, x11Lib);*/
#ifndef NO_XINERAMA_EXTENSIONS
	/* don't fail without Xinerama */
	xinLib = dlopen(XIN_LIB, dlFlags);
	if (xinLib != NULL)
		loadMotifSymbols(xinLib, xinFunctions);
#endif
	if( xtLib == NULL || x11Lib == NULL)
		return -1;

	if (loadMotifSymbols(xmLib, xmFunctions)  != 0) return -1;
	if (loadMotifSymbols(xtLib, xtFunctions)  != 0) return -1;
	if (loadMotifSymbols(x11Lib, x11Functions)  != 0) return -1;
#ifdef AIX
	if (loadMotifSymbols(motifShim, shimFunctions) !=0) return -1;
#endif

	return 0;
}
