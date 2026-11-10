cmake_minimum_required(VERSION 3.22.1)
project(novarun_node_runtime)
add_library(novarun_node_bridge SHARED native-lib.cpp)
set_target_properties(novarun_node_bridge PROPERTIES
    CXX_STANDARD 20
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
)
add_library(libnode SHARED IMPORTED)
set_target_properties(libnode PROPERTIES IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnode.so")
target_include_directories(novarun_node_bridge PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/node-include")
find_library(log-lib log)
target_link_libraries(novarun_node_bridge libnode ${log-lib})
