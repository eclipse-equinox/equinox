/*******************************************************************************
 * Copyright (c) 2026, 2026 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.launcher;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public abstract class SplashScreen {

	public abstract long getHandle();

	public abstract void update();

	public abstract boolean takeDown();

	static SplashScreen show(Path splashLocation, URL configurationLocation, String platformConfiguration) {
		try {
			URLClassLoader loader = createSWTClassLoader(configurationLocation, platformConfiguration);
			if (loader != null) {
				try (loader) {
					Class<?> clz = loader.loadClass(SplashScreen.class.getName() + "$SWTSplashLoader"); //$NON-NLS-1$
					Method load = clz.getDeclaredMethod("load", Path.class); //$NON-NLS-1$
					SplashScreen splash = (SplashScreen) load.invoke(null, splashLocation);
					splash.update(); // Update once to ensure everything is loaded
					return splash;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create splash screen", e); //$NON-NLS-1$
		}
		return null;
	}

	private static final Pattern COMMA = Pattern.compile(","); //$NON-NLS-1$
	private static final String REFERENCE_PREFIX = "reference:"; //$NON-NLS-1$

	//TODO: Explain this construct and its rational here in code

	private static URLClassLoader createSWTClassLoader(URL configurationLocation, String platformConfiguration) throws IOException, URISyntaxException {
		List<String> swtBundleNames = List.of( //
				"org.eclipse.swt." + platformConfiguration, //$NON-NLS-1$
				"org.eclipse.swt.svg"); //$NON-NLS-1$

		List<URL> swtBundles = new ArrayList<>();
		String osgiBundles = System.getProperty("osgi.bundles"); //$NON-NLS-1$
		if (osgiBundles != null) {
			addBundleLocations(swtBundles, COMMA.splitAsStream(osgiBundles) //
					.filter(b -> swtBundleNames.stream().anyMatch(b::contains)) //
					.map(b -> b.startsWith(REFERENCE_PREFIX) ? b.substring(REFERENCE_PREFIX.length()) : b));

		}
		if (swtBundles.isEmpty()) {
			long start = System.currentTimeMillis();
			Path bundlesInfo = Path.of(configurationLocation.toURI()).resolve("org.eclipse.equinox.simpleconfigurator/bundles.info"); //$NON-NLS-1$
			if (Files.isRegularFile(bundlesInfo)) {
				try (var lines = Files.lines(bundlesInfo)) {
					List<String> prefixes = swtBundleNames.stream().map(n -> n + ",").toList(); //$NON-NLS-1$
					addBundleLocations(swtBundles, lines.filter(l -> prefixes.stream().anyMatch(p -> l.startsWith(p))).map(l -> l.split(",")[2])); //$NON-NLS-1$
				}
			}
			System.out.println("Read took: " + (System.currentTimeMillis() - start));
		}
		if (swtBundles.isEmpty()) {
			return null;
		}
		swtBundles.add(SplashScreen.class.getProtectionDomain().getCodeSource().getLocation());
		return new URLClassLoader(swtBundles.toArray(URL[]::new), null) {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				//Ensure the splash-screen class is loaded from the application classloader, but everything that references SWT is loaded from this CL.
				if (SplashScreen.class.getName().equals(name)) {
					return SplashScreen.class;
				}
				return super.findClass(name);
			}
		};
	}

	static void addBundleLocations(List<URL> swtBundles, Stream<String> map) {
		map.map(l -> {
			try {
				return URI.create(l).toURL();
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}).forEach(swtBundles::add);
	}

	public static class SWTSplashLoader {

		public static SplashScreen load(Path imagePath) {
			//TODO: Run this in a different thread and just interrupt it, when the splash is to take down?
			Display display = new Display() {
				@Override
				protected void checkSubclass() {
					// Permit sub-classing
				}

				@Override
				protected void init() {
					super.init();
				}
			};
			Shell shell = new Shell(display, SWT.NO_TRIM | SWT.NO_MOVE);
			shell.setLayout(new FillLayout());
			Label label = new Label(shell, SWT.NONE);
			Image image = new Image(display, imagePath.toString());
			label.setImage(image);
			shell.pack();

			Rectangle displayBounds = display.getBounds();
			Rectangle imageBounds = image.getBounds();
			shell.setLocation(displayBounds.x + (displayBounds.width - imageBounds.width) / 2, displayBounds.y + (displayBounds.height - imageBounds.height) / 2);
			shell.open();

			return new SplashScreen() {
				@Override
				public long getHandle() {
					//TODO: Check the exact value? The shells handle?
					return shell.handle;
				}

				@Override
				public void update() {
					if (!shell.isDisposed()) {
						display.readAndDispatch();
					}
				}

				@Override
				public boolean takeDown() {
					image.dispose();
					shell.dispose();
					// display.dispose(); //TODO: This crashes the application (maybe related to the broken state of main SWT).
					return true; //TODO: check this somehow?!
				}
			};
		}

	}

}
