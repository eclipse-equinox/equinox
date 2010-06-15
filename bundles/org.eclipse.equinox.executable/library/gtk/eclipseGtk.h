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
#ifndef ECLIPSE_GTK_H
#define ECLIPSE_GTK_H

#include <gtk/gtk.h>
#include <gdk-pixbuf/gdk-pixbuf.h>
#include <gdk/gdkx.h>

struct GTK_PTRS { 
	short not_initialized;
	GtkObject*	(*gtk_adjustment_new)		(gdouble, gdouble, gdouble, gdouble, gdouble, gdouble);
	void		(*gtk_box_set_child_packing)(GtkBox*, GtkWidget*, gboolean, gboolean, guint, GtkPackType);
	void		(*gtk_container_add)		(GtkContainer*, GtkWidget*);
	gint		(*gtk_dialog_run)			(GtkDialog *);
	GtkWidget*	(*gtk_fixed_new)			();
	void		(*gtk_fixed_set_has_window)	(GtkFixed *, gboolean);
	GtkWidget*	(*gtk_image_new_from_pixbuf)(GdkPixbuf*);
	gboolean	(*gtk_init_check)			(int*, char***);
	GtkWidget*	(*gtk_message_dialog_new)	(GtkWindow*, GtkDialogFlags, GtkMessageType, GtkButtonsType, const gchar*, ...);
	void		(*gtk_scrolled_window_set_policy)(GtkScrolledWindow*, GtkPolicyType, GtkPolicyType);
	GtkWidget*	(*gtk_scrolled_window_new)	(GtkAdjustment*, GtkAdjustment*);
	gchar*		(*gtk_set_locale)			();
	gulong 		(*gtk_signal_connect_full)	(GtkObject*, const gchar*, GtkSignalFunc, GtkCallbackMarshal, gpointer, GtkDestroyNotify, gint, gint);
	GtkWidget*	(*gtk_vbox_new)				(gboolean, gint);
	void		(*gtk_widget_destroy)		(GtkWidget*);
	void		(*gtk_widget_destroyed)		(GtkWidget*, GtkWidget**);
	void		(*gtk_widget_show_all)		(GtkWidget*);
	GtkWidget*	(*gtk_window_new)			(GtkWindowType);
	void		(*gtk_window_resize)		(GtkWindow*, gint, gint);
	void		(*gtk_window_set_title)		(GtkWindow*, const gchar*);
	void		(*gtk_window_set_decorated)	(GtkWindow*, gboolean);
	void		(*gtk_window_set_position)	(GtkWindow*, GtkWindowPosition);
	 guint		(*g_log_set_handler)		(const gchar*, GLogLevelFlags, GLogFunc, gpointer);
	void		(*g_log_remove_handler)		(const gchar*, guint);
	gboolean	(*g_main_context_iteration)	(GMainContext*, gboolean);
	void		(*g_object_unref)			(gpointer);
	GObject*	(*g_object_new)				(GType, const gchar*, ...);
	guint       (*g_timeout_add)			(guint, GSourceFunc, gpointer);

#ifdef SOLARIS
	GString* 	(*g_string_insert_c) 		(GString *, gssize, gchar);
#endif	
		
	GdkPixbuf*	(*gdk_pixbuf_new_from_file)	(const char*, GError **);
	int			(*gdk_pixbuf_get_width)		(const GdkPixbuf*);
	int			(*gdk_pixbuf_get_height)	(const GdkPixbuf*);
	void		(*gdk_set_program_class)	(const char*);
	
	Window 		(*XGetSelectionOwner)		(Display*, Atom);
	void		(*XSetSelectionOwner)		(Display*, Atom, Window, Time);
	void 		(*XChangeProperty)			(Display*, Window, Atom, Atom, int, int, unsigned char *, int);
	Window 		(*XCreateWindow)			(Display*, Window, int, int, unsigned int, unsigned int, unsigned int, int, unsigned int, Visual*, unsigned long, XSetWindowAttributes*);
	void		(*XSync)					(Display*, Bool);
	int			(*XDefaultScreen)			(Display*);
	Window		(*XRootWindow)				(Display*, int);
	Atom 			(*XInternAtom)					(Display*, _Xconst char*, Bool	);
	Display          **gdk_display;
};

#define gtk_GDK_DISPLAY *(gtk.gdk_display)
extern struct GTK_PTRS gtk;

#define FN_TABLE_ENTRY(fn) { (void**)& gtk.fn, #fn } 
typedef struct {
	void ** fnPtr;
	char * fnName;
} FN_TABLE;

/* load the gtk libraries and initialize the function pointers */
extern int loadGtk();

#endif
