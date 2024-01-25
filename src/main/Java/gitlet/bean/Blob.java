package gitlet.bean;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.util.MyUtils.getObjectFile;
import static gitlet.util.MyUtils.saveObjectFile;
import static gitlet.util.Utils.*;

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

    /**
     * ���ݴ�����ļ�������path����������id
     * @param sourceFile File instance
     * @return SHA1 id
     */
    public static String generateId(File sourceFile) {
        String filePath = sourceFile.getPath();
        byte[] fileContent = readContents(sourceFile);
        return sha1(filePath, fileContent);
    }

    // ����id��ȡBlob����
    public static Blob fromFile(String id) {
        return readObject(getObjectFile(id), Blob.class);
    }

    // ��blob�ļ�������д��blob��Դ�ļ�
    public void writeContentToSource() {
        writeContents(source, content);
    }

    /**
     * ����blob��Ӧ�ļ������ݣ�utf-8���룩.
     * @return Blob content
     */
    public String getContentAsString() {
        return new String(content, StandardCharsets.UTF_8);
    }


}
