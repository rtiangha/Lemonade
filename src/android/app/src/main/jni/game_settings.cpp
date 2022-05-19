// Copyright 2019 Citra Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the license.txt file included.

#include "core/settings.h"

namespace GameSettings {

void LoadOverrides(u64 program_id) {
    if (program_id == 0x0004000000068B00 || program_id == 0x0004000000061300 ||
        program_id == 0x000400000004A700 || program_id == 0x000400000005D700) {
        // hack for Tales of the Abyss / Pac Man Party 3D
        Settings::values.display_transfer_hack = true;
        // crash on `g_state.geometry_pipeline.Reconfigure();`
        // state.regs.pipeline.gs_unit_exclusive_configuration = 0
        // state.regs.gs.max_input_attribute_index = 0
        Settings::values.skip_slow_draw = true;
        // may cause display issues
        Settings::values.texture_load_hack = false;
    } else if (program_id == 0x00040000001CCD00 || program_id == 0x00040000001B4500) {
        // The Alliance Alive
        Settings::values.core_downcount_hack = true;
    } else if (program_id == 0x0004000000120900 || program_id == 0x0004000000164300) {
        // Lord of Magna: Maiden Heaven
        Settings::values.core_downcount_hack = true;
    } else if (program_id == 0x000400000015CB00) {
        // New Atelier Rorona
        Settings::values.skip_slow_draw = true;
    } else if (program_id == 0x000400000018E900) {
        // My Hero Academia
        Settings::values.skip_slow_draw = true;
    } else if (program_id == 0x000400000016AD00) {
        // Dragon Quest Monsters Joker 3
        Settings::values.skip_slow_draw = true;
    } else if (program_id == 0x00040000001ACB00) {
        // Dragon Quest Monsters Joker 3 Professional
        Settings::values.skip_slow_draw = true;
    } else if (program_id == 0x00040000000D2800 || program_id == 0x0004000000065800 ||
               program_id == 0x0004000000766600) {
        // Rune Factory 4
        Settings::values.texture_load_hack = false;
    } else if (program_id == 0x0004000000055D00 || program_id == 0x0004000000055E00) {
        // Pokémon X/Y
        Settings::values.texture_load_hack = false;
    } else if (program_id == 0x000400000004B500) {
        // Monster Hunter 4
        Settings::values.texture_load_hack = true;
    } else if (program_id == 0x0004000000126300 || program_id == 0x000400000011D700 ||
               program_id == 0x0004000000126100 || program_id == 0x0004000000153200) {
        // Monster Hunter 4 Ultimate
        Settings::values.texture_load_hack = true;
    } else if (program_id == 0x0004000000155400) {
        // Monster Hunter X
        Settings::values.texture_load_hack = true;
    } else if (program_id == 0x000400000008FE00) {
        // 1001 Spikes
        Settings::values.core_downcount_hack = true;
    } else if (program_id == 0x0004000000049100 || program_id == 0x0004000000030400 ||
               program_id == 0x0004000000049000) {
        // Star Fox 64
        Settings::values.disable_clip_coef = true;
    } else if (program_id == 0x0004000000187E00 || program_id == 0x0004000000169A00) {
        // Picross 2
        Settings::values.disable_clip_coef = true;
    } else if (program_id == 0x00040000000DCA00 || program_id == 0x00040000000F4000) {
        // Danball Senki W Chou Custom, Danball Senki WARS
        Settings::values.y2r_perform_hack = true;
    } else if (program_id == 0x000400000008B400 || program_id == 0x0004000000030600 ||
               program_id == 0x0004000000030800 || program_id == 0x0004000000030700) {
        // Mario Kart 7
        Settings::values.skip_texture_copy = true;
    }

    const std::array<u64, 7> cpu_limit_ids = {
        0x000400000007C700, // Mario Tennis Open
        0x000400000007C800, // Mario Tennis Open
        0x0004000000064D00, // Mario Tennis Open
        0x00040000000B9100, // Mario Tennis Open
        0x00040000000DCD00, // Mario Golf: World Tour
        0x00040000000A5300, // Mario Golf: World Tour
        0x00040000000DCE00, // Mario Golf: World Tour
    };
    for (auto id : cpu_limit_ids) {
        if (program_id == id) {
            Settings::values.core_downcount_hack = true;
            break;
        }
    }

    const std::array<u64, 10> linear_ids = {
        0x00040000001AA200, // Attack On Titan 2
        0x0004000000134500, // Attack On Titan 1 CHAIN
        0x0004000000152000, // Attack On Titan 1 CHAIN
        0x0004000000134500, // Attack On Titan 1 CHAIN
        0x00040000000DF800, // Attack On Titan 1
    };
    for (auto id : linear_ids) {
        if (program_id == id) {
            Settings::values.use_linear_filter = true;
            break;
        }
    }

    const std::array<u64, 12> fifa_ids = {
        0x0004000000044700, // FIFA 12
        0x0004000000047A00, // FIFA 12
        0x0004000000044800, // FIFA 12
        0x00040000000A2B00, // FIFA 13
        0x00040000000A2900, // FIFA 13
        0x00040000000A3000, // FIFA 13
        0x00040000000E7900, // FIFA 14
        0x00040000000DEA00, // FIFA 14
        0x00040000000E7A00, // FIFA 14
        0x000400000013C700, // FIFA 15
        0x000400000013CA00, // FIFA 15
        0x000400000013CB00, // FIFA 15
    };
    for (auto id : fifa_ids) {
        if (program_id == id) {
            Settings::values.y2r_event_delay = true;
            break;
        }
    }

    const std::array<u64, 50> accurate_mul_ids = {
        0x0004000000134500, // Attack on Titan
        0x00040000000DF800, // Attack on Titan
        0x0004000000152000, // Attack on Titan
        0x00040000001AA200, // Attack on Titan
        0x0004000000054000, // Super Mario 3D Land
        0x0004000000053F00, // Super Mario 3D Land
        0x0004000000054100, // Super Mario 3D Land
        0x0004000000089F00, // Super Mario 3D Land
        0x0004000000089E00, // Super Mario 3D Land
        0x0004000000033400, // The Legend of Zelda: Ocarina of Time 3D
        0x0004000000033500, // The Legend of Zelda: Ocarina of Time 3D
        0x0004000000033600, // The Legend of Zelda: Ocarina of Time 3D
        0x000400000008F800, // The Legend of Zelda: Ocarina of Time 3D
        0x000400000008F900, // The Legend of Zelda: Ocarina of Time 3D
        0x0004000000132700, // Mario & Luigi: Paper Jam
        0x0004000000132600, // Mario & Luigi: Paper Jam
        0x0004000000132800, // Mario & Luigi: Paper Jam
        0x00040000001D1400, // Mario & Luigi: Bowsers Inside Story + Bowser Jrs Journey
        0x00040000001D1500, // Mario & Luigi: Bowsers Inside Story + Bowser Jrs Journey
        0x00040000001CA900, // Mario & Luigi: Bowsers Inside Story + Bowser Jrs Journey
        0x00040000001B8F00, // Mario & Luigi: Superstar Saga + Bowsers Minions
        0x00040000001B9000, // Mario & Luigi: Superstar Saga + Bowsers Minions
        0x0004000000194B00, // Mario & Luigi: Superstar Saga + Bowsers Minions
        0x00040000001CB000, // Captain Toad: Treasure Tracker
        0x00040000001CB200, // Captain Toad: Treasure Tracker
        0x00040000001CB100, // Captain Toad: Treasure Tracker
        0x00040000000EC200, // The Legend of Zelda: A Link Between Worlds
        0x00040000000EC300, // The Legend of Zelda: A Link Between Worlds
        0x00040000000EC400, // The Legend of Zelda: A Link Between Worlds
        0x000400000007AD00, // New Super Mario Bros. 2
        0x00040000000B8A00, // New Super Mario Bros. 2
        0x000400000007AE00, // New Super Mario Bros. 2
        0x000400000007AF00, // New Super Mario Bros. 2
        0x0004000000079600, // Jett Rocket II
        0x0004000000112600, // Cut the Rope
        0x0004000000116700, // Cut the Rope
        0x00040000000D0000, // Luigi's Mansion: Dark Moon
        0x0004000000076400, // Luigi's Mansion: Dark Moon
        0x0004000000055F00, // Luigi's Mansion: Dark Moon
        0x0004000000076500, // Luigi's Mansion: Dark Moon
        0x00040000000AFC00, // Digimon World Re:Digitize Decode
        0x0004000000125600, // The Legend of Zelda: Majoras Mask 3D
        0x0004000000125500, // The Legend of Zelda: Majoras Mask 3D
        0x00040000000D6E00, // The Legend of Zelda: Majoras Mask 3D
        0x0004000000154700, // Lego City Undercover
        0x00040000000AD600, // Lego City Undercover
        0x00040000000AD500, // Lego City Undercover
        0x00040000001D1800, // Luigi's Mansion
        0x00040000001D1A00, // Luigi's Mansion
        0x00040000001D1900, // Luigi's Mansion
    };
    for (auto id : accurate_mul_ids) {
        if (program_id == id) {
            Settings::values.shaders_accurate_mul = true;
            break;
        }
    }

    const std::array<u64, 30> new3ds_game_ids = {
        0x000400000F700000, // Xenoblade Chronicles 3D [JPN]
        0x000400000F700100, // Xenoblade Chronicles 3D [USA]
        0x000400000F700200, // Xenoblade Chronicles 3D [EUR]
        0x000400000F70CC00, // Fire Emblem Warriors [USA]
        0x000400000F70CD00, // Fire Emblem Warriors [EUR]
        0x000400000F70C100, // Fire Emblem Warriors [JPN]
        0x000400000F700800, // The Binding of Isaac: Rebirth [USA]
        0x000400000F701700, // The Binding of Isaac: Rebirth [JPN]
        0x000400000F700900, // The Binding of Isaac: Rebirth [EUR]
        0x00040000000CCE00, // Donkey Kong Country Returns 3D [USA]
        0x00040000000CC000, // Donkey Kong Country Returns 3D [JPN]
        0x00040000000CCF00, // Donkey Kong Country Returns 3D [EUR]
        0x0004000000127500, // Sonic Boom: Shattered Crystal [USA]
        0x000400000014AE00, // Sonic Boom: Shattered Crystal [JPN]
        0x000400000012C200, // Sonic Boom: Shattered Crystal [EUR]
        0x0004000000161300, // Sonic Boom: Fire & Ice [USA]
        0x0004000000170700, // Sonic Boom: Fire & Ice [JPN]
        0x0004000000164700, // Sonic Boom: Fire & Ice [EUR]
        0x00040000000B3500, // Sonic & All-Stars Racing Transformed [USA]
        0x000400000008FC00, // Sonic & All-Stars Racing Transformed [EUR]
        0x00040000001B8700, // Minecraft [USA]
        0x000400000F707F00, // Hyperlight EX [USA]
        0x000400000008FE00, // 1001 Spikes [USA]
        0x000400000007C700, // Mario Tennis Open
        0x000400000007C800, // Mario Tennis Open
        0x0004000000064D00, // Mario Tennis Open
        0x00040000000B9100, // Mario Tennis Open
        0x00040000000DCD00, // Mario Golf: World Tour
        0x00040000000A5300, // Mario Golf: World Tour
        0x00040000000DCE00, // Mario Golf: World Tour
    };
    for (auto id : new3ds_game_ids) {
        if (program_id == id) {
            Settings::values.is_new_3ds = true;
            break;
        }
    }
}

} // namespace GameSettings
