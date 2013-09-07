/*
 * Log.h
 *
 *  Created on: Jan 8, 2013
 *      Author: rob
 */

#ifndef LOG_H_
#define LOG_H_


#include <android/log.h>

#define LOG_TAG		"gscript"

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)

#endif /* LOG_H_ */
