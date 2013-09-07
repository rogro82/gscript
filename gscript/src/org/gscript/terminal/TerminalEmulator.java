package org.gscript.terminal;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.gscript.jni.JNIReference.JNIReferenceException;
import org.gscript.settings.ShellProfile;

import android.util.Log;

public class TerminalEmulator {

	static final byte[] VT100_ATTRIBUTES = { 27, '[', '?', '1', ';', '2', 'c' };
	static final String LOG_TAG = "TerminalEmulator";
	static final boolean DEBUG = true;

	static int LAST_ROWS = 24;
	static int LAST_COLS = 80;
	static int LAST_WIDTH = 720;
	static int LAST_HEIGHT = 1280;
	
	private OutputStream mOutputStream;
	private final ShellProfile mProfile;
	private final OnEmulatorEventListener mListener;

	private ScreenBuffer mScreenBuffer;
	private volatile EscapeSequence mEscapeSequence;

	int mScreenWidth;
	int mScreenHeight;
	
	Object mInputMutex = new Object();

	public TerminalEmulator(ShellProfile profile,
			OnEmulatorEventListener listener) {

		/* just some defaults */
		mScreenWidth = LAST_WIDTH;
		mScreenHeight = LAST_HEIGHT;
		mProfile = profile;
		mListener = listener;

		try {

			int backColor = mProfile.backcolor;
			int textColor = mProfile.textcolor;

			/* allocate a new screen buffer */
			mScreenBuffer = new ScreenBuffer(LAST_ROWS, LAST_COLS, textColor, backColor);

		} catch (JNIReferenceException e) {
		}
	}

	public void setOutputStream(OutputStream out) {
		mOutputStream = out;
	}

	public void processInput(byte[] input, int offset, int length) {

		synchronized(mInputMutex) {
			
			for (int index = offset; index < (offset + length); ++index) {
				processInput(input[index]);
			}
			
		}
	}

	void processInput(byte input) {

		switch (input) {
		case ControlCharacter.NUL:
			break;
		case ControlCharacter.BEL:
			break;
		case ControlCharacter.BS:
			mScreenBuffer.moveCursor(0, -1);
			break;
		case ControlCharacter.HT:
			break;
		case ControlCharacter.LF:
		case ControlCharacter.VT:
		case ControlCharacter.FF:
			mScreenBuffer.lineFeed();
			break;
		case ControlCharacter.CR:
			mScreenBuffer.setCursorCol(0);
			break;
		case ControlCharacter.SO:
			break;
		case ControlCharacter.SI:
			break;
		case ControlCharacter.CAN:
		case ControlCharacter.SUB:
			mEscapeSequence = null;
			break;
		case ControlCharacter.DEL:
			break;
		case ControlCharacter.ESC:
		case ControlCharacter.CSI:
			mEscapeSequence = new EscapeSequence(input);
			break;
		default:
			if (mEscapeSequence != null && !mEscapeSequence.isFinished()) {
				if (mEscapeSequence.append(input)) {
					/* finished sequence */
					processEscapeSequence(mEscapeSequence);
					mEscapeSequence = null;
				}
			} else {
				/* handle normal character */
				mScreenBuffer.append(input);
			}
		}
		dispatchEvent(TerminalEvent.SCREEN_UPDATE);
	}

	void processEscapeSequence(EscapeSequence s) {
		
		switch (s.getType()) {
		case EscapeSequence.SEQUENCE_TYPE_EL0:
		case EscapeSequence.SEQUENCE_TYPE_EL1:
		case EscapeSequence.SEQUENCE_TYPE_EL2:
			/* erase in line */
			mScreenBuffer.eraseInLine(
					s.getArgumentOrDefault(0)
					);
			break;
		case EscapeSequence.SEQUENCE_TYPE_ED0:
		case EscapeSequence.SEQUENCE_TYPE_ED1:
		case EscapeSequence.SEQUENCE_TYPE_ED2:
			/* erase in display/screen */
			mScreenBuffer.eraseInScreen(
					s.getArgumentOrDefault(0)
					);
			break;

		case EscapeSequence.SEQUENCE_TYPE_CUU:
			/* cursor up */
			mScreenBuffer.moveCursor(-s.getArgumentOrDefault(0, 1), 0);
			break;
		case EscapeSequence.SEQUENCE_TYPE_CUD:
			/* cursor down */
			mScreenBuffer.moveCursor(s.getArgumentOrDefault(0, 1), 0);
			break;
		case EscapeSequence.SEQUENCE_TYPE_CUF:
			/* cursor forward */
			mScreenBuffer.moveCursor(0, s.getArgumentOrDefault(0, 1));
			break;
		case EscapeSequence.SEQUENCE_TYPE_CUB:
			/* cursor backward */
			mScreenBuffer.moveCursor(0, -s.getArgumentOrDefault(0, 1));
			break;
		case EscapeSequence.SEQUENCE_TYPE_CNL:
			/* cursor next line */
			mScreenBuffer.moveCursor(s.getArgumentOrDefault(0, 1), 0);
			mScreenBuffer.setCursorCol(0);
			break;
		case EscapeSequence.SEQUENCE_TYPE_CPL:
			/* cursor previous line */
			mScreenBuffer.moveCursor(-s.getArgumentOrDefault(0, 1), 0);
			mScreenBuffer.setCursorCol(0);
			break;
		case EscapeSequence.SEQUENCE_TYPE_CHA:
			/* cursor horizontal absolute */
			int cha = s.getArgumentOrDefault(0, 0);
			cha = (cha > 0) ? (cha - 1) : 0;
			
			mScreenBuffer.setCursorCol(cha);
			break;
		case EscapeSequence.SEQUENCE_TYPE_CUP:
			/* cursor position */
			int cupRow = s.getArgumentOrDefault(0, 0);
			int cupCol = s.getArgumentOrDefault(1, 0);
			
			/* cursor position to internal zero-based indexing */
			cupRow = (cupRow > 0) ? (cupRow-1) : 0;
			cupCol = (cupCol > 0) ? (cupCol-1) : 0;
			
			mScreenBuffer.setCursor(cupRow, cupCol);	
			
			break;
			
		case EscapeSequence.SEQUENCE_TYPE_SGR:
			/* graphics rendition */

			final ArrayList<Integer> args = s.getArguments();
			int[] params = new int[args.size()];

			Iterator<Integer> iterator = args.iterator();
			for (int i = 0; i < params.length; ++i) {
				int val = iterator.next().intValue();
				params[i] = (val != EscapeSequence.ARGUMENT_DEFAULT) ? val : 0;
			}
			mScreenBuffer.setGraphicsRendition(params);
			break;

		default:
			if (DEBUG)
				Log.d(LOG_TAG, "Unhandled escape sequence: " + s.toString());
		}
	}

	public void processOutput(byte[] output, int offset, int length) {

		try {

			mOutputStream.write(output, offset, length);
			mOutputStream.flush();

		} catch (Exception e) {
		}
	}
	
	void dispatchEvent(int event) {
		mListener.OnEmulatorEvent(event);
	}

	public void resizeWindow(int rows, int cols, int width, int height) {

		mScreenWidth = width;
		mScreenHeight = height;

		LAST_COLS = cols;
		LAST_ROWS = rows;
		LAST_WIDTH = width;
		LAST_HEIGHT = height;
		
		synchronized(mInputMutex) {
			mScreenBuffer.resize(rows, cols);
		}
		
		dispatchEvent(TerminalEvent.SCREEN_RESIZE);		
	}

	public ScreenBuffer getScreenBuffer() {
		return mScreenBuffer;
	}

	public int getScreenRows() {
		return mScreenBuffer.getScreenRows();
	}

	public int getScreenCols() {
		return mScreenBuffer.getScreenCols();
	}

	public int getScreenWidth() {
		return mScreenWidth;
	}

	public int getScreenHeight() {
		return mScreenHeight;
	}

	public interface OnEmulatorEventListener {
		public void OnEmulatorEvent(int event);
	}

}
