all:
	./build.sh

clean:
	./build.sh clean

# For devs:
# Convienience target to move the launcher and the relevant *.so into your development eclipse folder for testing/verification.
# Typically, you can download the latest integration build from here: http://download.eclipse.org/eclipse/downloads/, and use it as your dev eclipse.
devinstall:
	# You should define "DEV_ECLIPSE" to be the directory of your testing eclipse, containing the 'eclipse' executable.
	# I.e the output of 'pwd' (Note: without trailing forward slash).
	# export DEV_ECLIPSE="/home/YOUR_USER/Downloads/eclipse-SDK-I20180115-2000-linux-gtk-x86_64/eclipse"
	cp eclipse ${DEV_ECLIPSE}/
	cp eclipse_*.so ${DEV_ECLIPSE}/plugins/org.eclipse.equinox.launcher.gtk.linux.x86_*/
