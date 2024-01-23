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
     * ���ɵ�gitlet��·����
     * [user.dir]
     * ������ .gitlet
     *     ������ refs
     *     ��   ������ heads
     *     ������ objects
     */
    // ����Java��������ʱ��·��
    private static final File CWD = new File(System.getProperty("user.dir"));
    //  .gitlet·��,�汾�������ڵ�·��
    private static final File GITLET_DIR = join(CWD, ".gitlet");
    // refs(�洢��֧��ĩ����Ϣ��������heads�ļ��У��洢��֧�������ύ)
    private static final File REFS_DIR = join(GITLET_DIR, "refs");
    // gitlet�����object�ļ��У���������object����ĳ־û���blob��commit��
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    // �� refs�£��洢������֧��ǰ��commit
    private static final File BRANCH_HEADS_DIR = join(REFS_DIR, "heads");
    // Ĭ�Ϸ�֧ : master
    private static final String DEFAULT_BRANCH_NAME = "master";
    // .gitlet �����HEAD�ļ�����¼��ǰ���ύ�汾
    private static final File HEAD = join(GITLET_DIR, "HEAD");
    // ��¼refs/heads/ ·��
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
     *  lazy �̳���Supplier<T>�ӿڣ����ӳٷ���ǰ�����˽�һ���ķ�װ
     *  ����lazy ����һ�� Supplier<T>�ӿڶ������Ǹ�����ʽ�ӿ������ӳټ��أ��������lambda��Ϊ��������ʵ��
     *  ����lambda�ĺ�����װΪdelegate,�Թ���������
     */
    private final Lazy<StagingArea> stagingArea = lazy(() -> {
        StagingArea s = INDEX.exists()
                ? StagingArea.fromFile()
                : new StagingArea();
        s.setTracked(HEADCommit.get().getTracked());
        return s;
    });


    public static void init() {
        // ��ʼ��Ŀ¼
        if (GITLET_DIR.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
        mkdir(GITLET_DIR);
        mkdir(REFS_DIR);
        mkdir(BRANCH_HEADS_DIR);
        mkdir(OBJECTS_DIR);
        // ���õ�ǰ��֧Ϊmaster
        setCurrentBranch(DEFAULT_BRANCH_NAME);
        // ���õ�ǰ��֧�ĵ�ǰcommitΪinit
        createInitialCommit();
    }

    private static void setCurrentBranch(String branchName) {
        // HEAD�ļ���¼��ǰ��֧�����Է�֧��д��HEAD�ļ�����ʵ���л�
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    private static void createInitialCommit() {
        // ��ʼ��һ��commit
        Commit initialCommit = new Commit();
        // ����commit������
        initialCommit.save();
        // ����commit��id�ͷ�֧���֣����÷�֧ͷ
        setBranchHeadCommit(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    private static void setBranchHeadCommit(String branchName, String commitId) {
        // �õ���ǰ��֧��ͷ�ļ�
        File branchHeadFile = getBranchHeadFile(branchName);
        // commitд��ͷ�ļ����������õ�ǰ��֧��commit��
        setBranchHeadCommit(branchHeadFile, commitId);
    }

    /**
     * ��ȡbranchName
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
     * ���gitlet�����ڻ��߲����ļ��о��˳�
     */
    public static void checkWorkingDir() {
        if (!GITLET_DIR.exists() || !GITLET_DIR.isDirectory()) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * ����ļ����ݴ���
     * @param fileName �ļ���
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
