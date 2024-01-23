package gitlet;

import static gitlet.util.MyUtils.exit;


public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        //判断是否有命令行参数，没有就退出
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        // 获取第一个参数（命令符）
        String firstArg = args[0];
        switch (firstArg) {
            // 初始化仓库
            case "init":
                // 符合预期
                validateNumArgs(args, 1);
                Repository.init();
                break;
            case "add":
                // 检查.gitlet文件和参数数量
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String fileName = args[1];
                new Repository().add(fileName);
                break;
            default:
                exit("没实现");
                break;
        }
    }

    /**
     * 检查参数数量是否符合预期
     * @param args 输入的命令
     * @param n    期望的参数数量
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exit("Incorrect operands.");
        }
    }
}
