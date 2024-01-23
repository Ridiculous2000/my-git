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
    // ����ļ����������ļ�·����ֵ���ļ��� SHA1 ��ϣ
    private final Map<String, String> added = new HashMap<>();
    // ��ɾ���ļ������洢�ļ�·��
    private final Set<String> removed = new HashSet<>();
    // ���ٵ��ļ�ӳ�䣬�����ļ�·����ֵ���ļ��� SHA1 ��ϣ
    private transient Map<String, String> tracked;

    /**
     * ��̬�������� INDEX �ļ���ȡ StagingArea ʵ��
     * @return StagingArea ����
     */
    public static StagingArea fromFile() {
        return readObject(Repository.INDEX, StagingArea.class);
    }

    // ���ø��ٵ��ļ�ӳ��
    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

    /**
     * ���ļ���ӵ��ݴ������ⲿ��Ҫ��������ۣ�
     * ��1���û�����|�������ļ���Ȼ���ύ�����°� ������blob�ļ���Ȼ��put��added�����ɣ�����true
     * ��2��֮ǰadd��һ��һģһ���ģ�����false
     * ��3���û�֮ǰ�� a.txt ����(eg:123)���ˣ�Ȼ���޸ĳ���(123456)��add�����ݴ�����û��commit
     * �����ָĻ���(123),�Ǿ�û��Ҫ�����blob��ֱ�Ӱ��ݴ������м�汾ɾ���˾ͺ��ˡ�Ȼ�󷵻�ture
     * ��4���û�֮ǰ�� a.txt ���ˣ�Ȼ����� rm ɾ���ˣ������û��ִ��������ˣ�����add��ʱ��Ҫ��ԭ����ɾ����¼ɾ�˷�ֹ��ɾ������true
     * @param file Ҫ��ӵ��ļ�
     * @return true �Ƿ���ӳɹ�
     */
    public boolean add(File file) {
        String filePath = file.getPath();
        // �����ļ� blob ����
        Blob blob = new Blob(file);
        String blobId = blob.getId();
        // �ļ��Ƿ񱻸��٣�commit����
        String trackedBlobId = tracked.get(filePath);
        if (trackedBlobId != null) {
            // ���ڽ����ļ���֮ǰcommit���ļ���ȫһ�£�û��Ҫ�ύ�ˣ���Ӧ�ð��ݴ�����ر仯ɾ��
            if (trackedBlobId.equals(blobId)) {
                // ɾ�� add �ı仯
                if (added.remove(filePath) != null) {
                    return true;
                }
                // ɾ�� remove ��
                return removed.remove(filePath);
            }
        }
        // û��commit������֮ǰ��add��һģһ����
        String prevBlobId = added.put(filePath, blobId);
        if (prevBlobId != null && prevBlobId.equals(blobId)) {
            return false;
        }
        // ����blob�ļ�
        if (!blob.getFile().exists()) {
            blob.save();
        }
        return true;
    }

    // �ݴ������ݳ־û���index�ļ�
    public void save() {
        writeObject(Repository.INDEX, this);
    }

}
