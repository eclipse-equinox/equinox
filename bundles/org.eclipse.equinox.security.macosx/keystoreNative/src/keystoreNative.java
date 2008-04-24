//
//  keystoreNative.java
//
//  Created by Kimberly Horne on 23/04/08.
//  Copyright (c) 2008, __MyCompanyName__. All rights reserved.
//

import java.util.*;

public class keystoreNative {

    static {
        // Ensure native JNI library is loaded
        System.loadLibrary("keyfile://localhost/Users/kim/keystoreNative/src/keystoreNative.javastoreNative");
    }

    public keystoreNative() {
        System.out.println("Instance created");
    }

    private native String getPassword(String serviceName, String accountName);
	private native void setPassword(String serviceName, String accountName, String password);

    public static void main (String args[]) {
        // insert code here...
        System.out.println("Started application");
        System.out.println("java.library.path = " + System.getProperty("java.library.path"));
        keystoreNative newjni = new keystoreNative();
		String password = "abby" + System.currentTimeMillis();
		System.out.println("Trying to set password to: " + password);
		try {
		newjni.setPassword("org.eclipse.sdk", "kim", password);
        String result = newjni.getPassword("org.eclipse.sdk", "kim");
	    System.out.println("Finished application. Answer is " + result);
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
    }

}
