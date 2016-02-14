/*******************************************************************************
 * Copyright (c) 2014, 2016 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;
import org.eclipse.equinox.http.servlet.internal.context.ContextController;
import org.eclipse.equinox.http.servlet.internal.context.DispatchTargets;
import org.eclipse.equinox.http.servlet.internal.registration.EndpointRegistration;
import org.eclipse.equinox.http.servlet.internal.registration.FilterRegistration;

/**
 * @author Raymond Augé
 */
public class ResponseStateHandler {

	public ResponseStateHandler(
		HttpServletRequest request, HttpServletResponse response,
		DispatchTargets dispatchTargets, DispatcherType dispatcherType) {

		this.request = request;
		this.response = response;
		this.dispatchTargets = dispatchTargets;
		this.dispatcherType = dispatcherType;
	}

	public void processRequest() throws IOException, ServletException {
		List<ServletRequestListener> servletRequestListeners = getServletRequestListener();
		EndpointRegistration<?> endpoint = dispatchTargets.getServletRegistration();
		List<FilterRegistration> filters = dispatchTargets.getMatchingFilterRegistrations();

		endpoint.addReference();

		for (FilterRegistration filterRegistration : filters) {
			filterRegistration.addReference();
		}

		ServletRequestEvent servletRequestEvent = null;

		try {
			if (dispatcherType == DispatcherType.FORWARD) {
				if (response.isCommitted()) {
					throw new IllegalStateException("Response is committed"); //$NON-NLS-1$
				}

				response.resetBuffer();
			}

			if ((dispatcherType == DispatcherType.REQUEST) && !servletRequestListeners.isEmpty()) {
				servletRequestEvent = new ServletRequestEvent(endpoint.getServletContext(), request);
				for (ServletRequestListener servletRequestListener : servletRequestListeners) {
					servletRequestListener.requestInitialized(servletRequestEvent);
				}
			}

			if (endpoint.getServletContextHelper().handleSecurity(request, response)) {
				if (filters.isEmpty()) {
					endpoint.service(request, response);
				}
				else {
					Collections.sort(filters);

					FilterChain chain = new FilterChainImpl(
						filters, endpoint, dispatcherType);

					chain.doFilter(request, response);
				}
			}
		}
		catch (Exception e) {
			if (!(e instanceof IOException) &&
				!(e instanceof RuntimeException) &&
				!(e instanceof ServletException)) {

				e = new ServletException(e);
			}

			setException(e);

			if (dispatcherType != DispatcherType.REQUEST) {
				throwException(e);
			}
		}
		finally {
			endpoint.removeReference();

			for (FilterRegistration filterRegistration : filters) {
				filterRegistration.removeReference();
			}

			if (dispatcherType == DispatcherType.FORWARD) {
				response.flushBuffer();

				HttpServletResponseWrapperImpl responseWrapper = HttpServletResponseWrapperImpl.findHttpRuntimeResponse(response);

				if (responseWrapper != null) {
					responseWrapper.setCompleted(true);
				}
				else {
					try {
						PrintWriter writer = response.getWriter();
						writer.close();
					}
					catch (IllegalStateException ise1) {
						try {
							ServletOutputStream outputStream = response.getOutputStream();
							outputStream.close();
						}
						catch (IllegalStateException ise2) {
							// ignore
						}
						catch (IOException ioe) {
							// ignore
						}
					}
					catch (IOException ioe) {
						// ignore
					}
				}
			}

			if (dispatcherType == DispatcherType.REQUEST) {
				handleErrors();

				for (ServletRequestListener servletRequestListener : servletRequestListeners) {
					servletRequestListener.requestDestroyed(servletRequestEvent);
				}
			}
		}
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	private List<ServletRequestListener> getServletRequestListener() {
		return dispatchTargets.getContextController().getEventListeners().get(ServletRequestListener.class);
	}

	private void handleErrors() throws IOException, ServletException {
		if (exception != null) {
			handleException();
		}
		else {
			handleResponseCode();
		}
	}

	private void handleException() throws IOException, ServletException {
		if (!(response instanceof HttpServletResponseWrapper)) {
			throw new IllegalStateException("Response isn't a wrapper"); //$NON-NLS-1$
		}

		HttpServletResponseWrapperImpl responseWrapper = HttpServletResponseWrapperImpl.findHttpRuntimeResponse(response);

		if (responseWrapper == null) {
			throw new IllegalStateException("Can't locate response impl"); //$NON-NLS-1$
		}

		HttpServletResponse wrappedResponse = (HttpServletResponse)responseWrapper.getResponse();

		if (wrappedResponse.isCommitted()) {
			throwException(exception);
		}

		ContextController contextController = dispatchTargets.getContextController();
		Class<? extends Exception> clazz = exception.getClass();
		String className = clazz.getName();

		DispatchTargets errorDispatchTargets = contextController.getDispatchTargets(
			className, null, null, null, null, null, Match.EXACT, null);

		if (errorDispatchTargets == null) {
			throwException(exception);
		}

		request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception);
		request.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE, className);
		request.setAttribute(RequestDispatcher.ERROR_MESSAGE, exception.getMessage());
		request.setAttribute(
			RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
		request.setAttribute(
			RequestDispatcher.ERROR_SERVLET_NAME,
			errorDispatchTargets.getServletRegistration().getName());
		request.setAttribute(
			RequestDispatcher.ERROR_STATUS_CODE,
			HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		responseWrapper.setCompleted(false);
		//responseWrapper = new HttpServletResponseWrapperImpl(wrappedResponse);

		ResponseStateHandler responseStateHandler = new ResponseStateHandler(
			request, responseWrapper, errorDispatchTargets, DispatcherType.ERROR);

		responseStateHandler.processRequest();

		wrappedResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	private void handleResponseCode() throws IOException, ServletException {
		if (!(response instanceof HttpServletResponseWrapper)) {
			throw new IllegalStateException("Response isn't a wrapper"); //$NON-NLS-1$
		}

		HttpServletResponseWrapperImpl responseWrapper = HttpServletResponseWrapperImpl.findHttpRuntimeResponse(response);

		if (responseWrapper == null) {
			throw new IllegalStateException("Can't locate response impl"); //$NON-NLS-1$
		}

		int status = responseWrapper.getStatus();

		if (status < HttpServletResponse.SC_BAD_REQUEST) {
			return;
		}

		if (status == -1) {
			// There's nothing more we can do here.
			return;
		}

		HttpServletResponse wrappedResponse = (HttpServletResponse)responseWrapper.getResponse();

		if (wrappedResponse.isCommitted()) {
			// the response is committed already, but we need to propagate the error code anyway
			wrappedResponse.sendError(status, responseWrapper.getMessage());
			// There's nothing more we can do here.
			return;
		}

		ContextController contextController = dispatchTargets.getContextController();

		DispatchTargets errorDispatchTargets = contextController.getDispatchTargets(
			String.valueOf(status), null, null, null, null, null, Match.EXACT, null);

		if (errorDispatchTargets == null) {
			wrappedResponse.sendError(status, responseWrapper.getMessage());

			return;
		}

		request.setAttribute(
			RequestDispatcher.ERROR_MESSAGE, responseWrapper.getMessage());
		request.setAttribute(
			RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
		request.setAttribute(
			RequestDispatcher.ERROR_SERVLET_NAME,
			errorDispatchTargets.getServletRegistration().getName());
		request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);

		responseWrapper.setCompleted(false);
		//responseWrapper = new HttpServletResponseWrapperImpl(wrappedResponse);

		ResponseStateHandler responseStateHandler = new ResponseStateHandler(
			request, responseWrapper, errorDispatchTargets, DispatcherType.ERROR);

		wrappedResponse.setStatus(status);

		responseStateHandler.processRequest();
	}

	private void throwException(Exception e)
		throws IOException, ServletException {

		if (e instanceof RuntimeException) {
			throw (RuntimeException)e;
		}
		else if (e instanceof IOException) {
			throw (IOException)e;
		}
		else if (e instanceof ServletException) {
			throw (ServletException)e;
		}
	}

	private DispatchTargets dispatchTargets;
	private DispatcherType dispatcherType;
	private Exception exception;
	private HttpServletRequest request;
	private HttpServletResponse response;

}