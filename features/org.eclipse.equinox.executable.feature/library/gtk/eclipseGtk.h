/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at 
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	short 		not_initialized;
	void		(*gtk_container_add)		(GtkContainer*, GtkWidget*);
	gint		(*gtk_dialog_run)			(GtkDialog *);
	GtkWidget*	(*gtk_image_new_from_pixbuf)(GdkPixbuf*);
	gboolean	(*gtk_init_with_args)		(int*, char***, const char *, void *, const char *, GError **);
	GtkWidget*	(*gtk_message_dialog_new)	(GtkWindow*, GtkDialogFlags, GtkMessageType, GtkButtonsType, const gchar*, ...);
	void		(*gtk_widget_destroy)		(GtkWidget*);
	void		(*gtk_widget_destroyed)		(GtkWidget*, GtkWidget**);
	void		(*gtk_widget_show_all)		(GtkWidget*);
	GtkWidget*	(*gtk_window_new)			(GtkWindowType);
	void		(*gtk_window_resize)		(GtkWindow*, gint, gint);
	void		(*gtk_window_set_title)		(GtkWindow*, const gchar*);
	void		(*gtk_window_set_decorated)	(GtkWindow*, gboolean);
	void		(*gtk_window_set_type_hint)	(GtkWindow*, int);
	void		(*gtk_window_set_position)	(GtkWindow*, GtkWindowPosition);

	gulong 		(*g_signal_connect_data)	(gpointer, const gchar*, GCallback, gpointer, GClosureNotify, GConnectFlags);
	gboolean	(*g_main_context_iteration)	(GMainContext*, gboolean);
	void		(*g_object_unref)			(gpointer);
	guint       (*g_timeout_add)			(guint, GSourceFunc, gpointer);
	void		(*g_error_free)				(GError *);
	void		(*g_type_init)            	();
	GDBusProxy*	(*g_dbus_proxy_new_for_bus_sync) (GBusType, GDBusProxyFlags, GDBusInterfaceInfo *, const gchar *,const gchar *, const gchar *, GCancellable *, GError **);
	GVariant *  (*g_dbus_proxy_call_sync) (GDBusProxy *, const gchar *, GVariant *, GDBusCallFlags, gint, GCancellable *, GError **);
	GVariantBuilder * (*g_variant_builder_new) (const GVariantType *);
	void 		(*g_variant_builder_add) 	(GVariantBuilder *, const gchar *, const gchar *);
	GVariant * 	(*g_variant_new) 			(const gchar *, GVariantBuilder *);
	void 		(*g_variant_builder_unref) 	(GVariantBuilder *);
	void		(*g_variant_unref) 			(GVariant *);
#ifdef SOLARIS
	GString* 	(*g_string_insert_c) 		(GString *, gssize, gchar);
#endif	
		
	GdkDisplay* (*gdk_display_get_default)  		();
	GdkPixbuf*	(*gdk_pixbuf_new_from_file)			(const char*, GError **);
	GdkPixbuf*	(*gdk_pixbuf_scale_simple)			(const GdkPixbuf*, int, int, GdkInterpType);
	int			(*gdk_pixbuf_get_width)				(const GdkPixbuf*);
	int			(*gdk_pixbuf_get_height)			(const GdkPixbuf*);
	GdkScreen *	(*gdk_screen_get_default)			();
	double		(*gdk_screen_get_resolution)		(GdkScreen *);
	
};

extern struct GTK_PTRS gtk;

#define FN_TABLE_ENTRY(fn, required) { (void**)& gtk.fn, #fn, required }
typedef struct {
	void ** fnPtr;
	char * fnName;
	int required;
} FN_TABLE;

/* load the gtk libraries and initialize the function pointers */
extern int loadGtk();

#endif
