package org.gscript.process;

public class ProcessState {

	public static final int STATE_NONE = 0;
	public static final int STATE_RUNNING = 0x1;

	/*
	 * any value above STATE_FINISHED should atleast be a finished state e.g.
	 * STATE_KILLED so that we can handle < or >= STATE_FINISHED to check if the
	 * process is still running.
	 */
	
	public static final int STATE_FINISHED = 0x2;
	public static final int STATE_KILLED = 0x4;
	public static final int STATE_ERROR = 0x8;

	public static final int ERROR_NO_CONTENT = 0x100;
	public static final int ERROR_EXECUTION_FAILED = 0x200;

	/* error states should contain STATE_ERROR and the actual error code */
	public static int STATE_ERROR(int errorCode) {
		return (STATE_ERROR | errorCode);
	}
	
	public static boolean isErrorState(int state) {
		return ((state & STATE_ERROR) != 0);
	}

	public static int getError(int state) {
		return ((state & STATE_ERROR) != 0) ? (state & ~STATE_ERROR) : 0;
	}

	public static boolean isActiveState(int state) {
		return state < STATE_FINISHED;
	}
}
