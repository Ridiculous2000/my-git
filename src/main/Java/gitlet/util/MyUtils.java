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
     * 根据传入的file对象创建目录
     * @param dir 要创建的目录
     */
    public static void mkdir(File dir) {
        // 尝试创建
        if (!dir.mkdir()) {
            // 创建失败就报错
            throw new IllegalArgumentException(String.format("mkdir: %s: Failed to create.", dir.getPath()));
        }
    }


    /**
     * 实例方法：将对象序列化后写入文件
     * @param file 要写入的文件路径
     * @param obj  要序列化的对象
     */
    public static void saveObjectFile(File file, Serializable obj) {
        // 获取父路径，看看在不在
        File dir = file.getParentFile();
        if (!dir.exists()) {
            mkdir(dir);
        }
        // 把序列化对象写入文件
        writeObject(file, obj);
    }


    /**
     * 创建Object文件，根据传入的id，前面2位为文件夹，后面几位为文件名，创建对应的object文件
     * @param id SHA1的id
     * @return 创建的Object文件
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
