package org.eclipse.osgi.internal.log;

import org.osgi.framework.ServiceReference;

public class Arguments {
	private final Object[] arguments;
	private final ServiceReference<?> serviceReference;
	private final Throwable throwable;

	public Arguments(Object... arguments) {
		if (arguments == null || arguments.length == 0) {
			this.arguments = new Object[0];
			serviceReference = null;
			throwable = null;
			return;
		}
		int length = arguments.length;
		ServiceReference<?> context = null;
		Throwable exception = null;
		Object object = arguments[arguments.length - 1];
		if (object instanceof Throwable) {
			length--;
			exception = (Throwable) object;
			if (arguments.length > 1) {
				object = arguments[arguments.length - 2];
				if (object instanceof ServiceReference) {
					length--;
					context = (ServiceReference<?>) object;
				}
			}
		} else if (object instanceof ServiceReference) {
			length--;
			context = (ServiceReference<?>) object;
		}
		serviceReference = context;
		throwable = exception;
		this.arguments = new Object[length];
		System.arraycopy(arguments, 0, this.arguments, 0, length);
	}

	public Object[] arguments() {
		return arguments;
	}

	public boolean isEmpty() {
		return arguments == null || arguments.length == 0;
	}

	public ServiceReference<?> serviceReference() {
		return serviceReference;
	}

	public Throwable throwable() {
		return throwable;
	}
}
