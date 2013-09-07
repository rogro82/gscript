/*
 * Input dialogs from commands line
 *
 * usage:
 * gscript-input type [options]
 *  */

#include <android/log.h>
#include <unistd.h>
#include <limits.h>
#include <fcntl.h>
#include <errno.h>
#include <endian.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>
#include <stdint.h>
#include <pwd.h>
#include <time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <sys/select.h>
#include <sys/time.h>

#include <../../gscript/include/interop.h>

#undef LOG_TAG
#define LOG_TAG 				"gscript-input"
#define ACTION_REQUEST 			"org.gscript.action.INPUT_REQUEST"
#define LOCAL_SOCKET_PATH 		"/org.gscript/ipc"
#define RESPONSE_OPT_LENGTH		4096

/* org.gscript.input.InputRequest.java */
#define TYPE_DIALOG_INFO 		"dialog-info"
#define TYPE_DIALOG_WARNING 	"dialog-warning"
#define TYPE_DIALOG_ERROR 		"dialog-error"
#define TYPE_DIALOG_MESSAGE 	"dialog-message"
#define TYPE_TEXT_ENTRY 		"text-entry"
#define TYPE_LIST 				"list"
#define TYPE_TOAST				"toast"

#define EXTRA_TITLE				"title"
#define EXTRA_MESSAGE			"message"
#define EXTRA_STYLE				"style"
#define EXTRA_LIST				"list"

#define RESPONSE_CODE_OK		1
#define RESPONSE_CODE_CANCEL	0
#define RESPONSE_CODE_ERROR		-1

struct response {
	int code;
	char opt[RESPONSE_OPT_LENGTH];
}typedef response_t;

static void LOGD(const char *log) {
	__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, log);
}

static void LOGE(const char *log) {
	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, log);
}

static double now_ms(void) {
	struct timespec res;
	clock_gettime(CLOCK_REALTIME, &res);
	return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;
}

static void socket_unlink(const char* socket_path) {
	unlink(socket_path);
}

static int socket_init(const char *socket_path) {

	int socket_fd;
	struct sockaddr_un socket_addr;

	/* create new socket */
	socket_fd = socket(AF_LOCAL, SOCK_STREAM, 0);
	if (socket_fd < 0) {
		return -1;
	}

	memset(&socket_addr, 0, sizeof(socket_addr));
	socket_addr.sun_family = AF_LOCAL;

	/* set sun_path eg: /org.gscript/ipc/socket_name
	 * and prefix with 0 to use abstract namespace */

	snprintf((socket_addr.sun_path + 1), (sizeof(socket_addr.sun_path)-1), "%s", socket_path);
	socket_addr.sun_path[0] = 0;

	/* unlink socket path to make sure it not bound */
	unlink(socket_addr.sun_path);

	/* do not use sizeof(socket_addr) on an abstract namespace, but instead use the length of
	 * the actual (name incl prefixed 0), because \0name does not equal \0name\0\0\0\... */

	int socket_addr_len = (sizeof(sa_family_t)+(strlen(socket_path) + 1));
	if (bind(socket_fd, (struct sockaddr*) &socket_addr, socket_addr_len)
			< 0) {
		LOGE("bind error");
		goto err;
	}

	if (listen(socket_fd, 1) < 0) {
		LOGE("listen error");
		goto err;
	}

	return socket_fd;

	err: close(socket_fd);
	return -1;
}

static int socket_accept(int serv_fd) {

	struct timeval tv;
	fd_set fds;
	int fd;

	/* Wait 20 seconds for a connection, then give up. */
	tv.tv_sec = 20;
	tv.tv_usec = 0;

	FD_ZERO(&fds);
	FD_SET(serv_fd, &fds);

	if (select(serv_fd + 1, &fds, NULL, NULL, &tv) < 1) {
		LOGE("socket error (possible timeout)");
		return -1;
	}

	fd = accept(serv_fd, NULL, NULL);
	if (fd < 0) {
		LOGE("socket accept error");
		return -1;
	}

	return fd;
}

static int socket_req(int fd) {
	/* currently unused as we send everything over a broadcast */
	return 0;
}

static int socket_recv(int fd, response_t* result, ssize_t result_len) {

	ssize_t len;

	len = read(fd, result, result_len);
	if (len < 0) {
		LOGE("socket read error");
		return -1;
	}

	return 0;
}

static void cancel(const char* path) {
	/* set var to ERR_NO_INPUT */
	if (path != NULL)
		socket_unlink(path);

	exit(RESPONSE_CODE_ERROR);
}

static void print_usage(const char* type) {

	if (type == NULL) {
		printf("Usage: gscript-input (type|types) [options]\n");

	} else {

		/* display help for type  */
		if (strcmp(type, TYPE_DIALOG_INFO) == 0
				|| strcmp(type, TYPE_DIALOG_WARNING) == 0
				|| strcmp(type, TYPE_DIALOG_ERROR) == 0
				|| strcmp(type, TYPE_TEXT_ENTRY) == 0) {
			printf("Usage: gscript-input %s title message\n", type);
		} else if (strcmp(type, TYPE_DIALOG_MESSAGE) == 0) {
			printf("Usage: gscript-input dialog-message title message "
					"[okcancel | yesno]\n");
			printf("Example: gscript-input dialog-message \"My question\" "
					"\"Answer yes or no\" yesno\n");
		} else if (strcmp(type, TYPE_LIST) == 0) {
			printf("Usage: gscript-input list title message "
					"\"; seperated key:value list\" "
					"[checkbox(default) | radio]\n");
			printf("Example: gscript-input list \"My title\" \"My message\" "
					"\"0:first item;1:second item;2:third item\" "
					"radio\n");
		} else if (strcmp(type, TYPE_TOAST) == 0) {
			printf("Usage: gscript-input toast message\n");

		} else if (strcmp(type, "types") == 0) {
			printf(TYPE_DIALOG_INFO"\n"
			TYPE_DIALOG_WARNING"\n"
			TYPE_DIALOG_ERROR"\n"
			TYPE_DIALOG_MESSAGE"\n"
			TYPE_TEXT_ENTRY"\n"
			TYPE_LIST"\n");
		} else {
			printf(
					"Unknown type %s.. use \"gscript-input types\" for a list of available types\n",
					type);
		}
	}
}

int build_extras(const char* type, interop_message_t* message, int argc,
		char**argv) {

	if (strcmp(type, TYPE_DIALOG_INFO) == 0
			|| strcmp(type, TYPE_DIALOG_WARNING) == 0
			|| strcmp(type, TYPE_DIALOG_ERROR) == 0
			|| strcmp(type, TYPE_DIALOG_MESSAGE) == 0
			|| strcmp(type, TYPE_TEXT_ENTRY) == 0
			|| strcmp(type, TYPE_LIST) == 0) {

		/* get common options ( title, message) for most types */

		if (argc < 4)
			return -1;

		/* default title and message */
		interop_put_extra(message, EXTRA_TYPE_STRING, EXTRA_TITLE, argv[2],
				strlen(argv[2]));
		interop_put_extra(message, EXTRA_TYPE_STRING, EXTRA_MESSAGE, argv[3],
				strlen(argv[3]));
	}
	if (strcmp(type, TYPE_DIALOG_MESSAGE) == 0) {
		if (argc < 5)
			return -1;

		/* style (yesno|okcancel) */
		interop_put_extra(message, EXTRA_TYPE_STRING, EXTRA_STYLE, argv[4],
				strlen(argv[4]));
	}
	if (strcmp(type, TYPE_LIST) == 0) {
		if (argc < 5)
			return -1;

		/* ; seperated key:value pairs */
		interop_put_extra(message, EXTRA_TYPE_STRING, EXTRA_LIST, argv[4],
				strlen(argv[4]));

		/* checkbox|radio option */
		if (argc == 6)
			interop_put_extra(message, EXTRA_TYPE_STRING, EXTRA_STYLE, argv[5],
					strlen(argv[5]));
	}
	if (strcmp(type, TYPE_TOAST) == 0) {
		if (argc < 3)
			return -1;

		/* toast only has a message */
		interop_put_extra(message, EXTRA_TYPE_STRING, EXTRA_MESSAGE, argv[2],
				strlen(argv[2]));
	}

	return 0;
}

int main(int argc, char **argv) {

	if (argc > 1) {

		const char* type = argv[1];

		if (argc == 2) {
			print_usage(type);

		} else {

			/* generate a socket path from pid and time */

			char socket_path[PATH_MAX];
			snprintf(socket_path, sizeof(socket_path), "%s/p%dt%f",
					LOCAL_SOCKET_PATH, getpid(), now_ms());

			/* data uri with scheme, path and query type */

			char data_uri[512];
			snprintf(data_uri, sizeof(data_uri), "ipc://%s?type=%s",
					socket_path, type);

			interop_message_t msg;
			interop_init(&msg, ACTION_REQUEST, data_uri);

			if (build_extras(type, &msg, argc, argv) < 0) {
				print_usage(type);
				return RESPONSE_CODE_ERROR;
			}

			if (strcmp(type, TYPE_TOAST) == 0) {

				/* handle input types that do not need any response but just broadcast the request */

				/* dispatch broadcast */
				if (interop_broadcast(&msg) < 0) {
					interop_free_extra(&msg);
					cancel(NULL);
				}

				interop_free_extra(&msg);

				return RESPONSE_CODE_OK;

			} else {

				/* handle input types that do need response */

				int socket_fd, fd;

				/* create local socket */
				socket_fd = socket_init(socket_path);
				if (socket_fd < 0) {
					LOGE("socket creation error");
					cancel(socket_path);
				}

				/* dispatch broadcast */

				if (interop_broadcast(&msg) < 0) {
					interop_free_extra(&msg);
					cancel(socket_path);
				}

				interop_free_extra(&msg);

				/* wait for connection */
				fd = socket_accept(socket_fd);
				if (fd < 0) {
					cancel(socket_path);
				}

				/* dispatch request */
				if (socket_req(fd)) {
					cancel(socket_path);
				}

				/* handle incoming response */

				response_t response;
				response.code = RESPONSE_CODE_ERROR;

				LOGD("inputchannel waiting for input");

				if (socket_recv(fd, &response, sizeof(response_t))) {
					cancel(socket_path);
				}

				/* clean up */

				close(fd);
				close(socket_fd);

				socket_unlink(socket_path);

				printf(response.opt);
				fflush(stdout);

				return response.code;
			}
		}

	} else {
		print_usage(0);
	}

	return RESPONSE_CODE_ERROR;
}
