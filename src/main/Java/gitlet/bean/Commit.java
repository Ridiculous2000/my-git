package gitlet.bean;

import java.io.File;
import java.io.Serializable;
import java.sql.Blob;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.util.MyUtils.*;
import static gitlet.util.Utils.sha1;


/**
 * The commit object.
 *
 * @author Exuanbo
 */
public class Commit implements Serializable {
    // 提交的data
    private final Date date;
    // 提交时的信息
    private final String message;
    // 父提交的 SHA1 哈希列表
    private final List<String> parents;
    // SHA1的id
    private final String id;
    // commit的记录文件，路径由 SHA1 哈希生成
    private final File file;
    // 跟踪的文件映射，键为文件路径，值为文件的 SHA1 哈希
    private final Map<String, String> tracked;

    public Commit(String message, List<String> parents, Map<String, String> trackedFilesMap) {
        date = new Date();
        this.message = message;
        this.parents = parents;
        this.tracked = trackedFilesMap;
        id = generateId();
        file = getObjectFile(id);
    }

    /**
     * 初始化提交，初始化日期，message等信息，
     */
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
     * 将此 Commit 实例保存到对象文件夹中
     */
    public void save() {
        saveObjectFile(file, this);
    }

    /**
     * Get the Date instance when the commit is created.
     *
     * @return Date instance
     */
    public Date getDate() {
        return date;
    }

    /**
     * Get the timestamp.
     *
     * @return Date and time
     */
    public String getTimestamp() {
        // Thu Jan 1 00:00:00 1970 +0000
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return dateFormat.format(date);
    }

    /**
     * Get the commit message.
     *
     * @return Commit message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the parent commit ids.
     *
     * @return Array of parent commit ids.
     */
    public List<String> getParents() {
        return parents;
    }

    /**
     * Get the tracked files Map with file path as key and SHA1 id as value.
     *
     * @return Map with file path as key and SHA1 id as value
     */
    public Map<String, String> getTracked() {
        return tracked;
    }



    /**
     * Get the SHA1 id.
     * @return SHA1 id
     */
    public String getId() {
        return id;
    }
}
