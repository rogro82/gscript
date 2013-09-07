package org.gscript.process;

public interface ProcessStateListener {
	void OnProcessStateChanged(ProcessTask task, int state);
	void OnProcessEvent(ProcessTask task, int event);
}