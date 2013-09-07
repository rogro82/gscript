package org.gscript.process;

import org.gscript.process.ProcessDescriptor;
import org.gscript.process.IProcessCallback;
import org.gscript.process.IServiceCallback;

interface IProcessService
{
	void registerProcessCallback(in ProcessDescriptor pd, in IProcessCallback cb);
	void unregisterProcessCallback(in ProcessDescriptor pd, in IProcessCallback cb);
	
	void registerServiceCallback(in IServiceCallback cb);
	void unregisterServiceCallback(in IServiceCallback cb);

	void requestKillProcess(in ProcessDescriptor pd);
	void requestKillProcesses(in IServiceCallback cb);
	
	ProcessDescriptor[] getProcesses(in IServiceCallback cb, int state);
	
	int getProcessState(in ProcessDescriptor pd);
	
	void dispatchProcessOutput(in ProcessDescriptor pd, in byte[] output, int offset, int length);
	void dispatchProcessEvent(in ProcessDescriptor pd, int event, in Bundle extras);
	void requestWindowSizeChange(in ProcessDescriptor pd, int rows, int cols, int width, int height);
}