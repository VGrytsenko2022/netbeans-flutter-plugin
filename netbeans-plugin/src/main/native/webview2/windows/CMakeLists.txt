cmake_minimum_required(VERSION 3.25)
project(nb_flutter_webview2_host LANGUAGES CXX)

if(NOT WIN32 OR NOT CMAKE_SIZEOF_VOID_P EQUAL 8)
    message(FATAL_ERROR "The current WebView2 adapter supports Windows x64 only.")
endif()
if(NOT DEFINED WEBVIEW2_SDK_ROOT)
    message(FATAL_ERROR "WEBVIEW2_SDK_ROOT must point at the extracted Microsoft.Web.WebView2 package.")
endif()

add_library(nb_flutter_webview2_host SHARED
    nb_webview2_host.cpp
    nb_webview2_host.h)
target_compile_features(nb_flutter_webview2_host PRIVATE cxx_std_17)
set_property(TARGET nb_flutter_webview2_host PROPERTY
    MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
target_compile_definitions(nb_flutter_webview2_host PRIVATE
    UNICODE
    _UNICODE
    WIN32_LEAN_AND_MEAN
    NOMINMAX)
target_include_directories(nb_flutter_webview2_host PRIVATE
    "${WEBVIEW2_SDK_ROOT}/build/native/include")
target_link_libraries(nb_flutter_webview2_host PRIVATE
    "${WEBVIEW2_SDK_ROOT}/build/native/x64/WebView2Loader.dll.lib"
    bcrypt
    ole32
    user32)

if(MSVC)
    target_compile_options(nb_flutter_webview2_host PRIVATE
        /W4 /WX /permissive- /EHsc /utf-8 /Brepro /Zc:__cplusplus)
    target_link_options(nb_flutter_webview2_host PRIVATE
        /INCREMENTAL:NO /OPT:REF /OPT:ICF /Brepro)
endif()

option(NBWV2_BUILD_SMOKE "Build the opt-in physical WebView2 smoke executable" OFF)
if(NBWV2_BUILD_SMOKE)
    add_executable(nb_flutter_webview2_smoke
        ../../../../test/native/webview2/windows/nb_webview2_smoke.cpp)
    target_compile_features(nb_flutter_webview2_smoke PRIVATE cxx_std_17)
    set_property(TARGET nb_flutter_webview2_smoke PROPERTY
        MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    target_compile_definitions(nb_flutter_webview2_smoke PRIVATE
        UNICODE _UNICODE WIN32_LEAN_AND_MEAN NOMINMAX)
    target_include_directories(nb_flutter_webview2_smoke PRIVATE
        "${CMAKE_CURRENT_SOURCE_DIR}")
    target_link_libraries(nb_flutter_webview2_smoke PRIVATE
        nb_flutter_webview2_host bcrypt user32)
    if(MSVC)
        target_compile_options(nb_flutter_webview2_smoke PRIVATE
            /W4 /WX /permissive- /EHsc /utf-8 /Brepro /Zc:__cplusplus)
        target_link_options(nb_flutter_webview2_smoke PRIVATE
            /INCREMENTAL:NO /OPT:REF /OPT:ICF /Brepro)
    endif()
endif()
