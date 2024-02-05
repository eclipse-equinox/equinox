/*******************************************************************************
 * Copyright (c) 2015, 2019 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé - bug fixes and enhancements
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.*;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO;
import org.eclipse.equinox.http.servlet.internal.dto.ExtendedErrorPageDTO.ErrorCodeType;
import org.eclipse.equinox.http.servlet.internal.error.HttpWhiteboardFailureException;
import org.osgi.dto.DTO;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.*;

/**
 * @author Raymond Augé
 */
public class DTOUtil {

	public static ExtendedErrorPageDTO assembleErrorPageDTO(ServiceReference<?> serviceReference, long contextId,
			boolean validated) {
		Object errorPageObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);

		if (errorPageObj == null) {
			return null;
		}

		ExtendedErrorPageDTO errorPageDTO = new ExtendedErrorPageDTO();

		errorPageDTO.asyncSupported = false;
		Object asyncSupportedObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);
		if (asyncSupportedObj == null) {
			// ignored
		} else if (Boolean.class.isInstance(asyncSupportedObj)) {
			errorPageDTO.asyncSupported = ((Boolean) asyncSupportedObj).booleanValue();
		} else if (String.class.isInstance(asyncSupportedObj)) {
			errorPageDTO.asyncSupported = Boolean.valueOf((String) asyncSupportedObj);
		}
		// There is no validation for this scenario, truthiness of any other input is
		// false

		List<String> errorPages = StringPlus.from(errorPageObj);
		if (errorPages.isEmpty()) {
			throw new HttpWhiteboardFailureException("'errorPage' expects String, String[] or Collection<String>", //$NON-NLS-1$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		List<String> exceptions = new ArrayList<>();
		Set<Long> errorCodeSet = new LinkedHashSet<>();

		for (String errorPage : errorPages) {
			try {
				if ("4xx".equals(errorPage)) { //$NON-NLS-1$
					errorPageDTO.errorCodeType = ErrorCodeType.RANGE_4XX;
					for (long code = 400; code < 500; code++) {
						errorCodeSet.add(code);
					}
				} else if ("5xx".equals(errorPage)) { //$NON-NLS-1$
					errorPageDTO.errorCodeType = ErrorCodeType.RANGE_5XX;
					for (long code = 500; code < 600; code++) {
						errorCodeSet.add(code);
					}
				} else if (errorPage.matches("\\d{3}")) { //$NON-NLS-1$
					errorPageDTO.errorCodeType = ErrorCodeType.SPECIFIC;
					long code = Long.parseLong(errorPage);
					errorCodeSet.add(code);
				} else {
					exceptions.add(errorPage);
				}
			} catch (NumberFormatException nfe) {
				exceptions.add(errorPage);
			}
		}

		errorPageDTO.errorCodes = new long[errorCodeSet.size()];
		int i = 0;
		for (Long code : errorCodeSet) {
			errorPageDTO.errorCodes[i] = code;
			i++;
		}

		errorPageDTO.exceptions = exceptions.toArray(new String[0]);

		errorPageDTO.initParams = ServiceProperties.parseInitParams(serviceReference,
				HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX);

		Object servletNameObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_NAME);
		if (servletNameObj == null) {
			// ignore
		} else if (String.class.isInstance(servletNameObj)) {
			errorPageDTO.name = (String) servletNameObj;
		} else if (validated) {
			throw new HttpWhiteboardFailureException("'name' expects String", //$NON-NLS-1$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		errorPageDTO.serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
		errorPageDTO.servletContextId = contextId;

		return errorPageDTO;
	}

	@SuppressWarnings("deprecation")
	public static org.eclipse.equinox.http.servlet.dto.ExtendedServletDTO assembleServletDTO(
			ServiceReference<?> serviceReference, long contextId, boolean validated) {
		org.eclipse.equinox.http.servlet.dto.ExtendedServletDTO servletDTO = new org.eclipse.equinox.http.servlet.dto.ExtendedServletDTO();

		servletDTO.asyncSupported = false;
		Object asyncSupportedObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);
		if (asyncSupportedObj == null) {
			// ignored
		} else if (Boolean.class.isInstance(asyncSupportedObj)) {
			servletDTO.asyncSupported = ((Boolean) asyncSupportedObj).booleanValue();
		} else if (String.class.isInstance(asyncSupportedObj)) {
			servletDTO.asyncSupported = Boolean.valueOf((String) asyncSupportedObj);
		}
		// There is no validation for this scenario, truthiness of any other input is
		// false

		servletDTO.initParams = ServiceProperties.parseInitParams(serviceReference,
				HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX);

		servletDTO.multipartEnabled = false;
		Object multipartEnabledObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED);
		if (multipartEnabledObj == null) {
			multipartEnabledObj = serviceReference.getProperty(Const.EQUINOX_HTTP_MULTIPART_ENABLED);
		}
		if (multipartEnabledObj == null) {
			// ignore
		} else if (Boolean.class.isInstance(multipartEnabledObj)) {
			servletDTO.multipartEnabled = ((Boolean) multipartEnabledObj).booleanValue();
		} else if (String.class.isInstance(multipartEnabledObj)) {
			servletDTO.multipartEnabled = Boolean.valueOf((String) multipartEnabledObj);
		}
		// There is no validation for this scenario, truthiness of any other input is
		// false

		servletDTO.multipartFileSizeThreshold = 0;
		servletDTO.multipartLocation = Const.BLANK;
		servletDTO.multipartMaxFileSize = -1L;
		servletDTO.multipartMaxRequestSize = -1L;

		if (servletDTO.multipartEnabled) {
			Object multipartFileSizeThresholdObj = serviceReference
					.getProperty(HTTP_WHITEBOARD_SERVLET_MULTIPART_FILESIZETHRESHOLD);
			if (multipartFileSizeThresholdObj == null) {
				multipartFileSizeThresholdObj = serviceReference
						.getProperty(Const.EQUINOX_HTTP_MULTIPART_FILESIZETHRESHOLD);
			}
			if (multipartFileSizeThresholdObj == null) {
				// ignore
			} else if (Integer.class.isInstance(multipartFileSizeThresholdObj)) {
				servletDTO.multipartFileSizeThreshold = ((Integer) multipartFileSizeThresholdObj).intValue();
			} else if (validated) {
				throw new HttpWhiteboardFailureException("'multipartFileSizeThreshold' expects int or Integer", //$NON-NLS-1$
						DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}

			Object multipartLocationObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_MULTIPART_LOCATION);
			if (multipartLocationObj == null) {
				multipartLocationObj = serviceReference.getProperty(Const.EQUINOX_HTTP_MULTIPART_LOCATION);
			}
			if (multipartLocationObj == null) {
				// ignore
			} else if (String.class.isInstance(multipartLocationObj)) {
				servletDTO.multipartLocation = (String) multipartLocationObj;
			} else if (validated) {
				throw new HttpWhiteboardFailureException("'multipartLocation' expects String", //$NON-NLS-1$
						DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}

			Object multipartMaxFileSizeObj = serviceReference
					.getProperty(HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE);
			if (multipartMaxFileSizeObj == null) {
				multipartMaxFileSizeObj = serviceReference.getProperty(Const.EQUINOX_HTTP_MULTIPART_MAXFILESIZE);
			}
			if (multipartMaxFileSizeObj == null) {
				// ignore
			} else if (Long.class.isInstance(multipartMaxFileSizeObj)) {
				servletDTO.multipartMaxFileSize = ((Long) multipartMaxFileSizeObj).longValue();
			} else if (validated) {
				throw new HttpWhiteboardFailureException("'multipartMaxFileSize' expects [L|l]ong", //$NON-NLS-1$
						DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}

			Object multipartMaxRequestSizeObj = serviceReference
					.getProperty(HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXREQUESTSIZE);
			if (multipartMaxRequestSizeObj == null) {
				multipartMaxRequestSizeObj = serviceReference.getProperty(Const.EQUINOX_HTTP_MULTIPART_MAXREQUESTSIZE);
			}
			if (multipartMaxRequestSizeObj == null) {
				// ignore
			} else if (Long.class.isInstance(multipartMaxRequestSizeObj)) {
				servletDTO.multipartMaxRequestSize = ((Long) multipartMaxRequestSizeObj).longValue();
			} else if (validated) {
				throw new HttpWhiteboardFailureException("'multipartMaxRequestSize' expects [L|l]ong", //$NON-NLS-1$
						DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}
		}

		Object servletNameObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_NAME);
		if (servletNameObj == null) {
			// ignore
		} else if (String.class.isInstance(servletNameObj)) {
			servletDTO.name = (String) servletNameObj;
		} else if (validated) {
			throw new HttpWhiteboardFailureException("'name' expects String", //$NON-NLS-1$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		Object patternObj = serviceReference.getProperty(HTTP_WHITEBOARD_SERVLET_PATTERN);
		if (patternObj == null) {
			servletDTO.patterns = new String[0];
		} else {
			servletDTO.patterns = sort(StringPlus.from(patternObj).toArray(new String[0]));

			if (validated && (servletDTO.patterns.length > 0)) {
				for (String pattern : servletDTO.patterns) {
					checkPattern(pattern);
				}
			}
		}

		servletDTO.serviceId = (Long) serviceReference.getProperty(Constants.SERVICE_ID);
		servletDTO.servletContextId = contextId;

		return servletDTO;
	}

	public static ErrorPageDTO clone(ErrorPageDTO original) {
		ErrorPageDTO clone = new ErrorPageDTO();

		clone.asyncSupported = copy(original.asyncSupported);
		clone.errorCodes = copy(original.errorCodes);
		clone.exceptions = copy(original.exceptions);
		clone.initParams = copyStringMap(original.initParams);
		clone.name = copy(original.name);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.servletInfo = copy(original.servletInfo);

		return clone;
	}

	public static FailedErrorPageDTO clone(FailedErrorPageDTO original) {
		FailedErrorPageDTO clone = new FailedErrorPageDTO();

		clone.asyncSupported = copy(original.asyncSupported);
		clone.errorCodes = copy(original.errorCodes);
		clone.exceptions = copy(original.exceptions);
		clone.failureReason = copy(original.failureReason);
		clone.initParams = copyStringMap(original.initParams);
		clone.name = copy(original.name);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.servletInfo = copy(original.servletInfo);

		return clone;
	}

	public static FailedFilterDTO clone(FailedFilterDTO original) {
		FailedFilterDTO clone = new FailedFilterDTO();

		clone.asyncSupported = copy(original.asyncSupported);
		clone.dispatcher = copy(original.dispatcher);
		clone.failureReason = copy(original.failureReason);
		clone.initParams = copyStringMap(original.initParams);
		clone.name = copy(original.name);
		clone.patterns = copy(original.patterns);
		clone.regexs = copy(original.regexs);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.servletNames = copy(original.servletNames);

		return clone;
	}

	public static FailedListenerDTO clone(FailedListenerDTO original) {
		FailedListenerDTO clone = new FailedListenerDTO();

		clone.failureReason = copy(original.failureReason);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.types = copy(original.types);

		return clone;
	}

	public static FailedPreprocessorDTO clone(FailedPreprocessorDTO original) {
		FailedPreprocessorDTO clone = new FailedPreprocessorDTO();

		clone.failureReason = copy(original.failureReason);
		clone.initParams = copyStringMap(original.initParams);
		clone.serviceId = copy(original.serviceId);

		return clone;
	}

	public static FailedResourceDTO clone(FailedResourceDTO original) {
		FailedResourceDTO clone = new FailedResourceDTO();

		clone.failureReason = copy(original.failureReason);
		clone.patterns = copy(original.patterns);
		clone.prefix = copy(original.prefix);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);

		return clone;
	}

	public static FailedServletContextDTO clone(FailedServletContextDTO original) {
		FailedServletContextDTO clone = new FailedServletContextDTO();

		clone.attributes = copyGenericMap(original.attributes);
		clone.contextPath = copy(original.contextPath);
		clone.errorPageDTOs = copy(original.errorPageDTOs);
		clone.failureReason = copy(original.failureReason);
		clone.filterDTOs = copy(original.filterDTOs);
		clone.initParams = copyStringMap(original.initParams);
		clone.listenerDTOs = copy(original.listenerDTOs);
		clone.name = copy(original.name);
		clone.resourceDTOs = copy(original.resourceDTOs);
		clone.serviceId = copy(original.serviceId);
		clone.servletDTOs = copy(original.servletDTOs);

		return clone;
	}

	public static FailedServletDTO clone(FailedServletDTO original) {
		FailedServletDTO clone = new FailedServletDTO();

		clone.asyncSupported = copy(original.asyncSupported);
		clone.failureReason = copy(original.failureReason);
		clone.initParams = copyStringMap(clone.initParams);
		clone.multipartEnabled = copy(original.multipartEnabled);
		clone.multipartFileSizeThreshold = copy(original.multipartFileSizeThreshold);
		clone.multipartLocation = copy(original.multipartLocation);
		clone.multipartMaxFileSize = copy(original.multipartMaxFileSize);
		clone.multipartMaxRequestSize = copy(original.multipartMaxRequestSize);
		clone.name = copy(original.name);
		clone.patterns = copy(original.patterns);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.servletInfo = copy(original.servletInfo);

		return clone;
	}

	public static FilterDTO clone(FilterDTO original) {
		FilterDTO clone = new FilterDTO();

		clone.asyncSupported = copy(original.asyncSupported);
		clone.dispatcher = copy(original.dispatcher);
		clone.initParams = copyStringMap(original.initParams);
		clone.name = copy(original.name);
		clone.patterns = copy(original.patterns);
		clone.regexs = copy(original.regexs);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.servletNames = copy(original.servletNames);

		return clone;
	}

	public static ListenerDTO clone(ListenerDTO original) {
		ListenerDTO clone = new ListenerDTO();

		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.types = copy(original.types);

		return clone;
	}

	public static ResourceDTO clone(ResourceDTO original) {
		ResourceDTO clone = new ResourceDTO();

		clone.patterns = copy(original.patterns);
		clone.prefix = copy(original.prefix);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);

		return clone;
	}

	public static ServletDTO clone(ServletDTO original) {
		ServletDTO clone = new ServletDTO();

		clone.asyncSupported = copy(original.asyncSupported);
		clone.initParams = copyStringMap(original.initParams);
		clone.multipartEnabled = copy(original.multipartEnabled);
		clone.multipartFileSizeThreshold = copy(original.multipartFileSizeThreshold);
		clone.multipartLocation = copy(original.multipartLocation);
		clone.multipartMaxFileSize = copy(original.multipartMaxFileSize);
		clone.multipartMaxRequestSize = copy(original.multipartMaxRequestSize);
		clone.name = copy(original.name);
		clone.patterns = copy(original.patterns);
		clone.serviceId = copy(original.serviceId);
		clone.servletContextId = copy(original.servletContextId);
		clone.servletInfo = copy(original.servletInfo);

		return clone;
	}

	private static long[] copy(long[] array) {
		if (array == null) {
			return new long[0];
		}
		if (array.length == 0) {
			return array;
		}
		return Arrays.copyOf(array, array.length);
	}

	private static String[] copy(String[] array) {
		if (array == null) {
			return new String[0];
		}
		if (array.length == 0) {
			return array;
		}
		return Arrays.copyOf(array, array.length);
	}

	private static <T> T[] copy(T[] array) {
		if (array == null) {
			return null;
		}
		if (array.length == 0) {
			return array;
		}
		return Arrays.copyOf(array, array.length);
	}

	private static int copy(int value) {
		return value;
	}

	private static long copy(long value) {
		return value;
	}

	private static boolean copy(boolean value) {
		return value;
	}

	private static String copy(String value) {
		return value;
	}

	private static Map<String, String> copyStringMap(Map<String, String> initParams) {
		if (initParams == null) {
			return Collections.emptyMap();
		}
		return new HashMap<>(initParams);
	}

	public static <V> Map<String, Object> copyGenericMap(Map<String, V> value) {
		if ((value == null) || value.isEmpty()) {
			return Collections.emptyMap();
		}
		HashMap<String, Object> result = new HashMap<>();
		for (Map.Entry<String, V> entry : value.entrySet()) {
			result.put(entry.getKey(), mapValue(entry.getValue()));
		}
		return result;
	}

	public static Object mapValue(Object v) {
		if ((v == null) || v instanceof Number || v instanceof Boolean || v instanceof Character || v instanceof String
				|| v instanceof DTO) {
			return v;
		}
		if (v instanceof Map) {
			Map<?, ?> m = (Map<?, ?>) v;
			Map<Object, Object> map = newMap(m.size());
			for (Map.Entry<?, ?> e : m.entrySet()) {
				map.put(mapValue(e.getKey()), mapValue(e.getValue()));
			}
			return map;
		}
		if (v instanceof List) {
			List<?> c = (List<?>) v;
			List<Object> list = newList(c.size());
			for (Object o : c) {
				list.add(mapValue(o));
			}
			return list;
		}
		if (v instanceof Set) {
			Set<?> c = (Set<?>) v;
			Set<Object> set = newSet(c.size());
			for (Object o : c) {
				set.add(mapValue(o));
			}
			return set;
		}
		if (v.getClass().isArray()) {
			final int length = Array.getLength(v);
			final Class<?> componentType = mapComponentType(v.getClass().getComponentType());
			Object array = Array.newInstance(componentType, length);
			for (int i = 0; i < length; i++) {
				Array.set(array, i, mapValue(Array.get(v, i)));
			}
			return array;
		}
		return String.valueOf(v);
	}

	private static void checkPattern(String pattern) {
		if (pattern == null) {
			throw new HttpWhiteboardFailureException("Pattern cannot be null", //$NON-NLS-1$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}

		if (pattern.indexOf("*.") == 0) { //$NON-NLS-1$
			return;
		}

		if (Const.BLANK.equals(pattern)) {
			return;
		}

		if (Const.SLASH.equals(pattern)) {
			return;
		}

		if (!pattern.startsWith(Const.SLASH) || (pattern.endsWith(Const.SLASH) && !pattern.equals(Const.SLASH))
				|| pattern.contains("**")) { //$NON-NLS-1$

			throw new HttpWhiteboardFailureException("Invalid pattern '" + pattern + "'", //$NON-NLS-1$ //$NON-NLS-2$
					DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
	}

	private static Class<?> mapComponentType(Class<?> componentType) {
		if (componentType.isPrimitive() || componentType.isArray() || Object.class.equals(componentType)
				|| Number.class.isAssignableFrom(componentType) || Boolean.class.isAssignableFrom(componentType)
				|| Character.class.isAssignableFrom(componentType) || String.class.isAssignableFrom(componentType)
				|| DTO.class.isAssignableFrom(componentType)) {
			return componentType;
		}
		if (Map.class.isAssignableFrom(componentType)) {
			return Map.class;
		}
		if (List.class.isAssignableFrom(componentType)) {
			return List.class;
		}
		if (Set.class.isAssignableFrom(componentType)) {
			return Set.class;
		}
		return String.class;
	}

	private static <E> List<E> newList(int size) {
		return new ArrayList<>(size);
	}

	private static <E> Set<E> newSet(int size) {
		return new HashSet<>(size);
	}

	private static <K, V> Map<K, V> newMap(int size) {
		return new HashMap<>(size);
	}

	private static String[] sort(String[] values) {
		if (values == null) {
			return null;
		}

		Arrays.sort(values);

		return values;
	}

}
