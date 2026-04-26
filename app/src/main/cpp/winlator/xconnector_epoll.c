#include <android/log.h>
#include <errno.h>
#include <jni.h>
#include <pthread.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include <sys/epoll.h>
#include <sys/eventfd.h>
#include <sys/poll.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/un.h>
#include <unistd.h>

#define LOG_TAG "AeroXConnectorEpoll"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define MAX_EVENTS 10
#define MAX_FDS 32
#define MAX_TRACKED_FDS 1024

typedef struct {
    int fd;
    bool owned;
} FdTracker;

static pthread_mutex_t fd_tracking_mutex = PTHREAD_MUTEX_INITIALIZER;
static FdTracker fd_tracking[MAX_TRACKED_FDS];
static bool fd_tracking_initialized = false;

static void init_fd_tracking_locked(void) {
    if (fd_tracking_initialized) return;
    for (int i = 0; i < MAX_TRACKED_FDS; i++) {
        fd_tracking[i].fd = -1;
        fd_tracking[i].owned = false;
    }
    fd_tracking_initialized = true;
}

static void track_fd(int fd) {
    if (fd < 0) return;

    pthread_mutex_lock(&fd_tracking_mutex);
    init_fd_tracking_locked();

    int free_slot = -1;
    for (int i = 0; i < MAX_TRACKED_FDS; i++) {
        if (fd_tracking[i].fd == fd) {
            fd_tracking[i].owned = true;
            pthread_mutex_unlock(&fd_tracking_mutex);
            return;
        }
        if (free_slot < 0 && fd_tracking[i].fd < 0) free_slot = i;
    }

    if (free_slot >= 0) {
        fd_tracking[free_slot].fd = fd;
        fd_tracking[free_slot].owned = true;
    } else {
        LOGD("fd tracker full, continuing without ownership tracking fd=%d", fd);
    }
    pthread_mutex_unlock(&fd_tracking_mutex);
}

static bool release_fd_ownership(int fd) {
    if (fd < 0) return false;

    pthread_mutex_lock(&fd_tracking_mutex);
    init_fd_tracking_locked();
    for (int i = 0; i < MAX_TRACKED_FDS; i++) {
        if (fd_tracking[i].fd == fd) {
            bool owned = fd_tracking[i].owned;
            fd_tracking[i].fd = -1;
            fd_tracking[i].owned = false;
            pthread_mutex_unlock(&fd_tracking_mutex);
            return owned;
        }
    }
    pthread_mutex_unlock(&fd_tracking_mutex);
    return false;
}

static void close_tracked_fd(int fd) {
    if (fd < 0) return;
    bool owned = release_fd_ownership(fd);
    if (!owned) {
        LOGD("skip close for untracked fd=%d", fd);
        return;
    }
    if (close(fd) != 0 && errno != EBADF) {
        LOGD("close failed fd=%d errno=%d (%s)", fd, errno, strerror(errno));
    }
}

static int wait_for_epoll_events(int epoll_fd, struct epoll_event *events, int max_events) {
    for (;;) {
        int num_fds = epoll_wait(epoll_fd, events, max_events, -1);
        if (num_fds >= 0) return num_fds;
        if (errno == EINTR) continue;
        LOGD("epoll_wait failed epollFd=%d errno=%d (%s)", epoll_fd, errno, strerror(errno));
        return -1;
    }
}

static int wait_for_poll_events(struct pollfd *pfds, nfds_t count) {
    for (;;) {
        int result = poll(pfds, count, -1);
        if (result >= 0) return result;
        if (errno == EINTR) continue;
        LOGD("poll failed errno=%d (%s)", errno, strerror(errno));
        return -1;
    }
}

static int accept_retry(int server_fd) {
    int client_fd;
    do {
        client_fd = accept(server_fd, NULL, NULL);
    } while (client_fd < 0 && errno == EINTR);
    return client_fd;
}

static ssize_t read_retry(int fd, void *buffer, size_t length) {
    ssize_t result;
    do {
        result = read(fd, buffer, length);
    } while (result < 0 && errno == EINTR);
    return result;
}

static ssize_t write_retry(int fd, const void *buffer, size_t length) {
    ssize_t result;
    do {
        result = write(fd, buffer, length);
    } while (result < 0 && errno == EINTR);
    return result;
}

static ssize_t recvmsg_retry(int fd, struct msghdr *msg, int flags) {
    ssize_t result;
    do {
        result = recvmsg(fd, msg, flags);
    } while (result < 0 && errno == EINTR);
    return result;
}

static ssize_t sendmsg_retry(int fd, const struct msghdr *msg, int flags) {
    ssize_t result;
    do {
        result = sendmsg(fd, msg, flags);
    } while (result < 0 && errno == EINTR);
    return result;
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_setRLimitToMax(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    struct rlimit limit;
    if (getrlimit(RLIMIT_NOFILE, &limit) < 0) {
        LOGD("getrlimit(RLIMIT_NOFILE) failed errno=%d (%s)", errno, strerror(errno));
        return;
    }
    limit.rlim_cur = limit.rlim_max;
    if (setrlimit(RLIMIT_NOFILE, &limit) < 0) {
        LOGD("setrlimit(RLIMIT_NOFILE) failed errno=%d (%s)", errno, strerror(errno));
    }
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_createAFUnixSocket(
        JNIEnv *env, jobject obj, jstring path, jboolean abstract_namespace) {
    (void)obj;

    if (path == NULL) return -1;

    const char *path_ptr = (*env)->GetStringUTFChars(env, path, NULL);
    if (path_ptr == NULL) return -1;

    size_t path_len = strlen(path_ptr);
    size_t max_path_len = abstract_namespace
            ? sizeof(((struct sockaddr_un *)0)->sun_path) - 1
            : sizeof(((struct sockaddr_un *)0)->sun_path) - 1;
    if (path_len == 0 || path_len > max_path_len) {
        LOGD(
                "AF_UNIX socket path is invalid or too long namespace=%s length=%zu max=%zu path=%s",
                abstract_namespace ? "abstract" : "pathname",
                path_len,
                max_path_len,
                path_ptr
        );
        (*env)->ReleaseStringUTFChars(env, path, path_ptr);
        return -1;
    }

    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        LOGD("socket(AF_UNIX) failed errno=%d (%s)", errno, strerror(errno));
        (*env)->ReleaseStringUTFChars(env, path, path_ptr);
        return -1;
    }
    track_fd(fd);

    struct sockaddr_un server_addr;
    memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sun_family = AF_LOCAL;
    socklen_t addr_length;
    if (abstract_namespace) {
        server_addr.sun_path[0] = '\0';
        memcpy(server_addr.sun_path + 1, path_ptr, path_len);
        addr_length = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + path_len + 1);
    } else {
        memcpy(server_addr.sun_path, path_ptr, path_len + 1);
        addr_length = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + path_len + 1);
        unlink(server_addr.sun_path);
    }
    (*env)->ReleaseStringUTFChars(env, path, path_ptr);

    if (bind(fd, (struct sockaddr *)&server_addr, addr_length) < 0) {
        LOGD(
                "bind failed namespace=%s fd=%d errno=%d (%s)",
                abstract_namespace ? "abstract" : "pathname",
                fd,
                errno,
                strerror(errno)
        );
        close_tracked_fd(fd);
        return -1;
    }
    if (listen(fd, MAX_EVENTS) < 0) {
        LOGD(
                "listen failed namespace=%s fd=%d errno=%d (%s)",
                abstract_namespace ? "abstract" : "pathname",
                fd,
                errno,
                strerror(errno)
        );
        close_tracked_fd(fd);
        return -1;
    }

    LOGD(
            "AF_UNIX socket ready namespace=%s fd=%d addr_length=%u",
            abstract_namespace ? "abstract" : "pathname",
            fd,
            (unsigned)addr_length
    );
    return fd;
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_createEpollFd(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    int fd = epoll_create(MAX_EVENTS);
    if (fd < 0) {
        LOGD("epoll_create failed errno=%d (%s)", errno, strerror(errno));
        return -1;
    }
    track_fd(fd);
    return fd;
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_createEventFd(JNIEnv *env, jobject obj) {
    (void)env;
    (void)obj;

    int fd = eventfd(0, EFD_NONBLOCK);
    if (fd < 0) {
        LOGD("eventfd failed errno=%d (%s)", errno, strerror(errno));
        return -1;
    }
    track_fd(fd);
    return fd;
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_closeFd(JNIEnv *env, jobject obj, jint fd) {
    (void)env;
    (void)obj;
    close_tracked_fd(fd);
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_signalFd(JNIEnv *env, jclass clazz, jint fd) {
    (void)env;
    (void)clazz;

    eventfd_t value = 1;
    int result;
    do {
        result = eventfd_write(fd, value);
    } while (result < 0 && errno == EINTR);
    if (result != 0) {
        LOGD("eventfd_write failed fd=%d errno=%d (%s)", fd, errno, strerror(errno));
    }
    return result == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_addFdToEpoll(
        JNIEnv *env, jobject obj, jint epoll_fd, jint fd) {
    (void)env;
    (void)obj;

    struct epoll_event event;
    memset(&event, 0, sizeof(event));
    event.data.fd = fd;
    event.events = EPOLLIN;
    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, fd, &event) < 0) {
        LOGD("epoll_ctl ADD failed epollFd=%d fd=%d errno=%d (%s)",
             epoll_fd, fd, errno, strerror(errno));
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_removeFdFromEpoll(
        JNIEnv *env, jobject obj, jint epoll_fd, jint fd) {
    (void)env;
    (void)obj;

    if (epoll_ctl(epoll_fd, EPOLL_CTL_DEL, fd, NULL) < 0 &&
        errno != EBADF && errno != ENOENT) {
        LOGD("epoll_ctl DEL failed epollFd=%d fd=%d errno=%d (%s)",
             epoll_fd, fd, errno, strerror(errno));
    }
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_doEpollIndefinitely(
        JNIEnv *env, jobject obj, jint epoll_fd, jint server_fd,
        jboolean add_client_to_epoll) {
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID handle_new_connection =
            (*env)->GetMethodID(env, cls, "handleNewConnection", "(I)V");
    jmethodID handle_existing_connection =
            (*env)->GetMethodID(env, cls, "handleExistingConnection", "(I)V");

    if (handle_new_connection == NULL || handle_existing_connection == NULL) {
        LOGD("failed to resolve XConnectorEpoll callbacks");
        return JNI_FALSE;
    }

    struct epoll_event events[MAX_EVENTS];
    int num_fds = wait_for_epoll_events(epoll_fd, events, MAX_EVENTS);
    if (num_fds < 0) return JNI_FALSE;

    for (int i = 0; i < num_fds; i++) {
        if (events[i].data.fd == server_fd) {
            int client_fd = accept_retry(server_fd);
            if (client_fd < 0) {
                if (errno != EAGAIN && errno != EWOULDBLOCK) {
                    LOGD("accept failed serverFd=%d errno=%d (%s)",
                         server_fd, errno, strerror(errno));
                }
                continue;
            }

            track_fd(client_fd);
            if (add_client_to_epoll) {
                struct epoll_event event;
                memset(&event, 0, sizeof(event));
                event.data.fd = client_fd;
                event.events = EPOLLIN;
                if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, client_fd, &event) < 0) {
                    LOGD("epoll_ctl ADD failed clientFd=%d errno=%d (%s)",
                         client_fd, errno, strerror(errno));
                    close_tracked_fd(client_fd);
                    continue;
                }
            }
            (*env)->CallVoidMethod(env, obj, handle_new_connection, client_fd);
        } else if ((events[i].events & EPOLLIN) != 0) {
            (*env)->CallVoidMethod(env, obj, handle_existing_connection, events[i].data.fd);
        } else if ((events[i].events & (EPOLLERR | EPOLLHUP)) != 0) {
            (*env)->CallVoidMethod(env, obj, handle_existing_connection, events[i].data.fd);
        }
    }

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_waitForSocketRead(
        JNIEnv *env, jobject obj, jint client_fd, jint shutdown_fd) {
    struct pollfd pfds[2];
    pfds[0].fd = client_fd;
    pfds[0].events = POLLIN;
    pfds[0].revents = 0;

    pfds[1].fd = shutdown_fd;
    pfds[1].events = POLLIN;
    pfds[1].revents = 0;

    int result = wait_for_poll_events(pfds, 2);
    if (result < 0 || (pfds[1].revents & POLLIN)) return JNI_FALSE;
    if (pfds[0].revents & (POLLERR | POLLHUP | POLLNVAL)) return JNI_FALSE;

    if (pfds[0].revents & POLLIN) {
        jclass cls = (*env)->GetObjectClass(env, obj);
        jmethodID handle_existing_connection =
                (*env)->GetMethodID(env, cls, "handleExistingConnection", "(I)V");
        if (handle_existing_connection == NULL) {
            LOGD("failed to resolve handleExistingConnection callback");
            return JNI_FALSE;
        }
        (*env)->CallVoidMethod(env, obj, handle_existing_connection, client_fd);
    }

    return JNI_TRUE;
}

JNIEXPORT jintArray JNICALL
Java_com_winlator_cmod_xconnector_XConnectorEpoll_pollEpollEvents(
        JNIEnv *env, jobject obj, jint epoll_fd, jint max_events) {
    (void)obj;

    if (max_events <= 0 || max_events > 1024) return NULL;

    struct epoll_event events[max_events];
    int num_fds = wait_for_epoll_events(epoll_fd, events, max_events);
    if (num_fds < 0) return NULL;

    jintArray result = (*env)->NewIntArray(env, num_fds);
    if (result == NULL) return NULL;

    jint *values = (*env)->GetIntArrayElements(env, result, NULL);
    if (values == NULL) return NULL;

    for (int i = 0; i < num_fds; i++) {
        values[i] = events[i].data.fd;
    }

    (*env)->ReleaseIntArrayElements(env, result, values, 0);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_ClientSocket_read(
        JNIEnv *env, jobject obj, jint fd, jobject data, jint offset, jint length) {
    (void)obj;

    char *data_addr = (*env)->GetDirectBufferAddress(env, data);
    if (data_addr == NULL || offset < 0 || length < 0) return -1;
    return (jint)read_retry(fd, data_addr + offset, (size_t)length);
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_ClientSocket_write(
        JNIEnv *env, jobject obj, jint fd, jobject data, jint length) {
    (void)obj;

    char *data_addr = (*env)->GetDirectBufferAddress(env, data);
    if (data_addr == NULL || length < 0) return -1;
    return (jint)write_retry(fd, data_addr, (size_t)length);
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_ClientSocket_recvAncillaryMsg(
        JNIEnv *env, jobject obj, jint client_fd, jobject data, jint offset, jint length) {
    char *data_addr = (*env)->GetDirectBufferAddress(env, data);
    if (data_addr == NULL || offset < 0 || length < 0) return -1;

    struct iovec iovmsg = {.iov_base = data_addr + offset, .iov_len = (size_t)length};
    struct {
        struct cmsghdr align;
        int fds[MAX_FDS];
    } ctrlmsg;
    memset(&ctrlmsg, 0, sizeof(ctrlmsg));

    struct msghdr msg = {
        .msg_name = NULL,
        .msg_namelen = 0,
        .msg_iov = &iovmsg,
        .msg_iovlen = 1,
        .msg_control = &ctrlmsg,
        .msg_controllen = sizeof(ctrlmsg)
    };

    int size = (int)recvmsg_retry(client_fd, &msg, 0);

    if (size >= 0) {
        for (struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg); cmsg; cmsg = CMSG_NXTHDR(&msg, cmsg)) {
            if (cmsg->cmsg_level == SOL_SOCKET && cmsg->cmsg_type == SCM_RIGHTS) {
                int num_fds = (int)((cmsg->cmsg_len - CMSG_LEN(0)) / sizeof(int));
                jclass cls = (*env)->GetObjectClass(env, obj);
                jmethodID add_ancillary_fd =
                        (*env)->GetMethodID(env, cls, "addAncillaryFd", "(I)V");
                if (add_ancillary_fd == NULL) {
                    LOGD("failed to resolve addAncillaryFd callback");
                    return -1;
                }
                for (int i = 0; i < num_fds && i < MAX_FDS; i++) {
                    int ancillary_fd = ((int *)CMSG_DATA(cmsg))[i];
                    track_fd(ancillary_fd);
                    (*env)->CallVoidMethod(env, obj, add_ancillary_fd, ancillary_fd);
                }
            }
        }
    }

    return size;
}

JNIEXPORT jint JNICALL
Java_com_winlator_cmod_xconnector_ClientSocket_sendAncillaryMsg(
        JNIEnv *env, jobject obj, jint client_fd, jobject data, jint length,
        jint ancillary_fd) {
    (void)obj;

    char *data_addr = (*env)->GetDirectBufferAddress(env, data);
    if (data_addr == NULL || length < 0 || ancillary_fd < 0) return -1;

    struct iovec iovmsg = {.iov_base = data_addr, .iov_len = (size_t)length};
    struct {
        struct cmsghdr align;
        int fds[1];
    } ctrlmsg;
    memset(&ctrlmsg, 0, sizeof(ctrlmsg));

    struct msghdr msg = {
        .msg_name = NULL,
        .msg_namelen = 0,
        .msg_iov = &iovmsg,
        .msg_iovlen = 1,
        .msg_flags = 0,
        .msg_control = &ctrlmsg,
        .msg_controllen = sizeof(ctrlmsg)
    };

    struct cmsghdr *cmsg = CMSG_FIRSTHDR(&msg);
    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;
    cmsg->cmsg_len = CMSG_LEN(sizeof(int));
    ((int *)CMSG_DATA(cmsg))[0] = ancillary_fd;

    return (jint)sendmsg_retry(client_fd, &msg, 0);
}
