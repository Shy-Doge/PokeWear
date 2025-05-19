# PokeWear - a [PokeStroller](https://github.com/jpcerrone/pokestroller) port
[PokeStroller](https://github.com/jpcerrone/pokestroller) is an experimental PokeWalker emulator for windows made by [jpcerrone](https://github.com/jpcerrone/). It loads up your eeprom memory file and lets you visualize your pokemon, scroll through the menus and play the dowsing and pokeradar minigames.

This is an Android Wear OS port as I thought it would be cool to have on a Watch (closest thing we have to the PokeWalker).

## Running
To run the emulator you'll need a copy of your PokeWalker's ROM and a copy of your EEPROM binary. You can dump both of these from your PokeWalker using [PoroCYon's dumper for DSi/3ds](https://gitlab.ulyssis.org/pcy/pokewalker-rom-dumper) or [DmitryGR's PalmOS app](https://dmitry.gr/?r=05.Projects&proj=28.%20pokewalker#_TOC_377b8050cfd1e60865685a4ca39bc4c0).

Place both the eeprom and rom files in the `assets` folder and rename them to `eeprom.bin` and `rom.bin` accordingly.

Run the emulator, the buttons are controlled with the touchscreen.

## TODO list
- Audio.
- IR emulation to connect to a Nintendo DS emulator or to another pokestroller instance.
- RTC.
- Accelerometer simulation (Step counting).
- Save changes to eeprom file.
- Fix pokeRadar bug that appears when clicking the wrong bush.
- Change how the rom and eeprom are loaded, preferably would be uploaded and selected.

## Compiling
### Android Wear OS
Build the project using Android Studio

## Contributing
Feel free to contribute by opening up a PR!
