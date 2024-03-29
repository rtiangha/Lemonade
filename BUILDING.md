## HOW TO BUILD THE APK?

**Dependencies (Windows)**
- Android Studio
- NDK and CMake
- Git

If you get the error ```invalid linker name in argument '-fuse-ld=gold'```
- LLVM For Windows
- Visual Stidio Build Tools (C++ Tools)
- [MinGW-W64](https://github.com/niXman/mingw-builds-binaries) (Make sure to get the online installer and add the bin directiry of MinGW-W64 to your PATH variable)

If you get the error ```SPIR-V Tools not found'```
 - Edit the cmakelists.txt file of glslang (located on the extenrnals folder)

Add ```set(ALLOW_EXTERNAL_SPIRV_TOOLS ON)```
 
###  Compiling
- Start Android Studio, on the startup dialog select Open
- Navigate to the citra/src/android directory and click on OK
- Build > Generate Signed Bundle/APK
- Select 'APK' and create a key
- **Make sure** to select CanaryRelease 
