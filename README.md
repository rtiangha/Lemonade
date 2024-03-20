<h1 align="center">
  <br>
    <a>Lemonade</a>
  <br>
    <b href="https://github.com/Gamer64ytb/Lemonade/blob/master/"><img src="https://github.com/Gamer64ytb/Lemonade/blob/master/assets/Lemonade.png" alt="Lemonade" width="200"></b>
  <br>
</h1>

![GitHub all releases](https://img.shields.io/github/downloads/Gamer64ytb/Lemonade/total)

# Lemonade is a 3DS Emulator ___fork___

***We plan to add new features and enhancements on top of the ___base___ emulator***

## Bug reports

Please, before you make a bug report confirm that the issues only happens on Lemonade

We will try to fix base bugs too, but we need to know if the issues were caused by us or was in the base itself

## PC Builds?

Soon...


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


## Contacting

**We are looking for developers! so please contact us on discord if you are interested**

[![](https://dcbadge.vercel.app/api/server/NVTYcV4v2Q)](https://discord.gg/NVTYcV4v2Q)

[![](https://patrolavia.github.io/telegram-badge/chat.png)](https://t.me/joinchat/lTkg6yC6pQAxNzM0)

## To do:

- Add 100% speed hack
- Add more hacks from MMJ