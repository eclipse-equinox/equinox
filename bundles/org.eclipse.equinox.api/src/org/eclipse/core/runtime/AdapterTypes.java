/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.lang.annotation.*;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * Annotation that can be used for components to specify the provided adapter
 * types. example use case
 * 
 * <pre>
 * 
 * &#64;Component
 * &#64;AdapterTypes(adaptableClass = Template.class, adapterNames = { ILabelProvider.class, IContentProvider.class })
 * public class TemplateAdapter implements IAdapterFactory {
 * 
 *     &#64;Override
 *     public &lt;T&gt; T getAdapter(Object adaptableObject, Class&lt;T&gt; adapterType) {
 *         if (adaptableObject instanceof Template template) {
 *            ...
 *         }
 *         return null;
 *     }
 * }
 * </pre>
 * 
 * @since 3.19
 */
@ComponentPropertyType
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdapterTypes {
	/**
	 * See {@link IAdapterFactory#SERVICE_PROPERTY_ADAPTABLE_CLASS}
	 * 
	 * @return the types that this class adapts from
	 */
	Class<?>[] adaptableClass();

	/**
	 * See {@link IAdapterFactory#SERVICE_PROPERTY_ADAPTER_NAMES}
	 * 
	 * @return the types that this class adapts to
	 */
	Class<?>[] adapterNames();
}
