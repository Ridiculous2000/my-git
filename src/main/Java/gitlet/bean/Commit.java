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
    // �ύ��data
    private final Date date;
    // �ύʱ����Ϣ
    private final String message;
    // ���ύ�� SHA1 ��ϣ�б�
    private final List<String> parents;
    // SHA1��id
    private final String id;
    // commit�ļ�¼�ļ���·���� SHA1 ��ϣ����
    private final File file;
    // ���ٵ��ļ�ӳ�䣬��Ϊ�ļ�·����ֵΪ�ļ��� SHA1 ��ϣ
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
     * ��ʼ���ύ����ʼ�����ڣ�message����Ϣ��
     */
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
     * ���� Commit ʵ�����浽�����ļ�����
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
