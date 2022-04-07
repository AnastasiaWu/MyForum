public class Server {
    public void main(String[] args) throws Exception {
        // Check the input
        if (args.length != 2) {
            System.out.println("Please input the port number.");
            return;
        }
        // Define socket parameters, address and Port#
        InetAddress IPAddress = InetAddress.getByName("localhost");
        int serverPort = Integer.parseInt(args[1]);

        // Socket settle

        // Authentication

        // Interaction with user
        while (true) {
            String command = showMenu();
            switch (command) {
                case "CRT":
                    createThread();
                    break;
                case "MSG":
                    postMessage();
                    break;
                case "DLT":
                    deleteMessage();
                    break;
                case "EDT":
                    editMessage();
                    break;
                case "LST":
                    listThreads();
                    break;
                case "RDT":
                    readThread();
                    break;
                case "UPD":
                    uploadFile();
                    break;
                case "DWN":
                    downloadFile();
                    break;
                case "RMV":
                    removeThread();
                    break;
                case "XIT":
                    int code = exit();
                    if (code == 0)
                        return;
                    break;
                default:
                    // show error message
                    System.out.println("Invalid command.");
                    break;
            }
        }
    };

    private String showMenu() {
        return null;
    };

    private void createThread() {
    };

    private void postMessage() {
    };

    private void deleteMessage() {
    };

    private void editMessage() {
    };

    private void listThreads() {
    };

    private void readThread() {
    };

    private void uploadFile() {
    };

    private void downloadFile() {
    };

    private void removeThread() {
    };

    private int exit() {
        return 0;
    };

    private void socketSend() {
    };

    private void socketReceive() {
    };
}
