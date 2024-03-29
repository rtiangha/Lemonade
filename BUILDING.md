# Common steps/errors

## Make sure to git clone properly 

git clone --recursive https://github.com/Lemonade-emu/Lemonade
cd Lemonade

**The --recursive option automatically clones the required Git submodules too.**

## Common errors

If you get the error ```invalid linker name in argument '-fuse-ld=gold'```
- LLVM For Windows
- Visual Stidio Build Tools (C++ Tools)
- [MinGW-W64](https://github.com/niXman/mingw-builds-binaries) (Make sure to get the online installer and add the bin directiry of MinGW-W64 to your PATH variable)

If you get the error ```SPIR-V Tools not found'```
 - Edit the cmakelists.txt file of glslang (located on the extenrnals folder)

Add ```set(ALLOW_EXTERNAL_SPIRV_TOOLS ON)```

# Building for Windows

## MSVC Build for Windows

Minimal Dependencies

On Windows, all library dependencies are automatically included within the "externals" folder or can be downloaded on-demand. To build Lemonade, you simply need to install:

- Visual Studio 2022 - Make sure to select C++ support in the installer.  
- CMake GUI - Used to generate Visual Studio project files. 2
- Git for Windows

(Make sure you select (Add Git to your system PATH) while installing it)

### Building

1 -Open the CMake GUI application and point it to the Lemonade folder you previosly cloned.

2 -For the build directory, use a build/ subdirectory inside the source directory or some other directory of your choice. (Tell CMake to create it.)

3 - Click the "Configure" button and choose Visual Studio 17 2022, with x64 for the optional platform.

**NOTE: If you get errors like "XXX does not contain a CMakeLists.txt file" at this step, it means you didn't use the `--recursive` flag in the clone step, or you used tools other than the git CLI. Please run "git submodule update --init --recursive" to get remaining dependencies.**

4 -Click "Generate" to create the project files.

5 - Open the solution file lemoande.sln in Visual Studio 2022, which is located in the build folder.

**Depending on which frontend (SDL2 or Qt) you want to build or run, select "lemoande" or "lemoande-qt" in the Solution Explorer, right-click and "Set as Startup Project".**

6 - Select the appropriate build type, Debug for debug purposes or Release for performance (in case of doubt choose the latter).

7 - Press F5 or select Build → Rebuild Solution in the menu.

**NOTE: Please refer to Common errors if any errors. If you did not find a solution feel free to ask us in the discord server.

# Building for Linux

## You'll need to download and install the following to build Lemoande on Linux:

    SDL2
        Deb: sudo apt install libsdl2-dev
        Arch: pacman -S sdl2
        Fedora: sudo dnf install SDL2-devel
        OpenSUSE: zypper in libSDL2-devel

    OpenSSL (optional)
        Deb: sudo apt install libssl-dev
        Arch: pacman -S openssl-1.0
        Fedora: sudo dnf install openssl-devel
        OpenSUSE: zypper in openssl-devel

    Qt 6.2+
        Only 6.2+ versions are tested. Lower version might or might not work. See the section Install new Qt version below if your distro does not provide a sufficient version of Qt
        Deb: sudo apt install qt6-base-dev qt6-base-private-dev qt6-multimedia-dev
            You may also need apt install qt6-l10n-tools qt6-tools-dev qt6-tools-dev-tools to build with translation support
            You may also need apt install libgl-dev if you run into WrapOpenGL issues while configuring with CMake.
        Arch: pacman -S qt6-base qt6-multimedia qt6-multimedia-ffmpeg
            You will also need to install a multimedia backend, either qt6-multimedia-ffmpeg or qt6-multimedia-gstreamer.
        Fedora: sudo dnf install qt6-qtbase-devel qt6-qtbase-private-devel qt6-qtmultimedia-devel
        OpenSUSE: zypper in qt6-base qt6-multimedia

    PORTAUDIO
        Deb: sudo apt install libasound-dev
        Fedora: sudo dnf install portaudio-devel
        OpenSUSE Leap 15: zypper in portaudio-devel
        OpenSUSE Tumbleweed: zypper in portaudio-devel

    XORG
        Deb: sudo apt install xorg-dev libx11-dev libxext-dev
        Fedora: sudo dnf install xorg-x11-server-devel libX11-devel libXext-devel
        OpenSUSE Leap 15: zypper in xorg-x11-util-devel libX11-devel libXext-devel
        OpenSUSE Tumbleweed: zypper in xorg-x11-util-devel libX11-devel libXext-devel

    JACK Audio Connection Kit
        Deb: sudo apt install jackd
        Fedora: sudo dnf install jack-audio-connection-kit-devel
        OpenSUSE Leap 15: zypper in libjack-devel
        OpenSUSE Tumbleweed: zypper in libjack-devel

    PipeWire
        Deb: sudo apt install libpipewire-0.3-dev
        Fedora: sudo dnf install pipewire-devel
        OpenSUSE Leap 15: zypper in pipewire-devel
        OpenSUSE Tumbleweed: zypper in pipewire-devel

### Optional dependencies

    sndio
        Deb: sudo apt install libsndio-dev
        Fedora: sudo dnf -y copr enable andykimpe/shadow && sudo dnf -y install sndio
        OpenSUSE Leap 15: zypper in sndio-devel
        OpenSUSE Tumbleweed: zypper in sndio-devel

    Optional dependencies

    Gnome esound
        Deb: echo "esound require build use source code https://download.gnome.org/sources/esound/"
        Fedora: sudo dnf install esound-devel
        OpenSUSE Leap 15: zypper in libesd0-devel
        OpenSUSE Tumbleweed: zypper in libesd0-devel

    Compiler: GCC or Clang. You only need one of these two:
        GCC 11.0+.
            Deb: apt install build-essential
            Arch: pacman -S base-devel
            Fedora: dnf install gcc-c++
            OpenSUSE: zypper in gcc-c++
        Clang 15.0+
            Deb: apt install clang clang-format libc++-dev
                Note for Ubuntu users: Clang 15 is available only from 22.10 onward. For earlier distro versions, see: https://apt.llvm.org/
            Arch: pacman -S clang, libc++ is in the AUR. Use pacaur or yaourt to install it.
            Fedora: dnf install clang libcxx-devel
            OpenSUSE: zypper in clang

    CMake 3.20+
        Deb: apt install cmake
        Arch: pacman -S cmake
        Fedora: dnf install cmake
        OpenSUSE: zypper in cmake extra-cmake-modules

    Note on Boost library: you don't need to install Boost library on your system, because Lemonade provides a bundled trimmed Boost library. However, if you already have Boost library installed on your system, please make sure its version is at least 1.66 (which contains a bug fix for GCC 7), otherwise compilation would fail.

### Building Lemoande in Debug Mode (Slow):

### Using gcc:
```
mkdir build
cd build
cmake ../
cmake --build . -- -j"$(nproc)"
sudo make install (optional)
```

#### Optionally, you can use "cmake -i .." to adjust various options (e.g. disable the Qt GUI).

### Using clang:

**Note: It is important you use libc++, otherwise your build will likely fail:**

```
mkdir build
cd build
cmake .. -DCMAKE_CXX_COMPILER=clang++-5.0 \
	-DCMAKE_C_COMPILER=clang-5.0 \
	-DCMAKE_CXX_FLAGS="-O2 -g -stdlib=libc++"
cmake --build . -- -j"$(nproc)"
sudo make install (optional)
```

**If you get a weird compile error related to std::span conversions, make sure you are using clang and libc++ 15 or up. This is an issue with libc++ 14.**

### Building Lemoande in Release Mode (Optimized):

```
mkdir build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . -- -j"$(nproc)"
sudo make install (optional)```
```

#### Building with debug symbols:

```
mkdir build
cd build
cmake .. -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build . -- -j"$(nproc)"
sudo make install (optional)
```

#### Running without installing:

After building, the binaries lemonade, lemoande-qt and lemoande-room (depending on your build options) will end up in build/bin/.

#### SDL
`cd build/bin/
./lemoande`

#### Qt
`cd build/bin/
./lemoande-qt`

#### Dedicated room
`cd build/bin/
./lemoande-room`

### Debugging:
```
cd Lemonade
gdb ../build/bin/lemoande-qt
(gdb) run
<crash>
(gdb) bt
```

#### Install new Qt version

If your distribution's version of Qt is too old, there are a few places you may be able to find newer versions.

    This Ubuntu PPA contains backports of Qt 6 to various older versions: https://launchpad.net/~savoury1/+archive/ubuntu/qt-6-2

    This unofficial CLI installer allows downloading and installing the latest first-party builds of Qt to your system (whether it works against your distribution may vary): https://github.com/miurahr/aqtinstall


## Building for Android?

**Dependencies (Windows)**
- Android Studio
- NDK and CMake
- Git
 
###  Compiling
1 - Start Android Studio, on the startup dialog select Open

2 - Navigate to the lemonade/src/android directory and click on OK

3 - Build > Generate Signed Bundle/APK

4 - Select 'APK' and create a key

5 - **Make sure** to select CanaryRelease 
