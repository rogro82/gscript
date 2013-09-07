package org.gscript.process;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.gscript.jni.JNIReference;

public class SubProcess extends JNIReference {

	public static final int SIGHUP = 1;
	public static final int SIGKILL = 9;
	
	public SubProcess(String cmd, String[] args, String[] env) throws JNIReferenceException {
		super(nativeCreate(cmd, args, env));
	}

	public void execute() {
		nativeExecute(getReferencePointer());
	}

	public int waitFor() {
		return nativeWaitFor(getReferencePointer());
	}

	public int sigKill(int signal) {
		return nativeSigKill(getReferencePointer(), signal);
	}
	
	FileDescriptor getFileDescriptor() {
		return nativeFileDescriptor(getReferencePointer());
	}
	
	public InputStream getInputStream() {
		return new FileInputStream(getFileDescriptor());
	}

	public OutputStream getOutputStream() {
		return new FileOutputStream(getFileDescriptor());
	}
	
	public void resizeWindow(int rows, int cols, int width, int height) {
		nativeResizeWindow(getReferencePointer(), rows, cols, width, height);
	}

	private static native long nativeCreate(String cmd, String[] args, String[] env);
	
	private static native void nativeExecute(long pointer);
	private static native int nativeWaitFor(long pointer);
	private static native int nativeSigKill(long pointer, int signal);
	private static native FileDescriptor nativeFileDescriptor(long pointer);
	private static native void nativeResizeWindow(long pointer, int rows, int cols, int width, int height);
}