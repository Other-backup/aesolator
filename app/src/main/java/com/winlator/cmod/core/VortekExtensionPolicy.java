package com.winlator.cmod.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class VortekExtensionPolicy {
    public static final String PROFILE_MALI_SYSTEM = "mali-system";
    public static final String PROFILE_MALI_COMPAT = "mali-compat";
    public static final String PROFILE_MAXIMUM = "maximum";
    public static final String PROFILE_CUSTOM = "custom";

    private static final String[] VORTEK_EXPOSED_EXTENSIONS = {
            "VK_KHR_surface",
            "VK_KHR_swapchain",
            "VK_KHR_get_physical_device_properties2",
            "VK_EXT_transform_feedback",
            "VK_EXT_conditional_rendering",
            "VK_EXT_vertex_attribute_divisor",
            "VK_EXT_index_type_uint8",
            "VK_EXT_robustness2",
            "VK_EXT_extended_dynamic_state",
            "VK_EXT_host_query_reset",
            "VK_KHR_create_renderpass2",
            "VK_KHR_depth_stencil_resolve",
            "VK_KHR_draw_indirect_count",
            "VK_KHR_timeline_semaphore",
            "VK_KHR_dedicated_allocation",
            "VK_KHR_get_memory_requirements2",
            "VK_KHR_descriptor_update_template",
            "VK_KHR_imageless_framebuffer",
            "VK_KHR_driver_properties",
            "VK_KHR_image_format_list",
            "VK_EXT_shader_demote_to_helper_invocation",
            "VK_KHR_shader_float_controls",
            "VK_EXT_4444_formats",
            "VK_EXT_conservative_rasterization",
            "VK_EXT_custom_border_color",
            "VK_EXT_depth_clip_enable",
            "VK_EXT_sample_locations",
            "VK_KHR_sampler_ycbcr_conversion",
            "VK_EXT_provoking_vertex",
            "VK_KHR_maintenance1",
            "VK_KHR_maintenance2",
            "VK_KHR_maintenance3",
            "VK_EXT_line_rasterization",
            "VK_EXT_border_color_swizzle",
            "VK_KHR_external_memory",
            "VK_KHR_external_memory_fd",
            "VK_KHR_external_fence",
            "VK_KHR_external_fence_fd",
            "VK_KHR_external_semaphore",
            "VK_KHR_external_semaphore_fd",
            "VK_KHR_vulkan_memory_model",
            "VK_KHR_synchronization2",
            "VK_EXT_depth_clip_control",
            "VK_KHR_dynamic_rendering",
            "VK_KHR_shader_float16_int8",
            "VK_KHR_push_descriptor",
            "VK_EXT_shader_stencil_export",
            "VK_EXT_shader_viewport_index_layer",
            "VK_KHR_sampler_mirror_clamp_to_edge",
            "VK_KHR_shader_draw_parameters",
            "VK_EXT_scalar_block_layout",
            "VK_EXT_color_write_enable",
            "VK_EXT_extended_dynamic_state3",
            "VK_EXT_shader_module_identifier",
            "VK_KHR_portability_subset"
    };

    private static final String[] VORTEK_IMPLEMENTED_EXTENSIONS = {
            "VK_KHR_swapchain",
            "VK_KHR_descriptor_update_template",
            "VK_EXT_private_data",
            "VK_EXT_memory_budget",
            "VK_EXT_map_memory_placed",
            "VK_KHR_map_memory2"
    };

    private static final String[] ANDROID_MALI_SYSTEM_EXTENSIONS = {
            "VK_ANDROID_external_memory_android_hardware_buffer",
            "VK_EXT_queue_family_foreign",
            "VK_KHR_external_memory_capabilities",
            "VK_KHR_external_semaphore_capabilities",
            "VK_KHR_external_fence_capabilities",
            "VK_EXT_global_priority",
            "VK_EXT_pipeline_creation_cache_control",
            "VK_EXT_tooling_info"
    };

    private static final String[] MALI_COMPAT_DISABLED_EXTENSIONS = {
            "VK_EXT_hdr_metadata",
            "VK_EXT_swapchain_maintenance1",
            "VK_KHR_shader_float_controls",
            "VK_EXT_extended_dynamic_state",
            "VK_EXT_extended_dynamic_state3",
            "VK_EXT_color_write_enable",
            "VK_KHR_push_descriptor"
    };

    private VortekExtensionPolicy() {
    }

    public static String normalizeProfile(String rawProfile) {
        String normalized = StringUtils.parseIdentifier(rawProfile == null ? "" : rawProfile);
        if ("mali-system-vulkan".equals(normalized) || "system-mali".equals(normalized)) return PROFILE_MALI_SYSTEM;
        if ("mali-compatibility".equals(normalized) || "mediatek-compat".equals(normalized)) return PROFILE_MALI_COMPAT;
        if ("max".equals(normalized) || "all".equals(normalized)) return PROFILE_MAXIMUM;
        if (PROFILE_MALI_SYSTEM.equals(normalized)
                || PROFILE_MALI_COMPAT.equals(normalized)
                || PROFILE_MAXIMUM.equals(normalized)
                || PROFILE_CUSTOM.equals(normalized)) {
            return normalized;
        }
        return PROFILE_MALI_SYSTEM;
    }

    public static String[] buildCandidateExtensions(String[] driverExtensions) {
        LinkedHashSet<String> extensions = new LinkedHashSet<>();
        addAll(extensions, VORTEK_EXPOSED_EXTENSIONS);
        addAll(extensions, VORTEK_IMPLEMENTED_EXTENSIONS);
        addAll(extensions, ANDROID_MALI_SYSTEM_EXTENSIONS);
        addAll(extensions, driverExtensions);
        return extensions.toArray(new String[0]);
    }

    public static String[] getSelectedExtensionsForProfile(String rawProfile, String[] candidateExtensions) {
        String profile = normalizeProfile(rawProfile);
        String[] candidates = candidateExtensions == null ? new String[0] : candidateExtensions;
        if (PROFILE_CUSTOM.equals(profile)) return new String[0];

        ArrayList<String> selected = new ArrayList<>();
        String[] disabled = getDisabledExtensionsForProfile(profile);
        for (String extension : candidates) {
            if (extension == null || extension.trim().isEmpty()) continue;
            if (contains(disabled, extension)) continue;
            selected.add(extension);
        }
        return selected.toArray(new String[0]);
    }

    public static String[] getDisabledExtensionsForProfile(String rawProfile) {
        String profile = normalizeProfile(rawProfile);
        if (PROFILE_MALI_COMPAT.equals(profile)) return MALI_COMPAT_DISABLED_EXTENSIONS.clone();
        return new String[0];
    }

    public static boolean isAllSelected(String[] selectedExtensions, String[] candidateExtensions) {
        if (selectedExtensions == null || candidateExtensions == null) return false;
        return selectedExtensions.length == candidateExtensions.length;
    }

    public static String joinExtensions(String[] extensions) {
        if (extensions == null || extensions.length == 0) return "";
        StringBuilder builder = new StringBuilder();
        for (String extension : extensions) {
            if (extension == null || extension.trim().isEmpty()) continue;
            if (builder.length() > 0) builder.append('|');
            builder.append(extension.trim());
        }
        return builder.toString();
    }

    public static String describeProfile(String rawProfile) {
        switch (normalizeProfile(rawProfile)) {
            case PROFILE_MALI_COMPAT:
                return "Mali compatibility: Vortek IPC over Android system Vulkan with conservative extension disables for fragile Mali ICD/DXVK paths.";
            case PROFILE_MAXIMUM:
                return "Maximum: exposes the full Vortek + Android + live ICD extension surface and keeps only native engine safety filters.";
            case PROFILE_CUSTOM:
                return "Custom: uses the exact extension set selected below.";
            case PROFILE_MALI_SYSTEM:
            default:
                return "Mali system Vulkan: routes Vortek to the Android system Vulkan loader and exposes the broad Vortek Mali surface.";
        }
    }

    private static void addAll(LinkedHashSet<String> out, String[] values) {
        if (values == null) return;
        for (String value : values) {
            if (value == null) continue;
            String trimmed = value.trim();
            if (trimmed.isEmpty()) continue;
            out.add(trimmed);
        }
    }

    private static boolean contains(String[] values, String needle) {
        if (values == null || needle == null) return false;
        for (String value : values) {
            if (needle.equals(value)) return true;
        }
        return false;
    }

    public static boolean isMaliProfile(String rawProfile) {
        String profile = normalizeProfile(rawProfile);
        return PROFILE_MALI_SYSTEM.equals(profile) || PROFILE_MALI_COMPAT.equals(profile);
    }
}
