# X11 Book Corpus Action Map 2026-04-16

## Corpus State

- Corpus root: `/data/data/com.termux/files/home/.cache/research/x11-books-20260416/corpus`
- Manifest: `/data/data/com.termux/files/home/.cache/research/x11-books-20260416/corpus/library-corpus-manifest.json`
- Status: `/data/data/com.termux/files/home/.cache/research/x11-books-20260416/corpus/library-intake-status.md`
- Result: `3/3` PDFs extracted as `ok`

## Books In Scope

### 1. `xlib (1).pdf`

- Pages: `513`
- Extraction: `ok`
- Strongest chapter surface observed:
  - X versions and concepts
  - display/screen/server-client model
  - window management
  - events
  - X extensions
  - protocol, buffering, resources, properties, atoms
  - window hierarchy, stacking, mapping, visibility
  - graphics contexts, drawables, pixmaps
  - interclient communication
  - user preferences/resource database
  - complete application
  - window management

Use in Chapter 2:

- canonical model for window lifecycle and resource ownership
- event queue and propagation semantics
- ICCCM/inter-client communication baseline
- window-manager semantics

### 2. `xlib.pdf`

- Pages: `462`
- Extraction: `ok`
- Strongest chapter surface observed:
  - window information functions
  - graphics and drawing functions
  - event functions
  - color and graphics-context functions
  - font and text functions
  - keyboard/pointer control
  - session and window manager functions
  - locales and internationalized text functions
  - inter-client communication functions

Use in Chapter 2:

- API-level reference map for what client-visible behavior our local X server must honor
- reference checklist for missing or weak semantic areas in events, GC, text, WM-facing behavior, and ICCCM

### 3. `xlibbook-0.5.pdf`

- Pages: `180`
- Extraction: `ok`
- Strongest chapter surface observed:
  - basic Xlib programming steps
  - connection setup and top-level windows
  - overlay service consequences
  - property changes
  - menus, mouse behavior, events
  - pixmaps, cursors, partial transparency
  - reducing server-client interaction using images
  - keyboard entry and text display
  - classic drawing

Use in Chapter 2:

- practical behavioral examples for validating our Java `XServer`
- especially useful for:
  - expose/update behavior
  - pointer/button event sequences
  - pixmap/image behavior
  - property update effects
  - overlay expectations and redraw consequences

## Product Mapping

### Core Protocol / Server Skeleton

Primary owner files:

- `app/src/main/java/com/winlator/cmod/xserver/XServer.java`
- `app/src/main/java/com/winlator/cmod/xserver/XClient.java`
- `app/src/main/java/com/winlator/cmod/xconnector/**`

Literature-backed focus:

- connection lifecycle
- request/reply/event model
- buffering and flush semantics
- resource IDs and object ownership
- extension registration discipline

### Window / Drawable / Pixmap Model

Primary owner files:

- `app/src/main/java/com/winlator/cmod/xserver/Window.java`
- `app/src/main/java/com/winlator/cmod/xserver/WindowManager.java`
- `app/src/main/java/com/winlator/cmod/xserver/Drawable.java`
- `app/src/main/java/com/winlator/cmod/xserver/PixmapManager.java`

Literature-backed focus:

- hierarchy, stacking, map/unmap, visibility
- attributes and configuration
- drawable/pixmap semantics
- expose damage consequences
- background/pixmap/property behavior

### Events / Input / Grabs

Primary owner files:

- `app/src/main/java/com/winlator/cmod/xserver/Pointer.java`
- `app/src/main/java/com/winlator/cmod/xserver/Keyboard.java`
- `app/src/main/java/com/winlator/cmod/xserver/InputDeviceManager.java`
- `app/src/main/java/com/winlator/cmod/xserver/GrabManager.java`

Literature-backed focus:

- event selection and propagation
- queue semantics
- input focus
- pointer and keyboard behavior
- grabs and replay behavior

### Window Management / ICCCM / EWMH Edge

Primary owner files:

- `app/src/main/java/com/winlator/cmod/xserver/Property.java`
- `app/src/main/java/com/winlator/cmod/xserver/SelectionManager.java`
- `app/src/main/java/com/winlator/cmod/xserver/DesktopHelper.java`
- `app/src/main/java/com/winlator/cmod/xserver/Window.java`

Literature-backed focus:

- atoms and properties
- selections and client coordination
- window-manager expectations
- `_NET_WM_*` and Wine compatibility bridges

### Rendering / Extension Surface

Primary owner files:

- `app/src/main/java/com/winlator/cmod/xserver/extensions/GLXExtension.java`
- `app/src/main/java/com/winlator/cmod/xserver/extensions/DRI3Extension.java`
- `app/src/main/java/com/winlator/cmod/xserver/extensions/PresentExtension.java`
- `app/src/main/java/com/winlator/cmod/xserver/extensions/MITSHMExtension.java`
- `app/src/main/java/com/winlator/cmod/xserver/extensions/SyncExtension.java`
- `app/src/main/java/com/winlator/cmod/xserver/extensions/XComposite.java`
- `app/src/main/java/com/winlator/cmod/xenvironment/components/VirGLRendererComponent.java`
- `app/src/main/java/com/winlator/cmod/xenvironment/components/VortekRendererComponent.java`

Literature-backed focus:

- what stays core-X semantics
- what is extension-owned behavior
- where redraw and image transport semantics leak into protocol behavior

## Immediate Engineering Consequences

1. Build an X11 parity ledger per owner class instead of vague “XServer needs more features”.
2. Treat `Window`, `Drawable`, `Pixmap`, `Property`, `Events`, and `WM` as separate closure fronts.
3. Validate extension work against baseline X semantics first, not only donor behavior.
4. Use the practical book examples to create deterministic regression cases for:
   - top-level window creation
   - map/unmap
   - property mutation
   - expose/update
   - pointer/button events
   - pixmap-backed redraw
5. Keep `VirGL` and `Vortek` transport logic clearly separated from core X protocol correctness.

## Remaining Literature Gaps

This corpus is strong for `Xlib` and protocol-facing client behavior, but it is not the whole X11 stack yet.

Still needed for full closure:

- official X11 protocol spec
- XCB documentation
- ICCCM specification
- EWMH / freedesktop WM specs
- extension-specific protocol specs for the exact gaps we choose to close next

## Immediate Next X11 Work

1. Derive a file-by-file X11 parity ledger from these books for:
   - windows
   - events
   - properties
   - pixmaps/drawables
   - window management
2. Compare that ledger against the current local extension matrix:
   - `MIT-SHM`
   - `DRI3`
   - `Present`
   - `Sync`
   - `XComposite`
   - `GLX`
3. Only after that choose the next concrete X11 owner batch.
