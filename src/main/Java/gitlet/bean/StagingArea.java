package gitlet.bean;

import gitlet.Repository;

import java.io.Serializable;
import java.util.Map;

import static gitlet.util.Utils.readObject;

public class StagingArea implements Serializable {

    /**
     * 跟踪的文件映射，键是文件路径，值是文件的 SHA1 哈希
     */
    private transient Map<String, String> tracked;

    /**
     * 静态方法：从 INDEX 文件获取 StagingArea 实例
     *
     * @return StagingArea instance
     */
    public static StagingArea fromFile() {
        return readObject(Repository.INDEX, StagingArea.class);
    }

    /**
     * Set tracked files.
     *
     * @param filesMap Map with file path as key and SHA1 id as value.
     */
    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

}
