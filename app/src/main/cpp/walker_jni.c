#include <jni.h>
#include <android/bitmap.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include "walker.h"

#define LCD_WIDTH 96
#define LCD_HEIGHT 64

AAssetManager* assetManager = NULL;
static uint8_t inputState = 0;

static uint64_t lastCycleDelta = 0;

// JNI init function
JNIEXPORT void JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_init(
        JNIEnv *env,
        jobject thiz,
        jobject java_asset_manager,
        jint width,
        jint height) {

    AAssetManager* native_asset_manager = AAssetManager_fromJava(env, java_asset_manager);
    if (native_asset_manager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "WalkerJNI", "Failed to get native AssetManager.");
        return;
    }

    initWalker(native_asset_manager);
}

JNIEXPORT jboolean JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_step(JNIEnv *env, jobject thiz) {
    static uint64_t cycleCount = 0;
    static uint64_t accumulatedCycles = 0;

    const uint64_t ticksPerSecond = 4;
    const uint64_t cyclesPerTick = SYSTEM_CLOCK_CYCLES_PER_SECOND / ticksPerSecond;

    bool error = false;

    uint64_t cyclesThisCall = 0;
    while (accumulatedCycles < cyclesPerTick) {
        uint64_t before = cycleCount;
        error = runNextInstruction(&cycleCount);
        accumulatedCycles += (cycleCount - before);
        cyclesThisCall += (cycleCount - before);
        if (error) return JNI_FALSE;
    }

    accumulatedCycles -= cyclesPerTick;

    quarterRTCInterrupt(); // Tick only after 1/4 second of cycles

    return JNI_TRUE;
}


JNIEXPORT void JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_setKeys(JNIEnv *env, jobject thiz, jint input) {
    inputState = (uint8_t)input;
    setKeys(inputState);
}

JNIEXPORT jobject JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_getFrame(JNIEnv *env, jobject thiz) {
    // Prepare Bitmap.Config.ARGB_8888
    jclass bitmapCls = (*env)->FindClass(env, "android/graphics/Bitmap");
    jmethodID createBitmap = (*env)->GetStaticMethodID(env, bitmapCls, "createBitmap",
                                                       "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jclass configCls = (*env)->FindClass(env, "android/graphics/Bitmap$Config");
    jmethodID valueOf = (*env)->GetStaticMethodID(env, configCls, "valueOf",
                                                  "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jstring argbStr = (*env)->NewStringUTF(env, "ARGB_8888");
    jobject bitmapConfig = (*env)->CallStaticObjectMethod(env, configCls, valueOf, argbStr);

    // Create bitmap with LCD resolution
    jobject bitmap = (*env)->CallStaticObjectMethod(env, bitmapCls, createBitmap,
                                                    LCD_WIDTH, LCD_HEIGHT, bitmapConfig);

    void* pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        return NULL;
    }

    quarterRTCInterrupt();
    fillVideoBuffer((uint32_t*)pixels);


    AndroidBitmap_unlockPixels(env, bitmap);
    return bitmap;
}

JNIEXPORT void JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_quarterRTCInterrupt(JNIEnv* env, jclass clazz) {
    quarterRTCInterrupt();
}


JNIEXPORT jlong JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_getCycleDelta(JNIEnv *env, jobject thiz) {
    return (jlong)lastCycleDelta;
}

JNIEXPORT jlong JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_systemClockCyclesPerSecond(JNIEnv *env, jobject thiz) {
    return SYSTEM_CLOCK_CYCLES_PER_SECOND;
}

JNIEXPORT void JNICALL
Java_com_shy_pokewear_presentation_NativeBridge_setAccelerometer(JNIEnv* env, jobject thiz, jbyte x, jbyte y, jbyte z) {
    setAccelerometer((int8_t)x, (int8_t)y, (int8_t)z);
}
