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
    // �ύ��data
    private final Date date;
    // �ύʱ����Ϣ
    private final String message;
    // ���ύ�� SHA1 ��ϣ�б�
    private final List<String> parents;
    // SHA1��id
    private final String id;
    // commit��Object�ļ���·���� SHA1 ��ϣ����
    private final File file;
    // Commit���ٵ��ļ�ӳ�䣬��Ϊ�ļ�·����ֵΪ�ļ��� SHA1 ��ϣ
    private final Map<String, String> tracked;

    // �����ύ
    public Commit(String message, List<String> parents, Map<String, String> trackedFilesMap) {
        date = new Date();
        this.message = message;
        this.parents = parents;
        this.tracked = trackedFilesMap;
        id = generateId();
        file = getObjectFile(id);
    }

    // ��ʼ���ύ����ʼ�����ڣ�message����Ϣ
    public Commit() {
        date = new Date(0);
        message = "initial commit";
        parents = new ArrayList<>();
        tracked = new HashMap<>();
        // ������Ϣ����id
        id = generateId();
        // ����id������objectĿ¼�¶�Ӧ��commit����û�������̻���
        file = getObjectFile(id);
    }

    /**
     * ���� SHA1 ��ϣ������ʱ�������Ϣ�����ύ�б�͸����ļ�ӳ��
     * @return SHA1 id
     */
    private String generateId() {
        return sha1(getTimestamp(), message, parents.toString(), tracked.toString());
    }

    /**
     * �� SHA1 ��ϣ��ȡ Commit ʵ��
     * @param id SHA1 id
     * @return Commit instance
     */
    public static Commit fromFile(String id) {
        return readObject(getObjectFile(id), Commit.class);
    }

    // ���� Commit ʵ�����浽�����ļ�����
    public void save() {
        saveObjectFile(file, this);
    }

    // ����Commit��������
    public Date getDate() {
        return date;
    }

    // ���ظ�ʽ��ʱ�� ��Thu Jan 1 00:00:00 1970 +0000��
    public String getTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        return dateFormat.format(date);
    }

    // ��ȡ�ύ��Ϣ
    public String getMessage() {
        return message;
    }

    // ���ظ��ύ�б�
    public List<String> getParents() {
        return parents;
    }

    // ����Commit���ٵ��ļ�ӳ��
    public Map<String, String> getTracked() {
        return tracked;
    }

    // ���ض�Ӧ��sha1 ID
    public String getId() {
        return id;
    }

    /**
     * ���� commit ��־���������ָ�������ǰcommit��id��������date��commit��message
     * �����merge�ύ����Ҫ�����ӡparents��Ϣ������merge�ò��Ŵ�ӡparent����Ϊ��һ������parent����Ϣ�ˣ�
     * @return Log content
     */
    public String getLog() {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("===").append("\n");
        logBuilder.append("commit").append(" ").append(id).append("\n");
        // ���ںϲ��ύ����ʾ���и��ύ�Ĺ�ϣ
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
     * �����ļ�·���ָ����ٵ��ļ���
     * @param filePath Path of the file
     * @return true if file exists in commit
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean restoreTracked(String filePath) {
        // commit��û�м�¼�ͷ���false
        String blobId = tracked.get(filePath);
        if (blobId == null) {
            return false;
        }
        //����Id�õ�Blob���󲢻ָ�
        Blob.fromFile(blobId).writeContentToSource();
        return true;
    }

    /**
     * �ָ����и��ٵ��ļ������������ļ�
     */
    public void restoreAllTracked() {
        for (String blobId : tracked.values()) {
            Blob.fromFile(blobId).writeContentToSource();
        }
    }


}
