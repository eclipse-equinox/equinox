/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * This class implements a storage for users, passwords and roles. The data is stored in a
 * properties-like file in the format /ssh/<username>/password=<password> and 
 * /ssh/<username>/roles=<comma_separated_list_of_roles>
 * 
 *
 */
public class SecureUserStore {

	private static final String USER_STORE_FILE_NAME = "org.eclipse.equinox.console.jaas.file";
	private static final String PASSWORD_KEY = "password";
	private static final String ROLES_KEY = "roles";
	private static final String SSH_PREFIX = "/ssh";
	private static final String DELIMITER = "/";
	private static final int USERNAME_INDEX = 2;
	private static final int KEY_ELEMENTS_COUNT = 4;
	
	/**
	 * Gets the usernames of all users.
	 * 
	 * @return String array containing the usernames
	 */
	public static String[] getUserNames() {
		String userFileLoc = null;
		InputStream in = null;
		
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null;
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			Set<String> userNames = new HashSet<String>();
			for (Object key : users.keySet()) {
				if (!(key instanceof String)) {
					continue;
				}
				String[] parts = ((String) key).split(DELIMITER);
				// since the key starts with DELIMITER, the first element of key.split(DELIMITER) is an empty string
				// that is why the result is {"", "ssh", "<username>", "password"} or {"", "ssh", "<username>", "roles"}
				if (parts.length < KEY_ELEMENTS_COUNT) {
					continue;
				}
				userNames.add(parts[USERNAME_INDEX]);
			}

			return userNames.toArray(new String[0]);
		
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
		}
	}
	
	public static String getPassword(String username) {
		return getProperty(username, PASSWORD_KEY);
	}
	
	public static String getRoles(String username) {
		return getProperty(username, ROLES_KEY);
	}
	
	/**
	 * Stores a user entry to the store.
	 * 
	 * @param username the name of the user
	 * @param password the password of the user
	 * @param roles comma-separated list of the roles of the user
	 */
	public static void putUser(String username, String password, String roles) {
		String userFileLoc = null;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			if (existsUser(username, users)){
				throw new IllegalArgumentException("The user already exists!");
			}
			
			if (roles == null) {
				roles = "";
			}
			
			String userPassKey = constructPropertyName(username, PASSWORD_KEY);
			String userRolesKey = constructPropertyName(username, ROLES_KEY);
			users.put(userPassKey, password);
			users.put(userRolesKey, roles);
			
			out = new FileOutputStream(userFileLoc);
			try {
				users.store(out, null);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot store properties in file " + userFileLoc);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}
	
	/**
	 * Adds roles for a particular user
	 * 
	 * @param username user to add roles to 
	 * @param roles comma-separated list of new roles for the user 
	 */
	public static void addRoles(String username, String roles) {
		String userFileLoc = null;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			if (roles == null || roles.length() == 0) {
				return;
			}
			
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			String userRolesKey = constructPropertyName(username, ROLES_KEY);
			String currentRoles = (String)users.remove(userRolesKey);
			Set<String> rolesSet = new HashSet<String>();
			
			if (currentRoles.length() > 0) {
				for (String role : currentRoles.split(",")) {
					rolesSet.add(role);
				}
			}
			
			for (String role : roles.split(",")) {
				rolesSet.add(role);
			}
			
			StringBuilder builder = new StringBuilder();
			for (String role : rolesSet) {
				builder.append(role);
				builder.append(",");
			}
			builder.deleteCharAt(builder.lastIndexOf(","));
			
			users.put(userRolesKey, builder.toString());
			
			out = new FileOutputStream(userFileLoc);
			try {
				users.store(out, null);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot store properties in file " + userFileLoc);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}
	
	/**
	 * Removes roles from a user
	 * 
	 * @param username user to remove roles from 
	 * @param rolesToRemove comma-separated list of roles to be removed
	 */
	public static void removeRoles(String username, String rolesToRemove) {
		String userFileLoc = null;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			if(rolesToRemove == null || rolesToRemove.length() == 0) {
				return;
			}
			
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			String userRolesKey = constructPropertyName(username, ROLES_KEY);
			String currentRoles = (String)users.remove(userRolesKey);
			Set<String> rolesSet = new HashSet<String>();
			
			for (String role : currentRoles.split(",")) {
				rolesSet.add(role);
			}
			
			for (String role : rolesToRemove.split(",")) {
				rolesSet.remove(role);
			}
			
			StringBuilder builder = new StringBuilder();
			for (String role : rolesSet) {
				builder.append(role);
				builder.append(",");
			}
			builder.deleteCharAt(builder.lastIndexOf(","));
			
			users.put(userRolesKey, builder.toString());
			
			out = new FileOutputStream(userFileLoc);
			try {
				users.store(out, null);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot store properties in file " + userFileLoc);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}
	
	/**
	 * Removes an entry for the user from the store.
	 * 
	 * @param username user to be removed
	 */
	public static void deleteUser(String username) {
		String userFileLoc = null;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			if (!existsUser(username, users)){
				throw new IllegalArgumentException("The user does not exist!");
			}
			
//			Set<Object> keys = users.keySet();
//			for (Object key : keys) {
//				if ((key instanceof String) && ((String) key).contains(DELIMITER + username + DELIMITER)) {
//						users.remove(key);
//				}
//			}
			String rolesProperty = constructPropertyName(username, ROLES_KEY);
			String passwordProperty = constructPropertyName(username, PASSWORD_KEY);
			
			users.remove(rolesProperty);
			users.remove(passwordProperty);
			
			out = new FileOutputStream(userFileLoc);
			try {
				users.store(out, null);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot store properties in file " + userFileLoc);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}
	
	/**
	 * Removes the password for a user
	 * 
	 * @param username user to reset the password
	 */
	public static void resetPassword(String username) {
		String userFileLoc = null;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			if (!existsUser(username, users)){
				throw new IllegalArgumentException("The user does not exist!");
			}
			
			for (Object key : users.keySet()) {
				if (key instanceof String && ((String) key).contains(DELIMITER + username + DELIMITER + PASSWORD_KEY)) {
					users.remove(key);
					break;
				}
			}
			
			out = new FileOutputStream(userFileLoc);
			try {
				users.store(out, null);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot store properties in file " + userFileLoc);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}
	
	/**
	 * Sets or changes the password for a user 
	 * 
	 * @param username user to set tha password for
	 * @param password the new password
	 */
	public static void setPassword(String username, String password) {
		String userFileLoc = null;
		InputStream in = null;
		OutputStream out = null;
		
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			if (!existsUser(username, users)){
				throw new IllegalArgumentException("The user does not exist!");
			}
			
			String passwordPropertyName = constructPropertyName(username, PASSWORD_KEY);
			for (Object key : users.keySet()) {
				if ((key instanceof String) && ((String) key).contains(passwordPropertyName)) {
					users.remove(key);
					break;
				}
			}
			
			users.put(passwordPropertyName, password);
			
			out = new FileOutputStream(userFileLoc);
			try {
				users.store(out, null);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot store properties in file " + userFileLoc);
			}
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
			
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}
	
	/**
	 * CHecks if an entry for a user exists in the store
	 *  
	 * @param username user to check
	 * @return true if there is an entry for this user in the store, false otherwise
	 */
	public static boolean existsUser(String username) {
		String userFileLoc = null;
		InputStream in = null;
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			return existsUser(username, users);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
		}
	}
	
	/**
	 * Creates the store file if it does not exist
	 * 
	 * @throws IOException
	 */
	public static void initStorage() throws IOException {
		String userFileLoc = getFileLocation();
		File file = new File(userFileLoc);
		if (!file.exists()) {
			OutputStream out = null;
			try {
				Properties props = new Properties();
				out = new FileOutputStream(file);
				props.store(out, null);
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}
	}
	
	private static String getProperty(String username, String propertyName) {
		String userFileLoc = null;
		InputStream in = null;
		try {
			userFileLoc = getFileLocation();
			in = new FileInputStream(userFileLoc);
			Properties users = null; 
			
			try {
				users = populateUserStore(in);
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot load properties from file " + userFileLoc);
			}
			
			return users.getProperty(constructPropertyName(username, propertyName));
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File " + userFileLoc + " does not exist");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					//do nothing
				}
			}
		}
	}
	
	private static Properties populateUserStore(InputStream in) throws IOException {
		Properties userProperties = new Properties();
		userProperties.load(in);
		return userProperties;
	}
	
	private static String getFileLocation(){
		String userFileLoc = System.getProperty(USER_STORE_FILE_NAME);
		if (userFileLoc == null) {
			throw new IllegalArgumentException("Property " + USER_STORE_FILE_NAME + " is not set; cannot use JAAS authentication");
		}
		
		return userFileLoc;
	}
	
	private static String constructPropertyName(String user, String propertyName) {
		StringBuilder builder = new StringBuilder();
		builder.append(SSH_PREFIX);
		builder.append(DELIMITER);
		builder.append(user);
		builder.append(DELIMITER);
		builder.append(propertyName);
		return builder.toString();
	}
	
	private static boolean existsUser(String username, Properties users) {
		for (Object user : users.keySet()) {
			if (user instanceof String && ((String) user).contains(DELIMITER + username + DELIMITER)) {
				 return true;
			}
		}
		return false;
	}
	
}
