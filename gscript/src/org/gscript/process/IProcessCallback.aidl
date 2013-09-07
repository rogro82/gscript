package org.gscript.process;

import org.gscript.process.ProcessDescriptor;
import org.gscript.terminal.ScreenBufferParcelable;

interface IProcessCallback
{
	void OnProcessStateChanged(in ProcessDescriptor pd, int state);
	void OnProcessRegistration(in ProcessDescriptor pd, in String profile, in ScreenBufferParcelable screenBuffer);
	void OnProcessEvent(in ProcessDescriptor pd, int event);
}