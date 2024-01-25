package gitlet;

import static gitlet.util.MyUtils.exit;


public class Main {

    /**
     * gitlet的main函数，控制台传参数
     */
    public static void main(String[] args) {
        //判断是否有命令行参数，没有就退出
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        // 获取第一个参数（命令符）
        String firstArg = args[0];
        String fileName;
        String message;
        String branchName;
        switch (firstArg) {
            // 初始化仓库
            case "init":
                // 符合预期
                validateNumArgs(args, 1);
                Repository.init();
                break;
            // 添加文件到暂存区
            case "add":
                // 检查.gitlet文件和参数数量
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                fileName = args[1];
                new Repository().add(fileName);
                break;
            // 提交内容
            case "commit":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                // 提交的介绍信息
                message = args[1];
                if (message.length() == 0) {
                    exit("Please enter a commit message.");
                }
                // 创建提交
                new Repository().commit(message);
                break;
            case "rm" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                fileName = args[1];
                new Repository().remove(fileName);
                break;
            case "log" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                new Repository().log();
                break;
            case "global-log" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                Repository.globalLog();
                break;
            case "find" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                message = args[1];
                if (message.length() == 0) {
                    exit("Found no commit with that message.");
                }
                Repository.find(message);
                break;
            case "status" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                new Repository().status();
                break;
            case "checkout" :
                Repository.checkWorkingDir();
                Repository repository = new Repository();
                /**
                 * checkout 命令比较复杂，有三种情况：
                 * （1）checkout – [filename]：改完后悔了，用当前commit 恢复|重写 文件内容
                 * （2）checkout [commitID] – [filename]：用指定commit的file 恢复|重写
                 * （3）checkout [branchname]：切换branch分支
                 */
                switch (args.length) {
                    case 3 :
                        if (!args[1].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        fileName = args[2];
                        // checkout – [filename]
                        repository.checkout(fileName);
                        break;
                    case 4 :
                        if (!args[2].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        String commitId = args[1];
                        fileName = args[3];
                        repository.checkout(commitId, fileName);
                        break;
                    case 2 :
                        String branch = args[1];
                        repository.checkoutBranch(branch);
                        break;
                    default :
                        exit("Incorrect operands.");
                }
                break;
            case "branch" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                branchName = args[1];
                new Repository().branch(branchName);
                break;
            case "rm-branch" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                branchName = args[1];
                new Repository().rmBranch(branchName);
                break;
            case "reset" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String commitId = args[1];
                new Repository().reset(commitId);
                break;
            case "merge" :
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                branchName = args[1];
                new Repository().merge(branchName);
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
