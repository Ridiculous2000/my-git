package gitlet.bean;

import gitlet.Repository;

import java.io.Serializable;
import java.util.Map;

import static gitlet.util.Utils.readObject;

public class StagingArea implements Serializable {

    /**
     * ���ٵ��ļ�ӳ�䣬�����ļ�·����ֵ���ļ��� SHA1 ��ϣ
     */
    private transient Map<String, String> tracked;

    /**
     * ��̬�������� INDEX �ļ���ȡ StagingArea ʵ��
     *
     * @return StagingArea instance
     */
    public static StagingArea fromFile() {
        return readObject(Repository.INDEX, StagingArea.class);
    }

    /**
     * Set tracked files.
     *
     * @param filesMap Map with file path as key and SHA1 id as value.
     */
    public void setTracked(Map<String, String> filesMap) {
        tracked = filesMap;
    }

}
