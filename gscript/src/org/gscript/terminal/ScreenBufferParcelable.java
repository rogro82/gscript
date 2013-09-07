package org.gscript.terminal;

import java.nio.ByteBuffer;

import org.gscript.jni.JNIReference;
import android.os.Parcel;
import android.os.Parcelable;

public class ScreenBufferParcelable extends JNIReference implements Parcelable {

	static final int COPY_RESULT_SUCCESS = 0;
	static final int COPY_RESULT_WRONG_SIZE = 1;
	
	/* TODO: Move ScreenLine flags to somewhere more general */
	static final int FLAG_LINEWRAPPED = 0x2;	

	protected ScreenBufferParcelable(long refpointer)
			throws JNIReferenceException {
		super(refpointer);
	}

	public static final Parcelable.Creator<ScreenBufferParcelable> CREATOR = new Parcelable.Creator<ScreenBufferParcelable>() {

		public ScreenBufferParcelable createFromParcel(Parcel in) {

			try {
				return new ScreenBufferParcelable(in);
			} catch (JNIReferenceException e) {
				return null;
			}
		}

		public ScreenBufferParcelable[] newArray(int size) {
			return null;
		}
	};

	public ScreenBufferParcelable(Parcel source) throws JNIReferenceException {
		super(cloneReferencePointer(source.readLong()));
		/*
		 * NOTE: DO NOT USE THE SAME REFPOINTER create a new native reference to
		 * same native object the other reference was pointing to. else the
		 * reference count of the actual object will not get raised and the
		 * backing native JNIReference might get deleted before we are.
		 */
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(getReferencePointer());
	}

	/*
	 * The ScreenBufferParcelable only contains the get methods but the
	 * definition of the native methods are defined in ScreenBuffer so that this
	 * is just an empty base class and we only have one JNI header to look after
	 */

	public int getTextColor() {
		return ScreenBuffer.nativeGetTextColor(getReferencePointer());
	}

	public int getBackColor() {
		return ScreenBuffer.nativeGetBackColor(getReferencePointer());
	}

	public int getScreenRows() {
		return ScreenBuffer.nativeGetScreenRows(getReferencePointer());
	}

	public int getScreenCols() {
		return ScreenBuffer.nativeGetScreenCols(getReferencePointer());
	}

	public int getScreenTop() {
		return ScreenBuffer.nativeGetScreenTop(getReferencePointer());
	}

	public int getScreenFillTop() {
		return ScreenBuffer.nativeGetScreenFillTop(getReferencePointer());
	}

	public int getCursorRow(boolean absolute) {
		return ScreenBuffer.nativeGetCursorRow(getReferencePointer(), absolute);
	}

	public int getCursorCol() {
		return ScreenBuffer.nativeGetCursorCol(getReferencePointer());
	}

	private ByteBuffer allocateRowBuffer() {
		return ByteBuffer.allocateDirect(
		/* int:rowflags */4 +
		/* int:columns */4 * getScreenCols());
	}
	
	public ByteBuffer getRowData(ByteBuffer buffer, int row, boolean absolute) {
		if (buffer == null || !buffer.isDirect()) {
			buffer = allocateRowBuffer();
		}

		if (!ScreenBuffer.nativeGetRowData(getReferencePointer(), row,
				absolute, buffer)) {

			/*
			 * the size of the row buffer does not match. reallocate row buffer
			 * to match sizes and then try again
			 */

			buffer = allocateRowBuffer();
			if (!ScreenBuffer.nativeGetRowData(getReferencePointer(), row,
					absolute, buffer)) {
				return null;
			}
		}

		buffer.position(0);

		return buffer;
	}
	
	public String getTranscript() {

		ByteBuffer rowBuffer = null;
		
		StringBuilder transcript = new StringBuilder(getScreenRows() * (getScreenCols()));
		StringBuilder line = new StringBuilder(getScreenCols());
		
		int rows = (this.getScreenTop() + this.getScreenRows());
		
		for(int row=0; row < rows; ++row) {

			if((rowBuffer=getRowData(rowBuffer, row, true))==null)
					return "failed to build transcript";
						
			int rowFlags = rowBuffer.getInt();
			
			if(row != 0 && (rowFlags & FLAG_LINEWRAPPED) != FLAG_LINEWRAPPED) {
				transcript.append(line.toString().trim());
				transcript.append("\n");
				
				line.setLength(0);
			}
			
			if(rowFlags != -1) {
				
				int pos = rowBuffer.position();
				int cols = (rowBuffer.capacity() - pos) / 4;
				
				for(int col=0; col < cols; ++col) {
					
					int encodedChar = rowBuffer.getInt();
					
					byte bTextEffects = (byte) ((encodedChar >> 16) & 0xff);
					if ((bTextEffects & TextEffects.IGNORE) != TextEffects.IGNORE) {
						line.append( (char) ((encodedChar >> 24) & 0xff) );
					} else {
						line.append(" ");
					}
				}
			}
		}
		
		if(line.length() > 0)
			transcript.append(line.toString().trim());
		
		return transcript.toString();
	}
}
