#!/usr/bin/env python3
import argparse
import json
import sys
from pathlib import Path


AESOLATOR_ROOT = Path(__file__).resolve().parents[1]
SUPPORT_MATRIX_PATH = AESOLATOR_ROOT / "docs" / "assets" / "aemali_support_matrix_2026-04-20.json"
GRAPHICS_RESEARCH_LEDGER_PATH = AESOLATOR_ROOT / "docs" / "GRAPHICS_GLOBAL_RESEARCH_LEDGER_2026-04-19.md"
BOOK_SYNTHESIS_PATH = AESOLATOR_ROOT / "docs" / "AEMALI_HABR_ARM_BOOK_SYNTHESIS_2026-04-20.md"
WRAPPER_CONTRACT_PATH = AESOLATOR_ROOT / "docs" / "GRAPHICS_WRAPPER_CONTRACT_2026-04-19.md"


def load_support_matrix() -> dict:
    with SUPPORT_MATRIX_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def find_route(matrix: dict, route_id: str) -> dict:
    for route in matrix.get("routes", []):
        if route.get("routeId") == route_id:
            return route
    raise SystemExit(f"Unknown routeId: {route_id}")


def find_default_panvk_route(matrix: dict) -> dict:
    for route in matrix.get("routes", []):
        if route.get("defaultAemaliTarget") and route.get("panvk"):
            return route
    raise SystemExit("Unable to resolve default AeMali PanVK route from support matrix")


def first_non_empty(*values: str) -> str:
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def unique_list(values) -> list:
    out = []
    for value in values or []:
        if value is None:
            continue
        text = str(value).strip()
        if text and text not in out:
            out.append(text)
    return out


def primary_lane_and_donors(route: dict) -> tuple[str, list]:
    priorities = route.get("ownerLanePriority") or []
    ranked = route.get("rankedKernelDonors") or {}
    if isinstance(ranked, dict):
        for lane in priorities:
            donors = unique_list(ranked.get(lane))
            if donors:
                return str(lane).strip(), donors
        for lane, values in ranked.items():
            donors = unique_list(values)
            if donors:
                return str(lane).strip(), donors
    return first_non_empty(*(str(v) for v in priorities)), []


def shared_product_sources(matrix: dict) -> dict:
    sources = matrix.get("sources", {})
    return {
        "supportMatrixAsset": str(SUPPORT_MATRIX_PATH),
        "ownerLaneRankingSource": sources.get("ownerLaneRanking", ""),
        "researchLedgerSource": str(GRAPHICS_RESEARCH_LEDGER_PATH),
        "bookSynthesisSource": str(BOOK_SYNTHESIS_PATH),
        "productContractSource": str(WRAPPER_CONTRACT_PATH),
        "localBookCorpusSource": sources.get("localBookCorpus", ""),
        "targetedBookCorpusSource": sources.get("targetedAemaliBooks", ""),
    }


def all_panvk_models(matrix: dict) -> tuple[list, list]:
    supported = []
    conformant = []
    for route in matrix.get("routes", []):
        if not route.get("panvk"):
            continue
        supported.extend(unique_list(route.get("models")))
        if route.get("panvkConformantModels"):
            conformant.extend(unique_list(route.get("panvkConformantModels")))
        elif route.get("panvkConformant"):
            conformant.extend(unique_list(route.get("models")))
    supported = unique_list(supported)
    conformant = unique_list(conformant)
    non_conformant = [model for model in supported if model not in conformant]
    return supported, conformant, non_conformant


def find_default_gallium_route(matrix: dict) -> dict:
    for route in matrix.get("routes", []):
        if route.get("defaultAemaliTarget") and route.get("opengl"):
            return route
    raise SystemExit("Unable to resolve default AeMali Gallium route from support matrix")


def all_gallium_models(matrix: dict) -> tuple[list, list]:
    supported = []
    route_ids = []
    for route in matrix.get("routes", []):
        if not route.get("opengl") or route.get("routeId") == "android-hal-vulkan-mali":
            continue
        supported.extend(unique_list(route.get("models")))
        route_ids.append(route.get("routeId", ""))
    return unique_list(supported), unique_list(route_ids)


def prune(value):
    if isinstance(value, dict):
        out = {}
        for key, item in value.items():
            pruned = prune(item)
            if pruned is None:
                continue
            if isinstance(pruned, str) and not pruned:
                continue
            if isinstance(pruned, (list, dict)) and not pruned:
                continue
            out[key] = pruned
        return out
    if isinstance(value, list):
        out = []
        for item in value:
            pruned = prune(item)
            if pruned is None:
                continue
            if isinstance(pruned, str) and not pruned:
                continue
            if isinstance(pruned, (list, dict)) and not pruned:
                continue
            out.append(pruned)
        return out
    return value


def build_aemali_panvk(args, matrix: dict) -> dict:
    primary_route = find_route(matrix, args.route_id) if args.route_id else find_default_panvk_route(matrix)
    owner_lane, ranked_donors = primary_lane_and_donors(primary_route)
    supported_models, conformant_models, non_conformant_models = all_panvk_models(matrix)
    supported_routes = unique_list(
        [route.get("routeId", "") for route in matrix.get("routes", []) if route.get("panvk")]
    )

    notes = first_non_empty(primary_route.get("notes", ""))
    notes_suffix = (
        "AeMali remains a Mesa render-node route and does not claim Android stock-HAL identity. "
        "Primary route metadata is anchored to the first live target while additional PanVK routes are "
        "published as upper-bound support evidence."
    )

    return {
        **shared_product_sources(matrix),
        "name": first_non_empty(args.name, "AeMali PanVK"),
        "version": args.version,
        "driverVersion": args.version,
        "providerLane": "aemali-panvk",
        "sourceRepo": first_non_empty(args.source_repo, "https://gitlab.freedesktop.org/mesa/mesa"),
        "sourceRef": args.source_ref,
        "sourceCommit": args.source_commit,
        "sourceCommitDate": args.source_commit_date,
        "artifactName": args.artifact_name,
        "libraryName": first_non_empty(args.library_name, "usr/lib/libvulkan_panfrost.so"),
        "rootLibraryPath": first_non_empty(args.root_library_path, "usr/lib/libvulkan_panfrost.so"),
        "driverKind": "aemali-panvk",
        "transport": "drm-render-node-experimental",
        "androidRoute": "experimental-userspace-icd-overlay",
        "policyEnv": "AEMALI_DRIVER=1",
        "kernelContract": "panthor-or-panfrost-render-node",
        "supportMatrixSource": matrix.get("sources", {}).get("mesaPanfrost", "https://docs.mesa3d.org/drivers/panfrost.html"),
        "routeId": primary_route.get("routeId"),
        "ownerLane": owner_lane,
        "ownerLanePriority": unique_list(primary_route.get("ownerLanePriority")),
        "supportClass": primary_route.get("supportClass"),
        "vulkanApiCeiling": first_non_empty(primary_route.get("vulkan"), "1.0"),
        "kernelEvidenceClass": unique_list(primary_route.get("kernelEvidenceClass")),
        "transportRequirements": unique_list(primary_route.get("transportRequirements")),
        "rankedKernelDonors": ranked_donors,
        "diagnosticKeys": unique_list(primary_route.get("diagnosticKeys")),
        "supportedArchitectures": ["Midgard", "Bifrost", "Valhall", "5th Gen"],
        "supportedRouteIds": supported_routes,
        "supportedVulkanModels": supported_models,
        "conformantModels": conformant_models,
        "nonConformantButSupportedModels": non_conformant_models,
        "unsupportedFamilies": ["Utgard"],
        "fallbackRoute": "lima-or-software",
        "requiresRenderNode": True,
        "experimental": True,
        "notes": first_non_empty(" ".join(part for part in [notes, notes_suffix] if part)),
    }


def build_aemali_gallium(args, matrix: dict) -> dict:
    primary_route = find_route(matrix, args.route_id) if args.route_id else find_default_gallium_route(matrix)
    owner_lane, ranked_donors = primary_lane_and_donors(primary_route)
    supported_models, supported_routes = all_gallium_models(matrix)
    notes = first_non_empty(primary_route.get("notes", ""))
    notes_suffix = (
        "AeMali Gallium stays the OpenGL/GLES half of the Mesa user-space plane. "
        "It is not Android stock-HAL truth and it must remain separate from wrapper-only Gladio/Vortek ownership."
    )

    return {
        **shared_product_sources(matrix),
        "name": first_non_empty(args.name, "AeMali Gallium"),
        "version": args.version,
        "driverVersion": args.version,
        "providerLane": "aemali-gallium",
        "sourceRepo": first_non_empty(args.source_repo, "https://gitlab.freedesktop.org/mesa/mesa"),
        "sourceRef": args.source_ref,
        "sourceCommit": args.source_commit,
        "sourceCommitDate": args.source_commit_date,
        "artifactName": args.artifact_name,
        "libraryName": first_non_empty(args.library_name, "usr/lib/libEGL.so"),
        "rootLibraryPath": first_non_empty(args.root_library_path, "usr/lib/libEGL.so"),
        "driverKind": "aemali-gallium",
        "transport": "drm-render-node-experimental",
        "androidRoute": "experimental-userspace-gl-overlay",
        "policyEnv": "AEMALI_OPENGL=1",
        "kernelContract": "panfrost-or-lima-render-node",
        "supportMatrixSource": matrix.get("sources", {}).get("mesaPanfrost", "https://docs.mesa3d.org/drivers/panfrost.html"),
        "routeId": primary_route.get("routeId"),
        "ownerLane": owner_lane,
        "ownerLanePriority": unique_list(primary_route.get("ownerLanePriority")),
        "supportClass": primary_route.get("supportClass"),
        "kernelEvidenceClass": unique_list(primary_route.get("kernelEvidenceClass")),
        "transportRequirements": [
            "drm-render-node-or-explicit-blocker",
            "mesa-panfrost-or-lima-gallium",
            "opengl-es-route",
            "no-hal-claim-without-hwvulkan-proof",
        ],
        "rankedKernelDonors": ranked_donors,
        "diagnosticKeys": unique_list(primary_route.get("diagnosticKeys")),
        "preferredGalliumDriver": "panfrost",
        "supportedGalliumDrivers": ["panfrost", "lima", "zink", "softpipe"],
        "supportedArchitectures": ["Utgard", "Midgard", "Bifrost", "Valhall", "5th Gen"],
        "supportedRouteIds": supported_routes,
        "supportedOpenGlModels": supported_models,
        "fallbackGalliumDriver": "softpipe",
        "requiresRenderNode": True,
        "experimental": True,
        "graphicsStackProfile": "aemali-universal",
        "notes": first_non_empty(" ".join(part for part in [notes, notes_suffix] if part)),
    }


def build_virgl_mesa_bridge(args, matrix: dict) -> dict:
    return {
        **shared_product_sources(matrix),
        "name": first_non_empty(args.name, "VirGL Mesa Bridge"),
        "version": args.version,
        "driverVersion": args.version,
        "providerLane": "virgl-universal",
        "sourceRepo": first_non_empty(args.source_repo, "https://gitlab.freedesktop.org/mesa/mesa"),
        "sourceRef": args.source_ref,
        "sourceCommit": args.source_commit,
        "sourceCommitDate": args.source_commit_date,
        "artifactName": args.artifact_name,
        "libraryName": first_non_empty(args.library_name, "usr/lib/libGL.so"),
        "rootLibraryPath": first_non_empty(args.root_library_path, "usr/lib/libGL.so"),
        "driverKind": "virgl-mesa-bridge",
        "transport": "userspace-virtio-gpu",
        "routeId": "virgl-universal-virtual-gpu",
        "supportClass": "separate-transport",
        "transportRequirements": [
            "mesa-virpipe-gallium",
            "companion-virglrenderer-host",
            "virtual-gpu-host-surface",
        ],
        "diagnosticKeys": [
            "AERO_VIRGL_GALLIUM_DRIVER",
            "VIRGL_NO_READBACK",
            "AERO_VIRGL_ROUTE_DEGRADED_REASON",
        ],
        "graphicsStackProfile": "universal-virgl",
        "preferredGalliumDriver": "virpipe",
        "archiveFormat": "tzst",
        "archiveLayout": "root-overlay",
        "installSurface": "graphics-driver",
        "ownerLane": "separate-transport",
        "companionTransportSourceRepo": "https://gitlab.freedesktop.org/virgl/virglrenderer",
        "supportMatrixSource": matrix.get("sources", {}).get("mesaPanfrost", "https://docs.mesa3d.org/drivers/panfrost.html"),
        "notes": (
            "Mesa guest OpenGL bridge for the universal VirGL route. "
            "Companion host virglrenderer remains a separate transport owner and must not be flattened "
            "into the guest package identity."
        ),
    }


def build_gladio_wrapper(args, _matrix: dict) -> dict:
    return {
        **shared_product_sources(_matrix),
        "name": first_non_empty(args.name, "Gladio"),
        "version": args.version,
        "driverVersion": args.version,
        "providerLane": "gladio-opengl",
        "sourceRepo": first_non_empty(args.source_repo, "https://github.com/Pipetto-crypto/gladiorenderer"),
        "artifactName": args.artifact_name,
        "libraryName": first_non_empty(args.library_name, "usr/lib/libGL.so.1.7.0"),
        "rootLibraryPath": first_non_empty(args.root_library_path, "usr/lib/libGL.so.1.7.0"),
        "driverKind": "opengl-wrapper",
        "transport": "bundled-root-overlay",
        "routeId": "mediatek-gladio-opengl",
        "supportClass": "wrapper-transport",
        "transportRequirements": [
            "gladiorenderer-root-overlay",
            "wined3d-opengl-route",
            "no-direct-mesa-identity",
        ],
        "diagnosticKeys": [
            "AERO_OPENGL_PACKAGE_ENTRY",
            "GLADIO_NO_ERROR",
            "AERO_GRAPHICS_ROUTE_DEGRADED_REASON",
        ],
        "graphicsStackProfile": "vortek-gladio",
        "ownerLane": "product-wrapper-plane",
        "companionProviderLane": "vortek-wrapper-vulkan",
        "archiveFormat": "tzst",
        "archiveLayout": "root-overlay",
        "installSurface": "graphics-driver",
        "notes": (
            "OpenGL/GLES half of the Vortek + Gladio MediaTek wrapper family. "
            "This package is product-owned wrapper logic and must not be presented as a Mesa Panfrost/PanVK route."
        ),
    }


def build_vortek_wrapper(args, _matrix: dict) -> dict:
    return {
        **shared_product_sources(_matrix),
        "name": first_non_empty(args.name, "Vortek"),
        "version": args.version,
        "driverVersion": args.version,
        "providerLane": "vortek-wrapper-vulkan",
        "sourceRepo": first_non_empty(args.source_repo, "https://github.com/brunodev85/vortek"),
        "artifactName": args.artifact_name,
        "libraryName": first_non_empty(args.library_name, "usr/lib/libvulkan_vortek.so"),
        "rootLibraryPath": first_non_empty(args.root_library_path, "usr/lib/libvulkan_vortek.so"),
        "driverKind": "vortek-wrapper",
        "transport": "bundled-root-overlay",
        "routeId": "mediatek-vortek-wrapper",
        "supportClass": "wrapper-transport",
        "transportRequirements": [
            "vortek-wrapper-icd",
            "delegated-vulkan-source",
            "no-direct-panvk-identity",
        ],
        "diagnosticKeys": [
            "AERO_VORTEK_VULKAN_SOURCE",
            "AERO_VORTEK_VULKAN_DRIVER_ENTRY",
            "AERO_GRAPHICS_ROUTE_DEGRADED_REASON",
        ],
        "graphicsStackProfile": "vortek-gladio",
        "ownerLane": "product-wrapper-plane",
        "companionProviderLane": "gladio-opengl",
        "archiveFormat": "tzst",
        "archiveLayout": "root-overlay",
        "installSurface": "graphics-driver",
        "loaderIcdInterfaceVersion": 5,
        "apiVersionManifestCeiling": "1.4.349",
        "neededSurface": ["liblog.so", "libdl.so", "libc.so"],
        "notes": (
            "Bundled wrapper ICD and bridge library for the MediaTek Vulkan lane. "
            "Actual Vulkan source ownership remains with Android system HAL or an explicitly imported custom Vulkan package. "
            "The client ICD exports loader interface 5 so Vulkan 1.1+ manifest versions do not violate LDP_DRIVER_7. "
            "Manifest API ceiling tracks the official Vulkan-Headers 1.4.349 line and must still be clamped at runtime to the real host/physical-device surface."
        ),
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Emit canonical graphics-driver package metadata.")
    parser.add_argument(
        "profile",
        choices=["aemali-panvk", "aemali-gallium", "virgl-mesa-bridge", "gladio-wrapper", "vortek-wrapper"],
    )
    parser.add_argument("--version", required=True)
    parser.add_argument("--name", default="")
    parser.add_argument("--route-id", default="")
    parser.add_argument("--source-repo", default="")
    parser.add_argument("--source-ref", default="")
    parser.add_argument("--source-commit", default="")
    parser.add_argument("--source-commit-date", default="")
    parser.add_argument("--artifact-name", default="")
    parser.add_argument("--library-name", default="")
    parser.add_argument("--root-library-path", default="")
    parser.add_argument("--output", default="-")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    matrix = load_support_matrix()

    builders = {
        "aemali-panvk": build_aemali_panvk,
        "aemali-gallium": build_aemali_gallium,
        "virgl-mesa-bridge": build_virgl_mesa_bridge,
        "gladio-wrapper": build_gladio_wrapper,
        "vortek-wrapper": build_vortek_wrapper,
    }
    metadata = prune(builders[args.profile](args, matrix))
    output = json.dumps(metadata, indent=2, ensure_ascii=False) + "\n"

    if args.output == "-" or not args.output:
        sys.stdout.write(output)
    else:
        Path(args.output).write_text(output, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
