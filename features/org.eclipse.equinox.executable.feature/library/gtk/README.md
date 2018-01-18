

# Building

To build:

    ./bulid.sh  

This will read the relevant make files.

To clean:

    ./build.sh clean

# Developer notes:

To inject into a test eclipse instance for testing:  

    cp eclipse (eclipseDir)/eclipse  
    cp eclipse_16xx.so (eclipseDir)/plugins/org.eclipse.equinox.launcher.<ws>.<os>.<arch>*/
