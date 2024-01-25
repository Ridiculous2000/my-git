package gitlet;

import static gitlet.util.MyUtils.exit;


public class Main {

    /**
     * gitlet��main����������̨������
     */
    public static void main(String[] args) {
        //�ж��Ƿ��������в�����û�о��˳�
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        // ��ȡ��һ���������������
        String firstArg = args[0];
        String fileName;
        String message;
        String branchName;
        switch (firstArg) {
            // ��ʼ���ֿ�
            case "init":
                // ����Ԥ��
                validateNumArgs(args, 1);
                Repository.init();
                break;
            // ����ļ����ݴ���
            case "add":
                // ���.gitlet�ļ��Ͳ�������
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                fileName = args[1];
                new Repository().add(fileName);
                break;
            // �ύ����
            case "commit":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                // �ύ�Ľ�����Ϣ
                message = args[1];
                if (message.length() == 0) {
                    exit("Please enter a commit message.");
                }
                // �����ύ
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
                 * checkout ����Ƚϸ��ӣ������������
                 * ��1��checkout �C [filename]���������ˣ��õ�ǰcommit �ָ�|��д �ļ�����
                 * ��2��checkout [commitID] �C [filename]����ָ��commit��file �ָ�|��д
                 * ��3��checkout [branchname]���л�branch��֧
                 */
                switch (args.length) {
                    case 3 :
                        if (!args[1].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        fileName = args[2];
                        // checkout �C [filename]
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
