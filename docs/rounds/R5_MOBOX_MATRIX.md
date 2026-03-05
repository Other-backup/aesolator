# Round 5 Matrix: `olegos2/mobox`

Date: `2026-03-05`  
Round state: `active`

## Round Scope

Donor source:
- `/home/mikhail/work/donor-analysis/src/mobox`

Primary code surfaces:
- `install`
- `patches/box64-setdirs.patch`
- `patches/box86-setdirs.patch`
- `patches/fix-address-space.diff`
- `patches/ge-8-25.patch`
- `components/*` (artifact model and layout)

## Transfer Matrix (Strict Round Closure)

| Signal Cluster | Donor Anchors | Aeolator Targets | Status | Decision |
|---|---|---|---|---|
| Bootstrap shell orchestration for runtime bootstrap/install | `install` | `ContentsManager`, package/bootstrap flow docs/contracts | `in_progress` | `integrate` |
| Termux+glibc path normalization for box86/box64 | `patches/box64-setdirs.patch`, `patches/box86-setdirs.patch` | runtime env/path contract (`AERO_*`, launcher bind/path lane) | `in_progress` | `integrate` |
| Termux esync/tmp and wineserver path rewrites | `patches/ge-8-25.patch` | FreeWine/runtime lanes (out of app-tree) | `pending` | `reject_with_rationale` |
| Address-space cap patching | `patches/fix-address-space.diff` | FreeWine memory map policy lane (out of app-tree) | `pending` | `reject_with_rationale` |
| Bundled binary component distribution model | `components/*.apk`, `components/*.deb` | Contents provenance/trust policy and source-lane modeling | `pending` | `integrate` |
| Interactive shell UX and menu flow | `install` interactive prompts | Aeolator in-app UX | `pending` | `reject_with_rationale` |

## Closure Criteria For Round 5

Round 5 can be marked `closed` only when:
1. Every row above has final status `integrated` or `rejected` with rationale.
2. Any accepted donor logic is translated to Aeolator contracts (no blind script copy).
3. Rejected rows explicitly point to owning repo/lane when outside app-tree.
4. Round summary is committed with coverage closure.

## Progress Log

### 2026-03-05 / Pass 1

- Round 5 opened after Round 4 closure.
- Base transfer lanes initialized for bootstrap, path normalization, artifact policy, and reject-boundary rows.

### 2026-03-05 / Pass 2

- Donor inventory anchored:
  - total files: `21`
  - high-signal script/patch/docs files scanned (`install`, `patches/*`, `README*`)
  - components bucket identified (`inputbridge.apk`, `termux-x11.apk`, `.deb` payloads).
- Initial boundary decision prepared:
  - raw Wine/box patch application belongs to runtime/build repos, not to Aeolator app-tree.
