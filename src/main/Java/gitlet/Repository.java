package gitlet;

import gitlet.bean.Blob;
import gitlet.bean.Commit;
import gitlet.bean.StagingArea;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static gitlet.bean.StagingArea.rm;
import static gitlet.util.MyUtils.*;
import static gitlet.util.Utils.*;

// gitlet 存储库，实现gitlet的大部分逻辑
public class Repository {

    /**
     * 生成的gitlet的路径：
     * [user.dir]
     * └── .gitlet
     *     ├── refs
     *     │   └── heads
     *     └── objects
     */
    // 返回Java程序运行时的路径
    private static final File CWD = new File(System.getProperty("user.dir"));
    //  .gitlet路径,版本控制所在的路径
    private static final File GITLET_DIR = join(CWD, ".gitlet");
    // refs(存储分支的末端信息，里面有heads文件夹，存储分支的最后的提交)
    private static final File REFS_DIR = join(GITLET_DIR, "refs");
    // gitlet下面的object文件夹，包含各种object对象的持久化（blob，commit）
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    // 在 refs下，存储各个分支当前的commit
    private static final File BRANCH_HEADS_DIR = join(REFS_DIR, "heads");
    // 默认分支 : master
    private static final String DEFAULT_BRANCH_NAME = "master";
    // .gitlet 下面的HEAD文件，记录当前的提交版本
    private static final File HEAD = join(GITLET_DIR, "HEAD");
    // 记录 "ref: refs/heads/" 这样一个固定前缀
    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";
    // index文件，用于持久化暂存区内容
    public static final File INDEX = join(GITLET_DIR, "index");
    // lazy是单例模式封装后的Supplier，这部分读取文件内容，去除前缀。eg: ref: refs/heads/master => master
    private final Lazy<String> currentBranch = lazy(() -> {
        String HEADFileContent = readContentsAsString(HEAD);
        return HEADFileContent.replace(HEAD_BRANCH_REF_PREFIX, "");
    });

    // 拿到工作区内的所有文件
    private static final Lazy<File[]> currentFiles = lazy(() -> CWD.listFiles(File::isFile));



    // Head文件存储的是当前分支名，而HEADComit可以根据当前分支名计算出当前分支的Commit
    private final Lazy<Commit> HEADCommit = lazy(() -> getBranchHeadCommit(currentBranch.get()));
    /**
     *  lazy 继承了Supplier<T>接口，在延迟返回前进行了进一步的封装
     *  所以lazy 接受一个 Supplier<T>接口对象（这是个函数式接口用于延迟加载），传入的lambda是为这个对象的实现
     *  所以lambda的函数封装为delegate,以供后续调用
     *  这部分实现的是把
     */
    private final Lazy<StagingArea> stagingArea = lazy(() -> {
        StagingArea s = INDEX.exists()
                ? StagingArea.fromFile()
                : new StagingArea();
        s.setTracked(HEADCommit.get().getTracked());
        return s;
    });

    // 初始化 .gitlet 区
    public static void init() {
        // 初始化目录
        if (GITLET_DIR.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
        mkdir(GITLET_DIR);
        mkdir(REFS_DIR);
        mkdir(BRANCH_HEADS_DIR);
        mkdir(OBJECTS_DIR);
        // 设置当前分支为master
        setCurrentBranch(DEFAULT_BRANCH_NAME);
        // 设置当前分支的当前commit为init
        createInitialCommit();
    }

    // 设置当前分支：写入HEAD文件即可（为了优雅，加一下前缀）
    private static void setCurrentBranch(String branchName) {
        // HEAD文件记录当前分支，所以分支名写入HEAD文件即可实现切换
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    // 创建初始化提交
    private static void createInitialCommit() {
        // 初始话一个commit
        Commit initialCommit = new Commit();
        // 保存commit到磁盘
        initialCommit.save();
        // 根据commit的id和分支名字，设置分支头
        setBranchHeadCommit(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    // 设置branchName 当前的commit 为 commitId
    private static void setBranchHeadCommit(String branchName, String commitId) {
        // 拿到当前分支的头文件
        File branchHeadFile = getBranchHeadFile(branchName);
        // commit写入头文件（即：设置当前分支的commit）
        setBranchHeadCommit(branchHeadFile, commitId);
    }

    /**
     * 获取branchName对应的branch file
     * @param branchName Name of the branch
     * @return File instance
     */
    private static File getBranchHeadFile(String branchName) {
        return join(BRANCH_HEADS_DIR, branchName);
    }

    /**
     * 根据 branchHead（存放branch对应的Commit） 记录的内容，转换成commitId，然后拿到对应的Commit文件
     * @param branchHeadFile File instance
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(File branchHeadFile) {
        String HEADCommitId = readContentsAsString(branchHeadFile);
        return Commit.fromFile(HEADCommitId);
    }

    /**
     * 把commitId写入对应branchHeadFile文件
     * @param branchHeadFile File instance
     * @param commitId       Commit SHA1 id
     */
    private static void setBranchHeadCommit(File branchHeadFile, String commitId) {
        writeContents(branchHeadFile, commitId);
    }

    // 如果gitlet不存在或者不是文件夹就退出
    public static void checkWorkingDir() {
        if (!GITLET_DIR.exists() || !GITLET_DIR.isDirectory()) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * 添加文件到暂存区
     * @param fileName 文件名
     */
    public void add(String fileName) {
        // 返回filename对应的file对象，用于判断file是否存在
        File file = getFileFromCWD(fileName);
        if (!file.exists()) {
            exit("File does not exist.");
        }
        // 将文件添加到暂存区
        if (stagingArea.get().add(file)) {
            // 暂存区持久化（保存到index文件）
            stagingArea.get().save();
        }
    }

    /**
     * 根据 filename 拿到一个 file 对象（如果是相对路径就结合一下工作目录，绝对路径就直接创建）
     * @param fileName Name of the file
     * @return File instance
     */
    private static File getFileFromCWD(String fileName) {
        return Paths.get(fileName).isAbsolute()
                ? new File(fileName)
                : join(CWD, fileName);
    }

    /**
     * 拿到 branchName 对应的最新的 Commit
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getBranchHeadCommit(branchHeadFile);
    }

    // 创建commit
    public void commit(String msg) {
        commit(msg, null);
    }

    /**
     * 根据消息和secondParent创建commit，默认当前commit为parent，
     * 如果是merge，有两个parent就需要传入非空的secondParent
     * @param msg          Commit message
     * @param secondParent Second parent Commit SHA1 id
     */
    private void commit(String msg, String secondParent) {
        // 期间没有更改就退出
        if (stagingArea.get().isClean()) {
            exit("No changes added to the commit.");
        }
        // 提交暂存区变化，更新得到当前状态的快照
        Map<String, String> newTrackedFilesMap = stagingArea.get().commit();
        // 持久化暂存区内容
        stagingArea.get().save();
        // 当前分支的commit作为parent
        List<String> parents = new ArrayList<>();
        parents.add(HEADCommit.get().getId());
        if (secondParent != null) {
            parents.add(secondParent);
        }
        // 基于消息，parents，当前快照 创建commit
        Commit newCommit = new Commit(msg, parents, newTrackedFilesMap);
        // 保存commit
        newCommit.save();
        // 更新当前分支点最新commit
        setBranchHeadCommit(currentBranch.get(), newCommit.getId());
    }


    /**
     * 删除文件
     * @param fileName Name of the file
     */
    public void remove(String fileName) {
        File file = getFileFromCWD(fileName);
        if (stagingArea.get().remove(file)) {
            stagingArea.get().save();
        } else {
            exit("No reason to remove the file.");
        }
    }

    /**
     * 对标git中的命令："git log --first-parent"
     * 从当前分支的当前commit开始，往前遍历first-parent(忽略后面的parent),打印commit信息
     * eg：
     *    commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
     *    Date: Thu Nov 9 20:00:05 2017 -0800
     *    A commit message.
     *    ===
     *    commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
     *    Date: Thu Nov 9 17:01:33 2017 -0800
     *    Another commit message.
     */
    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        // 从当前commit开始往前打印
        Commit currentCommit = HEADCommit.get();
        while (true) {
            // 添加commit日志信息
            logBuilder.append(currentCommit.getLog()).append("\n");
            // 顺着parent遍历（只看first parent）
            List<String> parentCommitIds = currentCommit.getParents();
            if (parentCommitIds.size() == 0) {
                break;
            }
            String firstParentCommitId = parentCommitIds.get(0);
            // 根据parentid 加载出来commit 对象
            currentCommit = Commit.fromFile(firstParentCommitId);
        }
        System.out.print(logBuilder);
    }


     // 按照date顺序打印所有commit的信息
    public static void globalLog() {
        StringBuilder logBuilder = new StringBuilder();
        // 传入一个function，接受commit为参数，把commit的log加入到logBuilder中
        forEachCommitInOrder(commit -> logBuilder.append(commit.getLog()).append("\n"));
        System.out.print(logBuilder);
    }

    /**
     * 基于创建日期遍历commit
     * @param cb 接受Commit为参数的函数（封装为Consumer接口）
     */
    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        // 时间排序
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        // 创建优先队列，并传递该队列与回调函数
        Queue<Commit> commitsPriorityQueue = new PriorityQueue<>(commitComparator);
        forEachCommit(cb, commitsPriorityQueue);
    }

    /**
     * 无序遍历commit
     * @param cb 接受Commit为参数的函数（封装为Consumer接口）
     */
    private static void forEachCommit(Consumer<Commit> cb) {
        // 普通队列
        Queue<Commit> commitsQueue = new ArrayDeque<>();
        forEachCommit(cb, commitsQueue);
    }

    /**
     * 遍历commit（有序|无序）
     * @param cb                 处理commit的callback函数
     * @param queueToHoldCommits commit的队列（优先队列|普通队列 均可）
     */
    @SuppressWarnings("ConstantConditions")
    private static void forEachCommit(Consumer<Commit> cb, Queue<Commit> queueToHoldCommits) {
        // set用于去重
        Set<String> checkedCommitIds = new HashSet<>();
        // 各个分支的文件（记录该分支当前的commitId）
        File[] branchHeadFiles = BRANCH_HEADS_DIR.listFiles();
        // 分支按照name排序
        Arrays.sort(branchHeadFiles, Comparator.comparing(File::getName));
        // 先把各个分支的头commit加入队列（期间用set去重）
        for (File branchHeadFile : branchHeadFiles) {
            String branchHeadCommitId = readContentsAsString(branchHeadFile);
            if (checkedCommitIds.contains(branchHeadCommitId)) {
                continue;
            }
            checkedCommitIds.add(branchHeadCommitId);
            Commit branchHeadCommit = Commit.fromFile(branchHeadCommitId);
            queueToHoldCommits.add(branchHeadCommit);
        }
        // 各个分支往parent上遍历
        while (true) {
            Commit nextCommit = queueToHoldCommits.poll();
            // accept为 Consumer 接口的函数，即：把commit加入log
            cb.accept(nextCommit);
            List<String> parentCommitIds = nextCommit.getParents();
            if (parentCommitIds.size() == 0) {
                break;
            }
            //把父commit都加进来（加之前去重）
            for (String parentCommitId : parentCommitIds) {
                if (checkedCommitIds.contains(parentCommitId)) {
                    continue;
                }
                checkedCommitIds.add(parentCommitId);
                Commit parentCommit = Commit.fromFile(parentCommitId);
                queueToHoldCommits.add(parentCommit);
            }
        }
    }

    /**
     * 打印所有与输入message相同的Commit的ID
     * @param msg commit的message
     */
    public static void find(String msg) {
        StringBuilder resultBuilder = new StringBuilder();
        // 查询不需要排序，普通列表即可
        forEachCommit(commit -> {
            if (commit.getMessage().equals(msg)) {
                resultBuilder.append(commit.getId()).append("\n");
            }
        });
        if (resultBuilder.length() == 0) {
            exit("Found no commit with that message.");
        }
        System.out.print(resultBuilder);
    }

    /**
     * 打印所有分支，当前分支 “*” 加重，打印其他内容：
     * === Branches ===
     * *master
     * other-branch
     *
     * === Staged Files ===
     * wug.txt
     * wug2.txt
     *
     * === Removed Files ===
     * goodbye.txt
     *
     * === Modifications Not Staged For Commit ===
     * junk.txt (deleted)
     * wug3.txt (modified)
     *
     * === Untracked Files ===
     * random.stuff
     */
    @SuppressWarnings("ConstantConditions")
    public void status() {
        StringBuilder statusBuilder = new StringBuilder();
        // 打印分支信息
        statusBuilder.append("=== Branches ===").append("\n");
        // 当前分支 * 加重
        statusBuilder.append("*").append(currentBranch.get()).append("\n");
        // 添加非当前分支（name排序）
        String[] branchNames = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch.get()));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");

        // 拿到暂存区 added 和 removed 的内容
        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();

        // 按照文件名顺序，添加added区域的内容
        statusBuilder.append("=== Staged Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, addedFilesMap.keySet());
        statusBuilder.append("\n");

        // 按照文件名顺序，添加removed区域的内容
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        statusBuilder.append("\n");

        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        // 拿到commit过，然后更改了但还没有保存到暂存区的文件
        List<String> modifiedNotStageFilePaths = new ArrayList<>();
        // 拿到commit过，没有rm删除文件，但文件以及被从工作区删除的文件
        Set<String> deletedNotStageFilePaths = new HashSet<>();

        // 获取当前工作区文件 <path,id>
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        // 结合added和remove的内容更新tracked，得到暂存区现在的跟踪的文件的状态
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();
        trackedFilesMap.putAll(addedFilesMap);
        for (String filePath : removedFilePathsSet) {
            trackedFilesMap.remove(filePath);
        }
        // 遍历跟踪的文件
        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String blobId = entry.getValue();

            // 尝试获取当前文件id
            String currentFileBlobId = currentFilesMap.get(filePath);
            // 工作区以及没有这个文件 就为null
            if (currentFileBlobId != null) {
                // 不相等说明改过了（工作区与 暂存区|commit 不服）
                if (!currentFileBlobId.equals(blobId)) {
                    modifiedNotStageFilePaths.add(filePath);
                }
                // 工作区和把暂存区对上了就删除，这样循环后剩下的就是最后要用的：没有添加到暂存区的工作区文件了
                currentFilesMap.remove(filePath);
            } else {
                // 暂存区|tracked 还有，工作区以及删除了，记录进来
                modifiedNotStageFilePaths.add(filePath);
                deletedNotStageFilePaths.add(filePath);
            }
        }

        modifiedNotStageFilePaths.sort(String::compareTo);

        // 优雅的写法，就是deleted有的打deleted标签，modified有deleted没有的打modified，
        // 但个人感觉modified只存modified也完全没毛病，虽然不够优雅（排序，循环都要两次），但清晰易懂
        for (String filePath : modifiedNotStageFilePaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName);
            if (deletedNotStageFilePaths.contains(filePath)) {
                statusBuilder.append(" ").append("(deleted)");
            } else {
                statusBuilder.append(" ").append("(modified)");
            }
            statusBuilder.append("\n");
        }
        statusBuilder.append("\n");

        // 没有添加到暂存区的工作区文件了
        statusBuilder.append("=== Untracked Files ===").append("\n");
        // 前面已经顺便处理currentFilesMap了，直接排序写入就好了
        appendFileNamesInOrder(statusBuilder, currentFilesMap.keySet());
        statusBuilder.append("\n");

        System.out.print(statusBuilder);
    }

    // 按照顺序把fileName添加到stringBuilder里面
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, Collection<String> filePathsCollection) {
        List<String> filePathsList = new ArrayList<>(filePathsCollection);
        appendFileNamesInOrder(stringBuilder, filePathsList);
    }

    // 按照顺序把fileName添加到stringBuilder里面
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, List<String> filePathsList) {
        filePathsList.sort(String::compareTo);
        for (String filePath : filePathsList) {
            String fileName = Paths.get(filePath).getFileName().toString();
            stringBuilder.append(fileName).append("\n");
        }
    }

    // 遍历当前的文件列表并建立映射，key是filepath，value是blobId
    private static Map<String, String> getCurrentFilesMap() {
        Map<String, String> filesMap = new HashMap<>();
        for (File file : currentFiles.get()) {
            String filePath = file.getPath();
            String blobId = Blob.generateId(file);
            filesMap.put(filePath, blobId);
        }
        return filesMap;
    }


    /**
     * 根据指定的commitid的前缀恢复blob文件
     * @param commitId 指定的commitid
     * @param fileName 要恢复的文件名
     */
    public void checkout(String commitId, String fileName) {
        // 根据传入的commitid前缀拿到完整的commitId（便于用户使用，不用每次都传一个40位的id了，传入大于4位的前缀即可）
        commitId = getActualCommitId(commitId);
        String filePath = getFileFromCWD(fileName).getPath();
        // 比起直接恢复，多了一个根据commitId拿到对应commit的步骤
        if (!Commit.fromFile(commitId).restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * 根据commit 恢复文件
     * @param fileName Name of the file
     */
    public void checkout(String fileName) {
        // 文件名拼凑成绝对路径
        String filePath = getFileFromCWD(fileName).getPath();
        // 尝试根据tracked记录恢复文件内容（不存在就exit）
        if (!HEADCommit.get().restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * 签出到指定的分支
     * @param targetBranchName 目标分支的名字
     */
    public void checkoutBranch(String targetBranchName) {
        // 拿到对应branch的heads文件
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("No such branch exists.");
        }
        // 目标分支跟当前分支一致
        if (targetBranchName.equals(currentBranch.get())) {
            exit("No need to checkout the current branch.");
        }
        // 拿到目标分支的当前commit
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        // 判断当前暂存区要跟踪的文件是否有未保存的更改，有就退出
        checkUntracked(targetBranchHeadCommit);
        // 没有就更新工作目录内容为commit的内容
        checkoutCommit(targetBranchHeadCommit);
        // 设置当前分支（把分支名写入HEAD）
        setCurrentBranch(targetBranchName);
    }

    /**
     * 根据传入的部分id来获取完整的id，从而简化用户操作（不用传入完整的40位id了）
     * @param commitId 用户传入的id(可以是完整的，也可以是大于4位的前缀)
     * @return 返回完整commitid
     */
    @SuppressWarnings("ConstantConditions")
    private static String getActualCommitId(String commitId) {
        // 给的commitid不完整
        if (commitId.length() < UID_LENGTH) {
            // 太少了
            if (commitId.length() < 4) {
                exit("Commit id should contain at least 4 characters.");
            }
            // 拿到id对应对象所在文件夹（按照id前2位创建的）
            String objectDirName = getObjectDirName(commitId);
            File objectDir = join(OBJECTS_DIR, objectDirName);
            // 不存在，说明传的前N位有问题
            if (!objectDir.exists()) {
                exit("No commit with that id exists.");
            }
            boolean isFound = false;
            // 前两位是文件夹，剩下的为文件名的前缀
            String objectFileNamePrefix = getObjectFileName(commitId);
            // 遍历问价夹
            for (File objectFile : objectDir.listFiles()) {
                String objectFileName = objectFile.getName();
                // 前缀对的上，而且是commit对象
                if (objectFileName.startsWith(objectFileNamePrefix) && isFileInstanceOf(objectFile, Commit.class)) {
                    if (isFound) {
                        exit("More than 1 commit has the same id prefix.");
                    }
                    commitId = objectDirName + objectFileName;
                    isFound = true;
                }
            }
            if (!isFound) {
                exit("No commit with that id exists.");
            }
        } else {
            // 给了完整的commit但出给错了
            if (!getObjectFile(commitId).exists()) {
                exit("No commit with that id exists.");
            }
        }
        return commitId;
    }

    /**
     * 如果目标提交将覆盖未跟踪的文件，则退出并显示消息
     * @param targetCommit Commit SHA1 id
     */
    private void checkUntracked(Commit targetCommit) {
        // 当前工作区文件
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        // 暂存区信息
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();
        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();
        // 跟踪的文件存在没有保存的更改
        List<String> untrackedFilePaths = new ArrayList<>();
        for (String filePath : currentFilesMap.keySet()) {
            // 该文件跟踪
            if (trackedFilesMap.containsKey(filePath)) {
                // 存在更改(add也需要判断吧？)
                if (removedFilePathsSet.contains(filePath)||addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            } else {
                // 可能没跟踪但add了，也就是用户准备跟踪
                if (addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            }
        }

        // 拿到目标commit保存的文件
        Map<String, String> targetCommitTrackedFilesMap = targetCommit.getTracked();
        // 如果目标commit的文件的id和当前没保存的文件的id不同，说明签出分支会覆盖未保存的提交
        for (String filePath : untrackedFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String targetBlobId = targetCommitTrackedFilesMap.get(filePath);
            if (!blobId.equals(targetBlobId)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     * 创建一个新分支
     * @param newBranchName 新的分支名
     */
    public void branch(String newBranchName) {
        // 在 refs/heads 下创建对应的 branchName 文件
        File newBranchHeadFile = getBranchHeadFile(newBranchName);
        if (newBranchHeadFile.exists()) {
            exit("A branch with that name already exists.");
        }
        // 拿到当前commit，作为新branch的初始commit（写入head文件）
        setBranchHeadCommit(newBranchHeadFile, HEADCommit.get().getId());
    }

    /**
     * 签出到目标commit
     * @param targetCommit 目标commit
     */
    private void checkoutCommit(Commit targetCommit) {
        // 清空暂存区（前面判断过了，没有未保存的更改）
        stagingArea.get().clear();
        // 记录暂存区
        stagingArea.get().save();
        // 删除工作区所有文件
        for (File file : currentFiles.get()) {
            rm(file);
        }
        // 恢复commit中的所有文件
        targetCommit.restoreAllTracked();
    }

    /**
     * 删除分支
     * @param targetBranchName 要删除的目标分支名字
     */
    public void rmBranch(String targetBranchName) {
        // 拿到分支HeadFile
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot remove the current branch.");
        }
        // 删除分支文件
        rm(targetBranchHeadFile);
    }

    /**
     * 更新当前内容为commitId对应的版本内容
     * @param commitId 目标commitId
     */
    public void reset(String commitId) {
        // 获取完整commitId（前缀补全与校验）
        commitId = getActualCommitId(commitId);
        // 根据commitId拿到targetComit
        Commit targetCommit = Commit.fromFile(commitId);
        // 校验是否存在覆盖提交
        checkUntracked(targetCommit);
        // 签出到目标commit（更改工作区内容）
        checkoutCommit(targetCommit);
        // 设置当前commit（写入Heads）
        setBranchHeadCommit(currentBranch.get(), commitId);
    }

    /**
     * 合并分支（8种情况），例如master和other,master分出一个other分支后，master和other两个分支中的文件都可能做了修改，
     * 进而导致了8中情况
     * （1）master中文件变了other没变（2）master没变other变了：以变化的最新版为准
     * （3）master和other 都没变或改动一样，那就不用纠结了
     * （4）ohter和master都改了，但改的不一样：提示冲突
     * （5）master的这个文件删了，other没改：说明master是新的操作，合并后也删除了
     * （6）other删除，master没改：同上，合并后也删除
     * （7）other删了，master改动了：肯定保存改动更稳妥，保存改动后的文件
     * （8）master删了，other改了：同上
     * @param targetBranchName 要合并到当前分支的活
     */
    public void merge(String targetBranchName) {
        // 拿到要合并的分支
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        // 分支不存在
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        // 跟当前分支是一个分支
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot merge a branch with itself.");
        }
        // 又没保存的提交
        if (!stagingArea.get().isClean()) {
            exit("You have uncommitted changes.");
        }
        // 拿到目标commit
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        // 校验是否有未提交的分支
        checkUntracked(targetBranchHeadCommit);
        // 拿到共同的祖先commit
        Commit lcaCommit = getLatestCommonAncestorCommit(HEADCommit.get(), targetBranchHeadCommit);
        String lcaCommitId = lcaCommit.getId();
        // 如果target是current的祖先的话，找到的lca就是target，这种不需要合并，current就是最新版本
        if (lcaCommitId.equals(targetBranchHeadCommit.getId())) {
            exit("Given branch is an ancestor of the current branch.");
        }
        // 如果current是target的祖先的话，找到的lca就是current，这种相当于checkout到target
        if (lcaCommitId.equals(HEADCommit.get().getId())) {
            checkoutCommit(targetBranchHeadCommit);
            setCurrentBranch(targetBranchName);
            exit("Current branch fast-forwarded.");
        }

        boolean hasConflict = false;
        // 当前commit的文件快照、目标commit的文件快照、共同祖先的文件快照
        Map<String, String> HEADCommitTrackedFilesMap = new HashMap<>(HEADCommit.get().getTracked());
        Map<String, String> targetBranchHeadCommitTrackedFilesMap = targetBranchHeadCommit.getTracked();
        Map<String, String> lcaCommitTrackedFilesMap = lcaCommit.getTracked();

        for (Map.Entry<String, String> entry : lcaCommitTrackedFilesMap.entrySet()) {
            // 拿到祖先快照原始版本文件
            String filePath = entry.getKey();
            File file = new File(filePath);
            String blobId = entry.getValue();
            // 拿到target和current的版本
            String targetBranchHeadCommitBlobId = targetBranchHeadCommitTrackedFilesMap.get(filePath);
            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(filePath);
            // target当中有这个版本的文件
            if (targetBranchHeadCommitBlobId != null) {
                // target有改动
                if (!targetBranchHeadCommitBlobId.equals(blobId)) {
                    // Head也有这个文件
                    if (HEADCommitBlobId != null) {
                        // Head没有改动
                        if (HEADCommitBlobId.equals(blobId)) {
                            // 记录target的版本
                            Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                            stagingArea.get().add(file);
                        } else {
                            // Head也有改动，但改动不一样
                            if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) {
                                // 说明有冲突
                                hasConflict = true;
                                // 拿到冲突的内容并写入文件
                                String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                                writeContents(file, conflictContent);
                                stagingArea.get().add(file);
                            }
                        }
                    } else {
                        // current 中没有这个文件，已经删除了
                        hasConflict = true;
                        // 写入target的内容即可
                        String conflictContent = getConflictContent(null, targetBranchHeadCommitBlobId);
                        writeContents(file, conflictContent);
                        stagingArea.get().add(file);
                    }
                }
            } else {
                // target中没有这个版本的内容
                // current中有这个版本
                if (HEADCommitBlobId != null) {
                    if (HEADCommitBlobId.equals(blobId)) {
                        // current没做修改，说明删除是新操作，删除了就行
                        stagingArea.get().remove(file);
                    } else {
                        // current改了，保存current内容
                        hasConflict = true;
                        String conflictContent = getConflictContent(HEADCommitBlobId, null);
                        writeContents(file, conflictContent);
                        stagingArea.get().add(file);
                    }
                }
            }
            // 合并处理完成
            HEADCommitTrackedFilesMap.remove(filePath);
            targetBranchHeadCommitTrackedFilesMap.remove(filePath);
        }

        // 处理祖先中没有，后续current和target新增的文件（current有target没有不用担心，因为这种不需要额外处理，所以遍历target即可）
        for (Map.Entry<String, String> entry : targetBranchHeadCommitTrackedFilesMap.entrySet()) {
            String targetBranchHeadCommitFilePath = entry.getKey();
            File targetBranchHeadCommitFile = new File(targetBranchHeadCommitFilePath);
            String targetBranchHeadCommitBlobId = entry.getValue();

            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(targetBranchHeadCommitFilePath);
            // 两边都添加了这个文件
            if (HEADCommitBlobId != null) {
                // 修改冲突
                if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) {
                    hasConflict = true;
                    String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                    writeContents(targetBranchHeadCommitFile, conflictContent);
                    stagingArea.get().add(targetBranchHeadCommitFile);
                }
                // 否则说明修改方式一致，不用管了就
            } else {
                // target有current没有
                // 添加进来即可
                Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                stagingArea.get().add(targetBranchHeadCommitFile);
            }
        }

        String newCommitMessage = "Merged" + " " + targetBranchName + " " + "into" + " " + currentBranch.get() + ".";
        commit(newCommitMessage, targetBranchHeadCommit.getId());
        // 提示用户解决merge的冲突
        if (hasConflict) {
            message("Encountered a merge conflict.");
        }
    }

    /**
     * 拿到俩个commit共同祖先的commit（两个分支原本总是一个分支分化的，找到共同祖先才知道分化后各自有什么变化）
     * @param commitA Commit instance
     * @param commitB Commit instance
     * @return Commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private static Commit getLatestCommonAncestorCommit(Commit commitA, Commit commitB) {
        // 时间排序的优先队列
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        Queue<Commit> commitsQueue = new PriorityQueue<>(commitComparator);
        commitsQueue.add(commitA);
        commitsQueue.add(commitB);
        // 初始化一个集合，用来存储已经检查过的提交的ID
        Set<String> checkedCommitIds = new HashSet<>();
        while (true) {
            // 弹出后找祖先commit
            Commit latestCommit = commitsQueue.poll();
            List<String> parentCommitIds = latestCommit.getParents();
            String firstParentCommitId = parentCommitIds.get(0);
            Commit firstParentCommit = Commit.fromFile(firstParentCommitId);
            // 已经包含在checked里面的话，说明找到共同祖先了
            if (checkedCommitIds.contains(firstParentCommitId)) {
                return firstParentCommit;
            }
            commitsQueue.add(firstParentCommit);
            checkedCommitIds.add(firstParentCommitId);
        }
    }

    /**
     * merge发生更改冲突时候，拿到冲突的内容
     * @param currentBlobId 当前commitid
     * @param targetBlobId  目标commitid
     * @return New content
     */
    private static String getConflictContent(String currentBlobId, String targetBlobId) {
        // Head的内容
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<<<<<<< HEAD").append("\n");
        if (currentBlobId != null) {
            Blob currentBlob = Blob.fromFile(currentBlobId);
            contentBuilder.append(currentBlob.getContentAsString());
        }
        // target的内容
        contentBuilder.append("=======").append("\n");
        if (targetBlobId != null) {
            Blob targetBlob = Blob.fromFile(targetBlobId);
            contentBuilder.append(targetBlob.getContentAsString());
        }
        contentBuilder.append(">>>>>>>");
        return contentBuilder.toString();
    }



}
