package org.gscript.terminal;

import java.nio.ByteBuffer;


public final class ScreenBuffer extends ScreenBufferParcelable {

	public ScreenBuffer(int screenRows, int screenCols, int foreColor, int backColor) throws JNIReferenceException {
		super(nativeCreate(screenRows, screenCols, foreColor, backColor));
	}

	/*
	 * ScreenBuffer only contains the set methods and the get methods are
	 * implemented in the underlying ScreenBufferParcelable yet for convenience
	 * the native get methods are defined here so that we only have one JNI
	 * header.
	 */
	
	public void append(byte input) {
		nativeAppend(getReferencePointer(), input);
	}
	
	public void lineFeed() {
		nativeLineFeed(getReferencePointer());
	}
	
	public void eraseInLine(int opt) {
		nativeEraseInLine(getReferencePointer(), opt);
	}

	public void eraseInScreen(int opt) {
		nativeEraseInScreen(getReferencePointer(), opt);
	}
	
	public void setGraphicsRendition(int[] params) {
		nativeSetGraphicsRendition(getReferencePointer(), params);
	}
	
	public void resize(int rows, int cols) {
		nativeResize(getReferencePointer(), rows, cols);
	}
	
	public void setCursorRow(int index) {
		nativeSetCursorRow(getReferencePointer(), index);
	}
	
	public void setCursorCol(int index) {
		nativeSetCursorCol(getReferencePointer(), index);
	}
	
	public void setCursor(int row, int col) {
		nativeSetCursor(getReferencePointer(), row, col);
	}
	
	public void moveCursor(int rowdir, int coldir) {
		nativeMoveCursor(getReferencePointer(), rowdir, coldir);
	}

	private static native long nativeCreate(int screenRows, int screenCols, int foreColor, int backColor);

	/* native set */
	
	private static native void nativeAppend(long pointer, byte input);
	private static native void nativeLineFeed(long pointer);
	private static native void nativeEraseInLine(long pointer, int opt);
	private static native void nativeEraseInScreen(long pointer, int opt);
	
	private static native void nativeSetGraphicsRendition(long pointer, int[] params);
	private static native void nativeResize(long pointer, int rows, int cols);
	
	private static native void nativeSetCursorRow(long pointer, int index);
	private static native void nativeSetCursorCol(long pointer, int index);
	private static native void nativeSetCursor(long pointer, int row, int col);
	private static native void nativeMoveCursor(long pointer, int rowdir, int coldir);
	
	/* native get ( called from ScreenBufferParcelable ) */
	
	protected static native int nativeGetTextColor(long pointer);
	protected static native int nativeGetBackColor(long pointer);
	
	protected static native int nativeGetScreenRows(long pointer);
	protected static native int nativeGetScreenCols(long pointer);
	protected static native int nativeGetScreenTop(long pointer);
	protected static native int nativeGetScreenFillTop(long pointer);
	
	protected static native int nativeGetCursorRow(long pointer, boolean absolute);
	protected static native int nativeGetCursorCol(long pointer);
	protected static native boolean nativeGetRowData(long pointer, int row, boolean absolute, ByteBuffer buffer);
	
}