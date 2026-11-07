``` java5
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

public class BundleProxyClassLoader extends ClassLoader {
    private Bundle bundle;
    private ClassLoader parent;

    public BundleProxyClassLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    public BundleProxyClassLoader(Bundle bundle, ClassLoader parent) {
        super(parent);
        this.parent = parent;
        this.bundle = bundle;
    }

    // Note: Both ClassLoader.getResources(...) and bundle.getResources(...) consult
    // the boot classloader. As a result, BundleProxyClassLoader.getResources(...)
    // might return duplicate results from the boot classloader. Prior to Java 5
    // Classloader.getResources was marked final. If your target environment requires
    // at least Java 5 you can prevent the occurence of duplicate boot classloader
    // resources by overriding ClassLoader.getResources(...) instead of
    // ClassLoader.findResources(...).
    public Enumeration findResources(String name) throws IOException {
        return bundle.getResources(name);
    }

    public URL findResource(String name) {
        return bundle.getResource(name);
    }

    public Class findClass(String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }

    public URL getResource(String name) {
        return (parent == null) ? findResource(name) : super.getResource(name);
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = (parent == null) ? findClass(name) : super.loadClass(name, false);
        if (resolve)
            super.resolveClass(clazz);

        return clazz;
    }
}
```

[Category:Equinox](Category:Equinox "wikilink")