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

struct GTK_PTRS gtk = { 1 }; /* initialize the first field "not_initialized" so we can tell when we've loaded the pointers */

/* tables to help initialize the function pointers */
/* functions from libgtk-x11-2.0 */
static FN_TABLE gtkFunctions[] = { 	FN_TABLE_ENTRY(gtk_adjustment_new),
									FN_TABLE_ENTRY(gtk_box_set_child_packing),
									FN_TABLE_ENTRY(gtk_container_add),
									FN_TABLE_ENTRY(gtk_dialog_run),
									FN_TABLE_ENTRY(gtk_fixed_new),
									FN_TABLE_ENTRY(gtk_fixed_set_has_window),
									FN_TABLE_ENTRY(gtk_image_new_from_pixbuf),
									FN_TABLE_ENTRY(gtk_init_check),
									FN_TABLE_ENTRY(gtk_message_dialog_new),
									FN_TABLE_ENTRY(gtk_scrolled_window_set_policy),
									FN_TABLE_ENTRY(gtk_scrolled_window_new),
									FN_TABLE_ENTRY(gtk_set_locale),
									FN_TABLE_ENTRY(gtk_signal_connect_full),
									FN_TABLE_ENTRY(gtk_vbox_new),
									FN_TABLE_ENTRY(gtk_widget_destroy),
									FN_TABLE_ENTRY(gtk_widget_destroyed),
									FN_TABLE_ENTRY(gtk_widget_show_all),
									FN_TABLE_ENTRY(gtk_window_new),
									FN_TABLE_ENTRY(gtk_window_resize),
									FN_TABLE_ENTRY(gtk_window_set_title),
									FN_TABLE_ENTRY(gtk_window_set_decorated),
									FN_TABLE_ENTRY(gtk_window_set_position),
									{ NULL, NULL }
								 };
/* functions from libgdk-x11-2.0 */
static FN_TABLE gdkFunctions[] = {	FN_TABLE_ENTRY(gdk_set_program_class), 
									FN_TABLE_ENTRY(gdk_display), /* not a function */
									{ NULL, NULL } 
						  		 };
/* functions from libgdk_pixbuf-2.0 */
static FN_TABLE pixFunctions[] = { 	FN_TABLE_ENTRY(gdk_pixbuf_new_from_file),
									FN_TABLE_ENTRY(gdk_pixbuf_get_width),
									FN_TABLE_ENTRY(gdk_pixbuf_get_height),
									{ NULL, NULL }
						  		 };
/* functions from libgobject-2.0 */
static FN_TABLE gobjFunctions[] = {	FN_TABLE_ENTRY(g_log_set_handler),
									FN_TABLE_ENTRY(g_log_remove_handler),
									FN_TABLE_ENTRY(g_main_context_iteration),
									FN_TABLE_ENTRY(g_object_unref),
									FN_TABLE_ENTRY(g_timeout_add),
#ifdef SOLARIS
									FN_TABLE_ENTRY(g_string_insert_c),
#endif
									{ NULL, NULL }
						   		  };

/* functions from libX11 */
static FN_TABLE x11Functions[] = {	FN_TABLE_ENTRY(XGetSelectionOwner),
									FN_TABLE_ENTRY(XSetSelectionOwner),
									FN_TABLE_ENTRY(XCreateWindow),
									FN_TABLE_ENTRY(XChangeProperty),
									FN_TABLE_ENTRY(XSync),
									FN_TABLE_ENTRY(XRootWindow),
									FN_TABLE_ENTRY(XDefaultScreen),
									FN_TABLE_ENTRY(XInternAtom),
									{ NULL, NULL } 
								};


static int loadGtkSymbols( void * library, FN_TABLE * table) {
	int i = 0;
	void * fn;
	for (i = 0; table[i].fnName != NULL; i++) {
		fn = findSymbol(library, table[i].fnName);
		if (fn != 0)
			*(table[i].fnPtr) = fn;
		else
			return -1;
	}
	return 0;
}

int loadGtk() {
#ifdef AIX 
#define DLFLAGS RTLD_LAZY | RTLD_MEMBER
#else
#define DLFLAGS RTLD_LAZY
#endif

	void * objLib = dlopen(GOBJ_LIB, DLFLAGS);
	void * gdkLib = dlopen(GDK_LIB, DLFLAGS);
	void * pixLib = dlopen(PIXBUF_LIB, DLFLAGS);
	void * gtkLib = dlopen(GTK_LIB, DLFLAGS);
	void * x11Lib = dlopen(X11_LIB, DLFLAGS);
	
	/* initialize ptr struct to 0's */
	memset(&gtk, 0, sizeof(struct GTK_PTRS));
	
	if ( gtkLib == NULL || loadGtkSymbols(gtkLib, gtkFunctions)  != 0) return -1;
	if ( gdkLib == NULL || loadGtkSymbols(gdkLib, gdkFunctions)  != 0) return -1;
	if ( pixLib == NULL || loadGtkSymbols(pixLib, pixFunctions)  != 0) return -1;
	if ( objLib == NULL || loadGtkSymbols(objLib, gobjFunctions) != 0) return -1;
	if ( x11Lib == NULL || loadGtkSymbols(x11Lib, x11Functions) != 0) return -1;
	
	return 0;
}
