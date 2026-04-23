# Prefix Donor Shortlist

Updated: `2026-03-21`

## Scope

Reflective donor map for `Prefix Pack`, installer routing, start-menu surfaces,
graphics diagnostics, and Wine-side helper tooling.

## Current Winner: Ajay Prefix Pro v1.6 Offline

- Official release:
  `https://github.com/ajay9634/Ajay-prefix/releases/tag/v1.6_offline`
- Asset:
  `Ajay_Prefix_Pro_v1.6_x64_final_offline.7z`
- Published:
  `2026-03-16`
- SHA-256:
  `e4a23f89c8cc5944b87d7228d04a820e659b494a7e230498910f2c93a2305aa6`
- Local donor base:
  `/data/data/com.termux/files/home/donors/ajay-prefix/v1.6_offline`

## Why Ajay Wins The Prefix/Installer Donor Slot

- It is the only fresh offline donor in this pool that ships a complete
  installable bundle instead of source code only.
- It exposes a real install contract:
  `Only Start Menu`, `Only Prefix`, `Both`.
- It keeps a visible save-data model under `Ajay_prefix/save_data` and writes
  registry/path changes deliberately instead of hiding everything in temp.
- It ships an actual offline component/tooling surface:
  `VC`, `XNA`, `PhysX`, `OpenAL`, `XAudio`, `DX helpers`, wrappers, Wine tools,
  registry helpers, and GPU/API test launchers.

## Ajay Strengths Worth Borrowing

- clear mode selector with `Only Start Menu / Prefix / Both`
- visible offline cache plus save-data roots
- first-run bootstrap script:
  `Ajay_Scripts/Necessary_Components.bat`
- strong graphics diagnostics surface:
  `D3D8`, `D3D9`, `D3D10`, `D3D11`, `D3D12`, `DDraw`, `OpenGL`, `nGlide`
- broad legacy middleware inventory:
  `PhysX`, `XNA 3.0/3.1/4.0`, `OpenAL`, `FAudio/XAudio`
- simple Wine tools surface:
  `Task Manager`, `Wine Configuration`, `Cmd`, `Explorer`, `Registry Editor`
- concrete legacy managed-runtime clues:
  repeated `mscoree` / `mscoreei` / `mscorlib` / `mscorwks` override writes and
  the explicit `Disable Mono(Fix DirectX 2010 Setup)` path
- explicit `XNA` prerequisite wording:
  Ajay's own `xnafx31/xnafx40` launchers tell the user that `Wine Mono` is the
  prerequisite, which is stronger donor evidence than our older assumption that
  XNA should redirect into the generic `.NET Framework 4` lane

## Ajay Limits That Must Not Be Copied Blindly

- installer launches are often naive `Start <exe>` hand-offs without a strong
  proof/state pipeline
- no robust registry proof-token model for legacy `.NET` prerequisite closure
- `Wine Mono` handling is still the old x86 MSI lane, not a richer managed
  runtime contract
- the shipped `.NET` note points to `dotnet9x`, which is not the same problem
  surface as our current `DXSDK Jun10 -> .NET 2.0 -> 51023` failure
- several helpers are deliberately brute-force:
  repeated registry writes with sleeps and loose `Start <exe>` hand-offs.
  Borrow the causal idea, not the weak proof semantics.
- wrappers and graphics payload ownership are wider than our current boundary;
  do not import `DXVK`, `VKD3D`, `dgVoodoo`, or `VulkanRT` into `Prefix Pack`
  just because Ajay surfaces them together

## Fresh Donor Findings For The Current Tail

- `legacy_dx_sdk`
  the useful donor transfer is the Mono/DLL-override compatibility guard, not
  another redistributable and not Ajay's loose fire-and-forget launcher.
- `xna`
  the strongest donor signal is `Wine Mono` first, not `.NET Framework 4`
  first. The active `Ae.solator` batch therefore pivots `xna` to a lane-owned
  `Wine Mono prerequisite -> XNA MSI` flow.
- `physx` / `glview`
  these lanes should keep the staged launcher and logs, but they must graduate
  from vague `queued` wording to an honest "the GUI installer really executed"
  contract once the helper hand-off is accepted.

## Ranked Donor Set By Layer

### Prefix / installer donor

1. `ajay9634/Ajay-prefix`
2. `christian-combine/template`
3. `MrPhryaNikFrosty/Winlator-Frost`
4. `jacojayy/winlator-omod`

### Runtime UI / Android app donor

1. `coffincolors/winlator`
2. `StevenMXZ/Winlator-Ludashi`
3. `Pipetto-crypto/winlator`
4. `brunodev85/winlator`
5. `winebox64/winlator`

### Contents / payload feed donor

1. `ziad9267/Winlator-Contents`
2. `Arihany/WinlatorWCPHub`

### Reference / ecosystem donor

1. `K11MCH1/Winlator101`

## Rejected Or Secondary Donors

- `christian-combine/template`
  useful only for raw start-menu layout seeding; too narrow to drive install
  or state logic
- `MrPhryaNikFrosty/Winlator-Frost`
  useful as evidence that Ajay-derived prefixes are reused in the wild and as a
  reference for compact mod packaging, but not stronger than Ajay for the
  actual prefix-local installer contract
- `jacojayy/winlator-omod`
  useful as a reference for "install Ajay prefix separately" distribution and
  old-prefix packaging, but it still inherits Ajay-style launcher looseness
- `Arihany/WinlatorWCPHub`
  useful for WCP packaging references, not for prefix-local redistributables
- `K11MCH1/Winlator101`
  strong documentation/reference donor, not an installer-flow donor

## Active Conclusion

- Ajay is the right donor base for:
  prefix-local redistributable coverage, Wine-side helper scripts, API tests,
  and start-menu grouping.
- Ajay is not the source-of-truth for:
  our `Prefix Pack` state contract, runtime hand-off proof, or legacy `.NET`
  prerequisite resolution.
- Use Ajay for coverage and scripting ideas, but keep `Ae.solator`'s
  `Prepare -> Install -> State/Logs` contract stricter than Ajay's launcher
  model.
