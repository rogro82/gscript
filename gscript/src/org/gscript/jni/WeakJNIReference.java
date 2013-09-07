package org.gscript.jni;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

class WeakJNIReference extends WeakReference<JNIReference> {

	private static final String LOG_TAG = "WeakJNIReference";

	static private ReferenceQueue<JNIReference> sRefQueue = new ReferenceQueue<JNIReference>();
	static private List<WeakJNIReference> sRefList = new ArrayList<WeakJNIReference>();

	private final long mPointer;

	WeakJNIReference(JNIReference object) {
		super(object, sRefQueue);
		this.mPointer = object.getReferencePointer();
		
		/* add our self to the list of weak references so that
		 * we can verify existance when disposing */
		
		synchronized (sRefList) {
			sRefList.add(this);
		}
	}

	void dispose() {
		synchronized (sRefList) {
			if (sRefList.remove(this)) {
				
				Log.v(LOG_TAG, "Disposing JNIReference");
				JNIReference.nativeDispose(mPointer);
			}
		}
	}

	static ReferenceQueue<JNIReference> referenceQueue() {
		return sRefQueue;
	}

	/* Garbage collection */
	private static Runnable sCollectRunnable = new Runnable() {
		public void run() {

			ReferenceQueue<JNIReference> refQueue = WeakJNIReference
					.referenceQueue();
			
			while (true) {

				WeakJNIReference weakRefObject;

				try {
					// remove is blocking until a reference becomes available
					Object reference = refQueue.remove();
					if (reference != null) {
						weakRefObject = (WeakJNIReference) reference;
						weakRefObject.dispose();

						Log.v(LOG_TAG, "Disposed reference " + weakRefObject.mPointer);
					}

				} catch (InterruptedException e) {
				}
			}
		};
	};

	static final String THREAD_NAME = "JNIRefCollector";
	
	private static Thread sJNICollectThread = new Thread(sCollectRunnable, THREAD_NAME);
	static {
		sJNICollectThread
				.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		
		sJNICollectThread.start();
	}
}