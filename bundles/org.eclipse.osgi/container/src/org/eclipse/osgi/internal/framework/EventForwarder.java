/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.framework;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.osgi.dto.DTO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.dto.BundleDTO;

/**
 * The {@link EventForwarder} is a little tool to allow forwarding events from
 * inside a running container to a local listener port to allow monitor the
 * system state in a very lightweight way.
 * <p>
 * To enable this the Equinox Framework has to be started with the property
 * <code>org.eclipse.osgi.internal.framework.forwarder=[port number]</code>
 * </p>
 * <p>
 * <b>important</b> This is equinox internal behavior it can change any time in
 * any way including vanishing completely.
 * </p>
 * <p>
 * A message frame is constructed in the following way:
 * <code>[framesize in bytes][messagetype as length encoded utf-8 string][payload depending on the messagetype]</code>
 * </p>
 * 
 */
public class EventForwarder implements Closeable, SynchronousBundleListener {

	private static final String PROPERTY_KEY = "org.eclipse.osgi.internal.framework.forwarder"; //$NON-NLS-1$
	private Socket socket;
	protected volatile boolean closed;
	private ObjectOutputStream outputStream;
	private BlockingQueue<DTO> eventQueue = new LinkedBlockingQueue<>();

	public EventForwarder(int port) throws IOException {
		socket = new Socket(InetAddress.getByName(null), port);
	}

	public static EventForwarder create(BundleContext bundleContext) {
		String property = bundleContext.getProperty(EventForwarder.PROPERTY_KEY);
		if (property == null) {
			// not enabled
			return null;
		}
		int port = Integer.parseInt(property);
		EventForwarder eventForwarder;
		try {
			eventForwarder = new EventForwarder(port);
		} catch (IOException e) {
			System.err.println("Can't enable event forwarding: " + e); //$NON-NLS-1$
			return null;
		}
		try {
			eventForwarder.start(bundleContext);
		} catch (IOException e) {
			eventForwarder.close();
			System.err.println("Can't start event forwarding: " + e); //$NON-NLS-1$
			return null;
		}
		return eventForwarder;

	}

	private void start(BundleContext bundleContext) throws IOException {
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		bundleContext.addBundleListener(this);
		for (Bundle bundle : bundleContext.getBundles()) {
			eventQueue.add(bundle.adapt(BundleDTO.class));
		}
		Thread thread = new Thread(new EventSender(this, outputStream, eventQueue), "Equinox-Event-Forwarder"); //$NON-NLS-1$
		thread.setDaemon(true);
		thread.start();
	}



	@Override
	public void close() {
		closed = true;
		eventQueue.add(new Goodby());
		if (outputStream != null) {
			synchronized (outputStream) {
				try {
					outputStream.writeInt(0); // good by!
					outputStream.close();
				} catch (IOException e) {
					// nothing we can do...
				}
			}
		}
		try {
			socket.close();
		} catch (IOException e) {
			// nothing to do here
		}
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		eventQueue.add(event.getBundle().adapt(BundleDTO.class));
	}

	private static final class EventSender implements Runnable {

		private static final Charset CHARSET = StandardCharsets.UTF_8;
		private static final String MSG_BUNDLE = "BundleDTO"; //$NON-NLS-1$

		private EventForwarder forwarder;
		private ObjectOutputStream outputStream;
		private BlockingQueue<DTO> eventQueue;

		public EventSender(EventForwarder forwarder, ObjectOutputStream outputStream, BlockingQueue<DTO> eventQueue) {
			this.forwarder = forwarder;
			this.outputStream = outputStream;
			this.eventQueue = eventQueue;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					DTO dto = eventQueue.take();
					if (dto instanceof Goodby) {
						return;
					}
					if (dto instanceof BundleDTO) {
						sendBundle((BundleDTO) dto);
					}
				} catch (InterruptedException e) {
					return;
				}
			}
		}

		private void sendBundle(BundleDTO dto) {
			if (forwarder.closed) {
				return;
			}
			if (dto != null) {
				synchronized (outputStream) {
					byte[] msg = MSG_BUNDLE.getBytes(CHARSET);
					byte[] bsn = dto.symbolicName.getBytes(CHARSET);
					byte[] version = dto.version.getBytes(CHARSET);
					int size = msg.length + 1;
					size += 8;// id
					size += 4;// state
					size += 8;// last modified
					size += bsn.length + 1;
					size += version.length + 1;
					try {
						outputStream.writeInt(size);
						writeString(msg);
						outputStream.writeLong(dto.id);
						outputStream.writeInt(dto.state);
						writeString(bsn);
						writeString(version);
						outputStream.writeLong(dto.lastModified);
						outputStream.flush();
					} catch (IOException e) {
						forwarder.close();
					}
				}
			}
		}

		private void writeString(byte[] stringbytes) throws IOException {
			outputStream.writeInt(stringbytes.length);
			outputStream.write(stringbytes);
		}

	}

	private static final class Goodby extends DTO {

		public Goodby() {
		}
	}

}
