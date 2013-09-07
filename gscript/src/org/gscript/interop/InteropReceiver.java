package org.gscript.interop;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

public class InteropReceiver {

	static final String LOG_TAG = "InteropService";
	static final String INTEROP_SOCKET_ADDRESS = "/org.gscript/interop";
	static final String ACTION_INPUT_REQUEST = "org.gscript.action.INPUT_REQUEST";
	static final String ACTION_EXEC = "org.gscript.action.EXEC";

	SocketListener mSocketListener;
	boolean mStopListening;
	Context mContext;
	
	public InteropReceiver(Context context) {
		mContext = context;
	}
	
	
	public void start() {
		mSocketListener = new SocketListener();
		mSocketListener.setName(InteropReceiver.class.getSimpleName());
		mSocketListener.start();
	}

	
	public void stop() {
		mStopListening = true;
	}

	class SocketListener extends Thread {

		@Override
		public void run() {
			try {
				final LocalServerSocket server = new LocalServerSocket(
						INTEROP_SOCKET_ADDRESS);

				while (!mStopListening) {

					LocalSocket receiver = server.accept();
					if (receiver != null) {

						final InputStream input = receiver.getInputStream();
						
						/* build message from input stream */

						InteropMessage message = new InteropMessage(input);

						Log.d(LOG_TAG, "received message " + message);

						/*
						 * we do not handle the incoming messages here as we
						 * might block any new incoming messages so just
						 * dispatch the messages to a broadcast receiver
						 */

						mContext.sendBroadcast(message.toIntent());

						input.close();
					}

					receiver.close();
				}

			} catch (IOException e) {
			}
		}
	}

}
