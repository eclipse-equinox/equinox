/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Julien HENRY - Linux implementation
 *     Red Hat Inc. - add validation method to handle KDE failures
 *     Red Hat Inc. - modified to make JNA version
 *     Aleksandar Kurtakov - modified to make Java FFM version
 *******************************************************************************/
package org.eclipse.equinox.internal.security.linux;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.internal.security.auth.AuthPlugin;
import org.eclipse.equinox.internal.security.storage.Base64;
import org.eclipse.equinox.internal.security.storage.provider.IValidatingPasswordProvider;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

public class LinuxPasswordProvider extends PasswordProvider implements IValidatingPasswordProvider {

	/**
	 * The length of the randomly generated password in bytes
	 */
	private static final int PASSWORD_LENGTH = 64;

	private static final String SECRET_COLLECTION_DEFAULT = "default"; //$NON-NLS-1$

	/** Backs {@link #fEquinoxSchema}; lives as long as this provider. */
	private final Arena fSchemaArena = Arena.ofAuto();

	private final SecretSchema fEquinoxSchema = new SecretSchema(fSchemaArena, "org.eclipse.equinox", //$NON-NLS-1$
			SecretSchemaFlags.SECRET_SCHEMA_NONE);
	private LibSecret fLibSecret;
	private LibGio fLibGio;

	private void unlockSecretService() {

		fLibGio = LibGio.getInstance();
		fLibSecret = LibSecret.getInstance();

		// GObject references owned by this method, released on every exit path
		List<MemorySegment> owned = new ArrayList<>(3);
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment gerror = arena.allocate(ValueLayout.ADDRESS);
			owned.add(fLibGio.busGetSync(GBusType.G_BUS_TYPE_SESSION, MemorySegment.NULL, gerror));
			requireNoError(gerror, "Unable to get DBus session bus: "); //$NON-NLS-1$

			MemorySegment secretService = fLibSecret.serviceGetSync(SecretServiceFlags.SECRET_SERVICE_LOAD_COLLECTIONS,
					MemorySegment.NULL, gerror);
			owned.add(secretService);
			requireNoError(gerror, "Unable to get secret service: "); //$NON-NLS-1$

			MemorySegment defaultCollection = fLibSecret.collectionForAliasSync(secretService,
					arena.allocateFrom(SECRET_COLLECTION_DEFAULT), SecretCollectionFlags.SECRET_COLLECTION_NONE,
					MemorySegment.NULL, gerror);
			owned.add(defaultCollection);
			requireNoError(gerror, "Unable to get secret collection: "); //$NON-NLS-1$
			if (MemorySegment.NULL.equals(defaultCollection)) {
				throw new SecurityException("Unable to find default secret collection"); //$NON-NLS-1$
			}
			if (fLibSecret.collectionGetLocked(defaultCollection)) {
				MemorySegment list = fLibGio.listAppend(MemorySegment.NULL, defaultCollection);
				MemorySegment unlocked = arena.allocate(ValueLayout.ADDRESS);
				try {
					fLibSecret.serviceUnlockSync(secretService, list, MemorySegment.NULL, unlocked, gerror);
				} finally {
					// the out list holds a reference to each object it names
					fLibGio.listFreeFullUnref(unlocked.get(ValueLayout.ADDRESS, 0));
					fLibGio.listFree(list);
				}

				requireNoError(gerror, "Unable to unlock: "); //$NON-NLS-1$
			}
		} finally {
			owned.forEach(fLibGio::objectUnref);
		}

	}

	private String getMasterPassword() throws SecurityException {
		unlockSecretService();
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment gerror = arena.allocate(ValueLayout.ADDRESS);
			MemorySegment password = fLibSecret.passwordLookupSync(fEquinoxSchema.segment(), MemorySegment.NULL,
					gerror);

			requireNoError(gerror, ""); //$NON-NLS-1$
			if (MemorySegment.NULL.equals(password)) {
				throw new SecurityException("Unable to find password"); //$NON-NLS-1$
			}
			try {
				return Foreign.readString(password);
			} finally {
				fLibSecret.passwordFree(password);
			}
		}
	}

	private void saveMasterPassword(String password) throws SecurityException {
		unlockSecretService();
		try (Arena arena = Arena.ofConfined()) {
			MemorySegment gerror = arena.allocate(ValueLayout.ADDRESS);

			boolean stored = fLibSecret.passwordStoreSync(fEquinoxSchema.segment(),
					arena.allocateFrom(SECRET_COLLECTION_DEFAULT), arena.allocateFrom("Equinox master password"), //$NON-NLS-1$
					arena.allocateFrom(password), MemorySegment.NULL, gerror);

			requireNoError(gerror, ""); //$NON-NLS-1$
			if (!stored) {
				// libsecret can refuse the call without setting the error, and a
				// password that was never stored must not be handed out as valid
				throw new SecurityException("Unable to store password"); //$NON-NLS-1$
			}
		}
	}

	private void requireNoError(MemorySegment gerror, String details) {
		MemorySegment error = gerror.get(ValueLayout.ADDRESS, 0);
		if (!MemorySegment.NULL.equals(error)) {
			String message = GError.readMessage(error);
			fLibGio.errorFree(error);
			throw new SecurityException(details + message);
		}
	}

	@Override
	public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {

		boolean newPassword = (passwordType & CREATE_NEW_PASSWORD) != 0;
		boolean passwordChange = (passwordType & PASSWORD_CHANGE) != 0;

		if (!newPassword && !passwordChange) {
			try {
				return new PBEKeySpec(getMasterPassword().toCharArray());
			} catch (SecurityException e) {
				AuthPlugin.getDefault().logError(LinuxPasswordProviderMessages.getMasterPasswordError, e);
				return null;
			}
		}

		byte[] rawPassword = new byte[PASSWORD_LENGTH];
		new SecureRandom().nextBytes(rawPassword);
		String newPasswordString = Base64.encode(rawPassword);

		// add info message in the log
		AuthPlugin.getDefault().logMessage(LinuxPasswordProviderMessages.newMasterPasswordGenerated);

		try {
			saveMasterPassword(newPasswordString);
			return new PBEKeySpec(newPasswordString.toCharArray());
		} catch (SecurityException e) {
			AuthPlugin.getDefault().logError(LinuxPasswordProviderMessages.saveMasterPasswordError, e);
			return null;
		}
	}

	@Override
	public boolean isValid() {
		try {
			unlockSecretService();
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

}
