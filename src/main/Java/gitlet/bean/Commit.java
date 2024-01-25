package gitlet.bean;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.util.MyUtils.*;
import static gitlet.util.Utils.readObject;
import static gitlet.util.Utils.sha1;

public class Commit implements Serializable {
    // 提交的data
    private final Date date;
    // 提交时的信息
    private final String message;
    // 父提交的 SHA1 哈希列表
    private final List<String> parents;
    // SHA1的id
    private final String id;
    // commit的Object文件，路径由 SHA1 哈希生成
    private final File file;
    // Commit跟踪的文件映射，键为文件路径，值为文件的 SHA1 哈希
    private final Map<String, String> tracked;

    // 创建提交
    public Commit(String message, List<String> parents, Map<String, String> trackedFilesMap) {
        date = new Date();
        this.message = message;
        this.parents = parents;
        this.tracked = trackedFilesMap;
        id = generateId();
        file = getObjectFile(id);
    }

    // 初始化提交，初始化日期，message等信息
    public Commit() {
        date = new Date(0);
        message = "initial commit";
        parents = new ArrayList<>();
        tracked = new HashMap<>();
        // 根据信息生成id
        id = generateId();
        // 根据id生成在object目录下对应的commit对象（没报错到磁盘还）
        file = getObjectFile(id);
    }

    /**
     * 生成 SHA1 哈希，包含时间戳、消息、父提交列表和跟踪文件映射
     * @return SHA1 id
     */
    private String generateId() {
        return sha1(getTimestamp(), message, parents.toString(), tracked.toString());
    }

    /**
     * 从 SHA1 哈希获取 Commit 实例
     * @param id SHA1 id
     * @return Commit instance
     */
    public static Commit fromFile(String id) {
        return readObject(getObjectFile(id), Commit.class);
    }

    // 将此 Commit 实例保存到对象文件夹中
    public void save() {
        saveObjectFile(file, this);
    }

    // 返回Commit创建日期
    public Date getDate() {
        return date;
    }

    // 返回格式化时间 （Thu Jan 1 00:00:00 1970 +0000）
    public String getTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return dateFormat.format(date);
    }

    // 获取提交信息
    public String getMessage() {
        return message;
    }

    // 返回父提交列表
    public List<String> getParents() {
        return parents;
    }

    // 返回Commit跟踪的文件映射
    public Map<String, String> getTracked() {
        return tracked;
    }

    // 返回对应的sha1 ID
    public String getId() {
        return id;
    }

    /**
     * 返回 commit 日志，包括：分隔符，当前commit的id，创建的date，commit的message
     * 如果是merge提交，还要额外打印parents信息（不是merge用不着打印parent，因为下一条就是parent的信息了）
     * @return Log content
     */
    public String getLog() {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("===").append("\n");
        logBuilder.append("commit").append(" ").append(id).append("\n");
        // 对于合并提交，显示所有父提交的哈希
        if (parents.size() > 1) {
            logBuilder.append("Merge:");
            for (String parent : parents) {
                logBuilder.append(" ").append(parent, 0, 7);
            }
            logBuilder.append("\n");
        }
        logBuilder.append("Date:").append(" ").append(getTimestamp()).append("\n");
        logBuilder.append(message).append("\n");
        return logBuilder.toString();
    }

    /**
     * 根据文件路径恢复跟踪的文件，
     * @param filePath Path of the file
     * @return true if file exists in commit
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean restoreTracked(String filePath) {
        // commit中没有记录就返回false
        String blobId = tracked.get(filePath);
        if (blobId == null) {
            return false;
        }
        //根据Id拿到Blob对象并恢复
        Blob.fromFile(blobId).writeContentToSource();
        return true;
    }

    /**
     * 恢复所有跟踪的文件，覆盖现有文件
     */
    public void restoreAllTracked() {
        for (String blobId : tracked.values()) {
            Blob.fromFile(blobId).writeContentToSource();
        }
    }


}
