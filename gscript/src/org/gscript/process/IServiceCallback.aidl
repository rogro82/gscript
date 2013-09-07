package org.gscript.process;

import org.gscript.process.ProcessDescriptor;

interface IServiceCallback 
{
	void OnProcessStateChanged(in ProcessDescriptor pd, int state);
}