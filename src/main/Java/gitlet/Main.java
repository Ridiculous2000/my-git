package gitlet;

import static gitlet.util.MyUtils.exit;


public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        //�ж��Ƿ��������в�����û�о��˳�
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        // ��ȡ��һ���������������
        String firstArg = args[0];
        switch (firstArg) {
            // ��ʼ���ֿ�
            case "init":
                // ����Ԥ��
                validateNumArgs(args, 1);
                Repository.init();
                break;
            case "add":
                // ���.gitlet�ļ��Ͳ�������
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String fileName = args[1];
                new Repository().add(fileName);
                break;
            default:
                exit("ûʵ��");
                break;
        }
    }

    /**
     * �����������Ƿ����Ԥ��
     * @param args ���������
     * @param n    �����Ĳ�������
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exit("Incorrect operands.");
        }
    }
}
