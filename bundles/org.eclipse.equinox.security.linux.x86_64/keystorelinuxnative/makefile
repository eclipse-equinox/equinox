LIBSECRET_CFLAGS = `pkg-config --cflags libsecret-1`
LIBSECRET_LIBS = `pkg-config --libs libsecret-1`

CFLAGS := $(CFLAGS) -fPIC -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux" $(LIBSECRET_CFLAGS)
LDFLAGS := $(LDFLAGS) -shared $(LIBSECRET_LIBS)

all: ../libkeystorelinuxnative.so

clean:
	rm ../libkeystorelinuxnative.so ../target/keystoreLinuxNative.o

../libkeystorelinuxnative.so: ../target/keystoreLinuxNative.o
	$(CC) $(LDFLAGS) -o $@ $<

../target/keystoreLinuxNative.o: ../target keystoreLinuxNative.c
	$(CC) -c $(CFLAGS) -o $@ $(TESTFLAGS) keystoreLinuxNative.c

../target:
	mkdir -p ../target
