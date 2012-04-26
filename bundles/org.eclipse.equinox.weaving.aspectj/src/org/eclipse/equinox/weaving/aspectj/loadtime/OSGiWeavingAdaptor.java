/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation
 *   Matthew Webster           Eclipse 3.2 changes
 *   Heiko Seeberger           AJDT 1.5.1 changes
 *   Martin Lippert            minor changes and bugfixes
 *   Martin Lippert            reworked
 *   Martin Lippert            caching of generated classes
 *   Martin Lippert            added locking for weaving
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.aspectj.weaver.IUnwovenClassFile;
import org.aspectj.weaver.bcel.BcelWeakClassLoaderReference;
import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;
import org.aspectj.weaver.tools.GeneratedClassHandler;
import org.aspectj.weaver.tools.Trace;
import org.aspectj.weaver.tools.TraceFactory;
import org.eclipse.equinox.weaving.aspectj.AspectJWeavingStarter;

/**
 * The weaving adaptor for AspectJs load-time weaving API that deals with the
 * OSGi specifics for load-time weaving
 */
public class OSGiWeavingAdaptor extends ClassLoaderWeavingAdaptor {

    /**
     * internal class to collect generated classes (produced by the weaving) to
     * define then after the weaving itself
     */
    class GeneratedClass {

        private final byte[] bytes;

        private final String name;

        public GeneratedClass(final String name, final byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * generated class handler to collect generated classes (produced by the
     * weaving) to define then after the weaving itself
     */
    class OSGiGeneratedClassHandler implements GeneratedClassHandler {

        private final ConcurrentLinkedQueue<GeneratedClass> classesToBeDefined;

        private final BcelWeakClassLoaderReference loaderRef;

        public OSGiGeneratedClassHandler(final ClassLoader loader) {
            loaderRef = new BcelWeakClassLoaderReference(loader);
            classesToBeDefined = new ConcurrentLinkedQueue<GeneratedClass>();
        }

        /**
         * Callback when we need to define a generated class in the JVM
         */
        public void acceptClass(final String name, final byte[] bytes) {
            try {
                if (shouldDump(name.replace('/', '.'), false)) {
                    dump(name, bytes, false);
                }
            } catch (final Throwable throwable) {
                throwable.printStackTrace();
            }
            classesToBeDefined.offer(new GeneratedClass(name, bytes));
        }

        public void defineGeneratedClasses() {
            while (!classesToBeDefined.isEmpty()) {
                final GeneratedClass generatedClass = classesToBeDefined.poll();
                if (generatedClass != null) {
                    defineClass(loaderRef.getClassLoader(),
                            generatedClass.getName(), generatedClass.getBytes());
                } else {
                    break;
                }
            }
        }

    }

    private static Trace trace = TraceFactory.getTraceFactory().getTrace(
            ClassLoaderWeavingAdaptor.class);

    private final ClassLoader classLoader;

    private Method defineClassMethod;

    private boolean initialized;

    private boolean initializing;

    private final String namespace;

    private final OSGiWeavingContext weavingContext;

    /**
     * The OSGi weaving adaptor provides a bridge to the AspectJ weaving adaptor
     * implementation for general classloaders. This weaving adaptor exists per
     * bundle that should be woven.
     * 
     * @param loader The classloader of the bundle to be woven
     * @param context The bridge to the weaving context
     * @param namespace The namespace of this adaptor, some kind of unique ID
     *            for this weaver
     */
    public OSGiWeavingAdaptor(final ClassLoader loader,
            final OSGiWeavingContext context, final String namespace) {
        super();
        this.classLoader = loader;
        this.weavingContext = context;
        this.namespace = namespace;
    }

    private void defineClass(final ClassLoader loader, final String name,
            final byte[] bytes) {
        if (trace.isTraceEnabled()) {
            trace.enter("defineClass", this,
                    new Object[] { loader, name, bytes });
        }
        Object clazz = null;
        debug("generating class '" + name + "'");

        try {
            if (defineClassMethod == null) {
                defineClassMethod = ClassLoader.class.getDeclaredMethod(
                        "defineClass",
                        new Class[] { String.class, bytes.getClass(),
                                int.class, int.class });
            }
            defineClassMethod.setAccessible(true);
            clazz = defineClassMethod.invoke(loader, new Object[] { name,
                    bytes, new Integer(0), new Integer(bytes.length) });
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof LinkageError) {
                warn("define generated class failed", e.getTargetException());
                // is already defined (happens for X$ajcMightHaveAspect interfaces since aspects are reweaved)
                // TODO maw I don't think this is OK and
            } else {
                warn("define generated class failed", e.getTargetException());
            }
        } catch (final Exception e) {
            warn("define generated class failed", e);
        }

        if (trace.isTraceEnabled()) {
            trace.exit("defineClass", clazz);
        }
    }

    /**
     * In some situations the weaving creates new classes on the fly that are
     * not part of the original bundle. This is the case when the weaver needs
     * to create closure-like constructs for the woven code.
     * 
     * This method returns a map of the generated classes (name -> bytecode) and
     * flushes the internal cache afterwards to avoid memory damage over time.
     * 
     * @param className The name of the class for which additional classes might
     *            got generated
     * @return the map of generated class names and bytecodes for those
     *         generated classes
     */
    public Map<String, byte[]> getGeneratedClassesFor(final String className) {
        final Map<?, ?> generated = this.generatedClasses;
        final Map<String, byte[]> result = new HashMap<String, byte[]>();

        final Iterator<?> generatedClassNames = generated.keySet().iterator();
        while (generatedClassNames.hasNext()) {
            final String name = (String) generatedClassNames.next();
            final IUnwovenClassFile unwovenClass = (IUnwovenClassFile) generated
                    .get(name);

            if (!className.equals(name)) {
                result.put(name, unwovenClass.getBytes());
            }
        }

        flushGeneratedClasses();
        return result;
    }

    /**
     * @see org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor#getNamespace()
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    /**
     * initialize the weaving adaptor
     */
    public void initialize() {
        if (!initializing) {
            if (!initialized) {
                initializing = true;
                super.initialize(classLoader, weavingContext);
                this.generatedClassHandler = new OSGiGeneratedClassHandler(
                        classLoader);
                initialized = true;
                initializing = false;

                if (AspectJWeavingStarter.verbose) {
                    if (isEnabled())
                        System.err
                                .println("[org.eclipse.equinox.weaving.aspectj] info weaving bundle '"
                                        + weavingContext.getClassLoaderName()
                                        + "'");
                    else
                        System.err
                                .println("[org.eclipse.equinox.weaving.aspectj] info not weaving bundle '"
                                        + weavingContext.getClassLoaderName()
                                        + "'");
                }
            }
        }
    }

    /**
     * @see org.aspectj.weaver.tools.WeavingAdaptor#weaveClass(java.lang.String,
     *      byte[], boolean)
     */
    @Override
    public byte[] weaveClass(final String name, byte[] bytes,
            final boolean mustWeave) throws IOException {

        /* Avoid recursion during adaptor initialization */
        if (!initializing) {
            if (!initialized) {
                initializing = true;
                super.initialize(classLoader, weavingContext);
                this.generatedClassHandler = new OSGiGeneratedClassHandler(
                        classLoader);
                initialized = true;
                initializing = false;
            }

            synchronized (this) {
                bytes = super.weaveClass(name, bytes, mustWeave);
            }

            ((OSGiGeneratedClassHandler) this.generatedClassHandler)
                    .defineGeneratedClasses();
        }
        return bytes;
    }

}
