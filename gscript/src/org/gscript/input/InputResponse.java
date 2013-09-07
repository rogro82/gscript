package org.gscript.input;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class InputResponse {

	static final int MAX_RESPONSE_OPT_BYTES = 4096;

	int requestId;
	int responseCode;
	String responseOpt;

	public InputResponse(int requestId, int responseCode, String opt) {

		this.requestId = requestId;
		this.responseCode = responseCode;
		this.responseOpt = opt;

	}

	byte[] getBytes() {
		
		ByteBuffer buffer = ByteBuffer.allocate(4 + MAX_RESPONSE_OPT_BYTES);
		buffer.order(ByteOrder.nativeOrder());
		
		buffer.putInt(responseCode);

		if (responseOpt != null) {
			
			buffer.position(4);
			
			byte[] optBytes = responseOpt.getBytes();
			int length = (optBytes.length > MAX_RESPONSE_OPT_BYTES ? MAX_RESPONSE_OPT_BYTES
					: optBytes.length);
			
			buffer.put(optBytes, 0, length);
		}

		return buffer.array();
	}
	
	@Override
	public String toString() {
		return String.format(Locale.getDefault(), "InputResponse [code:%d, opt:%s]", responseCode, responseOpt);
	}
}
