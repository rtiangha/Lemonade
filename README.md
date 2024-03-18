<h1 align="center">
  <br>
    <a>Lemonade</a>
  <br>
    <b href="https://github.com/Gamer64ytb/Lemonade/blob/master/"><img src="https://github.com/Gamer64ytb/Lemonade/blob/master/assets/Lemonade.png" alt="Lemonade" width="200"></b>
  <br>
</h1>

![GitHub all releases](https://img.shields.io/github/downloads/Gamer64ytb/Lemonade/total)

# Lemonade is an Android-focused 3DS Emulator ___fork___

***We plan to add new features and enhancements on top of the ___base___ emulator***

# Notice
## PC Builds?

For now we have no plans to make a PC version of Lemonade for 2 reasons 

- There are other forks focusing on PC development 
- We lack the knowledge to actually improve the PC version of this emulator 

However if you want to help with making a PC version of this emulator you can contact us on discord and we may reverse this decision

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
 - Like the error says, edit the cmakelists.txt file of glslang (located on the extenrnals folder)

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
