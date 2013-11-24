/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "BID"

#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"

#include <utils/Log.h>

#include <time.h>

namespace android
{    

static jint android_server_BID_getBID(JNIEnv* env, jobject clazz)
{
    return getbid();
}

static void andriod_server_BID_setBID(JNIEnv* env, jobject clazz, jint value)
{
    int bid = value;
    setbid(bid);
}

static JNINativeMethod method_table[] = {
    {"getBID", "()I",  (void *)android_server_BID_getBID},
    {"setBID", "(I)V", (void *)andriod_server_BID_setBID},
};

int register_android_server_BID(JNIEnv* env)
{
    return jniRegisterNativeMethods(env, "com/android/server/BID", method_table, NELEM(method_table));
}

}
