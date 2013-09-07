package org.gscript.input;

import java.io.OutputStream;

import org.gscript.R;
import org.gscript.input.InputDialog;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class InputChannel  {

	static final String LOG_TAG = "InputChannel";
	static final int INPUT_NOTIFICATION_ID = 10000;
	
	int mId;
	Thread mChannelThread;
	InputRequest mRequest;
	volatile InputResponse mResponse;

	Context mContext;
	ChannelListener mListener;
	LocalSocket mLocalSocket;
	Intent mDialogIntent;

	public InputChannel(Context context, InputRequest request,
			ChannelListener listener) {

		mContext = context;
		mRequest = request;
		mListener = listener;
		mLocalSocket = new LocalSocket();

		mDialogIntent = new Intent(context, InputDialog.class);
		mDialogIntent.putExtras(mRequest.extras);
		mDialogIntent.putExtra(InputRequest.EXTRA_REQUEST_ID, mRequest.id);
		mDialogIntent.putExtra(InputRequest.EXTRA_REQUEST_TYPE, mRequest.type);
		mDialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		createNotification();
		
		context.startActivity(mDialogIntent);
		
		/* start channel thread */
		mChannelThread = new Thread(new ChannelRunnable());
		mChannelThread.start();
	}

	int getRequestId() {
		return mRequest.id;
	}

	void sendResponse(InputResponse response) {
		mResponse = response;
	}

	class ChannelRunnable implements Runnable {
		@Override
		public void run() {

			try {

				mLocalSocket.connect(new LocalSocketAddress(mRequest.path,
						LocalSocketAddress.Namespace.ABSTRACT));

				while (mResponse == null) {
					Thread.sleep(100);
				}

				if (mResponse != null && mLocalSocket.isConnected()) {

					OutputStream os = mLocalSocket.getOutputStream();
					os.write(mResponse.getBytes());
					os.flush();
					os.close();
				}

				mLocalSocket.close();

			} catch (Exception ex) {
				Log.e(LOG_TAG, ex.getMessage());
			}

			removeNotification();
			
			mListener.OnInputChannelClosed(InputChannel.this);
		}
	}
	
	void createNotification() {
		
		int notificationId = INPUT_NOTIFICATION_ID + mRequest.id;
		
		NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
				notificationId, mDialogIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		String title = mContext.getResources().getString(R.string.input_notification_title);
		String text = mContext.getResources().getString(R.string.input_notification_text);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				mContext)
				.setContentIntent(pendingIntent)
				.setContentTitle(title)
				.setContentText(text)
				.setSmallIcon(R.drawable.ic_osk)
				.setOngoing(true)
				.setAutoCancel(false);
		
		nm.notify(notificationId, mBuilder.build());
	}
	
	void removeNotification() {
		
		NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(INPUT_NOTIFICATION_ID +  mRequest.id);		
		
	}	

	public interface ChannelListener {
		public void OnInputChannelClosed(InputChannel channel);
	}
}
