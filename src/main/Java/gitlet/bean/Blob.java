package gitlet.bean;

import java.io.File;
import java.io.Serializable;

import static gitlet.util.MyUtils.getObjectFile;
import static gitlet.util.MyUtils.saveObjectFile;
import static gitlet.util.Utils.readContents;
import static gitlet.util.Utils.sha1;

// ����gitlet������ļ�����
public class Blob implements Serializable {
    // Դ�ļ�
    private final File source;
    // Դ�ļ�����
    private final byte[] content;
    // SHA1������id��path��contentһ�����ɣ�ֻ��·�����ݶ�һ������һ����id��
    private final String id;
    // ������blob������ļ�
    private final File file;
    // ���ݴ����sourceFile �����µ��ļ����󣨶������ݣ���
    public Blob(File sourceFile) {
        source = sourceFile;
        String filePath = sourceFile.getPath();
        content = readContents(sourceFile);
        id = sha1(filePath, content);
        file = getObjectFile(id);
    }

    //�ѱ�Bolb����д���Ӧ��Object�ļ���ʵ�ֳ־û�
    public void save() {
        saveObjectFile(file, this);
    }

    // ��ȡblob�����id
    public String getId() {
        return id;
    }

    // ��ȡblob��object file
    public File getFile() {
        return file;
    }

}
