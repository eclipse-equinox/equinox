/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

#include "eclipseGtk.h"
#include "eclipseCommon.h"
#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>

struct GTK_PTRS gtk = { 1 }; /* initialize the first field "not_initialized" so we can tell when we've loaded the pointers */

/* tables to help initialize the function pointers */
/* functions from libgtk-x11-2.0 or libgtk-3.so.0*/
static FN_TABLE gtkFunctions[] = {
	FN_TABLE_ENTRY(gtk_container_add, 1),
	FN_TABLE_ENTRY(gtk_dialog_run, 1),
	FN_TABLE_ENTRY(gtk_image_new_from_pixbuf, 1),
	FN_TABLE_ENTRY(gtk_init_check, 1),
	FN_TABLE_ENTRY(gtk_init_with_args, 0),
	FN_TABLE_ENTRY(gtk_message_dialog_new, 1),
	FN_TABLE_ENTRY(gtk_set_locale, 0),
	FN_TABLE_ENTRY(gtk_widget_destroy, 1),
	FN_TABLE_ENTRY(gtk_widget_destroyed, 1),
	FN_TABLE_ENTRY(gtk_widget_show_all, 1),
	FN_TABLE_ENTRY(gtk_window_new, 1),
	FN_TABLE_ENTRY(gtk_window_resize, 1),
	FN_TABLE_ENTRY(gtk_window_set_title, 1),
	FN_TABLE_ENTRY(gtk_window_set_decorated, 1),
	FN_TABLE_ENTRY(gtk_window_set_type_hint, 1),
	FN_TABLE_ENTRY(gtk_window_set_position, 1),
	{ NULL, NULL }
};
/* functions from libgdk-x11-2.0 or libgdk-3.so.0*/
static FN_TABLE gdkFunctions[] = {
	FN_TABLE_ENTRY(gdk_set_program_class, 1),
	FN_TABLE_ENTRY(gdk_display_get_default, 1),
	FN_TABLE_ENTRY(gdk_x11_display_get_xdisplay, 1),
	{ NULL, NULL }
};
/* functions from libgdk_pixbuf-2.0 */
static FN_TABLE pixFunctions[] = {
	FN_TABLE_ENTRY(gdk_pixbuf_new_from_file, 1),
	FN_TABLE_ENTRY(gdk_pixbuf_get_width, 1),
	FN_TABLE_ENTRY(gdk_pixbuf_get_height, 1),
	{ NULL, NULL }
};
/* functions from libgobject-2.0 */
static FN_TABLE gobjFunctions[] = {
	FN_TABLE_ENTRY(g_signal_connect_data, 1),
	FN_TABLE_ENTRY(g_main_context_iteration, 1),
	FN_TABLE_ENTRY(g_object_unref, 1),
	FN_TABLE_ENTRY(g_timeout_add, 1),
	FN_TABLE_ENTRY(g_error_free, 1),
#ifdef SOLARIS
	FN_TABLE_ENTRY(g_string_insert_c, 1),
#endif
	{ NULL, NULL }
};

/* functions from libX11 */
static FN_TABLE x11Functions[] = {
	FN_TABLE_ENTRY(XGetSelectionOwner, 1),
	FN_TABLE_ENTRY(XSetSelectionOwner, 1),
	FN_TABLE_ENTRY(XCreateWindow, 1),
	FN_TABLE_ENTRY(XChangeProperty, 1),
	FN_TABLE_ENTRY(XSync, 1),
	FN_TABLE_ENTRY(XRootWindow, 1),
	FN_TABLE_ENTRY(XDefaultScreen, 1),
	FN_TABLE_ENTRY(XInternAtom, 1),
	{ NULL, NULL }
};


static int loadGtkSymbols( void * library, FN_TABLE * table) {
	int i = 0;
	void * fn;
	for (i = 0; table[i].fnName != NULL; i++) {
		fn = findSymbol(library, table[i].fnName);
		if (fn != 0) {
			*(table[i].fnPtr) = fn;
		} else {
			if (table[i].required) return -1;
		}
	}
	return 0;
}

int loadGtk() {
#ifdef AIX 
#define DLFLAGS RTLD_LAZY | RTLD_MEMBER
#else
#define DLFLAGS RTLD_LAZY
#endif

	char *overlayScrollbar = getenv("LIBOVERLAY_SCROLLBAR");
	if (overlayScrollbar == NULL) {
		setenv("LIBOVERLAY_SCROLLBAR", "0", 0);
	}

	void *gdkLib = NULL, *gtkLib = NULL, *objLib = NULL, *pixLib = NULL, *x11Lib = NULL;
	
	char *gtk3 = getenv("SWT_GTK3");
	if (gtk3 == NULL || strcmp(gtk3,"1") == 0) {
		gdkLib = dlopen(GDK3_LIB, DLFLAGS);
		gtkLib = dlopen(GTK3_LIB, DLFLAGS);
		if (gtkLib != NULL) {
			const char * (*func)(int, int, int);
			dlerror();
			*(void**) (&func) = dlsym(gtkLib, "gtk_check_version");
			if (dlerror() == NULL && func) {
				const char *check = (*func)(3, 9, 0);
				if (check == NULL) {
					dlclose(gdkLib);
					dlclose(gtkLib);
					gdkLib = gtkLib = NULL;
					setenv("SWT_GTK3","0",1);
				}
			}
		}
	}
	if (!gtkLib || !gdkLib) {
		gdkLib = dlopen(GDK_LIB, DLFLAGS);
		gtkLib = dlopen(GTK_LIB, DLFLAGS);
		setenv("SWT_GTK3","0",1);
	}
	objLib = dlopen(GOBJ_LIB, DLFLAGS);
	pixLib = dlopen(PIXBUF_LIB, DLFLAGS);
	x11Lib = dlopen(X11_LIB, DLFLAGS);
	
	/* initialize ptr struct to 0's */
	memset(&gtk, 0, sizeof(struct GTK_PTRS));
	
	if ( gtkLib == NULL || loadGtkSymbols(gtkLib, gtkFunctions)  != 0) return -1;
	if ( gdkLib == NULL || loadGtkSymbols(gdkLib, gdkFunctions)  != 0) return -1;
	if ( pixLib == NULL || loadGtkSymbols(pixLib, pixFunctions)  != 0) return -1;
	if ( objLib == NULL || loadGtkSymbols(objLib, gobjFunctions) != 0) return -1;
	if ( x11Lib == NULL || loadGtkSymbols(x11Lib, x11Functions) != 0) return -1;
	
	return 0;
}
