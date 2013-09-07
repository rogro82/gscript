/*
 * PseudoTerminal.h
 *
 *  Created on: Jan 8, 2013
 *      Author: rob
 */

#ifndef PSEUDOTERMINAL_H_
#define PSEUDOTERMINAL_H_

#ifndef INVALID_FD
#define INVALID_FD	-1
#endif

#include <Object.h>

struct termios;
struct winsize;

namespace gscript {

class PseudoTerminal :
	public Referenceable {
public:
	PseudoTerminal();
	PseudoTerminal(termios *termp, winsize *winp);

	bool open_pty(termios *termp, winsize *winp);
	void close_pty();
	bool is_pty_open();
	const char* get_pts_name();
	int get_ptm();
	int get_pts();
	int open_pts();

	void get_termios(termios& termp);
	void set_termios(termios* termp, int opt=0x5404 /* TCSAFLUSH */);
	void set_winsize(int rows, int cols, int width, int height);

protected:
	virtual ~PseudoTerminal();

private:

	int m_ptm;
	int m_pts;
	char* m_ptsname;
};

/* typedef to pty for (in)convenience.. */
typedef PseudoTerminal		pty;

} /* namespace gscript */
#endif /* PSEUDOTERMINAL_H_ */
