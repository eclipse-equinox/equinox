package org.eclipse.core.runtime;

/**
 * <p>
 * An {@link AdapterProxy} can be used to interface the Eclipse Adapter
 * Framework with other techniques. To do so one has to provide a generic
 * {@link IAdapterFactory} that adapts this other frameworks objects to the
 * {@link AdapterProxy} interface, then as a last resort, the Eclipse Adapter
 * Framework will ask this proxy as if the original object would have
 * implemented {@link IAdaptable}.
 * </p>
 * <p>
 * One example is the OSGi <a href=
 * "https://docs.osgi.org/specification/osgi.cmpn/7.0.0/util.converter.html">Converter
 * Specification</a> that allows to adapt/convert objects in an extensible way,
 * therefore it is not possible to register a "classic" {@link IAdapterFactory}
 * because the types that are probably convertible are unknown in advance. Also
 * the objects itself can't be made to implement the {@link IAdaptable}
 * interface. An implementation then might look like this:
 * </p>
 * 
 * <pre>
 * &#64;Component
 * &#64;AdapterTypes(adaptableClass = Object.class, adapterNames = AdapterProxy.class)
 * public class OSGiConverterProxyFactory implements IAdapterFactory {
 * 
 * 	&#64;Reference
 * 	private Converter converter;
 * 
 * 	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
 * 		Converting converting = converter.convert(adaptableObject);
 * 		return converting.to(adapterType);
 * 	}
 * 
 * }
 * </pre>
 * 
 * @since 3.20
 */
public interface AdapterProxy extends IAdaptable {
	// This is a specialized type that do not define any methods
}
