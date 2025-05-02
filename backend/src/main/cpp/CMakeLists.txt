cmake_minimum_required(VERSION 3.10)
project(QuantLibJNI)

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 禁用 C4819 警告
if(MSVC)
    add_compile_options(/wd4819)
endif()

# 设置 Boost 路径
set(BOOST_ROOT "D:/QuantLib/temp/boost_1_84_0")
set(BOOST_INCLUDEDIR "${BOOST_ROOT}")
set(BOOST_LIBRARYDIR "${BOOST_ROOT}/lib64-msvc-14.3")

# 查找 Boost
find_package(Boost REQUIRED)
include_directories(${Boost_INCLUDE_DIRS})

# 设置 JNI 头文件路径
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# 设置 QuantLib 库路径
set(QuantLib_ROOT "D:/QuantLib")
set(QuantLib_INCLUDE_DIR "${QuantLib_ROOT}/include")
include_directories(${QuantLib_INCLUDE_DIR})

# 设置源文件
set(SOURCE_FILES 
    FXOptionJNI.cpp
)

# 设置输出路径
#set(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../resources/lib/win32/$<CONFIG>)

if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(LIBRARY_OUTPUT_PATH "${PROJECT_SOURCE_DIR}/../resources/lib/win32/Debug")
else()
    set(LIBRARY_OUTPUT_PATH "${PROJECT_SOURCE_DIR}/../resources/lib/win32/Release")
endif()


# 创建动态库
add_library(QuantLibJNI SHARED ${SOURCE_FILES})

# 为不同配置设置不同的编译选项和库
set_target_properties(QuantLibJNI PROPERTIES
    COMPILE_FLAGS_DEBUG "/MTd /Zi /Ob0 /Od /RTC1"
    COMPILE_FLAGS_RELEASE "/MT /O2 /Ob2 /DNDEBUG"
    COMPILE_DEFINITIONS_DEBUG "_ITERATOR_DEBUG_LEVEL=2"
    COMPILE_DEFINITIONS_RELEASE "_ITERATOR_DEBUG_LEVEL=0"
)

# 根据配置设置不同的库路径和名称
set_target_properties(QuantLibJNI PROPERTIES
    DEBUG_POSTFIX "d"
    LIBRARY_OUTPUT_DIRECTORY_DEBUG "${CMAKE_SOURCE_DIR}/src/main/resources/lib/win32/Debug"
    LIBRARY_OUTPUT_DIRECTORY_RELEASE "${CMAKE_SOURCE_DIR}/src/main/resources/lib/win32/Release"
    RUNTIME_OUTPUT_DIRECTORY_DEBUG "${CMAKE_SOURCE_DIR}/src/main/resources/lib/win32/Debug"
    RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_SOURCE_DIR}/src/main/resources/lib/win32/Release"
)

# 为 Debug 和 Release 配置分别设置不同的库
target_link_libraries(QuantLibJNI 
    debug "${QuantLib_ROOT}/build_debug/ql/Debug/QuantLib-x64-mt-sgd.lib"
    optimized "${QuantLib_ROOT}/lib/QuantLib-x64-mt-s.lib"
)

# 链接QuantLib库
#target_link_libraries(QuantLibJNI ${QuantLib_ROOT}/lib/QuantLib.lib)
target_link_libraries(QuantLibJNI ${QuantLib_ROOT}/lib/QuantLib-x64-mt-s.lib)

# 输出配置信息
message(STATUS "Debug QuantLib library: ${QuantLib_ROOT}/build_debug/ql/Debug/QuantLib-x64-mt-sgd.lib")
message(STATUS "Release QuantLib library: ${QuantLib_ROOT}/lib/QuantLib-x64-mt-s.lib")
message(STATUS "Using Boost include: ${Boost_INCLUDE_DIRS}")