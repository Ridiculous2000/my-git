package gitlet.util;

import gitlet.Exception.GitletException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

public class Utils {

    /** The length of a complete SHA-1 UID as a hexadecimal numeral. */
    public static final int UID_LENGTH = 40;

    /* SHA-1 HASH VALUES. */

    /**
     * �����ɶ��ֵƴ�Ӻ�� SHA-1 ��ϣֵ��
     * ��Щֵ������������ϵ��ֽ�������ַ�����
     * @param vals �����������������ֽ�������ַ���
     * @return ����õ��� SHA-1 ��ϣֵ��ʮ�������ַ���
     */
    public static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    public static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    public static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    public static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    // ��ȡ�ļ����ݣ��ж��ǲ�����ͨ�ļ������Ǳ����Ǿ����byte��
    public static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    // ��ȡfile����,����UTF-8 ��װ��String
    public static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }


    /**
     * ��contents������д��file
     * @param file Ҫд����ļ���ֻ�����ļ������������ļ��У�
     * @param contents Ҫд������ݣ��������ַ��������ַ�����
     */
    public static void writeContents(File file, Object... contents) {
        try {
            // file���ļ��о���������
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            // ����������
            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            // ����contents�����ֽ�����ֱ��д�룬���ַ���ת������д��
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Throws IllegalArgumentException in case of problems. */
    public static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                 | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write OBJ to FILE. */
    public static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    public static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    public static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /* OTHER FILE UTILITIES */

    /**
     * ���ݴ����ĸ��ļ��м����Լ�others���ڸ��ļ����´����ļ������� first��objects�ļ��У�others��/a��/b.txt��
     * ������object�ļ������洴�� a/b.txt Ȼ�󷵻��������
     * @param first ���ļ���·��
     * @param others �����ļ�
     * @return
     */
    public static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    /**
     * ���ݴ����ĸ��ļ��м����Լ�others���ڸ��ļ����´����ļ������� first��objects�ļ��У�others��/a��/b.txt��
     * ������object�ļ������洴�� a/b.txt Ȼ�󷵻��������
     * @param first ���ļ���
     * @param others �����ļ�
     * @return
     */
     public static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ. */
    public static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw error("Internal error serializing commit.");
        }
    }



    /* MESSAGES AND ERROR REPORTING */

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    public static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /**
     * ��ӡmessage
     * @param msg Ҫ��ӡ����Ϣ
     * @param args   Ҫ��ӡ�Ĳ���
     */
    public static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }
}
