# Graphics Global Research Ledger 2026-04-19

## Purpose

This ledger records public evidence used for the Chapter 2 graphics-wrapper and
AeMali/PanVK lane. It is not a claim that the internet is exhausted. It is the
reproducible source map for the current widening pass.

## Official / Upstream Sources

| Source | Evidence class | Product use |
| --- | --- | --- |
| `https://docs.mesa3d.org/drivers/panfrost.html` | Official Mesa driver documentation | Panfrost/PanVK capability boundary and Mali generation ownership. |
| `https://source.android.com/docs/core/graphics/implement-vulkan` | Official Android platform documentation | Android Vulkan HAL and loader contract; validates that Android stock Vulkan is HAL/vendor-module routed. |
| `https://android.googlesource.com/platform/frameworks/native/+/master/vulkan/include/hardware/hwvulkan.h` | AOSP source contract | `hwvulkan` HAL module ABI and Android loader-facing driver boundary. |
| `https://gitlab.freedesktop.org/mesa/mesa` | Upstream Mesa source | Source owner for Zink, Turnip, Panfrost, Lima, PanVK, Vulkan runtime, Android WSI/HAL-adjacent code. |
| `https://gitlab.freedesktop.org/virgl/virglrenderer` | Upstream VirGL source | Source owner for virglrenderer refresh and Android/bionic port probes. |
| `https://developer.arm.com/search#numberOfResults=48&q=Mali` | Vendor search surface | Public Arm Mali corpus front door; used with Coveo token/search API to build a reproducible vendor-source ledger. |
| `https://en.opensuse.org/ARM_Mali_GPU` | Operational distro documentation | Cross-check for Lima/Panfrost split, kernel floor expectations, and distro-facing downstream-vs-upstream boundaries. |
| `https://bakhi.github.io/mobileGPU/mali/` | Public RE notes | Low-level Mali kernel/user-space, MMU, job-chain, and trace-stream notes; useful as RE knowledge, not source-of-truth code. |
| `https://pine64.org/documentation/General/Mali_driver/` | Board-vendor documentation | Historical binary-driver packaging examples and DRM/X11 coexistence pitfalls for older Mali downstream lanes. |
| `https://github.com/fxlin/mali` | Public RE repo | Mali driver/kernel reverse-engineering notes; supports internal model of kbase/MMU/job-slot internals. |
| `https://github.com/tsukumijima/libmali-rockchip` | Public binary-packaging repo | Proprietary Rockchip libmali packaging/reference surface; useful for ABI/package/firmware census, not acceptable code donor. |
| `https://github.com/AgentFabulous/begonia/tree/a10-rebase` | Android vendor-kernel repo | Full Android 10 vendor kernel tree for `xiaomi-mt6785` / `begonia`; high-value source for Mali kernel-driver archaeology, BSP power/memory integration, and IDE-level reverse-engineering analysis of legacy kbase-era behavior. |
| `https://github.com/CherishOS-Devices/android_device_realme_RM6785/tree/tiramisu` | Android device-tree repo | MT6785 / `Mali-G76 MC4` Android product-integration surface; useful for proprietary-file census, board config, sepolicy, graphics properties, and runtime packaging assumptions around the same SoC family. |
| `https://github.com/realme-kernel-opensource` | Public vendor-kernel organization | Broad Realme/Oppo-family kernel-source donor universe; current API census found `473` repos and live MediaTek/Mali GPU paths in MT6833/MT6789 candidates. |
| `https://habr.com/ru/articles/655673/` | Public technical article | Reverse-engineering methodology and Mesa-driver implementation framing. |

## Vortek / Wrapper Analysis Sources

| Source | Evidence class | Product use |
| --- | --- | --- |
| `https://dev.to/possiblyquestionable/vortek-internals-part-1-command-buffers-3n7h` | Public technical analysis | Vortek command-buffer/IPC model and compatibility-workaround framing. |
| `https://dev.to/possiblyquestionable/vortek-internals-part-2-driver-specific-workarounds-2d8l` | Public technical analysis | Driver-specific extension/workaround policy model; mapped into AeMali extension profile and env-policy surface. |
| `https://github.com/leegao/vortek-deep-dive` | Public donor-analysis repo | Cross-check for Vortek concepts and runtime route evidence. |
| `https://github.com/leegao/bionic-vulkan-wrapper` | Public donor/source repo | Android bionic Vulkan wrapper route evidence. |

## Local Reproducible Discovery Artifacts

| Artifact | Evidence class | Summary |
| --- | --- | --- |
| `/data/data/com.termux/files/home/.cache/omega-donor-research/20260419T202520Z-graphics-global-github` | Raw GitHub API cache | 15 graphics-focused queries; 218 repositories seen; 48 selected candidates; 31 graphics-source/runtime candidates. |
| `/data/data/com.termux/files/home/.codex/donors/chapter2_donor_manifest.graphics_expanded.json` | Expanded donor manifest | 610 total donors; 518 enabled; 8 graphics source donors; 22 graphics runtime donors. |
| `/data/data/com.termux/files/home/.cache/omega-donor-frontier/20260419T203202Z-graphics-global-inventory` | Graphics donor inventory | 30 graphics donors inventoried; 0 blocked. |
| `/data/data/com.termux/files/home/aesolator/patches/mesa/aemali/0001-panvk-aemali-policy-surface.patch` | Product downstream source patch | AeMali policy integration into PanVK instance/device properties and extension filtering. |
| `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420` | Arm vendor corpus intake | `3879` raw hits, `3262` unique results, `81` raw search pages, `1499` HTML page candidates. |
| `/data/data/com.termux/files/home/.cache/research/habr-mali-intake-20260420/corpus` | Habr Mali corpus intake | `40` search hits, `20` unique article/news pages, `0` fetch errors. |
| `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus` | Expanded Habr AeMali corpus intake | `202` search hits, `87` unique pages, `0` fetch errors across Mali/Panfrost/PanVK/Mesa/Bifrost/Valhall/kbase/MediaTek/GED/Vulkan/OpenGL queries. |
| `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420/arm-body-fetch-blocker-20260420.json` | Arm body-fetch blocker ledger | Direct body harvest hit HTTP `403` on `21` attempts; active route is Coveo metadata/excerpts, accessible official pages, mirrors, and explicit blocked rows. |
| `/data/data/com.termux/files/home/.cache/research/graphics-seed-links-20260420` | Seed hyperlink frontier | First-hop link ledgers for `bakhi`, `pine64`, `Habr`, `GitHub`, and `Elvees` Mali pages. |
| `/data/data/com.termux/files/home/aesolator/patches/mesa/aemali/0002-panvk-aemali-api-truth-surface.patch` | Product downstream source patch | Closes the remaining instance-vs-physical-device Vulkan API truth split in AeMali PanVK. |
| `/data/data/com.termux/files/home/.cache/research/realme-kernel-opensource-intake-20260420` | GitHub org census | `473` public repos enumerated, `60` top candidates API-probed, then widened into a full git-tree sweep. |
| `/data/data/com.termux/files/home/.cache/research/realme-kernel-git-tree-probe-20260420` | Git transport kernel probe | API-rate-limit fallback; `6/6` Realme kernel candidates probed with partial no-checkout git tree reads, `0` failures. |
| `/data/data/com.termux/files/home/.cache/research/realme-kernel-full-git-tree-probe-20260420` | Full Realme kernel git-tree probe | Full `473`-repo Realme kernel donor sweep through shallow blobless no-checkout git transport; final reduction found `175` `panfrost` source-tree donors, `175` `lima` source-tree donors, `2` `panthor` source-tree donors, `28` dense MediaTek kbase/GED donors, `18` MediaTek kbase donors, `66` MediaTek display-DRM-only donors, and `186` no-signal repos. |
| `/data/data/com.termux/files/home/tools/aemali_kernel_donor_reduce.py` | Kernel donor reducer | Converts raw per-repo git probes into ranked AeMali source-tree evidence classes: Panfrost source tree, Lima source tree, Panthor source tree, MediaTek kbase/GED, MediaTek display DRM, and low-signal GPU trees. |
| `/data/data/com.termux/files/home/tools/aemali_kernel_enablement_probe.py` | Kernel enablement reducer | Second-stage Kconfig/Makefile/defconfig/DT probe that separates source presence from device/build/runtime route evidence. |
| `/data/data/com.termux/files/home/.cache/research/realme-kernel-enablement-probe-20260420/enablement.partial.json` | Realme top-sample enablement evidence | Top `12` dense MTK donors all classified as `vendor-kbase-enabled-candidate`; none became a proven Panfrost route from Kconfig/defconfig/DT evidence. |
| `/data/data/com.termux/files/home/.cache/research/aemali-mainline-kernel-enablement-20260420/enablement.json` | Mainline top-sample enablement evidence | Top `16` mainline (`2` `panthor` + `14` `panfrost`) donors all classified as `mainline-source-present-enable-unproven`; strong source/build signal, no honest runtime-route proof yet. |
| `/data/data/com.termux/files/home/.cache/research/nothing-kernel-mt6878-20260420` | Nothing MT6878 kernel donor | Android 6.1 MT6878 reference: branch `mt6878/Tetris/u`, head `d6343dbbb3eaeaf4a2a52d6ef6a30501a09ea464`, `panfrost`/`lima`/`mediatek drm` source trees, defconfig enables `CONFIG_DRM_PANFROST=m` and `CONFIG_DRM_LIMA=m`, quick DT tree pass found no explicit `mt6878` / `tetris` / `nothing` DTS path under `arch/arm64/boot/dts/mediatek`, current class `mainline-source-present-enable-unproven`. |
| `/data/data/com.termux/files/home/.cache/research/nothing-kernel-mt6878-20260420/dt_render_probe_20260420.txt` | Nothing MT6878 DT/render probe | Generic upstream Mediatek DTS coverage plus Mali-backed examples are present, but no explicit `mediatek,mt6878` / `tetris` device-tree match surfaced. |
| `/data/data/com.termux/files/home/tools/aemali_kernel_lane_rank.py` | Owner-lane ranking tool | Builds deterministic donor ranking for `mainline-source`, `vendor-kbase diagnostics`, and `Android BSP integration` from current machine evidence plus explicit manual-ledger donors. |
| `/data/data/com.termux/files/home/aesolator/docs/assets/aemali_kernel_owner_lane_ranking_2026-04-20.json` | Owner-lane ranking asset | Current machine-readable transfer order for AeMali kernel donor work. |
| `/data/data/com.termux/files/home/aesolator/docs/AEMALI_KERNEL_OWNER_LANE_RANKING_2026-04-20.md` | Owner-lane ranking report | Human-readable ranking and rationale per owner lane. |
| `/data/data/com.termux/files/home/aesolator/docs/AEMALI_MT6833_KERNEL_RE_NOTES_2026-04-20.md` | Kernel RE note | Selected MT6833/MT6785 GED/kbase/power reading with product implications for AeMali diagnostics and route classification. |
| `/data/data/com.termux/files/home/aesolator/docs/AEMALI_HABR_ARM_BOOK_SYNTHESIS_2026-04-20.md` | Habr/Arm/book synthesis | Local corpus-backed integration of Habr GPU-driver RE, Android driver delivery, Arm public Mali constraints, and local graphics/ARM book doctrine into AeMali product requirements. |
| `/data/data/com.termux/files/home/.cache/research/aemali-books-corpus-20260420` | Targeted local PDF extraction | `3` PDFs extracted; `2` quality `ok`, `1` `small_fragment`. Used as model-level background, not as Mali UAPI/ISA authority. |
| `/data/data/com.termux/files/home/tools/github_org_repo_intake.py` | Reusable donor-intake tool | Enumerates GitHub public org/user repos, preserves raw API pages, classifies by keyword, and probes contents paths without cloning huge kernel trees. |
| `/data/data/com.termux/files/home/tools/github_git_tree_probe.py` | Reusable API-limit fallback tool | Uses shallow blobless no-checkout git clones plus `git ls-tree` to probe donor paths without GitHub REST API. |
| `/data/data/com.termux/files/home/aesolator/docs/AEMALI_UNIVERSAL_MALI_DRIVER_MASTER_ROADMAP_2026-04-20.md` | Product roadmap | Final AeMali universal Mali driver execution roadmap with donor/book/site/kernel/toolchain/device gates. |
| `/data/data/com.termux/files/home/aesolator/docs/assets/aemali_support_matrix_2026-04-20.json` | Product matrix seed | First machine-readable support/transport/conformance matrix for Lima/Panfrost/PanVK/HAL routes. |

## Current Engineering Conclusions

- Android stock Vulkan must be treated as a HAL/vendor-driver route, not as a
  generic desktop ICD route.
- Mesa PanVK/Panfrost remains a DRM/render-node based upstream driver family;
  Android source files and HAL-adjacent symbols do not remove that kernel
  ownership requirement.
- Public Arm search/vendor material, openSUSE operational docs, and Mesa docs
  align on one point: there is no honest "one custom userspace blob fixes all
  Mali" path. The support matrix is architecture- and kernel-dependent, and
  PanVK/Panfrost/Lima must remain split by real hardware family.
- The correct first live AeMali target is `Bifrost v7 / Mali-G76 MC4`, not
  Utgard and not newest Valhall-first. It has Mesa support, Android product
  evidence, and vendor-kernel archaeology while still exercising the modern
  Panfrost/PanVK stack. `Valhall v10 / Mali-G610` is the second target because
  it is the PanVK conformance reference.
- Vortek-style logic is product policy, IPC, workaround, and extension-surface
  management. It can be integrated into AeMali as explicit PanVK policy hooks,
  but blind mixing into shader compiler or kernel UAPI internals is not
  justified by current evidence.
- `libmali-rockchip` is useful as proprietary ABI/package/firmware evidence for
  RK35xx-family systems, but its EULA and binary nature make it a reference
  surface only. It is not an acceptable code donor for AeMali.
- `bakhi/mobileGPU` and `fxlin/mali` are useful RE knowledge donors for
  kbase/MMU/job-slot/timeline internals, but they are note-style evidence and
  must not override upstream Mesa/kernel truth on support claims.
- `AgentFabulous/begonia:a10-rebase` is a valuable vendor-kernel archaeology
  donor for `Mali-G76 MC4` / MT6785-era Android integration. It belongs in the
  kernel RE and BSP behavior lane, not in blind direct import into the Chapter 2
  upstream-Mesa route.
- `realme-kernel-opensource` materially expands that kernel archaeology lane:
  the saved API census confirms current MT6833 AndroidT/AndroidS kernels expose
  `drivers/misc/mediatek/gpu/gpu_mali`, and the AndroidS MT6833 candidate
  exposes `mali_bifrost`, `mali_valhall`, `ged`, and MediaTek power glue in one
  tree. This is high-value for support-matrix and diagnostics work, but still
  not a direct Mesa userspace source import.
- The full Realme kernel donor layer must not be reduced to the first `60`
  API-probed candidates. The final full git-tree reduction over all `473`
  records found `175` repos with `drivers/gpu/drm/panfrost`, `175` with
  `drivers/gpu/drm/lima`, `28` dense MediaTek kbase/GED donors, and `2`
  `panthor` source-tree donors. Kernel donor selection is therefore
  complete-org first, ranked-by-tree second.
- Kernel source tree presence is not device proof. `panfrost` / `lima` path
  hits require follow-up defconfig, Kconfig, device-tree, permission, and
  render-node evidence before they can become Android runtime route claims.
- The top dense Realme enablement sample confirms that the strongest MTK donor
  cluster is vendor-kbase/GED/BSP evidence first. It should drive AeMali
  diagnostics and Android integration policy, not a fake claim that those same
  trees already give a working Mesa DRM route.
- The top mainline Realme enablement sample confirms the complementary half:
  even when `CONFIG_DRM_PANFROST`, `CONFIG_DRM_LIMA`, or `CONFIG_DRM_PANTHOR`
  are present in defconfig/common-source donors, current evidence still stops
  at `mainline-source-present-enable-unproven` until board DT/render-node
  proof is visible.
- `NothingOSS/android_kernel_6.1_nothing_mt6878` is the current best non-Realme
  mainline-source reference in the donor set: Panfrost/Lima source trees are
  present and enabled as modules in defconfig, but DT/render-node proof is
  still absent, so it remains `mainline-source-present-enable-unproven`.
- Donor transfer order is no longer "all strongest repos first". It is now
  lane-ordered:
  `mainline-source` -> Mesa/PanVK/Panfrost source policy,
  `vendor-kbase diagnostics` -> power/DVFS/tuner/forensics,
  `Android BSP integration` -> config/display/device packaging surface.
- Selected MT6833 GED/kbase files show GPU bridge commands, DVFS query/commit
  callbacks, tuner state, MediaTek GPufreq coupling, MFG bus-idle polling, and
  platform power callbacks. AeMali should consume this as diagnostic and
  support-class evidence, not as Mesa userspace code.
- `CherishOS-Devices/android_device_realme_RM6785:tiramisu` complements that
  kernel donor from the Android product side: board config, proprietary-files
  manifests, init/sepolicy/vendor-prop routing, and SoC-family graphics
  packaging assumptions for `MT6785` / `Mali-G76 MC4`.
- The three targeted local PDFs changed the reasoning model only:
  `Computer Organization and Design ARM edition` supports ARM/memory/cache/
  pipeline background, `TDCI_Arch` supports GPU pipeline/latency/SIMD/cache
  tradeoff framing, and `Modern GPU Architecture` extracted as a small fragment.
  None of them can override Mesa/kernel/Arm source truth for Mali support.
- PanVK/Mali Mesa 26.1 requires CLC/LLVM for this lane; a build that silently
  disables LLVM is not valid.
- The current local LLVM 22.1.1 compiler exists, but the matching LLVM link
  library surface is incomplete. Falling back to Termux LLVM 21 would violate
  the declared 22.1.1 release-toolchain rule.
- Mesa source evidence shows the Panfrost/PanVK lane pulls `with_clc` through
  `with_driver_using_cl`, and CLC then requires `libclc`, LLVM, compatible
  `LLVMSPIRVLib`, `SPIRV-Tools`, and `clang-cpp` or Clang component libraries.
- Git tag evidence confirms `SPIRV-LLVM-Translator` publishes `v22.1.1`, which
  matches the mandatory LLVM `22.1.1` toolchain version for the current
  AeMali support build.
- Local package evidence confirms Termux already provides `SPIRV-Tools`
  `2026.1.1`; the missing pieces are the matching LLVM/Clang development
  surface, `LLVMSPIRVLib`, and `libclc`.
- A concrete PanVK product-truth gap existed in the downstream patchset:
  `vkEnumerateInstanceVersion()` was clamped by AeMali policy, but
  `VkPhysicalDeviceProperties.apiVersion` still exposed raw upstream
  `get_api_version()`. That split is a real compatibility/forensics defect and
  is now tracked as a downstream owner fix.
- The packaging lane also had stale truth drift: shipped asset names still
  carried hard-coded `26.1.0` strings while the actual selected source was
  derived from `RESOLVED_MESA_REF` and `COMMIT`. That is a packaging-owner
  defect, not a runtime-driver feature.
- `0002-panvk-aemali-api-truth-surface.patch` has been verified as an
  incremental patch after `0001` against `mesa-main origin/staging/26.1`
  (`patch_chain_ok=1`).

## Open Evidence Gaps

- No Mali device proof exists on the current Adreno/KGSL device.
- Arm vendor corpus body harvest is not semantically complete yet: metadata
  intake is complete, but the `1499` HTML page-candidate surface still needs a
  resumable accessible-body/link pass or explicit blocker rows for pages that
  return HTTP `403`.
- `realme-kernel-opensource` REST probing is blocked by unauthenticated API
  quota for now, but the active kernel donor path uses git tree probing and is
  no longer blocked by REST API quota.
- The current pass did not mine every GitHub repository or every global web
  page; it created a wider reproducible donor/source frontier and must be
  iterated for zero-residual claims.
- The graphics donor inventory is path/patch inventory. Candidate patches still
  require license, owner, product-contract, and build/runtime arbitration before
  import.
