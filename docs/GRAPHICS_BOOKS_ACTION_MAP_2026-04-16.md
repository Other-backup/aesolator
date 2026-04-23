# Graphics Books Action Map 2026-04-16

This map turns the raw book dumps into a product-facing reading order for the
Chapter 2 graphics frontier.

## Sources

- repo:
  `/data/data/com.termux/files/home/.cache/research/graphics-books-20260416/repos/graphics_books`
- repo:
  `/data/data/com.termux/files/home/.cache/research/graphics-books-20260416/repos/MyData`
- local book corpus:
  `/data/data/com.termux/files/home/.cache/research/graphics-books-20260416/corpus`

## Classification

`graphics_books` is a focused graphics/programming dump.  
`MyData` is a mixed archive; only the `3d/` subtree is currently relevant to
this frontier. Its visible high-value files include `OpenGL Insights.pdf`,
`Real-Time Rendering, Third Edition.pdf`, `WebGL Insights.pdf`, and `GPU Gems`
/ `GPU Pro` directories.

### Tier 1: direct driver and API ownership

Use first for `Vortek`, `Turnip`, `Zink`, Vulkan loader/ICD, GL/Vulkan route
translation, and shader/runtime debugging.

- `vkspec.pdf`
- `vkspec1.2.pdf`
- `Vulkan Programming Guide - Graham Sellers.pdf`
- `vulkan10-reference-guide.pdf`
- `vulkan11-reference-guide.pdf`
- `learnopengl_book.pdf`
- `learnopengl_printablebook.pdf`
- `Modern OpenGL Guide.pdf`
- `OpenGL Superbible_ Comprehensive Tutorial and Reference.pdf`
- `OpenGL_SuperBible__Comprehensive_Tutorial_and_Reference__5th_Edition.pdf`
- `GLSL_ES_Specification_3.00.pdf`
- `GLSLangSpec.3.30.pdf`
- `glspec33.core.pdf`

### Tier 2: renderer architecture and performance model

Use for render-path design, resource lifetime, pipeline reasoning, and GPU
cost models around `VirGL`, `Gladio`, `Vortek`, and host/device sync.

- `Real_Time_Rendering_4th_Edition.pdf`
- `Real-Time Rendering, Third Edition.pdf`
- `Game_Engine_Architecture-en.pdf`
- `Computer Graphics.pdf`
- `ComputerGraphics.pdf`
- `Shaders for Game Programming and Artists.pdf`
- `David Wolff - OpenGL 4.0 Shading Language Cookbook .pdf`

### Tier 3: mathematical and systems support

Use when the defect reaches coordinate transforms, matrix math, shader algebra,
or low-level C/C++ implementation quality.

- `3D Math Primer for Graphics and Game Development (2nd Ed)(gnv64).pdf`
- `Foundations of Game Engine Development, Volume 1 Mathematics ( PDFDrive.com ).pdf`
- `matrixcookbook.pdf`
- `linearAlgebra.pdf`
- `Craig Scott - Professional CMake_ A Practical Guide.pdf`
- `Scott Meyers - Effective Modern C++.pdf`

### Local deck corpus

`/storage/emulated/0/Download/FULL.1pp (1).pdf` is a 452-page Mike Bailey deck
corpus. Treat it as a structured lecture/reference aid for OpenGL and graphics
pipeline intuition, not as the canonical spec.

## Product mapping

- `Vortek` / `Turnip` / `Zink`:
  start with Vulkan spec + Vulkan Programming Guide + Mesa docs
- `VirGL` / `Gladio` / OpenGL route:
  start with OpenGL spec + GLSL specs + LearnOpenGL/SuperBible + Mesa virgl docs
- `X11` / `GLX` / window-system interaction:
  use the X11 local book corpus first, then GLX/X11 extension code in-tree
- shader/compiler frontier:
  Mesa NIR docs/source stay authoritative; books are only supporting context

## Hard rule

Do not treat this corpus as decorative reading. When a repeated pattern becomes
useful, distill it into:

- code contracts
- tests
- docs
- `.codex/skills/chapter2-graphics-stack-closure`
