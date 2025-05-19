cmake_minimum_required(VERSION 3.10.2)
project(pokewear)

set(CMAKE_C_STANDARD 11)

add_library(
        walker
        SHARED

        lcdMemViewer.c
        queue.c
        utils.c
        walker.c
        walker_jni.c
)

# Find the Android logging library (liblog.so)
find_library(
        log-lib
        log
)

# Find the jnigraphics library (needed for Android Bitmap operations)
find_library(
        jnigraphics-lib
        jnigraphics
)

target_link_libraries(
        walker
        android
        log
        jnigraphics
        ${log-lib}
        ${jnigraphics-lib}
)
