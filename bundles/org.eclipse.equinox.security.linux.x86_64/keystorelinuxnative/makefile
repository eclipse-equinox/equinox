LIBSECRET_CFLAGS = `pkg-config --cflags libsecret-1`
LIBSECRET_LIBS = `pkg-config --libs libsecret-1`

CFLAGS = -shared -fPIC -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux"

all: ../libkeystorelinuxnative.so

clean:
	rm ../libkeystorelinuxnative.so ../target/keystoreLinuxNative.o
	
../libkeystorelinuxnative.so: ../target/keystoreLinuxNative.o
	$(CC) $(CFLAGS) -o $@ $< $(LIBSECRET_LIBS)
	
../target/keystoreLinuxNative.o: ../target keystoreLinuxNative.c
	$(CC) -c -fPIC -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux" -o $@ $(TESTFLAGS) $(LIBSECRET_CFLAGS) keystoreLinuxNative.c

../target: 
	mkdir -p ../target
