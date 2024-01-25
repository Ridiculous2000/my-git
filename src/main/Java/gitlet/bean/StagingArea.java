package gitlet.bean;

import gitlet.Repository;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gitlet.util.Utils.readObject;
import static gitlet.util.Utils.writeObject;

public class StagingArea implements Serializable {
    // 添加文件区，键是文件路径，值是文件的 SHA1 哈希
    private final Map<String, String> added = new HashMap<>();
    // 已删除文件区，存储文件路径
    private final Set<String> removed = new HashSet<>();
    // 跟踪commit过的文件当前的最新状况，key是path，value是sha1_Id
    private transient Map<String, String> tracked;

    /**
     * 静态方法：从 INDEX 文件获取 StagingArea 实例
     * @return StagingArea 对象
     */
    public static StagingArea fromFile() {
        return readObject(Repository.INDEX, StagingArea.class);
    }

    // 设置跟踪的文件映射
    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

    /**
     * 将文件添加到暂存区：这部分要分情况讨论：
     * （1）用户创建|更改了文件，然后提交了最新版 ，创建blob文件，然后put到added区即可，返回true
     * （2）之前add过一次一模一样的，返回false
     * （3）用户之前把 a.txt 内容(eg:123)交了，然后修改成了(123456)后add到了暂存区但没有commit
     * 现在又改回了(123),那就没必要做多个blob，直接把暂存区的中间版本删除了就好了。然后返回ture
     * （4）用户之前把 a.txt 交了，然后后来 rm 删除了，现在用户又创建回来了，所以add的时候要把原来的删除记录删了防止误删，返回true
     * @param file 要添加的文件
     * @return true 是否添加成功
     */
    public boolean add(File file) {
        String filePath = file.getPath();
        // 创建文件 blob 对象
        Blob blob = new Blob(file);
        String blobId = blob.getId();
        // 文件是否被跟踪（commit过）
        String trackedBlobId = tracked.get(filePath);
        if (trackedBlobId != null) {
            // 现在交的文件和之前commit的文件完全一致，没必要提交了，且应该把暂存区相关变化删除
            if (trackedBlobId.equals(blobId)) {
                // 删除 add 的变化
                if (added.remove(filePath) != null) {
                    return true;
                }
                // 删除 remove 区
                return removed.remove(filePath);
            }
        }
        // 没有commit过，但之前就add过一模一样的
        String prevBlobId = added.put(filePath, blobId);
        if (prevBlobId != null && prevBlobId.equals(blobId)) {
            return false;
        }
        // 保存blob文件
        if (!blob.getFile().exists()) {
            blob.save();
        }
        return true;
    }

    // 暂存区内容持久化到index文件
    public void save() {
        writeObject(Repository.INDEX, this);
    }

    /**
     * 判断暂存区是否有内容（added和removed都为空说明没有文件添加、修改、删除）
     * @return true if is clean
     */
    public boolean isClean() {
        return added.isEmpty() && removed.isEmpty();
    }

    /**
     * 执行提交（更新跟踪的文件，清理暂存区），返回提交后的跟踪文件映射
     * @return Map with file path as key and SHA1 id as value.
     */
    public Map<String, String> commit() {
        // tracked维护commit过的文件的当前状况（注意区别，tracked是所有commit过的文件的状况，added和remove记录的是变化）
        // 所以putAll 根据增量更新tracked
        tracked.putAll(added);
        // 遍历remove，更新tracked
        for (String filePath : removed) {
            tracked.remove(filePath);
        }
        // 清除暂存区
        clear();
        // 返回当前的快照给commit保存
        return tracked;
    }

    // 清空暂存区
    public void clear() {
        added.clear();
        removed.clear();
    }

    /**
     * 从暂存区中移除文件，一个是add中移除，不然后面commit会个加回去，然后把本地文件也删除了（这个逻辑可选其实），
     * 最后把路径放入remove，后面commit的时候才会修改（注意只有commit操作会更改tracked，tracked就是提交的版本快照）
     * @param file File instance
     * @return true if the staging area is changed
     */
    public boolean remove(File file) {
        String filePath = file.getPath();
        // 该文件有变化，移除变化
        String addedBlobId = added.remove(filePath);
        // 该文件没有变化，且工作区有该文件，移除跟踪，并添加到remove
        if (tracked.get(filePath) != null) {
            if (file.exists()) {
                rm(file);
            }
            return removed.add(filePath);
        }
        return false;
    }

    // 删除file
    public static void rm(File file) {
        if (!file.delete()) {
            throw new IllegalArgumentException(String.format("rm: %s: Failed to delete.", file.getPath()));
        }
    }

    /**
     * 获取已添加的文件映射
     *
     * @return Map with file path as key and SHA1 id as value.
     */
    public Map<String, String> getAdded() {
        return added;
    }

    /**
     * 获取已删除的文件集合
     *
     * @return Set of files paths.
     */
    public Set<String> getRemoved() {
        return removed;
    }


}
