package com.gridlegends.graphicsassistant.data

object ParamDefinitions {

    val allParams: List<GraphicsParam> = listOf(
        GraphicsParam(
            key = "FramerateLimit",
            displayName = "当前帧率上限",
            description = "当前配置使用的帧率上限。样本中出现 30/35/40/60/70/90/120。",
            controlType = ParamControlType.RADIO_GROUP,
            section = ParamSection.DISPLAY,
            options = mapOf(
                "30" to "30",
                "35" to "35",
                "40" to "40",
                "60" to "60",
                "70" to "70",
                "90" to "90",
                "120" to "120"
            )
        ),
        GraphicsParam(
            key = "Setup/FixedScreenHeight",
            displayName = "渲染分辨率高度",
            description = "真实配置位于 Setup/FixedScreenHeight，数值越高越清晰也越吃性能。",
            controlType = ParamControlType.SLIDER,
            section = ParamSection.DISPLAY,
            minValue = 360f,
            maxValue = 2160f,
            step = 1f,
            unitSuffix = "px"
        ),
        GraphicsParam(
            key = "Graphics/FrameSizeMinimumPercentage",
            displayName = "动态分辨率下限",
            description = "Graphics 档位里的 FrameSizeMinimumPercentage，影响动态分辨率最低比例。",
            controlType = ParamControlType.SLIDER,
            section = ParamSection.DISPLAY,
            minValue = 30f,
            maxValue = 100f,
            step = 5f,
            unitSuffix = "%"
        ),
        GraphicsParam(
            key = "Graphics/FrameSizeMaximumPercentage",
            displayName = "动态分辨率上限",
            description = "Graphics 档位里的 FrameSizeMaximumPercentage，影响动态分辨率最高比例。",
            controlType = ParamControlType.SLIDER,
            section = ParamSection.DISPLAY,
            minValue = 40f,
            maxValue = 100f,
            step = 5f,
            unitSuffix = "%"
        ),
        GraphicsParam(
            key = "DynamicResolution",
            displayName = "当前动态分辨率",
            description = "当前配置中的动态分辨率等级。0 关闭，1/3 为样本中常见启用等级。",
            controlType = ParamControlType.RADIO_GROUP,
            section = ParamSection.DISPLAY,
            options = levelOptions(includeZero = true)
        ),
        GraphicsParam(
            key = "HDR",
            displayName = "HDR",
            description = "仅在屏幕和系统支持 HDR 时启用。",
            controlType = ParamControlType.SWITCH,
            section = ParamSection.DISPLAY,
            options = switchOptions()
        ),

        GraphicsParam(
            key = "TextureQuality",
            displayName = "当前纹理质量",
            description = "当前配置中的纹理质量。样本主要使用 2/3。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.QUALITY,
            options = mapOf("1" to "低", "2" to "中", "3" to "高")
        ),
        GraphicsParam(
            key = "AntiAliasing",
            displayName = "当前抗锯齿",
            description = "当前配置中的抗锯齿等级。样本主要使用 2/3。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.QUALITY,
            options = mapOf("1" to "低", "2" to "中", "3" to "高")
        ),
        GraphicsParam(
            key = "Shadows",
            displayName = "当前阴影",
            description = "当前配置中的阴影等级。极限预设使用 3。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.QUALITY,
            options = mapOf("1" to "低", "2" to "中", "3" to "高")
        ),
        GraphicsParam(
            key = "LODQuality",
            displayName = "当前细节距离",
            description = "当前配置中的 LOD 等级。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.QUALITY,
            options = mapOf("1" to "中", "2" to "高", "3" to "极高")
        ),
        GraphicsParam(
            key = "GroundCover",
            displayName = "当前地面植被",
            description = "0 可关闭地面植被，3 为样本高画质常见值。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.QUALITY,
            options = levelOptions(includeZero = true)
        ),
        GraphicsParam(
            key = "Crowds",
            displayName = "当前观众",
            description = "观众数量。低帧率/低配预设通常为 0。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.QUALITY,
            options = mapOf("0" to "关闭", "1" to "开启")
        ),

        GraphicsParam(
            key = "PostProcess",
            displayName = "当前后处理",
            description = "当前后处理等级。极限预设使用 2。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.POST_PROCESSING,
            options = mapOf("0" to "关闭", "1" to "低", "2" to "高")
        ),
        GraphicsParam(
            key = "MotionBlur",
            displayName = "当前动态模糊",
            description = "样本中普通预设为 1，极限预设为 3。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.POST_PROCESSING,
            options = mapOf("1" to "低", "2" to "中", "3" to "高")
        ),
        GraphicsParam(
            key = "Setup/EnableSSR",
            displayName = "SSR 反射开关",
            description = "真实配置位于 Setup/EnableSSR。高画质样本使用 3。",
            controlType = ParamControlType.RADIO_GROUP,
            section = ParamSection.POST_PROCESSING,
            options = mapOf("0" to "关", "3" to "高")
        ),
        GraphicsParam(
            key = "Setup/EnableVolumetrics",
            displayName = "体积光开关",
            description = "真实配置位于 Setup/EnableVolumetrics。0 关闭，1 普通，3 极限。",
            controlType = ParamControlType.RADIO_GROUP,
            section = ParamSection.POST_PROCESSING,
            options = mapOf("0" to "关", "1" to "普通", "3" to "极限")
        ),
        GraphicsParam(
            key = "VolumetricLights",
            displayName = "当前体积光",
            description = "当前配置中的体积光等级。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.POST_PROCESSING,
            options = mapOf("1" to "低", "2" to "高")
        ),
        GraphicsParam(
            key = "FireworksAndConfetti",
            displayName = "烟花彩纸",
            description = "普通样本为 1，极限样本为 3。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.POST_PROCESSING,
            options = mapOf("1" to "普通", "3" to "极限")
        ),

        GraphicsParam(
            key = "ScreenSpaceReflections",
            displayName = "当前屏幕空间反射",
            description = "当前配置中的反射等级。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.REFLECTIONS,
            options = mapOf("1" to "普通", "2" to "高")
        ),
        GraphicsParam(
            key = "VehicleReflections",
            displayName = "当前车辆反射",
            description = "当前配置中的车辆反射等级。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.REFLECTIONS,
            options = mapOf("1" to "普通", "3" to "高")
        ),
        GraphicsParam(
            key = "SmokeShadows",
            displayName = "当前烟雾阴影",
            description = "当前配置中的烟雾阴影等级。",
            controlType = ParamControlType.LEVEL_SELECTOR,
            section = ParamSection.REFLECTIONS,
            options = mapOf("1" to "普通", "2" to "高")
        ),

        GraphicsParam(
            key = "PresetOverride",
            displayName = "覆盖游戏预设",
            description = "极限样本会开启此项，让自定义值覆盖游戏预设。",
            controlType = ParamControlType.SWITCH,
            section = ParamSection.DETAIL,
            options = switchOptions()
        ),
        GraphicsParam(
            key = "DevicePreset",
            displayName = "设备预设等级",
            description = "普通样本为 0，极限最终版为 1。",
            controlType = ParamControlType.RADIO_GROUP,
            section = ParamSection.DETAIL,
            options = mapOf("0" to "默认", "1" to "高")
        ),
        GraphicsParam(
            key = "Setup/EnableEdgeBlurOnQuarterRes",
            displayName = "低分辨率边缘模糊",
            description = "极低分辨率 120 帧样本关闭此项。",
            controlType = ParamControlType.SWITCH,
            section = ParamSection.DETAIL,
            options = switchOptions()
        )
    )

    fun getParamsBySection(section: ParamSection): List<GraphicsParam> {
        return allParams.filter { it.section == section }
    }

    fun findByKey(key: String): GraphicsParam? {
        return allParams.find { it.key == key }
    }

    private fun switchOptions() = mapOf("0" to "关", "1" to "开")

    private fun levelOptions(includeZero: Boolean): Map<String, String> {
        return if (includeZero) {
            mapOf("0" to "关", "1" to "低", "2" to "中", "3" to "高")
        } else {
            mapOf("1" to "低", "2" to "中", "3" to "高")
        }
    }
}
