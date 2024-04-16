/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.container.namespaces;

import org.osgi.framework.Constants;
import org.osgi.resource.Namespace;

/**
 * Equinox module data capability namespace. This namespace is used to store
 * immutable data about a module revision. This includes the following
 * <ul>
 * <li>The activation policy as specified by the
 * {@link Constants#BUNDLE_ACTIVATIONPOLICY Bundle-ActivationPolicy}
 * header.</li>
 * <li>The activator as specified by the {@link Constants#BUNDLE_ACTIVATOR
 * Bundle-Activator} header.</li>
 * <li>The class path as specified by the {@link Constants#BUNDLE_CLASSPATH
 * Bundle-ClassPath} header.</li>
 * <li>The bundle class loading policy.
 * </ul>
 *
 * This capability is provided for informational purposes and should not be
 * considered as effective by the resolver.
 * <p>
 * This class defines the names for the attributes and directives for this
 * namespace. Capabilities in this namespace are not intended to be used to
 * match requirements and should not be considered as effective by a resolver.
 *
 * @Immutable
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 3.10
 */
public class EquinoxModuleDataNamespace extends Namespace {
	/**
	 * The Equinox specific header for specifying a list of buddy policies.
	 */
	public final static String BUDDY_POLICY_HEADER = "Eclipse-BuddyPolicy"; //$NON-NLS-1$

	/**
	 * The Equinox specific header for specifying a list of symbolic names to
	 * register as a buddy with.
	 */
	public final static String REGISTERED_BUDDY_HEADER = "Eclipse-RegisterBuddy"; //$NON-NLS-1$

	/**
	 * The Equinox specific header for specifying the lazy start policy
	 */
	public static final String LAZYSTART_HEADER = "Eclipse-LazyStart"; //$NON-NLS-1$

	/**
	 * An Eclipse-LazyStart header attribute used to specify exception classes for
	 * auto start
	 */
	public static final String LAZYSTART_EXCEPTIONS_ATTRIBUTE = "exceptions"; //$NON-NLS-1$

	/**
	 * The Equinox specific header for specifying the lazy start policy
	 * 
	 * @deprecated use {@link #LAZYSTART_HEADER}
	 */
	public static final String AUTOSTART_HEADER = "Eclipse-AutoStart"; //$NON-NLS-1$

	/**
	 * Namespace name for equinox module data. Unlike typical name spaces this
	 * namespace is not intended to be used as an attribute.
	 */
	public static final String MODULE_DATA_NAMESPACE = "equinox.module.data"; //$NON-NLS-1$

	/**
	 * The directive value identifying a {@link #CAPABILITY_EFFECTIVE_DIRECTIVE
	 * capability} that is effective for information purposes. Capabilities in this
	 * namespace must have an effective directive value of information.
	 *
	 * @see #CAPABILITY_EFFECTIVE_DIRECTIVE
	 */
	public final static String EFFECTIVE_INFORMATION = "information"; //$NON-NLS-1$

	/**
	 * The capability attribute contains the
	 * {@link Constants#BUNDLE_ACTIVATIONPOLICY activation policy} for the providing
	 * module revision. The value of this attribute must be of type {@code String}.
	 * When not specified then the module revision uses an eager activation policy.
	 */
	public final static String CAPABILITY_ACTIVATION_POLICY = "activation.policy"; //$NON-NLS-1$

	/**
	 * An {@link #CAPABILITY_ACTIVATION_POLICY activation policy} attribute value
	 * indicating the lazy activation policy is used.
	 */
	public final static String CAPABILITY_ACTIVATION_POLICY_LAZY = "lazy"; //$NON-NLS-1$

	/**
	 * When the {@link #CAPABILITY_ACTIVATION_POLICY_LAZY lazy} policy is used this
	 * attribute contains the package names that must trigger the activation when a
	 * class is loaded of these packages. If the attribute is not defined then the
	 * default is all package names. The value of this attribute must be of type
	 * {@code List<String>}.
	 */
	public final static String CAPABILITY_LAZY_INCLUDE_ATTRIBUTE = "lazy.include"; //$NON-NLS-1$

	/**
	 * When the {@link #CAPABILITY_ACTIVATION_POLICY_LAZY lazy} policy is used this
	 * attribute contains the package names that must not trigger the activation
	 * when a class is loaded of these packages. If the attribute is not defined
	 * then the default is no package names. The value of this attribute must be of
	 * type {@code List<String>}.
	 */
	public final static String CAPABILITY_LAZY_EXCLUDE_ATTRIBUTE = "lazy.exclude"; //$NON-NLS-1$

	/**
	 * The capability attribute contains the {@link Constants#BUNDLE_ACTIVATOR
	 * activator} for the providing module revision. The value of this attribute
	 * must be of type {@code String}. When not specified then the module revision
	 * has no activator.
	 */
	public final static String CAPABILITY_ACTIVATOR = "activator"; //$NON-NLS-1$

	/**
	 * The capability attribute contains the {@link Constants#BUNDLE_CLASSPATH class
	 * path} for the providing module revision. The value of this attribute must be
	 * of type {@code List<String>}. When not specified the module revision uses the
	 * default class path of '.'.
	 */
	public final static String CAPABILITY_CLASSPATH = "classpath"; //$NON-NLS-1$

	/**
	 * The capability attribute contains the list buddy loading policies for the
	 * providing module revision as specified in the Eclipse-BuddyPolicy header. The
	 * value of this attribute must be of type {@code List<String>}.
	 */
	public final static String CAPABILITY_BUDDY_POLICY = "buddy.policy"; //$NON-NLS-1$

	/**
	 * The capability attribute contains the list of symbolic names the providing
	 * module revision is a registered buddy of as specified by the
	 * Eclipse-BuddyPolicy header. The value of this attribute must be of type
	 * {@code List<String>}.
	 */
	public final static String CAPABILITY_BUDDY_REGISTERED = "buddy.registered"; //$NON-NLS-1$
}
