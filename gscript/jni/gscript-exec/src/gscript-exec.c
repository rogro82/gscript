/*
 * request file execution from command-line
 *
 * usage:
 * gscript-exec filename
 *  */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <../../gscript/include/interop.h>

const char PATH_SEPARATOR[2] = { '/', 0 };
#define ABSOLUTE_PATH(x)	x[0] == '/'
#define ACTION_EXEC 		"org.gscript.action.EXEC"

static int broadcast_request(const char* path) {

	interop_message_t msg;

	char file_path[512];
	sprintf(file_path, "file://%s", path);

	interop_init(&msg, ACTION_EXEC, file_path);

	return (interop_broadcast(&msg));
}

int main(int argc, char **argv) {

	if (argc > 1) {

		const char* file = argv[1];

		char abspath[PATH_MAX];
		char path[PATH_MAX];

		if (ABSOLUTE_PATH(file)) {
			strcpy(path, file);

		} else {

			/* build absolute path from cwd */
			getcwd(path, sizeof(path));
			strcat(path, PATH_SEPARATOR);
			strcat(path, file);
		}

		/* convert to realpath */
		char *res = realpath(path, abspath);
		if (!res) {
			return EXIT_FAILURE;
		} else {
			/* send path to broadcast receiver */
			broadcast_request(abspath);
		}

	} else {

		printf("usage: gscript-exec filename\n");
		return EXIT_FAILURE;

	}

	return EXIT_SUCCESS;
}
