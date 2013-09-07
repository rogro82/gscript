/*
 * PseudoTerminal.cpp
 *
 *  Created on: Jan 8, 2013
 *      Author: rob
 */

#include "PseudoTerminal.h"

#include <sys/types.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <termios.h>
#include <signal.h>

namespace gscript {

PseudoTerminal::PseudoTerminal() :
		m_ptm(INVALID_FD), m_pts(INVALID_FD), m_ptsname(NULL) {

	if (!open_pty(NULL, NULL)) {
		LOGD("failed to initialize pseudo-terminal");
	}
}

PseudoTerminal::PseudoTerminal(termios *termp, winsize *winp) :
		m_ptm(INVALID_FD), m_pts(INVALID_FD), m_ptsname(NULL) {

	if (!open_pty(termp, winp)) {
		LOGD("failed to initialize pseudo-terminal");
	}
}

bool PseudoTerminal::open_pty(termios *termp, winsize *winp) {

	if (m_ptm != INVALID_FD || m_pts != INVALID_FD)
		return false;

	m_ptm = open("/dev/ptmx", O_RDWR);
	if (m_ptm < 0) {
		LOGE("failed to open /dev/ptmx");
		return false;
	}

	fcntl(m_ptm, F_SETFD, FD_CLOEXEC);

	if (grantpt(m_ptm) || unlockpt(m_ptm)) {
		LOGE("grantpt / unlockpt failed");
		return false;
	}

	if ((m_ptsname = ptsname(m_ptm)) == 0) {
		LOGE("failed to get ptsname");
		return false;
	}

	return true;
}

void PseudoTerminal::close_pty() {

	/* make sure we are in the same process */
	if (is_pty_open()) {

		if (m_pts != INVALID_FD && close(m_pts) < 0) {
			LOGE("failed to close pts %d", m_pts);
		}

		if (m_ptm != INVALID_FD && close(m_ptm) < 0) {
			LOGE("failed to close ptm %d", m_ptm);
		}

		LOGD("closed pty %s", m_ptsname);

		m_pts = INVALID_FD;
		m_ptm = INVALID_FD;
		m_ptsname = NULL;

	}
}

void PseudoTerminal::get_termios(termios& termp) {

	if (m_ptm != INVALID_FD) {
		tcgetattr(m_ptm, &termp);
	}
}

void PseudoTerminal::set_termios(termios *termp, int opt) {

	if (m_ptm != INVALID_FD && termp != NULL)
		tcsetattr(m_ptm, opt, termp);
}

void PseudoTerminal::set_winsize(int rows, int cols, int width, int height) {

	winsize winp;
	winp.ws_row = rows;
	winp.ws_col = cols;
	winp.ws_xpixel = width;
	winp.ws_ypixel = height;

	if (m_ptm != INVALID_FD)
		ioctl(m_ptm, TIOCSWINSZ, &winp);
}

bool PseudoTerminal::is_pty_open() {
	return m_ptm != INVALID_FD;
}

const char* PseudoTerminal::get_pts_name() {
	return m_ptsname;
}

int PseudoTerminal::get_ptm() {
	return m_ptm;
}

int PseudoTerminal::get_pts() {
	return m_pts;
}

int PseudoTerminal::open_pts() {
	m_pts = open(m_ptsname, O_RDWR);
	return m_pts;
}

PseudoTerminal::~PseudoTerminal() {
	close_pty();
}

} /* namespace gscript */
