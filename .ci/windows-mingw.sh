#!/bin/sh -ex

mkdir build && cd build
cmake .. -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_COMPILER_LAUNCHER=ccache \
    -DCMAKE_CXX_COMPILER_LAUNCHER=ccache \
    -DENABLE_QT_TRANSLATION=ON \
    -DLEMONADE_ENABLE_COMPATIBILITY_REPORTING=OFF \
    -DENABLE_COMPATIBILITY_LIST_DOWNLOAD=OFF \
    -DUSE_DISCORD_PRESENCE=ON \
    -DLEMONADE_USE_PRECOMPILED_HEADERS=OFF \
    -DCMAKE_CXX_FLAGS="-fno-discard-sec-directives" \
    -DALLOW_EXTERNAL_SPIRV_TOOLS=ON
ninja
ninja bundle
strip -s bundle/*.exe

ccache -s -v

ctest -VV -C Release || echo "::error ::Test error occurred on Windows build"
