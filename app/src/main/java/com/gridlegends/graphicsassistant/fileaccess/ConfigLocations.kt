package com.gridlegends.graphicsassistant.fileaccess

internal object ConfigLocations {
    const val GAME_PACKAGE = "com.feralinteractive.gridlegends_android"
    const val GAME_DATA_DIR = "/sdcard/Android/data/$GAME_PACKAGE"
    const val PREFS_SUBDIR = "files/feral_app_support"
    const val CONFIG_FILE_NAME = "preferences"
}
