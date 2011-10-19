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

package org.eclipse.equinox.console.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.eclipse.equinox.console.common.Scanner;
import org.eclipse.equinox.console.storage.DigestUtil;
import org.eclipse.equinox.console.storage.SecureUserStore;
import org.eclipse.equinox.console.common.ConsoleInputScanner;

/**
 * This class provides commands for administering users: adding, removing and listing users; setting or changing password;
 * resetting password; adding and removing roles
 * 
 *
 */
public class UserAdminCommand {
    private static final String INPUT_SCANNER = "INPUT_SCANNER";
    private static final String SSH_INPUT_SCANNER = "SSH_INPUT_SCANNER";
    private static final String DEFAULT_USER = "equinox";
	private static final int MINIMAL_PASSWORD_LENGTH = 8;
	private static final int PASSWORD_INPUT_TRIALS_LIMIT = 3;
	
	/**
	 * Command for adding a user
	 *  
	 * @param args command line arguments in the format -username <username> -password <password> -roles <comma-separated list of user roles (optional)>
	 * @throws Exception
	 */
	@Descriptor("Add user with password and roles")
	public void addUser(@Descriptor("-username <username>\r\n-password <password>\r\n-roles <comma-separated list of user roles (optional)>") String[] args) throws Exception {
		String username = null;
		String password = null;
		String roles = "";
		
		for (int i = 0; i < args.length; i++) {
			if ("-username".equals(args[i]) && i < args.length - 1) {
				username = args[i + 1];
				i++;
			} else if ("-password".equals(args[i]) && i < args.length - 1) {
				password = args[i + 1];
				i++;
			} else if ("-roles".equals(args[i]) && i < args.length - 1) {
				roles = args[i + 1];
				i++;
			}
		}
		
		if (! validateUsername(username)) {
			throw new Exception("Invalid username");
		}
				
		if (password == null) {
			throw new Exception("Password not specified");
		}
		
		if (password.length() < MINIMAL_PASSWORD_LENGTH) {
			throw new Exception("Password should be at least 8 symblos");
		}
		
		SecureUserStore.putUser(username, DigestUtil.encrypt(password), roles);
		
		if(SecureUserStore.existsUser(DEFAULT_USER)) {
			SecureUserStore.deleteUser(DEFAULT_USER);
		}
	}
	
	/**
	 * Command for setting or changing the password of a user.
	 * 
	 * @param args command-line arguments in the format -username <username> -password <password>
	 * @throws Exception
	 */
	@Descriptor("Set or change password")
	public void setPassword(@Descriptor("-username <username>\r\n-password <password>") String[] args) throws Exception {
		String username = null;
		String password = null;
		
		for (int i = 0; i < args.length; i++) {
			if ("-username".equals(args[i]) && i < args.length - 1) {
				username = args[i + 1];
				i++;
			} else if ("-password".equals(args[i]) && i < args.length - 1) {
				password = args[i + 1];
				i++;
			}
		}
		
		if (! validateUsername(username)) {
			throw new Exception("Invalid username");
		}
				
		if (password == null) {
			throw new Exception("Password not specified");
		}
		
		if (password.length() < MINIMAL_PASSWORD_LENGTH) {
			throw new Exception("Password should be at least 8 symblos");
		}
		
		SecureUserStore.setPassword(username, DigestUtil.encrypt(password));
	}
	
	/**
	 * Command for adding a user. The command interactively asks for username, password and roles; the
	 * input plain text password is encrypted before storing.
	 * 
	 * @param session 
	 * @return true if the user was successfully added
	 * 
	 * @throws Exception
	 */
	@Descriptor("Add user with password and roles interactively")
	public boolean addUser(final CommandSession session) throws Exception {

		ConsoleInputScanner inputScanner = (ConsoleInputScanner) session.get(INPUT_SCANNER);
		Scanner scanner = (Scanner) session.get(SSH_INPUT_SCANNER);

		try {
			// switch off the history so that username, password and roles will not be saved in console history
			if (scanner != null) {
				inputScanner.toggleHistoryEnabled(false);
			}			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String username = readUsername(reader);
			if (!validateUsername(username)) {
				System.out.println("Invalid username");
				return false;
			}
			
			if (SecureUserStore.existsUser(username)) {
				System.out.println("Username already exists");
				return false;
			}

			// switch off the echo so that the password will not be printed in the console
			if (scanner != null) {
				scanner.toggleEchoEnabled(false);
			}
			String password = readPassword(reader);
			if (password == null){
				return false;
			}
			if (scanner != null) {
				scanner.toggleEchoEnabled(true);
			}

			String roles = readRoles(reader);
			if (roles == null) {
				return false;
			}

			SecureUserStore.putUser(username, DigestUtil.encrypt(password), roles);
			
			if(SecureUserStore.existsUser(DEFAULT_USER)) {
				SecureUserStore.deleteUser(DEFAULT_USER);
			}
		} finally {
			if (scanner != null) {
				inputScanner.toggleHistoryEnabled(true);
				scanner.toggleEchoEnabled(true);
			}	
		}

		return true;
	}
	
	@Descriptor("Delete user")
	public void deleteUser(@Descriptor("username of the user to be deleted") String username) throws Exception {
		if (SecureUserStore.existsUser(username)) {
			SecureUserStore.deleteUser(username);
		}
	}
	
	/**
	 * Command to remove the password for a user
	 * 
	 * @param username user to remove the password for
	 * @throws Exception
	 */
	@Descriptor("Reset password")
	public void resetPassword(@Descriptor("username of the user whose password will be reset") String username) throws Exception {
		if (!SecureUserStore.existsUser(username)) {
			throw new Exception("Such user does not exist");
		}
		
		SecureUserStore.resetPassword(username);
	}
	
	/**
	 * Command to set or change the password for a user; the command asks interactively for the new password; the
	 * input plain text password is encrypted before storing.
	 * 
	 * @param session 
	 * @param username the user whose password will be changed
	 * @throws Exception
	 */
	@Descriptor("Set or change password")
	public void setPassword(final CommandSession session, @Descriptor("Username of the user whose password will be changed") String username) throws Exception {
		if ("".equals(username)) {
			System.out.println("Username not specified");
			return;
		}		
		
		if (!SecureUserStore.existsUser(username)) {
			throw new Exception("Such user does not exist");
		}

		ConsoleInputScanner inputScanner = (ConsoleInputScanner) session.get(INPUT_SCANNER);
		Scanner scanner = (Scanner) session.get(SSH_INPUT_SCANNER);

		try {
			// switch off echo and history so that the password is neither echoed to the console, nor saved in history
			if (scanner != null) {
				inputScanner.toggleHistoryEnabled(false);
				scanner.toggleEchoEnabled(false);
			}			

			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String password = readPassword(reader);
			if (password == null) {
				return;
			}
			
			SecureUserStore.setPassword(username, DigestUtil.encrypt(password));
		} finally {
			if (scanner != null) {
				inputScanner.toggleHistoryEnabled(true);
				scanner.toggleEchoEnabled(true);
			}	
		}
	}
	
	/**
	 * Command to add roles to a user
	 * 
	 * @param args command line arguments in the format -username <username>\r\n-roles <comma-separated list of roles to add>
	 * @throws Exception
	 */
	@Descriptor("Add roles to user")
	public void addRoles(@Descriptor("-username <username>\r\n-roles <comma-separated list of roles to add>") String[] args) throws Exception {
		String username = null;
		String roles = "";
		
		for (int i = 0; i < args.length; i++) {
			if ("-username".equals(args[i]) && i < args.length - 1) {
				username = args[i + 1];
				i++;
			} else if ("-roles".equals(args[i]) && i < args.length - 1) {
				roles = args[i + 1];
				i++;
			}
		}
		
		if (username == null) {
			throw new Exception("Username not specified");
		}
		
		if("".equals(roles)) {
			return;
		}
		
		if (!SecureUserStore.existsUser(username)) {
			throw new Exception("Such user does not exist");
		}
							
		SecureUserStore.addRoles(username, roles);
	}
	
	/**
	 * Command to remove roles for a particular user
	 * 
	 * @param args command line arguments in the format -username <username>\r\n-roles <comma-separated list of roles to remove>
	 * @throws Exception
	 */
	@Descriptor("Remove user roles")
	public void removeRoles(@Descriptor("-username <username>\r\n-roles <comma-separated list of roles to remove>") String[] args) throws Exception {
		String username = null;
		String roles = "";
		
		for (int i = 0; i < args.length; i++) {
			if ("-username".equals(args[i]) && i < args.length - 1) {
				username = args[i + 1];
				i++;
			} else if ("-roles".equals(args[i]) && i < args.length - 1) {
				roles = args[i + 1];
				i++;
			}
		}
		
		if (username == null) {
			throw new Exception("Username not specified");
		}
		
		if("".equals(roles)) {
			return;
		}
		
		if (!SecureUserStore.existsUser(username)) {
			throw new Exception("Such user does not exist");
		}
		
		SecureUserStore.removeRoles(username, roles);
	}
	
	/**
	 * Command to list available users
	 * 
	 * @throws Exception
	 */
	@Descriptor("Lists available users")
	public void listUsers()	throws Exception {
		
		String[] users = SecureUserStore.getUserNames();
		
		if(users.length == 0) {
			System.out.println("No users available");
			return;
		}
		
		for(String user : users) {
			System.out.println(user);
		}
	}

	private String readPassword(BufferedReader reader) {
		String password = null;
		int count = 0;

		while (password == null && count < PASSWORD_INPUT_TRIALS_LIMIT){
			System.out.print("password: ");
			System.out.flush();

			try {
				password = reader.readLine();
			} catch (IOException e) {
				System.out.println("Error while reading password");
				return null;
			}


			if (password == null || "".equals(password)) {
				System.out.println("Password not specified");
				password = null;
			} else if (password.length() < MINIMAL_PASSWORD_LENGTH) {
				System.out.println("Password should be at least 8 symblos");
				password = null;
			}
			
			count++;
		}
		
		if (password == null) {
			return null;
		}

		String passwordConfirmation = null;
		count = 0;

		while (passwordConfirmation == null && count < PASSWORD_INPUT_TRIALS_LIMIT){
			System.out.print("Confirm password: ");
			System.out.flush();

			try {
				passwordConfirmation = reader.readLine();
				if (!password.equals(passwordConfirmation)) {
					System.out.println("The passwords do not match!");
					passwordConfirmation = null;
				}
			} catch (IOException e) {
				System.out.println("Error while reading password");
				return null;
			}
			
			count++;
		}
		if (passwordConfirmation == null){
			return null;
		}
		return password;
	}
	
	private String readUsername (BufferedReader reader) {
		System.out.print("username: ");
		System.out.flush();
		String username = null;

		try {
			username = reader.readLine();
		} catch (IOException e) {
			System.out.println("Error while reading username");
			return null;
		}

		if (username == null || "".equals(username)) {
			System.out.println("Username not specified");
			return null;
		}

		return username;
	}
	
	private String readRoles (BufferedReader reader){
		//roles input validation
		System.out.print("roles: ");
		System.out.flush();
		String roles = null;
		try {
			roles = reader.readLine();
		} catch (IOException e) {
			System.out.println("Error while reading roles");
			return null;
		}

		if (roles == null) {
			roles = "";
		}
		return roles;
	}
	
	private static boolean validateUsername (String username){
		if( username == null){
			return false; 
		}else{
			Pattern allowedChars = Pattern.compile("[A-Za-z0-9_.]+");
			Matcher matcher = allowedChars.matcher(username);
			return matcher.matches();
		}
	}

}
