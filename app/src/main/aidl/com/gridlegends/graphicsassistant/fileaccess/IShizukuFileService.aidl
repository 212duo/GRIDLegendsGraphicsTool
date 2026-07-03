package com.gridlegends.graphicsassistant.fileaccess;

interface IShizukuFileService {
    /** 读取文件内容 */
    String readFile(String path);

    /** 写入文件内容 */
    boolean writeFile(String path, String content);

    /** 检查文件是否存在 */
    boolean fileExists(String path);

    /** 列出目录内容，返回文件名（每行一个） */
    String listDir(String path);

    /** 递归查找指定文件名，返回完整路径 */
    String findFile(String startDir, String fileName);
}
