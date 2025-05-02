cmake_minimum_required(VERSION 3.10)
project(Backend)

# 设置构建类型为 Debug
set(CMAKE_BUILD_TYPE Debug)

# 设置 MSVC 运行时库
if(MSVC)
    set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreadedDebug")
    set(CompilerFlags
        CMAKE_CXX_FLAGS
        CMAKE_CXX_FLAGS_DEBUG
        CMAKE_CXX_FLAGS_RELEASE
        CMAKE_C_FLAGS
        CMAKE_C_FLAGS_DEBUG
        CMAKE_C_FLAGS_RELEASE
    )
    foreach(CompilerFlag ${CompilerFlags})
        string(REPLACE "/MD" "/MT" ${CompilerFlag} "${${CompilerFlag}}")
    endforeach()
endif()

# 设置 Boost 路径
set(BOOST_ROOT "D:/QuantLib/temp/boost_1_84_0")
set(BOOST_INCLUDEDIR "${BOOST_ROOT}")
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME ON)
set(Boost_NO_SYSTEM_PATHS ON)

# 设置 QuantLib 路径
set(QUANTLIB_ROOT "D:/QuantLib")
set(QUANTLIB_INCLUDE_DIR "${QUANTLIB_ROOT}/include")
set(QUANTLIB_LIBRARY "${QUANTLIB_ROOT}/build_debug/ql/Debug/QuantLib-x64-mt-sgd.lib")

# 查找 Boost 组件
find_package(Boost REQUIRED)
if(Boost_FOUND)
    message(STATUS "Boost found: ${Boost_INCLUDE_DIRS}")
    message(STATUS "Boost version: ${Boost_VERSION}")
else()
    message(FATAL_ERROR "Could not find Boost")
endif()

# 添加 cpp 子目录
add_subdirectory(src/main/cpp)

# 设置全局编译选项
if(MSVC)
    # 添加 /utf-8 以解决编码问题，禁用特定警告
    add_compile_options(
        /W4 
        /MTd 
        /utf-8
        /wd4267  # 禁用 size_t 到 int 的转换警告
        /wd4819  # 禁用代码页警告
    )
    add_compile_definitions(_DEBUG)
    # 添加预处理器定义
    add_definitions(
        -DQL_DEBUG
        -DQL_ENABLE_THREAD_SAFE_OBSERVER_PATTERN
        -DBOOST_ENABLE_ASSERT_DEBUG_HANDLER
    )
else()
    add_compile_options(-Wall -Wextra -Wpedantic -g)
endif()

# 输出构建信息
message(STATUS "Build type: ${CMAKE_BUILD_TYPE}")
message(STATUS "QuantLib library: ${QUANTLIB_LIBRARY}")
message(STATUS "QuantLib include dir: ${QUANTLIB_INCLUDE_DIR}")
message(STATUS "Boost include dirs: ${Boost_INCLUDE_DIRS}")
message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
if(MSVC)
    message(STATUS "MSVC Runtime Library: ${CMAKE_MSVC_RUNTIME_LIBRARY}")
endif() 