
package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.aspectj.weaver.loadtime.definition.DocumentParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * The Aspect definition registry parses and caches the aop.xml files on a per
 * bundle base.
 * 
 * The parsing and caching is done at bundle resolve time, the removal from the
 * cache is done at unresolved events. The initial state is re-created by the
 * initialize method.
 * 
 * @author Martin Lippert
 */
public class AspectDefinitionRegistry implements SynchronousBundleListener {

    private static final String AOP_CONTEXT_LOCATION_HEADER = "Eclipse-AspectContext"; //$NON-NLS-1$

    private static final String DEFAULT_AOP_CONTEXT_LOCATION = "META-INF/aop.xml"; //$NON-NLS-1$

    private final Map<Bundle, Definition> aspectDefinitions;

    /**
     * Create a registry to manage aspect definition files
     */
    public AspectDefinitionRegistry() {
        this.aspectDefinitions = new ConcurrentHashMap<Bundle, Definition>();
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(final BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED) {
            bundleResolved(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED) {
            bundleUnresolved(event.getBundle());
        }
    }

    /**
     * Do the parsing when a bundle is resolved
     * 
     * @param bundle The bundle that is resolved (should not be null)
     */
    public void bundleResolved(final Bundle bundle) {
        if (!this.aspectDefinitions.containsKey(bundle)) {
            this.aspectDefinitions.put(bundle,
                    parseDefinitionFromRequiredBundle(bundle));
        }
    }

    /**
     * Remove the cached aspect definitions from the aspect definition registry
     * 
     * @param bundle The bundle that got unresolved (should not be null)
     */
    public void bundleUnresolved(final Bundle bundle) {
        this.aspectDefinitions.remove(bundle);
    }

    /**
     * Returns the cached aspect definition for the given bundle, if the bundle
     * has an aspect definition
     * 
     * @param bundle The bundle for which the aspect definition should be
     *            returned
     * @return The parsed and cached aspect definition for the given bundle or
     *         null, if the bundle doesn't contain an aspect definition
     */
    public Definition getAspectDefinition(final Bundle bundle) {
        return this.aspectDefinitions.get(bundle);
    }

    /**
     * Finds the location of the aspect definition within the given bundle. The
     * default location is "META-INF/aop.xml", but if the bundles manifest
     * contains an entry for "Eclipse-AspectContext", that value is used to
     * search for the aop.xml file.
     * 
     * @param bundle The bundle for which to calculate the location of the
     *            aspect definition file
     * @return The path to the aspect definition relately to the given bundle
     */
    public String getDefinitionLocation(final Bundle bundle) {
        String aopContextHeader = (String) bundle.getHeaders().get(
                AOP_CONTEXT_LOCATION_HEADER);
        if (aopContextHeader != null) {
            aopContextHeader = aopContextHeader.trim();
            return aopContextHeader;
        }

        return DEFAULT_AOP_CONTEXT_LOCATION;
    }

    /**
     * Initialize the state of the aspect definition registry for the given
     * bundles. This should typically be called when the weaving service bundle
     * is started to set up the aspect definitions for all resolved bundles
     * 
     * @param bundles All bundles that should be taken into account and searched
     *            for aspect definitions
     * 
     */
    public void initialize(final Bundle[] bundles) {
        for (final Bundle bundle : bundles) {
            final int state = bundle.getState();
            if (state != Bundle.INSTALLED && state != Bundle.UNINSTALLED) {
                final Definition aspectDefinitions = parseDefinitionFromRequiredBundle(bundle);
                if (aspectDefinitions != null) {
                    this.aspectDefinitions.put(bundle, aspectDefinitions);
                }
            }
        }
    }

    /**
     * Parse the aspect definition for the given bundle, if there is one.
     * 
     * @param bundle The bundle for which the aspect definition should be parsed
     * @return The parsed definition or null, if the bundle does not provide an
     *         aspect definition
     */
    public Definition parseDefinitionFromRequiredBundle(final Bundle bundle) {
        try {
            final URL aopXmlDef = bundle
                    .getEntry(getDefinitionLocation(bundle));
            if (aopXmlDef != null) {
                return DocumentParser.parse(aopXmlDef);
            }
        } catch (final Exception e) {
            //            warn("parse definitions failed", e);
        }
        return null;
    }

}
