set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${PROJECT_SOURCE_DIR}/CMakeModules)

add_executable(lemonade
    lemonade.cpp
    lemonade.rc
    config.cpp
    config.h
    default_ini.h
    emu_window/emu_window_sdl2.cpp
    emu_window/emu_window_sdl2.h
    precompiled_headers.h
    resource.h
)

if (ENABLE_SOFTWARE_RENDERER)
    target_sources(lemonade PRIVATE
        emu_window/emu_window_sdl2_sw.cpp
        emu_window/emu_window_sdl2_sw.h
    )
endif()
if (ENABLE_OPENGL)
    target_sources(lemonade PRIVATE
        emu_window/emu_window_sdl2_gl.cpp
        emu_window/emu_window_sdl2_gl.h
    )
endif()
if (ENABLE_VULKAN)
    target_sources(lemonade PRIVATE
        emu_window/emu_window_sdl2_vk.cpp
        emu_window/emu_window_sdl2_vk.h
    )
endif()

create_target_directory_groups(lemonade)

set_target_properties(lemonade PROPERTIES OUTPUT_NAME "lemonade")

target_link_libraries(lemonade PRIVATE lemonade_common lemonade_core input_common network)
target_link_libraries(lemonade PRIVATE inih)
if (MSVC)
    target_link_libraries(lemonade PRIVATE getopt)
endif()
target_link_libraries(lemonade PRIVATE ${PLATFORM_LIBRARIES} SDL2::SDL2 Threads::Threads)

if (ENABLE_OPENGL)
    target_link_libraries(lemonade PRIVATE glad)
endif()

if(UNIX AND NOT APPLE)
    install(TARGETS lemonade RUNTIME DESTINATION "${CMAKE_INSTALL_PREFIX}/bin")
endif()

if (LEMONADE_USE_PRECOMPILED_HEADERS)
    target_precompile_headers(lemonade PRIVATE precompiled_headers.h)
endif()

# Bundle in-place on MSVC so dependencies can be resolved by builds.
if (MSVC)
    include(BundleTarget)
    bundle_target_in_place(lemonade)
endif()
