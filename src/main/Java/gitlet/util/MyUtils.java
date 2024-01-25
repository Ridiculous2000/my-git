package gitlet.util;

import gitlet.Lazy;
import gitlet.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.function.Supplier;

import static gitlet.util.Utils.*;


public class MyUtils {

    /**
     * ���ݴ����file���󴴽�Ŀ¼
     * @param dir Ҫ������Ŀ¼
     */
    public static void mkdir(File dir) {
        // ���Դ���
        if (!dir.mkdir()) {
            // ����ʧ�ܾͱ���
            throw new IllegalArgumentException(String.format("mkdir: %s: Failed to create.", dir.getPath()));
        }
    }


    /**
     * ʵ�����������������л���д���ļ�
     * @param file Ҫд����ļ�·��
     * @param obj  Ҫ���л��Ķ���
     */
    public static void saveObjectFile(File file, Serializable obj) {
        // ��ȡ��·���������ڲ���
        File dir = file.getParentFile();
        if (!dir.exists()) {
            mkdir(dir);
        }
        // �����л�����д���ļ�
        writeObject(file, obj);
    }


    /**
     * ����Object�ļ������ݴ����id��ǰ��2λΪ�ļ��У����漸λΪ�ļ�����������Ӧ��object�ļ�
     * @param id SHA1��id
     * @return ������Object�ļ�
     */
    public static File getObjectFile(String id) {
        String dirName = getObjectDirName(id);
        String fileName = getObjectFileName(id);
        return join(Repository.OBJECTS_DIR, dirName, fileName);
    }

    /**
     * Get directory name from SHA1 id in the objects folder.
     *
     * @param id SHA1 id
     * @return Name of the directory
     */
    public static String getObjectDirName(String id) {
        return id.substring(0, 2);
    }

    /**
     * Get file name from SHA1 id.
     *
     * @param id SHA1 id
     * @return Name of the file
     */
    public static String getObjectFileName(String id) {
        return id.substring(2);
    }

    public static void exit(String message, Object... args) {
        message(message, args);
        System.exit(0);
    }

    /**
     * Get a lazy initialized value.
     * @param delegate Function to get the value
     * @param <T>      Type of the value
     * @return Lazy instance
     */
    public static <T> Lazy<T> lazy(Supplier<T> delegate) {
        return new Lazy<>(delegate);
    }

    /**
     * Tells if the deserialized object instance of given class.
     * @param file File instance
     * @param c    Target class
     * @return true if is instance
     */
    public static boolean isFileInstanceOf(File file, Class<?> c) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return c.isInstance(in.readObject());
        } catch (Exception ignored) {
            return false;
        }
    }

}
