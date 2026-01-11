# Sims FreePlay APK Extraction

This repository contains extracted components from the Sims FreePlay APK (v5.09.0) for analysis and integration purposes.

## Contents

### Native Libraries (`native-libs/`)

ARM64-v8a native libraries extracted from the APK:

| Library | Description |
|---------|-------------|
| `libfreeplay.so` | Main game engine library |
| `libunity.so` | Unity game engine runtime |
| `libil2cpp.so` | IL2CPP compiled game code |
| `libmain.so` | Main entry point |
| And others... | Supporting libraries |

### Lua Schemas (`lua-schemas/`)

Game configuration and layout schemas written in Lua:

- `layout_schema.lua` - UI layout definitions and element schemas

### Assets (`assets/`)

Game configuration and metadata files:

- `build.config` - Build configuration
- `builddatas.json` - Build metadata
- `info.txt` - Version information

### Documentation (`docs/`)

Analysis documentation and integration guides.

## Usage

These extracted components are intended for:

1. **Analysis** - Understanding game architecture and patterns
2. **Integration** - Incorporating simulation patterns into other projects
3. **Reference** - Learning from professional game development practices

## Related Projects

- [dream-vortex](https://github.com/o9nn/dream-vortex) - Main simulation platform integrating these patterns
- [dreamcog](https://github.com/o9nn/dreamcog) - Cognitive agent framework
- [vorticog](https://github.com/o9nn/vorticog) - Vortex cognitive integration

## APK Information

- **Package**: com.ea.games.simsfreeplay_row
- **Version**: 5.09.0
- **Architecture**: ARM64-v8a

## License

This repository contains extracted components for educational and research purposes only. All original content remains property of Electronic Arts Inc.
