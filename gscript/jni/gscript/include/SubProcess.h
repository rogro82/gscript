/*
 * Process.h
 *
 *  Created on: Jan 7, 2013
 *      Author: rob
 */

#ifndef SUBPROCESS_H_
#define SUBPROCESS_H_

#include <Object.h>
#include <PseudoTerminal.h>
#include <vector>

#define INVALID_PID ((pid_t)-1)
#define SIGHUP 1

namespace gscript {

class SubProcess :
	public Referenceable {
public:
	SubProcess();
	SubProcess(pty* term);

	pty* get_pty();
	pid_t get_pid();

	void set_command(const std::string& cmd);
	void set_arglist(const std::vector<std::string>& arglist);
	void set_envlist(const std::vector<std::string>& envlist);

	int execute();
	int waitFor();
	int sigKill(int sig=SIGHUP);
	bool is_running();
	void resizeWindow(int rows, int cols, int width, int height);

protected:
	virtual ~SubProcess();

private:
	ref_ptr<pty> m_pty;
	pid_t m_pid;

	std::string m_cmd;
	std::vector<std::string> m_arglist;
	std::vector<std::string> m_envlist;

	bool m_err_to_out;
};

}

#endif /* SUBPROCESS_H_ */
