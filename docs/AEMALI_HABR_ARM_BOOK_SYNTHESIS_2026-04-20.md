# AeMali Habr / Arm / Book Synthesis 2026-04-20

## Status

Это synthesis ledger, а не claim о полном прочтении всего интернета.
Материалы считаются интегрированными только если есть local artifact,
source URL, извлеченный технический тезис и product decision.

## Habr Corpus

Expanded corpus:
- path:
  `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus`
- topic queries: `11`
- search hits: `202`
- unique pages: `87`
- fetch errors: `0`

High-signal pages used in this pass:
- `https://habr.com/ru/articles/655673/`
  - local text:
    `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus/texts/articles-655673.txt`
  - product use:
    Valhall/Panfrost reverse-engineering model, XML descriptors, unit tests,
    drm-shim, device-tree bring-up, MediaTek cache/power hazards.
- `https://habr.com/ru/articles/656189/`
  - local text:
    `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus/texts/articles-656189.txt`
  - product use:
    Android GPU driver delivery model, vendor partition, signed driver APK
    mechanics, Android loader interception limits, Adreno Tools analogy, and
    why Mali route cannot be treated as simple desktop ICD replacement.
- `https://habr.com/ru/companies/playrix/articles/498564/`
  - local text:
    `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus/texts/articles-498564.txt`
  - product use:
    mobile GPU family/performance context; model-level only.
- `https://habr.com/ru/articles/334868/`
  - local text:
    `/data/data/com.termux/files/home/.cache/research/habr-aemali-expanded-20260420/corpus/texts/articles-334868.txt`
  - product use:
    shader compilation latency and hybrid shader strategy as runtime UX
    context for emulator workloads; not Mali driver source truth.
- Habr Mesa release pages:
  - `https://habr.com/ru/news/884120/`
  - `https://habr.com/ru/news/907948/`
  - `https://habr.com/ru/news/935032/`
  - `https://habr.com/ru/news/995766/`
  - product use:
    news-level release tracking only; official Mesa docs/release notes remain
    authoritative.

## Arm Corpus

Search artifact:
- `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420`
- raw hits: `3879`
- unique results: `3262`
- raw search pages: `81`
- HTML candidates: `1499`

Body blocker:
- `/data/data/com.termux/files/home/.cache/research/arm-mali-intake-20260420/arm-body-fetch-blocker-20260420.json`
- direct HTTP body fetch blocked by `403` on sampled pages.

Accessible facts used:
- Arm public Bifrost kernel driver page states that open kernel components are
  only part of the stack and version-compatible Mali DDK userspace is needed
  for proprietary OpenGL ES stack.
- Arm public user-space binary page lists proprietary binary API families and
  EULA constraints. It is reference evidence, not source import permission.

Product decisions:
- AeMali cannot ship a fake source-derived proprietary Arm stack.
- Proprietary `libmali` packages are ABI/package reference only.
- Open source route must stay Mesa Panfrost/PanVK/Lima plus real kernel route.
- Arm 403 rows stay as blockers, not silently ignored pages.

## Local Books

Existing semantic frontier:
- `/data/data/com.termux/files/home/.codex/skills/local-corpus-knowledge-promotion/references/book-corpus-131-ledger-2026-04-17.md`
- processed entries: `131/131`
- bytes read: `85123014`

Targeted extraction:
- `/data/data/com.termux/files/home/.cache/research/aemali-books-corpus-20260420`
- `Computer Organization and Design ARM edition`: `2535003` chars
- `TDCI_Arch`: `21519` chars
- `Modern GPU Architecture - Ismayil Tahmazov`: `3197` chars, weak fragment

Book-derived decisions:
- Treat Mali as tile-based/mobile GPU system where memory bandwidth, cache
  coherency, tiling, shader compilation, and synchronization dominate product
  behavior.
- Separate ARM CPU memory/cache model from GPU kernel UAPI. The books support
  reasoning, not support claims.
- Do not flatten OpenGL implicit state, Vulkan explicit resource/sync model,
  and kernel command submission into one wrapper abstraction.

## Integrated Technical Thesis

AeMali must be built as four separable planes:
- Mesa user-space plane:
  Lima, Panfrost, PanVK, NIR, compiler backend, descriptor packing, command
  stream, drm-shim and conformance testing.
- Kernel/runtime plane:
  render node, panfrost/panthor/lima UAPI, MediaTek kbase/GED diagnostics,
  power/DVFS/cache coherency hazards.
- Android delivery plane:
  system Vulkan loader, `hwvulkan` HAL, signed driver APK mechanics,
  package import constraints, Ae.solator runtime route selection.
- Product wrapper plane:
  Vortek/Gladio policy, extension profile, route choice, forensic logging,
  user-visible configuration and failure reasons.

Universal driver means universal product lane and support matrix, not one
single compiler or one kernel path for all Mali generations.

## Immediate Product Requirements

- Support matrix must expose generation, model, Mesa route, API ceilings,
  conformance status, kernel/runtime transport, donor evidence, and blocker.
- Runtime diagnostics must report:
  `gpu_model`, `render_node_present`, `panfrost_kernel_surface`,
  `panthor_kernel_surface`, `vendor_kbase_surface`, `ged_dvfs_surface`,
  `hwvulkan_surface`, `mesa_driver_present`, and `transport_block_reason`.
- Device proof must include both success and blocked route states. A HAL-only
  Android Vulkan path is not PanVK proof.
- Kernel donor evidence must be ranked by actual tree shape, not repository
  name or first API probe.

## Execution Binding

This synthesis is bound into:
- `/data/data/com.termux/files/home/aesolator/docs/AEMALI_UNIVERSAL_MALI_DRIVER_MASTER_ROADMAP_2026-04-20.md`
- `/data/data/com.termux/files/home/aesolator/docs/AEMALI_DONOR_BOOK_KERNEL_EXECUTION_ROADMAP_2026-04-20.md`
- `/data/data/com.termux/files/home/aesolator/docs/assets/aemali_support_matrix_2026-04-20.json`
- `/data/data/com.termux/files/home/aesolator/docs/AEMALI_MT6833_KERNEL_RE_NOTES_2026-04-20.md`
