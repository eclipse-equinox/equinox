

# Building

    ./build.sh
    ./build.sh clean

# Developer notes:

See make_linux.mak for info on how to build/test. See dev_build_install target. Common use case:

   export DEV_ECLIPSE="your/dev/eclipse"
   make -f make_linux.mak clean dev_build_install
   
