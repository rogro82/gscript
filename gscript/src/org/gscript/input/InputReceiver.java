package org.gscript.input;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.Toast;

public class InputReceiver extends BroadcastReceiver {

	public static final String ACTION_REQUEST = "org.gscript.action.INPUT_REQUEST";
	public static final String ACTION_RESPONSE = "org.gscript.action.INPUT_RESPONSE";

	public static final String EXTRA_REQUEST_ID = "request_id";
	public static final String EXTRA_RESPONSE_CODE = "response_code";
	public static final String EXTRA_RESPONSE_OPT = "response_opt";

	static final String LOG_TAG = "InputReceiver";
	static final String QUERY_TYPE = "type";

	static final int INVALID_ID = -1;

	static final AtomicInteger mRequestId = new AtomicInteger(0);
	static final SparseArray<InputChannel> mInputChannels = new SparseArray<InputChannel>();
	static final Listener mChannelListener = new Listener();

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (ACTION_REQUEST.equals(intent.getAction())) {

			Uri data = intent.getData();

			String queryType = data.getQueryParameter(QUERY_TYPE);
			
			if (InputRequest.TYPE_TOAST.equals(queryType)) {

				/* handle simple toast broadcast */
				Bundle extras = intent.getExtras();
				String message = extras.getString(InputRequest.EXTRA_MESSAGE);
				
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText(context, message, duration);
				toast.show();
				
			} else if (queryType != null) {
				
				/* handle requests which except an input response */
				
				Bundle extras = intent.getExtras();
				String path = data.getPath();

				int requestId = mRequestId.getAndAdd(1);

				InputRequest request = new InputRequest(requestId, queryType,
						path, extras);

				mInputChannels.put(requestId, new InputChannel(context,
						request, mChannelListener));
			}
		}

		if (ACTION_RESPONSE.equals(intent.getAction())) {

			Bundle bundle = intent.getExtras();

			int requestId = bundle.getInt(EXTRA_REQUEST_ID, INVALID_ID);
			int responseCode = bundle.getInt(EXTRA_RESPONSE_CODE, INVALID_ID);
			String responseOpt = bundle.getString(EXTRA_RESPONSE_OPT);

			if (requestId != INVALID_ID) {

				InputChannel channel = mInputChannels.get(requestId);
				if (channel != null) {

					InputResponse response = new InputResponse(requestId,
							responseCode, responseOpt);

					channel.sendResponse(response);
				}
			}
		}
	}

	static class Listener implements InputChannel.ChannelListener {

		@Override
		public void OnInputChannelClosed(InputChannel channel) {

			int requestId = channel.getRequestId();
			mInputChannels.remove(requestId);

		}
	}
}
