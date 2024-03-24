set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${PROJECT_SOURCE_DIR}/CMakeModules)

add_executable(lemonade-room
    precompiled_headers.h
    lemonade-room.cpp
    lemonade-room.rc
)

create_target_directory_groups(lemonade-room)

set_target_properties(lemonade-room PROPERTIES OUTPUT_NAME "lemonade-room")

target_link_libraries(lemonade-room PRIVATE lemonade_common network)
if (ENABLE_WEB_SERVICE)
    target_link_libraries(lemonade-room PRIVATE web_service)
endif()

target_link_libraries(lemonade-room PRIVATE cryptopp)
if (MSVC)
    target_link_libraries(lemonade-room PRIVATE getopt)
endif()
target_link_libraries(lemonade-room PRIVATE ${PLATFORM_LIBRARIES} Threads::Threads)

if(UNIX AND NOT APPLE)
    install(TARGETS lemonade-room RUNTIME DESTINATION "${CMAKE_INSTALL_PREFIX}/bin")
endif()

if (CITRA_USE_PRECOMPILED_HEADERS)
    target_precompile_headers(lemonade-room PRIVATE precompiled_headers.h)
endif()

# Bundle in-place on MSVC so dependencies can be resolved by builds.
if (MSVC)
    include(BundleTarget)
    bundle_target_in_place(lemonade-room)
endif()
