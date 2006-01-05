/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import org.eclipse.equinox.http.Http;
import org.eclipse.equinox.http.HttpMsg;

/**
 * The implementation of the HttpSession interface.
 *
 * <pre>
 *
 * //Get the session object - "request" represents the HTTP servlet request
 * HttpSession session = request.getSession(true);
 * <BR>
 * //Get the session data value - an Integer object is read from
 * //the session, incremented, then written back to the session.
 * //sessiontest.counter identifies values in the session
 * Integer ival = (Integer) session.getValue("sessiontest.counter");
 * if (ival==null)
 *     ival = new Integer(1);
 * else
 *     ival = new Integer(ival.intValue() + 1);
 * session.putValue("sessiontest.counter", ival);
 *
 * </pre>
 *
 * <P> When an application layer stores or removes data from the
 * session, the session layer checks whether the object implements
 * HttpSessionBindingListener.  If it does, then the object is notified
 * that it has been bound or unbound from the session.
 *
 * <P>An implementation of HttpSession represents the server's view
 * of the session. The server considers a session to be new until
 * it has been joined by the
 * client.  Until the client joins the session, the isNew method
 * returns true. A value of true can indicate one of these three cases:
 * <UL>
 * <LI>the client does not yet know about the session
 * <LI>the session has not yet begun
 * <LI>the client chooses not to join the session. This case will occur
 * if the client supports
 * only cookies and chooses to reject any cookies sent by the server.
 * If the server supports URL rewriting, this case will not commonly occur.
 * </UL>
 *
 * <P>It is the responsibility of developers
 * to design their applications to account for situations where a client
 * has not joined a session. For example, in the following code
 * snippet isNew is called to determine whether a session is new. If it
 * is, the server will require the client to start a session by directing
 * the client to a welcome page <tt>welcomeURL</tt> where
 * a user might be required to enter some information and send it to the
 * server before gaining access to
 * subsequent pages.
 *
 * <pre>
 * //Get the session object - "request" represents the HTTP servlet request
 * HttpSession session = request.getSession(true);
 * <BR>
 * //insist that the client starts a session
 * //before access to data is allowed
 * //"response" represents the HTTP servlet response
 * if (session.isNew()) {
 *     response.sendRedirect (welcomeURL);
 * }
 *
 * </pre>
 *
 * @see HttpSessionBindingListener
 * @see HttpSessionContext
 *
 */

public class HttpSessionImpl implements HttpSession {
	protected Http http;
	protected String sessionId;
	protected Hashtable values;
	protected long creationTime; /* milliseconds */
	protected long lastAccess; /* milliseconds */
	protected boolean isValid;
	protected boolean canExpire;
	protected Cookie cookie;
	protected long maxInactive; /* milliseconds */
	// BUGBUG cookie name MUST be "JSESSIONID"
	// Servlet 2.2 Section 7.1.2
	protected static final String sessionCookieName = "org.eclipse.equinox.http.session"; //$NON-NLS-1$

	protected HttpSessionImpl(Http http) {
		this.http = http;
		lastAccess = -1;
		maxInactive = -1;
		canExpire = false;
		isValid = true;
		sessionId = String.valueOf(hashCode());
		values = new Hashtable();
		creationTime = System.currentTimeMillis();
		cookie = new Cookie(sessionCookieName, sessionId);
		// BUGBUG Sessions should be ServletContext specific. That is
		// the ServletContext should manage the sessions.
		// Servlet 2.2 Section 7.3
		http.addSession(this);
	}

	/**
	 * Return the cookie associated with this session.
	 *
	 * @return Session cookie
	 */
	protected Cookie getCookie() {
		return (cookie);
	}

	/**
	 * Returns the time at which this session representation was created,
	 * in milliseconds since midnight, January 1, 1970 UTC.
	 *
	 * @return the time when the session was created
	 */
	public long getCreationTime() {
		return (creationTime);
	}

	/**
	 * Returns the identifier assigned to this session. An HttpSession's
	 * identifier is a unique string that is created and maintained by
	 * HttpSessionContext.
	 *
	 * @return the identifier assigned to this session
	 */
	public String getId() {
		return (sessionId);
	}

	/**
	 * Returns the last time the client sent a request carrying the identifier
	 * assigned to the session, or -1 if the session is new. Time is expressed
	 * as milliseconds since midnight, January 1,
	 * 1970 UTC.
	 *
	 * Application level operations, such as getting or setting a value
	 * associated with the session, do not affect the access time.
	 *
	 * <P> This information is particularly useful in session management
	 * policies.  For example,
	 * <UL>
	 * <LI>a session manager could leave all sessions
	 * which have not been used in a long time
	 * in a given context.
	 * <LI>the sessions can be sorted according to age to optimize some task.
	 * </UL>
	 *
	 * @return the last time the client sent a request carrying the identifier
	 * assigned to the session
	 */
	public long getLastAccessedTime() {
		return (lastAccess);
	}

	/**
	 * Returns the maximum amount of time, in seconds, that a session is
	 * guaranteed to be maintained in the servlet engine without a request from
	 * the client.  After the maximum inactive time, the session may be expired
	 * by the servlet engine.  If this session will not expire, this method will
	 * return -1.  This method should throw an IllegalStateException if it is
	 * called after this session has been invalidated.
	 *
	 * @return
	 * @throws IllegalStateException
	 */
	public int getMaxInactiveInterval() {
		checkValid();

		if (canExpire) {
			return ((int) (maxInactive / 1000L));
		} else {
			return (-1);
		}
	}

	/**
	 * Sets the amount of time that a session can be inactive
	 * before the servlet engine is allowed to expire it.
	 *
	 * @param interval Time in seconds.
	 */
	public void setMaxInactiveInterval(int interval) {
		if (isValid) {
			if (interval == -1) {
				maxInactive = -1;
				canExpire = false;
			} else {
				if (interval < 0) {
					throw new IllegalArgumentException("negative value"); //$NON-NLS-1$
				} else {
					maxInactive = ((long) interval) * 1000L;
					canExpire = true;
				}
			}
		}
	}

	/**
	 * Returns the context object within which sessions on the server are held.
	 *
	 * This method has been deprecated as all the methods of HttpSessionContext
	 * are deprecated.  This method should now return an object which has an
	 * empty implementation of the HttpSessionContext interface.
	 *
	 * @return An empty implementation of the HttpSessionContext interface.
	 * @deprecated As of Servlet version 2.1.
	 */
	public HttpSessionContext getSessionContext() {
		return (new HttpSessionContextImpl());
	}

	/**
	 * Returns the object bound to the given name in the session's
	 * application layer data.  Returns null if there is no such binding.
	 *
	 * @param name the name of the binding to find
	 * @return the value bound to that name, or null if the binding does
	 * not exist.
	 * @exception IllegalStateException if an attempt is made to access
	 * HttpSession's session data after it has been invalidated
	 */
	public Object getValue(String name) {
		checkValid();

		return (values.get(name));
	}

	/**
	 * Returns an array of the names of all the application layer
	 * data objects bound into the session. For example, if you want to delete
	 * all of the data objects bound into the session, use this method to
	 * obtain their names.
	 *
	 * @return an array containing the names of all of the application layer
	 * data objects bound into the session
	 * @exception IllegalStateException if an attempt is made to access
	 * session data after the session has been invalidated
	 */
	public String[] getValueNames() {
		checkValid();

		return (getValueNames0());
	}

	private String[] getValueNames0() {
		synchronized (values) {
			int size = values.size();
			String[] names = new String[size];

			if (size > 0) {
				int i = 0;
				Enumeration enum = values.keys();
				while (enum.hasMoreElements()) {
					names[i] = (String) enum.nextElement();
					i++;
				}
			}

			return (names);
		}
	}

	/**
	 * Causes this representation of the session to be invalidated and removed
	 * from its context.
	 *
	 */
	public synchronized void invalidate() {
		if (isValid) {
			/*
			 * Remove session.
			 */
			http.removeSession(this);

			/*
			 * Unbind values.
			 */
			String[] names = getValueNames0();
			int size = names.length;

			for (int i = 0; i < size; i++) {
				String name = names[i];

				Object oldValue = values.remove(name);

				unbound(name, oldValue);
			}

			/*
			 * invalidate session
			 */
			isValid = false;
			values = null;
			canExpire = false;
		}
	}

	/**
	 * A session is considered to be "new" if it has been created by the server,
	 * but the client has not yet acknowledged joining the session. For example,
	 * if the server supported only cookie-based sessions and the client had
	 * completely disabled the use of cookies, then calls to
	 * HttpServletRequest.getSession() would
	 * always return "new" sessions.
	 *
	 * @return true if the session has been created by the server but the
	 * client has not yet acknowledged joining the session; false otherwise
	 * @exception IllegalStateException if an attempt is made to access
	 * session data after the session has been invalidated
	 */
	public boolean isNew() {
		checkValid();

		return (lastAccess == -1);
	}

	/**
	 * Binds the specified object into the session's application layer data
	 * with the given name.  Any existing binding with the same name is
	 * replaced.  New (or existing) values that implement the
	 * HttpSessionBindingListener interface will call its
	 * valueBound() method.
	 *
	 * @param name the name to which the data object will be bound.  This
	 * parameter cannot be null.
	 * @param value the data object to be bound.  This parameter cannot be null.
	 * @exception IllegalStateException if an attempt is made to access
	 * session data after the session has been invalidated
	 */
	public void putValue(String name, Object value) {
		checkValid();

		Object oldValue = values.put(name, value);

		unbound(name, oldValue);

		// BUGBUG valueBound must be called before the object is available
		// via getValue.
		// Servlet 2.2 Section 7.4
		bound(name, value);
	}

	/**
	 * Notify HttpSessionBindingListener of valueBound event.
	 *
	 * @param name the name to which the data object will be bound.  This
	 * parameter cannot be null.
	 * @param value the data object to be bound.  This parameter cannot be null.
	 */
	private void bound(String name, Object value) {
		if (value instanceof HttpSessionBindingListener) {
			HttpSessionBindingEvent e = new HttpSessionBindingEvent(this, name);

			try {
				((HttpSessionBindingListener) value).valueBound(e);
			} catch (Throwable t) {
				if (Http.DEBUG) {
					http.logDebug("HttpSessionImpl.putValue event exception", t); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Removes the object bound to the given name in the session's
	 * application layer data.  Does nothing if there is no object
	 * bound to the given name.  The value that implements the
	 * HttpSessionBindingListener interface will call its
	 * valueUnbound() method.
	 *
	 * @param name the name of the object to remove
	 * @exception IllegalStateException if an attempt is made to access
	 * session data after the session has been invalidated
	 */
	public void removeValue(String name) {
		checkValid();

		Object oldValue = values.remove(name);

		unbound(name, oldValue);
	}

	/**
	 * Notify HttpSessionBindingListener of valueUnbound event.
	 *
	 * @param name the name to which the data object will be bound.  This
	 * parameter cannot be null.
	 * @param value the data object to be bound.
	 */
	private void unbound(String name, Object value) {
		if (value instanceof HttpSessionBindingListener) {
			HttpSessionBindingEvent e = new HttpSessionBindingEvent(this, name);

			try {
				((HttpSessionBindingListener) value).valueUnbound(e);
			} catch (Throwable t) {
				if (Http.DEBUG) {
					http.logDebug("HttpSessionImpl.removeValue event exception", t); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * If the session has expired, invalidate it.
	 * If access it true and the session is valid, update
	 * the lastAccess time.
	 *
	 * @return true if the session is valid.
	 */
	public boolean isValid(boolean access) {
		if (canExpire) {
			long currentTime = System.currentTimeMillis();

			long compareTime = (lastAccess == -1) ? creationTime : lastAccess;

			if ((currentTime - compareTime) > maxInactive) {
				invalidate();
			}
		}

		if (access && isValid) {
			lastAccess = System.currentTimeMillis();
		}

		return (isValid);
	}

	/**
	 * If the session has expired, invalidate it.
	 * If the session is invalid, throw an IllegalStateException
	 *
	 * @throws IllegalStateException.
	 */
	private void checkValid() {
		if (canExpire) {
			long currentTime = System.currentTimeMillis();

			long compareTime = (lastAccess == -1) ? creationTime : lastAccess;

			if ((currentTime - compareTime) > maxInactive) {
				invalidate();
			}
		}

		if (!isValid) {
			throw new IllegalStateException("HttpSession has been invalidated"); //$NON-NLS-1$
		}
	}

	/**
	 * @see javax.servlet.http.HttpSession#getAttribute(String)
	 */
	public Object getAttribute(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpSession#getAttributeNames()
	 */
	public Enumeration getAttributeNames() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpSession#getServletContext()
	 */
	public ServletContext getServletContext() throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpSession#removeAttribute(String)
	 */
	public void removeAttribute(String name) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

	/**
	 * @see javax.servlet.http.HttpSession#setAttribute(String, Object)
	 */
	public void setAttribute(String name, Object value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException(HttpMsg.HTTP_ONLY_SUPPORTS_2_1);
	}

}
