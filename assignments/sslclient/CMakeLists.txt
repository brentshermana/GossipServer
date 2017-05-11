cmake_minimum_required(VERSION 3.7)
project(sslclient)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES
        forumDownload.cpp
        forumDownload.h)

add_executable(sslclient ${SOURCE_FILES})