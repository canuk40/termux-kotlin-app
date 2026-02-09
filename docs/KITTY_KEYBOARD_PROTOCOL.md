# Kitty Keyboard Protocol Support

This document describes the Kitty keyboard protocol implementation in Termux.

## Overview

The Kitty keyboard protocol is an enhanced keyboard protocol that provides better handling of keyboard input, especially for:
- Modified key combinations (Ctrl, Alt, Shift with any key)
- Distinguishing between different key events
- Better support for international keyboards

## Implementation Details

### DECSET Mode 2017

The protocol is enabled using the DECSET mode 2017:

```bash
# Enable Kitty keyboard mode
printf '\033[>1;2017h'

# Disable Kitty keyboard mode
printf '\033[>1;2017l'
```

### Key Encoding

When Kitty mode is enabled, keys are encoded using the CSI u format:

**Format:** `ESC [ codepoint ; modifiers u`

**Modifiers:**
- Shift = 1
- Alt = 2  
- Ctrl = 4
- Combined modifiers are added (e.g., Ctrl+Shift = 5)

**Examples:**
- `Ctrl+A` → `\033[97;5u` (codepoint 97 = 'a', modifier 5 = Ctrl+1)
- `Alt+B` → `\033[98;3u` (codepoint 98 = 'b', modifier 3 = Alt+1)
- `Shift+Up` → `\033[1;2A` (navigation keys use traditional format with modifiers)

### Special Keys

Navigation and function keys use their traditional escape sequences but with modifier support:

**Navigation Keys:**
- Arrow keys: `ESC [1;mod{A,B,C,D}` (Up, Down, Right, Left)
- Home/End: `ESC [1;mod{H,F}`
- Page Up/Down: `ESC [{5,6};mod~`

**Function Keys:**
- F1-F4: `ESC [1;mod{P,Q,R,S}`
- F5-F12: `ESC [{15,17,18,19,20,21,23,24};mod~`

### Testing

Use the included test script to verify Kitty keyboard mode:

```bash
test-kitty-keyboard
```

This will:
1. Enable Kitty keyboard mode
2. Show instructions for testing
3. Allow you to test various key combinations
4. Disable the mode on exit

## Code Files Modified

- `terminal-emulator/src/main/kotlin/com/termux/terminal/TerminalEmulator.kt`
  - Added `DECSET_BIT_KITTY_KEYBOARD_MODE` (mode 2017)
  - Added `isKittyKeyboardMode()` method
  
- `terminal-emulator/src/main/kotlin/com/termux/terminal/KeyHandler.kt`
  - Added `getCodeForKitty()` method for CSI u encoding
  - Added modifier translation for Kitty protocol

- `terminal-view/src/main/kotlin/com/termux/view/TerminalView.kt`
  - Modified `handleKeyCode()` to use Kitty encoding when mode is active
  - Modified `inputCodePoint()` to handle modified printable characters

## Terminal Compatibility

Applications can detect Kitty keyboard support by:

1. Sending `ESC [?u` to query capabilities (not yet implemented)
2. Enabling mode 2017 and checking if input changes

Common applications that support Kitty protocol:
- Neovim (with proper configuration)
- Helix editor
- Kitty terminal
- WezTerm

## Usage in Applications

### Neovim

Add to your Neovim config:

```lua
vim.o.termguicolors = true
-- Neovim will auto-detect Kitty protocol support
```

### Shell (Bash/Zsh)

The protocol is transparent to shells. They will receive properly encoded key sequences.

### Custom Applications

Applications should:
1. Enable mode with `printf '\033[>1;2017h'`
2. Parse incoming `ESC [ ... u` sequences
3. Disable mode on exit with `printf '\033[>1;2017l'`

## References

- [Kitty Keyboard Protocol Specification](https://sw.kovidgoyal.net/kitty/keyboard-protocol/)
- [CSI u (fixterms) protocol](https://www.leonerd.org.uk/hacks/fixterms/)
