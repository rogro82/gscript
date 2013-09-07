/*
 * SubProcess.cpp
 *
 *  Created on: Jan 7, 2013
 *      Author: rob
 */

#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>
#include <termios.h>
#include <algorithm>

#include <SubProcess.h>
#include <JNIReference.h>
#include <org_gscript_process_SubProcess.h>

namespace gscript {

SubProcess::SubProcess() :
		m_pty(new pty()), m_pid(INVALID_PID ), m_err_to_out(true) {
}

SubProcess::SubProcess(pty* term) :
		m_pty(term), m_pid(INVALID_PID ), m_err_to_out(true) {
}

SubProcess::~SubProcess() {
}

void SubProcess::set_command(const std::string& cmd) {
	this->m_cmd.assign(cmd);
}

void SubProcess::set_arglist(const std::vector<std::string>& arglist) {
	this->m_arglist = arglist;
}

void SubProcess::set_envlist(const std::vector<std::string>& envlist) {
	this->m_envlist = envlist;
}

char *convert_cstr(const std::string & s) {

	char *pc = new char[s.size() + 1];

	std::strcpy(pc, s.c_str());
	pc[s.size()] = '\0';

	return pc;
}

int SubProcess::execute() {

	pty* term = m_pty.get();

	if (term == NULL || !term->is_pty_open()) {
		LOGE("process execute failed... no open pty");
		return -1;
	}

	int pid = fork();

	if (pid < 0)
		return -1;

	if (pid == 0) {

		/* close master */
		close(term->get_ptm());

		setsid();

		int pts = term->open_pts();

		if (pts < 0)
			exit(-1);

		if (dup2(pts, STDIN_FILENO) < 0) {
			exit(-1);
		}
		if (dup2(pts, STDOUT_FILENO) < 0) {
			exit(-1);
		}

		if (m_err_to_out) {
			if (dup2(STDOUT_FILENO, STDERR_FILENO) < 0) {
				exit(-1);
			}
		} else {

			if (dup2(pts, STDERR_FILENO) < 0) {
				exit(-1);
			}
		}

		for (std::vector<std::string>::iterator env = m_envlist.begin();
				env != m_envlist.end(); ++env) {
			putenv(env->c_str());
		}

		std::vector<char*> argv;
		std::transform(m_arglist.begin(), m_arglist.end(),
				std::back_inserter(argv), convert_cstr);

		/* NULL terminate argv array */
		argv.push_back(NULL);

		if(execv(this->m_cmd.c_str(), &argv[0]) < 0)
		{
			LOGE("execv error: %d", errno);
		}

		/* free argv */
		for (size_t i = 0; i < m_arglist.size(); ++i)
			delete[] argv[i];

		exit(-1);

	} else {

		m_pid = pid;
		return 0;
	}
}

int SubProcess::waitFor() {

	int status;
	int result = -1;

	if (m_pid != INVALID_PID ) {

		waitpid(m_pid, &status, 0);

		/* close pseudo terminal */

		m_pty->close_pty();

		result = 0;

		if (WIFEXITED(status)) {
			result = WEXITSTATUS(status);
		}
	}

	return result;
}

int SubProcess::sigKill(int sig) {

	int res = 0;

	if (m_pid != INVALID_PID ) {
		if ((res = kill(m_pid, sig)) < 0) {
			LOGV("kill process failed: %s", strerror(errno));
			return res;
		}

		LOGD("successfully killed process");
		m_pid = INVALID_PID;
	}
	return res;
}

void SubProcess::resizeWindow(int rows, int cols, int width, int height) {
	this->m_pty->set_winsize(rows, cols, width, height);
}

pid_t SubProcess::get_pid() {
	return this->m_pid;
}

pty* SubProcess::get_pty() {
	return this->m_pty.get();
}

bool SubProcess::is_running() {

	/* check if process is running using kill */
	if (m_pid != INVALID_PID && kill(m_pid, 0) == 0) {
		return true;

	} else if (errno == ESRCH) {
		LOGV("no such process");
	}
	return false;
}

}

JNIEXPORT jlong JNICALL Java_org_gscript_process_SubProcess_nativeCreate(
		JNIEnv *env, jclass clazz, jstring jcmd, jobjectArray jargs,
		jobjectArray jenv) {

	ref_ptr<gscript::SubProcess> p = new gscript::SubProcess();

	int argLength = env->GetArrayLength(jargs);
	std::vector<std::string> arglist;

	for (int i = 0; i < argLength; ++i) {
		jstring jargStr = (jstring) env->GetObjectArrayElement(jargs, i);
		const char *argStr = env->GetStringUTFChars(jargStr, 0);
		arglist.push_back(argStr);
		env->ReleaseStringUTFChars(jargStr, argStr);
	}

	int envLength = env->GetArrayLength(jenv);
	std::vector<std::string> envlist;

	for (int i = 0; i < envLength; ++i) {
		jstring jenvStr = (jstring) env->GetObjectArrayElement(jenv, i);
		const char *envStr = env->GetStringUTFChars(jenvStr, 0);
		envlist.push_back(envStr);
		env->ReleaseStringUTFChars(jenvStr, envStr);
	}

	const char *cmdStr = env->GetStringUTFChars(jcmd, 0);

	p->set_command(cmdStr);
	p->set_arglist(arglist);
	p->set_envlist(envlist);

	env->ReleaseStringUTFChars(jcmd, cmdStr);

	JNIReference* ref = new JNIReference(p.get());

	return (jlong) ref;
}

JNIEXPORT void JNICALL Java_org_gscript_process_SubProcess_nativeExecute(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::SubProcess> proc = JNIReference::cast<gscript::SubProcess*>(jptr);
	proc->execute();
}

JNIEXPORT jint JNICALL Java_org_gscript_process_SubProcess_nativeWaitFor(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::SubProcess> proc = JNIReference::cast<gscript::SubProcess*>(jptr);

	return proc->waitFor();
}

JNIEXPORT jint JNICALL Java_org_gscript_process_SubProcess_nativeSigKill(
		JNIEnv *env, jclass clazz, jlong jptr, jint signal) {

	ref_ptr<gscript::SubProcess> proc = JNIReference::cast<gscript::SubProcess*>(jptr);
	return proc->sigKill(signal);

}

struct java_file_descriptor {

	jclass clazz;
	jfieldID descriptor;
	jmethodID init;

	bool load(JNIEnv *env) {

		if (clazz == NULL || descriptor == NULL || init == NULL) {

			jclass clazz_local = env->FindClass("java/io/FileDescriptor");
			if (clazz_local != NULL) {

				clazz = (jclass) env->NewGlobalRef(clazz_local);
				descriptor = env->GetFieldID(clazz, "descriptor", "I");
				init = env->GetMethodID(clazz, "<init>", "()V");

				env->DeleteLocalRef(clazz_local);
			}
		}

		return (clazz != NULL && descriptor != NULL && init != NULL);
	}

} jfiledescriptor;

JNIEXPORT jobject JNICALL Java_org_gscript_process_SubProcess_nativeFileDescriptor(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::SubProcess> proc = JNIReference::cast<gscript::SubProcess*>(jptr);

	if (jfiledescriptor.load(env)) {

		jobject fd = env->NewObject(jfiledescriptor.clazz,
				jfiledescriptor.init);

		if (fd) {
			env->SetIntField(fd, jfiledescriptor.descriptor,
					proc->get_pty()->get_ptm());

			return fd;
		}
	}

	return NULL;
}

JNIEXPORT void JNICALL Java_org_gscript_process_SubProcess_nativeResizeWindow
  (JNIEnv *env, jclass clazz, jlong jptr, jint rows, jint cols, jint width, jint height) {

	ref_ptr<gscript::SubProcess> proc = JNIReference::cast<gscript::SubProcess*>(jptr);
	proc->resizeWindow(rows, cols, width, height);

}

