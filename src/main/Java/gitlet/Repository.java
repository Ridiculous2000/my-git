package gitlet;

import gitlet.bean.Commit;
import gitlet.bean.StagingArea;

import java.io.File;
import java.nio.file.Paths;

import static gitlet.util.MyUtils.*;
import static gitlet.util.Utils.join;
import static gitlet.util.Utils.writeContents;

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
    // 记录refs/heads/ 路径
    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";
    /**
     * The index file.
     */
    public static final File INDEX = join(GITLET_DIR, "index");
    /**
     * The commit that HEAD points to.
     */
    private final Lazy<Commit> HEADCommit = lazy(() -> getBranchHeadCommit(currentBranch.get()));

    /**
     *  lazy 继承了Supplier<T>接口，在延迟返回前进行了进一步的封装
     *  所以lazy 接受一个 Supplier<T>接口对象（这是个函数式接口用于延迟加载），传入的lambda是为这个对象的实现
     *  所以lambda的函数封装为delegate,以供后续调用
     */
    private final Lazy<StagingArea> stagingArea = lazy(() -> {
        StagingArea s = INDEX.exists()
                ? StagingArea.fromFile()
                : new StagingArea();
        s.setTracked(HEADCommit.get().getTracked());
        return s;
    });


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

    private static void setCurrentBranch(String branchName) {
        // HEAD文件记录当前分支，所以分支名写入HEAD文件即可实现切换
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    private static void createInitialCommit() {
        // 初始话一个commit
        Commit initialCommit = new Commit();
        // 保存commit到磁盘
        initialCommit.save();
        // 根据commit的id和分支名字，设置分支头
        setBranchHeadCommit(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    private static void setBranchHeadCommit(String branchName, String commitId) {
        // 拿到当前分支的头文件
        File branchHeadFile = getBranchHeadFile(branchName);
        // commit写入头文件（即：设置当前分支的commit）
        setBranchHeadCommit(branchHeadFile, commitId);
    }

    /**
     * 获取branchName
     * @param branchName Name of the branch
     * @return File instance
     */
    private static File getBranchHeadFile(String branchName) {
        return join(BRANCH_HEADS_DIR, branchName);
    }

    /**
     * Set branch head.
     * @param branchHeadFile File instance
     * @param commitId       Commit SHA1 id
     */
    private static void setBranchHeadCommit(File branchHeadFile, String commitId) {
        writeContents(branchHeadFile, commitId);
    }

    /**
     * 如果gitlet不存在或者不是文件夹就退出
     */
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
        File file = getFileFromCWD(fileName);
        if (!file.exists()) {
            exit("File does not exist.");
        }
        if (stagingArea.get().add(file)) {
            stagingArea.get().save();
        }
    }

    /**
     * Get a File instance from CWD by the name.
     * @param fileName Name of the file
     * @return File instance
     */
    private static File getFileFromCWD(String fileName) {
        return Paths.get(fileName).isAbsolute()
                ? new File(fileName)
                : join(CWD, fileName);
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getBranchHeadCommit(branchHeadFile);
    }

}
