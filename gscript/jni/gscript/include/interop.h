#ifndef INTEROP_H_
#define INTEROP_H_

#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>

#define INTEROP_SOCKET_ADDRESS 	"/org.gscript/interop"

/* match with org.gscript.interop.InteropMessage */

#define EXTRA_TYPE_INT			0
#define EXTRA_TYPE_STRING		1


struct interop_header {
	char action[256];
	char data[512];
	int extra;
} typedef interop_header_t;

struct interop_extra {
	char key[256];
	int type;
	int size;
	void* data;
} typedef interop_extra_t;

struct interop_message {
	interop_header_t header;
	interop_extra_t* extra[256];

} typedef interop_message_t;

void interop_init(interop_message_t* msg, const char* action, const char* data) {

	strncpy(msg->header.action, action, sizeof(msg->header.action));
	strncpy(msg->header.data, data, sizeof(msg->header.data));
	msg->header.extra = 0;

}
void interop_put_extra(interop_message_t* msg, int type, const char* key, void* data, size_t size) {

	interop_extra_t* extra = (interop_extra_t*)malloc(sizeof(interop_extra_t));

	strncpy(extra->key, key, sizeof(extra->key));
	extra->type = type;
	extra->size = size;
	extra->data = (void*)malloc(size);
	memcpy(extra->data, data, size);

	msg->extra[msg->header.extra] = extra;
	msg->header.extra++;
}
void interop_free_extra(interop_message_t* msg) {
	int index;
	for(index=0; index < msg->header.extra; ++index) {
		interop_extra_t* extra = msg->extra[index];
		free(extra->data);
		free(extra);

		msg->extra[index]=NULL;
	}
}
int interop_broadcast(interop_message_t* msg) {

	struct sockaddr_un addr;
	socklen_t len;

	/* use abstract namespace for socket path */
	addr.sun_family = AF_LOCAL;
	addr.sun_path[0] = '\0';

	strcpy(&addr.sun_path[1], INTEROP_SOCKET_ADDRESS);
	len = offsetof(struct sockaddr_un, sun_path)+ 1 + strlen(&addr.sun_path[1]);

	int fd = socket(PF_LOCAL, SOCK_STREAM, 0);
	if (fd < 0) {
		return -1;
	}

	if (connect(fd, (struct sockaddr *) &addr, len) < 0) {
	        close(fd);
	        return -1;
	    }

	int result = write(fd, &msg->header, sizeof(interop_header_t));
	if(result < 0)
		return -1;

	int index=0;
	for(; index < msg->header.extra; ++index) {
		result = write(fd, msg->extra[index], (sizeof(interop_extra_t)-sizeof(void*)));
		if(result < 0)
			return -1;

		result = write(fd, msg->extra[index]->data, msg->extra[index]->size);
		if(result < 0)
			return -1;
	}

	close(fd);

	return 0;
}

#endif
