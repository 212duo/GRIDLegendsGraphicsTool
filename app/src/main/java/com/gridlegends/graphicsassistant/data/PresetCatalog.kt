package com.gridlegends.graphicsassistant.data

object PresetCatalog {
    val all: List<PresetMetadata> = listOf(
        preset("low_very_low", "极低专属", PresetGroup.LOW, "最低画质负载，适合性能有限的设备。"),
        preset("low_very_low_120", "极低120帧", PresetGroup.LOW, "极低画质与高帧率组合。"),
        preset("low_ultra_low", "超低配专属", PresetGroup.LOW, "优先降低发热和画面负载。"),
        preset("low_ultra_low_120", "超低配120帧", PresetGroup.LOW, "超低画质并提高目标帧率。"),
        preset("low_dedicated", "低配专属", PresetGroup.LOW, "适合低配设备的常规方案。"),
        preset("low_720p_60_no_crowds", "低720p+60帧（无观众）", PresetGroup.LOW, "720p、60 帧并关闭观众。"),
        preset("low_power_balanced", "平衡优化版（低功耗）", PresetGroup.LOW, "兼顾可玩性、功耗和温度。"),
        preset("mid_dedicated", "中配专属", PresetGroup.MID, "适合中端设备的基础方案。"),
        preset("mid_extreme_balanced", "极致平衡（中配专属）", PresetGroup.MID, "在中端设备上平衡清晰度和流畅度。"),
        preset("mid_balanced", "平衡画质", PresetGroup.MID, "画质与性能相对均衡。"),
        preset("high_dedicated", "高配专属", PresetGroup.HIGH, "适合高性能设备的常规高画质。"),
        preset("high_optimized_70", "极致优化（70帧）", PresetGroup.HIGH, "以 70 帧为目标的高配优化。"),
        preset("high_optimized_90", "极致优化（90帧）", PresetGroup.HIGH, "以 90 帧为目标的高配优化。"),
        preset("high_optimized_balanced", "极致优化平衡（高配）", PresetGroup.HIGH, "高配设备的综合平衡方案。"),
        preset("high_1080p_120", "高配专属1080p120帧", PresetGroup.HIGH, "1080p 与 120 帧的高负载组合。"),
        preset("extreme_2k_30", "超极限2k+30帧", PresetGroup.EXTREME, "以高分辨率换取较低目标帧率。"),
        preset("extreme_2k_120", "超极限画质2K+120帧", PresetGroup.EXTREME, "高分辨率与 120 帧的极高负载方案。"),
        preset("extreme_final", "超越极限（最终版）", PresetGroup.EXTREME, "完整极限画质配置。"),
        preset("extreme_final_120", "超越极限（最终版120帧）", PresetGroup.EXTREME, "极限画质并提高到 120 帧。"),
        preset(
            "extreme_final_120_hdr",
            "超越极限最终版120帧HDR（仅限支持HDR使用）",
            PresetGroup.EXTREME,
            "仅限支持 HDR 的高性能设备。"
        )
    )

    init {
        check(all.size == 20) { "内置预设数量必须为 20" }
        check(all.map { it.id }.distinct().size == all.size) { "内置预设 ID 必须唯一" }
        check(all.groupingBy { it.group }.eachCount() == mapOf(
            PresetGroup.LOW to 7,
            PresetGroup.MID to 3,
            PresetGroup.HIGH to 5,
            PresetGroup.EXTREME to 5
        )) { "内置预设分组数量不正确" }
    }

    private fun preset(
        id: String,
        name: String,
        group: PresetGroup,
        description: String
    ) = PresetMetadata(id, name, group, description)
}
