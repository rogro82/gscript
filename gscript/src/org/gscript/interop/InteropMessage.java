package org.gscript.interop;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class InteropMessage {

	static final String LOG_TAG = "InteropMessage";

	static final int ACTION_BYTES = 256;
	static final int DATA_BYTES = 512;
	static final int EXTRAS_BYTES = 4;
	static final int HEADER_SIZE = ACTION_BYTES + DATA_BYTES + EXTRAS_BYTES;

	static final int EXTRA_KEY_BYTES = 256;
	static final int EXTRA_TYPE_BYTES = 4;
	static final int EXTRA_SIZE_BYTES = 4;
	static final int EXTRA_SIZE = EXTRA_KEY_BYTES + EXTRA_TYPE_BYTES
			+ EXTRA_SIZE_BYTES;

	static final int EXTRA_TYPE_INT = 0;
	static final int EXTRA_TYPE_STRING = 1;

	String action;
	String data;
	Bundle extras = new Bundle();

	String bytesToString(byte[] bytes, int offset, int length) {

		for (int i = offset; i < (offset + length); ++i) {
			if (bytes[i] == 0) {
				return new String(bytes, offset, ((i) - offset));
			}
		}
		return new String(bytes, offset, offset+length);
	}

	public InteropMessage(InputStream in) {

		ByteBuffer intBuffer = ByteBuffer.allocate(4);
		intBuffer.order(ByteOrder.nativeOrder());

		DataInputStream dataInput = new DataInputStream(in);

		/* get header (action, data, extra_size) */

		byte[] bytes = new byte[HEADER_SIZE];

		try {
			dataInput.readFully(bytes, 0, HEADER_SIZE);
		} catch (IOException e) {
			return;
		}

		int offset = 0;

		action = bytesToString(bytes, offset, ACTION_BYTES);
		offset += ACTION_BYTES;

		data = bytesToString(bytes, offset, DATA_BYTES);

		intBuffer.put(bytes, HEADER_SIZE - 4, 4);
		int extrasCount = intBuffer.getInt(0);

		/* get extras */

		byte[] extraBytes = new byte[EXTRA_SIZE];

		for (int i = 0; i < extrasCount; ++i) {

			try {
				dataInput.readFully(extraBytes, 0, EXTRA_SIZE);
			} catch (IOException e) {
				return;
			}

			String extraKey = bytesToString(extraBytes, 0, EXTRA_KEY_BYTES);

			intBuffer.clear();
			intBuffer.put(extraBytes, EXTRA_SIZE - 8, 4);

			int extraType = intBuffer.getInt(0);

			intBuffer.clear();
			intBuffer.put(extraBytes, EXTRA_SIZE - 4, 4);

			/* get extra data */

			int extraDataSize = intBuffer.getInt(0);
			byte[] extraData = new byte[extraDataSize];

			try {
				dataInput.readFully(extraData, 0, extraDataSize);

			} catch (IOException e) {
				return;
			}

			switch (extraType) {
			case EXTRA_TYPE_INT:

				intBuffer.clear();
				intBuffer.put(extraData, 0, 4);

				int intVal = intBuffer.getInt(0);
				extras.putInt(extraKey, intVal);

				break;
			case EXTRA_TYPE_STRING:

				String stringVal = bytesToString(extraData, 0, extraDataSize);
				extras.putString(extraKey, stringVal);

				break;
			}

		}
	}

	@Override
	public String toString() {
		return String.format("InteropMessage [ %s %s ]", action, data);
	}

	public Intent toIntent() {
		return new Intent(action).setData(Uri.parse(data)).putExtras(extras);
	}

}
