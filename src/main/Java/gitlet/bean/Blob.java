package gitlet.bean;

import java.io.File;
import java.io.Serializable;

import static gitlet.util.MyUtils.getObjectFile;
import static gitlet.util.MyUtils.saveObjectFile;
import static gitlet.util.Utils.readContents;
import static gitlet.util.Utils.sha1;

// 代表gitlet管理的文件对象
public class Blob implements Serializable {
    // 源文件
    private final File source;
    // 源文件内容
    private final byte[] content;
    // SHA1处理后的id（path和content一起生成，只有路径内容都一样才是一样的id）
    private final String id;
    // 处理后的blob对象的文件
    private final File file;
    // 根据传入的sourceFile 创建新的文件对象（读入内容，）
    public Blob(File sourceFile) {
        source = sourceFile;
        String filePath = sourceFile.getPath();
        content = readContents(sourceFile);
        id = sha1(filePath, content);
        file = getObjectFile(id);
    }

    //把本Bolb对象写入对应的Object文件，实现持久化
    public void save() {
        saveObjectFile(file, this);
    }

    // 获取blob对象的id
    public String getId() {
        return id;
    }

    // 获取blob的object file
    public File getFile() {
        return file;
    }

}
