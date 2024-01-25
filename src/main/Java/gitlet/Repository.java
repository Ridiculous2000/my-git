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

// gitlet �洢�⣬ʵ��gitlet�Ĵ󲿷��߼�
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
    // ��¼ "ref: refs/heads/" ����һ���̶�ǰ׺
    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";
    // index�ļ������ڳ־û��ݴ�������
    public static final File INDEX = join(GITLET_DIR, "index");
    // lazy�ǵ���ģʽ��װ���Supplier���ⲿ�ֶ�ȡ�ļ����ݣ�ȥ��ǰ׺��eg: ref: refs/heads/master => master
    private final Lazy<String> currentBranch = lazy(() -> {
        String HEADFileContent = readContentsAsString(HEAD);
        return HEADFileContent.replace(HEAD_BRANCH_REF_PREFIX, "");
    });

    // �õ��������ڵ������ļ�
    private static final Lazy<File[]> currentFiles = lazy(() -> CWD.listFiles(File::isFile));



    // Head�ļ��洢���ǵ�ǰ��֧������HEADComit���Ը��ݵ�ǰ��֧���������ǰ��֧��Commit
    private final Lazy<Commit> HEADCommit = lazy(() -> getBranchHeadCommit(currentBranch.get()));
    /**
     *  lazy �̳���Supplier<T>�ӿڣ����ӳٷ���ǰ�����˽�һ���ķ�װ
     *  ����lazy ����һ�� Supplier<T>�ӿڶ������Ǹ�����ʽ�ӿ������ӳټ��أ��������lambda��Ϊ��������ʵ��
     *  ����lambda�ĺ�����װΪdelegate,�Թ���������
     *  �ⲿ��ʵ�ֵ��ǰ�
     */
    private final Lazy<StagingArea> stagingArea = lazy(() -> {
        StagingArea s = INDEX.exists()
                ? StagingArea.fromFile()
                : new StagingArea();
        s.setTracked(HEADCommit.get().getTracked());
        return s;
    });

    // ��ʼ�� .gitlet ��
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

    // ���õ�ǰ��֧��д��HEAD�ļ����ɣ�Ϊ�����ţ���һ��ǰ׺��
    private static void setCurrentBranch(String branchName) {
        // HEAD�ļ���¼��ǰ��֧�����Է�֧��д��HEAD�ļ�����ʵ���л�
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    // ������ʼ���ύ
    private static void createInitialCommit() {
        // ��ʼ��һ��commit
        Commit initialCommit = new Commit();
        // ����commit������
        initialCommit.save();
        // ����commit��id�ͷ�֧���֣����÷�֧ͷ
        setBranchHeadCommit(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    // ����branchName ��ǰ��commit Ϊ commitId
    private static void setBranchHeadCommit(String branchName, String commitId) {
        // �õ���ǰ��֧��ͷ�ļ�
        File branchHeadFile = getBranchHeadFile(branchName);
        // commitд��ͷ�ļ����������õ�ǰ��֧��commit��
        setBranchHeadCommit(branchHeadFile, commitId);
    }

    /**
     * ��ȡbranchName��Ӧ��branch file
     * @param branchName Name of the branch
     * @return File instance
     */
    private static File getBranchHeadFile(String branchName) {
        return join(BRANCH_HEADS_DIR, branchName);
    }

    /**
     * ���� branchHead�����branch��Ӧ��Commit�� ��¼�����ݣ�ת����commitId��Ȼ���õ���Ӧ��Commit�ļ�
     * @param branchHeadFile File instance
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(File branchHeadFile) {
        String HEADCommitId = readContentsAsString(branchHeadFile);
        return Commit.fromFile(HEADCommitId);
    }

    /**
     * ��commitIdд���ӦbranchHeadFile�ļ�
     * @param branchHeadFile File instance
     * @param commitId       Commit SHA1 id
     */
    private static void setBranchHeadCommit(File branchHeadFile, String commitId) {
        writeContents(branchHeadFile, commitId);
    }

    // ���gitlet�����ڻ��߲����ļ��о��˳�
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
        // ����filename��Ӧ��file���������ж�file�Ƿ����
        File file = getFileFromCWD(fileName);
        if (!file.exists()) {
            exit("File does not exist.");
        }
        // ���ļ���ӵ��ݴ���
        if (stagingArea.get().add(file)) {
            // �ݴ����־û������浽index�ļ���
            stagingArea.get().save();
        }
    }

    /**
     * ���� filename �õ�һ�� file ������������·���ͽ��һ�¹���Ŀ¼������·����ֱ�Ӵ�����
     * @param fileName Name of the file
     * @return File instance
     */
    private static File getFileFromCWD(String fileName) {
        return Paths.get(fileName).isAbsolute()
                ? new File(fileName)
                : join(CWD, fileName);
    }

    /**
     * �õ� branchName ��Ӧ�����µ� Commit
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getBranchHeadCommit(branchHeadFile);
    }

    // ����commit
    public void commit(String msg) {
        commit(msg, null);
    }

    /**
     * ������Ϣ��secondParent����commit��Ĭ�ϵ�ǰcommitΪparent��
     * �����merge��������parent����Ҫ����ǿյ�secondParent
     * @param msg          Commit message
     * @param secondParent Second parent Commit SHA1 id
     */
    private void commit(String msg, String secondParent) {
        // �ڼ�û�и��ľ��˳�
        if (stagingArea.get().isClean()) {
            exit("No changes added to the commit.");
        }
        // �ύ�ݴ����仯�����µõ���ǰ״̬�Ŀ���
        Map<String, String> newTrackedFilesMap = stagingArea.get().commit();
        // �־û��ݴ�������
        stagingArea.get().save();
        // ��ǰ��֧��commit��Ϊparent
        List<String> parents = new ArrayList<>();
        parents.add(HEADCommit.get().getId());
        if (secondParent != null) {
            parents.add(secondParent);
        }
        // ������Ϣ��parents����ǰ���� ����commit
        Commit newCommit = new Commit(msg, parents, newTrackedFilesMap);
        // ����commit
        newCommit.save();
        // ���µ�ǰ��֧������commit
        setBranchHeadCommit(currentBranch.get(), newCommit.getId());
    }


    /**
     * ɾ���ļ�
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
     * �Ա�git�е����"git log --first-parent"
     * �ӵ�ǰ��֧�ĵ�ǰcommit��ʼ����ǰ����first-parent(���Ժ����parent),��ӡcommit��Ϣ
     * eg��
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
        // �ӵ�ǰcommit��ʼ��ǰ��ӡ
        Commit currentCommit = HEADCommit.get();
        while (true) {
            // ���commit��־��Ϣ
            logBuilder.append(currentCommit.getLog()).append("\n");
            // ˳��parent������ֻ��first parent��
            List<String> parentCommitIds = currentCommit.getParents();
            if (parentCommitIds.size() == 0) {
                break;
            }
            String firstParentCommitId = parentCommitIds.get(0);
            // ����parentid ���س���commit ����
            currentCommit = Commit.fromFile(firstParentCommitId);
        }
        System.out.print(logBuilder);
    }


     // ����date˳���ӡ����commit����Ϣ
    public static void globalLog() {
        StringBuilder logBuilder = new StringBuilder();
        // ����һ��function������commitΪ��������commit��log���뵽logBuilder��
        forEachCommitInOrder(commit -> logBuilder.append(commit.getLog()).append("\n"));
        System.out.print(logBuilder);
    }

    /**
     * ���ڴ������ڱ���commit
     * @param cb ����CommitΪ�����ĺ�������װΪConsumer�ӿڣ�
     */
    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        // ʱ������
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        // �������ȶ��У������ݸö�����ص�����
        Queue<Commit> commitsPriorityQueue = new PriorityQueue<>(commitComparator);
        forEachCommit(cb, commitsPriorityQueue);
    }

    /**
     * �������commit
     * @param cb ����CommitΪ�����ĺ�������װΪConsumer�ӿڣ�
     */
    private static void forEachCommit(Consumer<Commit> cb) {
        // ��ͨ����
        Queue<Commit> commitsQueue = new ArrayDeque<>();
        forEachCommit(cb, commitsQueue);
    }

    /**
     * ����commit������|����
     * @param cb                 ����commit��callback����
     * @param queueToHoldCommits commit�Ķ��У����ȶ���|��ͨ���� ���ɣ�
     */
    @SuppressWarnings("ConstantConditions")
    private static void forEachCommit(Consumer<Commit> cb, Queue<Commit> queueToHoldCommits) {
        // set����ȥ��
        Set<String> checkedCommitIds = new HashSet<>();
        // ������֧���ļ�����¼�÷�֧��ǰ��commitId��
        File[] branchHeadFiles = BRANCH_HEADS_DIR.listFiles();
        // ��֧����name����
        Arrays.sort(branchHeadFiles, Comparator.comparing(File::getName));
        // �ȰѸ�����֧��ͷcommit������У��ڼ���setȥ�أ�
        for (File branchHeadFile : branchHeadFiles) {
            String branchHeadCommitId = readContentsAsString(branchHeadFile);
            if (checkedCommitIds.contains(branchHeadCommitId)) {
                continue;
            }
            checkedCommitIds.add(branchHeadCommitId);
            Commit branchHeadCommit = Commit.fromFile(branchHeadCommitId);
            queueToHoldCommits.add(branchHeadCommit);
        }
        // ������֧��parent�ϱ���
        while (true) {
            Commit nextCommit = queueToHoldCommits.poll();
            // acceptΪ Consumer �ӿڵĺ�����������commit����log
            cb.accept(nextCommit);
            List<String> parentCommitIds = nextCommit.getParents();
            if (parentCommitIds.size() == 0) {
                break;
            }
            //�Ѹ�commit���ӽ�������֮ǰȥ�أ�
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
     * ��ӡ����������message��ͬ��Commit��ID
     * @param msg commit��message
     */
    public static void find(String msg) {
        StringBuilder resultBuilder = new StringBuilder();
        // ��ѯ����Ҫ������ͨ�б���
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
     * ��ӡ���з�֧����ǰ��֧ ��*�� ���أ���ӡ�������ݣ�
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
        // ��ӡ��֧��Ϣ
        statusBuilder.append("=== Branches ===").append("\n");
        // ��ǰ��֧ * ����
        statusBuilder.append("*").append(currentBranch.get()).append("\n");
        // ��ӷǵ�ǰ��֧��name����
        String[] branchNames = BRANCH_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch.get()));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");

        // �õ��ݴ��� added �� removed ������
        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();

        // �����ļ���˳�����added���������
        statusBuilder.append("=== Staged Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, addedFilesMap.keySet());
        statusBuilder.append("\n");

        // �����ļ���˳�����removed���������
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        statusBuilder.append("\n");

        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        // �õ�commit����Ȼ������˵���û�б��浽�ݴ������ļ�
        List<String> modifiedNotStageFilePaths = new ArrayList<>();
        // �õ�commit����û��rmɾ���ļ������ļ��Լ����ӹ�����ɾ�����ļ�
        Set<String> deletedNotStageFilePaths = new HashSet<>();

        // ��ȡ��ǰ�������ļ� <path,id>
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        // ���added��remove�����ݸ���tracked���õ��ݴ������ڵĸ��ٵ��ļ���״̬
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();
        trackedFilesMap.putAll(addedFilesMap);
        for (String filePath : removedFilePathsSet) {
            trackedFilesMap.remove(filePath);
        }
        // �������ٵ��ļ�
        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String blobId = entry.getValue();

            // ���Ի�ȡ��ǰ�ļ�id
            String currentFileBlobId = currentFilesMap.get(filePath);
            // �������Լ�û������ļ� ��Ϊnull
            if (currentFileBlobId != null) {
                // �����˵���Ĺ��ˣ��������� �ݴ���|commit ������
                if (!currentFileBlobId.equals(blobId)) {
                    modifiedNotStageFilePaths.add(filePath);
                }
                // �������Ͱ��ݴ��������˾�ɾ��������ѭ����ʣ�µľ������Ҫ�õģ�û����ӵ��ݴ����Ĺ������ļ���
                currentFilesMap.remove(filePath);
            } else {
                // �ݴ���|tracked ���У��������Լ�ɾ���ˣ���¼����
                modifiedNotStageFilePaths.add(filePath);
                deletedNotStageFilePaths.add(filePath);
            }
        }

        modifiedNotStageFilePaths.sort(String::compareTo);

        // ���ŵ�д��������deleted�еĴ�deleted��ǩ��modified��deletedû�еĴ�modified��
        // �����˸о�modifiedֻ��modifiedҲ��ȫûë������Ȼ�������ţ�����ѭ����Ҫ���Σ����������׶�
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

        // û����ӵ��ݴ����Ĺ������ļ���
        statusBuilder.append("=== Untracked Files ===").append("\n");
        // ǰ���Ѿ�˳�㴦��currentFilesMap�ˣ�ֱ������д��ͺ���
        appendFileNamesInOrder(statusBuilder, currentFilesMap.keySet());
        statusBuilder.append("\n");

        System.out.print(statusBuilder);
    }

    // ����˳���fileName��ӵ�stringBuilder����
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, Collection<String> filePathsCollection) {
        List<String> filePathsList = new ArrayList<>(filePathsCollection);
        appendFileNamesInOrder(stringBuilder, filePathsList);
    }

    // ����˳���fileName��ӵ�stringBuilder����
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, List<String> filePathsList) {
        filePathsList.sort(String::compareTo);
        for (String filePath : filePathsList) {
            String fileName = Paths.get(filePath).getFileName().toString();
            stringBuilder.append(fileName).append("\n");
        }
    }

    // ������ǰ���ļ��б�����ӳ�䣬key��filepath��value��blobId
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
     * ����ָ����commitid��ǰ׺�ָ�blob�ļ�
     * @param commitId ָ����commitid
     * @param fileName Ҫ�ָ����ļ���
     */
    public void checkout(String commitId, String fileName) {
        // ���ݴ����commitidǰ׺�õ�������commitId�������û�ʹ�ã�����ÿ�ζ���һ��40λ��id�ˣ��������4λ��ǰ׺���ɣ�
        commitId = getActualCommitId(commitId);
        String filePath = getFileFromCWD(fileName).getPath();
        // ����ֱ�ӻָ�������һ������commitId�õ���Ӧcommit�Ĳ���
        if (!Commit.fromFile(commitId).restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * ����commit �ָ��ļ�
     * @param fileName Name of the file
     */
    public void checkout(String fileName) {
        // �ļ���ƴ�ճɾ���·��
        String filePath = getFileFromCWD(fileName).getPath();
        // ���Ը���tracked��¼�ָ��ļ����ݣ������ھ�exit��
        if (!HEADCommit.get().restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * ǩ����ָ���ķ�֧
     * @param targetBranchName Ŀ���֧������
     */
    public void checkoutBranch(String targetBranchName) {
        // �õ���Ӧbranch��heads�ļ�
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("No such branch exists.");
        }
        // Ŀ���֧����ǰ��֧һ��
        if (targetBranchName.equals(currentBranch.get())) {
            exit("No need to checkout the current branch.");
        }
        // �õ�Ŀ���֧�ĵ�ǰcommit
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        // �жϵ�ǰ�ݴ���Ҫ���ٵ��ļ��Ƿ���δ����ĸ��ģ��о��˳�
        checkUntracked(targetBranchHeadCommit);
        // û�о͸��¹���Ŀ¼����Ϊcommit������
        checkoutCommit(targetBranchHeadCommit);
        // ���õ�ǰ��֧���ѷ�֧��д��HEAD��
        setCurrentBranch(targetBranchName);
    }

    /**
     * ���ݴ���Ĳ���id����ȡ������id���Ӷ����û����������ô���������40λid�ˣ�
     * @param commitId �û������id(�����������ģ�Ҳ�����Ǵ���4λ��ǰ׺)
     * @return ��������commitid
     */
    @SuppressWarnings("ConstantConditions")
    private static String getActualCommitId(String commitId) {
        // ����commitid������
        if (commitId.length() < UID_LENGTH) {
            // ̫����
            if (commitId.length() < 4) {
                exit("Commit id should contain at least 4 characters.");
            }
            // �õ�id��Ӧ���������ļ��У�����idǰ2λ�����ģ�
            String objectDirName = getObjectDirName(commitId);
            File objectDir = join(OBJECTS_DIR, objectDirName);
            // �����ڣ�˵������ǰNλ������
            if (!objectDir.exists()) {
                exit("No commit with that id exists.");
            }
            boolean isFound = false;
            // ǰ��λ���ļ��У�ʣ�µ�Ϊ�ļ�����ǰ׺
            String objectFileNamePrefix = getObjectFileName(commitId);
            // �����ʼۼ�
            for (File objectFile : objectDir.listFiles()) {
                String objectFileName = objectFile.getName();
                // ǰ׺�Ե��ϣ�������commit����
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
            // ����������commit����������
            if (!getObjectFile(commitId).exists()) {
                exit("No commit with that id exists.");
            }
        }
        return commitId;
    }

    /**
     * ���Ŀ���ύ������δ���ٵ��ļ������˳�����ʾ��Ϣ
     * @param targetCommit Commit SHA1 id
     */
    private void checkUntracked(Commit targetCommit) {
        // ��ǰ�������ļ�
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        // �ݴ�����Ϣ
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();
        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();
        // ���ٵ��ļ�����û�б���ĸ���
        List<String> untrackedFilePaths = new ArrayList<>();
        for (String filePath : currentFilesMap.keySet()) {
            // ���ļ�����
            if (trackedFilesMap.containsKey(filePath)) {
                // ���ڸ���(addҲ��Ҫ�жϰɣ�)
                if (removedFilePathsSet.contains(filePath)||addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            } else {
                // ����û���ٵ�add�ˣ�Ҳ�����û�׼������
                if (addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            }
        }

        // �õ�Ŀ��commit������ļ�
        Map<String, String> targetCommitTrackedFilesMap = targetCommit.getTracked();
        // ���Ŀ��commit���ļ���id�͵�ǰû������ļ���id��ͬ��˵��ǩ����֧�Ḳ��δ������ύ
        for (String filePath : untrackedFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String targetBlobId = targetCommitTrackedFilesMap.get(filePath);
            if (!blobId.equals(targetBlobId)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     * ����һ���·�֧
     * @param newBranchName �µķ�֧��
     */
    public void branch(String newBranchName) {
        // �� refs/heads �´�����Ӧ�� branchName �ļ�
        File newBranchHeadFile = getBranchHeadFile(newBranchName);
        if (newBranchHeadFile.exists()) {
            exit("A branch with that name already exists.");
        }
        // �õ���ǰcommit����Ϊ��branch�ĳ�ʼcommit��д��head�ļ���
        setBranchHeadCommit(newBranchHeadFile, HEADCommit.get().getId());
    }

    /**
     * ǩ����Ŀ��commit
     * @param targetCommit Ŀ��commit
     */
    private void checkoutCommit(Commit targetCommit) {
        // ����ݴ�����ǰ���жϹ��ˣ�û��δ����ĸ��ģ�
        stagingArea.get().clear();
        // ��¼�ݴ���
        stagingArea.get().save();
        // ɾ�������������ļ�
        for (File file : currentFiles.get()) {
            rm(file);
        }
        // �ָ�commit�е������ļ�
        targetCommit.restoreAllTracked();
    }

    /**
     * ɾ����֧
     * @param targetBranchName Ҫɾ����Ŀ���֧����
     */
    public void rmBranch(String targetBranchName) {
        // �õ���֧HeadFile
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot remove the current branch.");
        }
        // ɾ����֧�ļ�
        rm(targetBranchHeadFile);
    }

    /**
     * ���µ�ǰ����ΪcommitId��Ӧ�İ汾����
     * @param commitId Ŀ��commitId
     */
    public void reset(String commitId) {
        // ��ȡ����commitId��ǰ׺��ȫ��У�飩
        commitId = getActualCommitId(commitId);
        // ����commitId�õ�targetComit
        Commit targetCommit = Commit.fromFile(commitId);
        // У���Ƿ���ڸ����ύ
        checkUntracked(targetCommit);
        // ǩ����Ŀ��commit�����Ĺ��������ݣ�
        checkoutCommit(targetCommit);
        // ���õ�ǰcommit��д��Heads��
        setBranchHeadCommit(currentBranch.get(), commitId);
    }

    /**
     * �ϲ���֧��8�������������master��other,master�ֳ�һ��other��֧��master��other������֧�е��ļ������������޸ģ�
     * ����������8�����
     * ��1��master���ļ�����otherû�䣨2��masterû��other���ˣ��Ա仯�����°�Ϊ׼
     * ��3��master��other ��û���Ķ�һ�����ǾͲ��þ�����
     * ��4��ohter��master�����ˣ����ĵĲ�һ������ʾ��ͻ
     * ��5��master������ļ�ɾ�ˣ�otherû�ģ�˵��master���µĲ������ϲ���Ҳɾ����
     * ��6��otherɾ����masterû�ģ�ͬ�ϣ��ϲ���Ҳɾ��
     * ��7��otherɾ�ˣ�master�Ķ��ˣ��϶�����Ķ������ף�����Ķ�����ļ�
     * ��8��masterɾ�ˣ�other���ˣ�ͬ��
     * @param targetBranchName Ҫ�ϲ�����ǰ��֧�Ļ�
     */
    public void merge(String targetBranchName) {
        // �õ�Ҫ�ϲ��ķ�֧
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        // ��֧������
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        // ����ǰ��֧��һ����֧
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot merge a branch with itself.");
        }
        // ��û������ύ
        if (!stagingArea.get().isClean()) {
            exit("You have uncommitted changes.");
        }
        // �õ�Ŀ��commit
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        // У���Ƿ���δ�ύ�ķ�֧
        checkUntracked(targetBranchHeadCommit);
        // �õ���ͬ������commit
        Commit lcaCommit = getLatestCommonAncestorCommit(HEADCommit.get(), targetBranchHeadCommit);
        String lcaCommitId = lcaCommit.getId();
        // ���target��current�����ȵĻ����ҵ���lca����target�����ֲ���Ҫ�ϲ���current�������°汾
        if (lcaCommitId.equals(targetBranchHeadCommit.getId())) {
            exit("Given branch is an ancestor of the current branch.");
        }
        // ���current��target�����ȵĻ����ҵ���lca����current�������൱��checkout��target
        if (lcaCommitId.equals(HEADCommit.get().getId())) {
            checkoutCommit(targetBranchHeadCommit);
            setCurrentBranch(targetBranchName);
            exit("Current branch fast-forwarded.");
        }

        boolean hasConflict = false;
        // ��ǰcommit���ļ����ա�Ŀ��commit���ļ����ա���ͬ���ȵ��ļ�����
        Map<String, String> HEADCommitTrackedFilesMap = new HashMap<>(HEADCommit.get().getTracked());
        Map<String, String> targetBranchHeadCommitTrackedFilesMap = targetBranchHeadCommit.getTracked();
        Map<String, String> lcaCommitTrackedFilesMap = lcaCommit.getTracked();

        for (Map.Entry<String, String> entry : lcaCommitTrackedFilesMap.entrySet()) {
            // �õ����ȿ���ԭʼ�汾�ļ�
            String filePath = entry.getKey();
            File file = new File(filePath);
            String blobId = entry.getValue();
            // �õ�target��current�İ汾
            String targetBranchHeadCommitBlobId = targetBranchHeadCommitTrackedFilesMap.get(filePath);
            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(filePath);
            // target����������汾���ļ�
            if (targetBranchHeadCommitBlobId != null) {
                // target�иĶ�
                if (!targetBranchHeadCommitBlobId.equals(blobId)) {
                    // HeadҲ������ļ�
                    if (HEADCommitBlobId != null) {
                        // Headû�иĶ�
                        if (HEADCommitBlobId.equals(blobId)) {
                            // ��¼target�İ汾
                            Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                            stagingArea.get().add(file);
                        } else {
                            // HeadҲ�иĶ������Ķ���һ��
                            if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) {
                                // ˵���г�ͻ
                                hasConflict = true;
                                // �õ���ͻ�����ݲ�д���ļ�
                                String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                                writeContents(file, conflictContent);
                                stagingArea.get().add(file);
                            }
                        }
                    } else {
                        // current ��û������ļ����Ѿ�ɾ����
                        hasConflict = true;
                        // д��target�����ݼ���
                        String conflictContent = getConflictContent(null, targetBranchHeadCommitBlobId);
                        writeContents(file, conflictContent);
                        stagingArea.get().add(file);
                    }
                }
            } else {
                // target��û������汾������
                // current��������汾
                if (HEADCommitBlobId != null) {
                    if (HEADCommitBlobId.equals(blobId)) {
                        // currentû���޸ģ�˵��ɾ�����²�����ɾ���˾���
                        stagingArea.get().remove(file);
                    } else {
                        // current���ˣ�����current����
                        hasConflict = true;
                        String conflictContent = getConflictContent(HEADCommitBlobId, null);
                        writeContents(file, conflictContent);
                        stagingArea.get().add(file);
                    }
                }
            }
            // �ϲ��������
            HEADCommitTrackedFilesMap.remove(filePath);
            targetBranchHeadCommitTrackedFilesMap.remove(filePath);
        }

        // ����������û�У�����current��target�������ļ���current��targetû�в��õ��ģ���Ϊ���ֲ���Ҫ���⴦�����Ա���target���ɣ�
        for (Map.Entry<String, String> entry : targetBranchHeadCommitTrackedFilesMap.entrySet()) {
            String targetBranchHeadCommitFilePath = entry.getKey();
            File targetBranchHeadCommitFile = new File(targetBranchHeadCommitFilePath);
            String targetBranchHeadCommitBlobId = entry.getValue();

            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(targetBranchHeadCommitFilePath);
            // ���߶����������ļ�
            if (HEADCommitBlobId != null) {
                // �޸ĳ�ͻ
                if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) {
                    hasConflict = true;
                    String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                    writeContents(targetBranchHeadCommitFile, conflictContent);
                    stagingArea.get().add(targetBranchHeadCommitFile);
                }
                // ����˵���޸ķ�ʽһ�£����ù��˾�
            } else {
                // target��currentû��
                // ��ӽ�������
                Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                stagingArea.get().add(targetBranchHeadCommitFile);
            }
        }

        String newCommitMessage = "Merged" + " " + targetBranchName + " " + "into" + " " + currentBranch.get() + ".";
        commit(newCommitMessage, targetBranchHeadCommit.getId());
        // ��ʾ�û����merge�ĳ�ͻ
        if (hasConflict) {
            message("Encountered a merge conflict.");
        }
    }

    /**
     * �õ�����commit��ͬ���ȵ�commit��������֧ԭ������һ����֧�ֻ��ģ��ҵ���ͬ���Ȳ�֪���ֻ��������ʲô�仯��
     * @param commitA Commit instance
     * @param commitB Commit instance
     * @return Commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private static Commit getLatestCommonAncestorCommit(Commit commitA, Commit commitB) {
        // ʱ����������ȶ���
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        Queue<Commit> commitsQueue = new PriorityQueue<>(commitComparator);
        commitsQueue.add(commitA);
        commitsQueue.add(commitB);
        // ��ʼ��һ�����ϣ������洢�Ѿ��������ύ��ID
        Set<String> checkedCommitIds = new HashSet<>();
        while (true) {
            // ������������commit
            Commit latestCommit = commitsQueue.poll();
            List<String> parentCommitIds = latestCommit.getParents();
            String firstParentCommitId = parentCommitIds.get(0);
            Commit firstParentCommit = Commit.fromFile(firstParentCommitId);
            // �Ѿ�������checked����Ļ���˵���ҵ���ͬ������
            if (checkedCommitIds.contains(firstParentCommitId)) {
                return firstParentCommit;
            }
            commitsQueue.add(firstParentCommit);
            checkedCommitIds.add(firstParentCommitId);
        }
    }

    /**
     * merge�������ĳ�ͻʱ���õ���ͻ������
     * @param currentBlobId ��ǰcommitid
     * @param targetBlobId  Ŀ��commitid
     * @return New content
     */
    private static String getConflictContent(String currentBlobId, String targetBlobId) {
        // Head������
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<<<<<<< HEAD").append("\n");
        if (currentBlobId != null) {
            Blob currentBlob = Blob.fromFile(currentBlobId);
            contentBuilder.append(currentBlob.getContentAsString());
        }
        // target������
        contentBuilder.append("=======").append("\n");
        if (targetBlobId != null) {
            Blob targetBlob = Blob.fromFile(targetBlobId);
            contentBuilder.append(targetBlob.getContentAsString());
        }
        contentBuilder.append(">>>>>>>");
        return contentBuilder.toString();
    }



}
