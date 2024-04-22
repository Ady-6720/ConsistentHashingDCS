import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

class Globals {

  public static final Map<Integer, String> keyvalueMap = new HashMap<>();
  public static final List<Integer> contactedservers = new ArrayList<Integer>();
  public static Integer successorID = 0;
  public static Integer predecessorID = 0;
  public static final Map<Integer, Socket> successor = new HashMap<>();
  public static final Map<Integer, Socket> predecessor = new HashMap<>();
}

class connectionThread extends Thread {

  private int port;

  connectionThread(int port) {
    this.port = port;
  }

  @Override
  public void run() {
    try {
      ServerSocket server = new ServerSocket(port);
      System.out.println("bootstrap Server started");

      while (true) {
        System.out.println("Waiting for another NameServer\n");

        Socket socket = server.accept();
        System.out.println("nameServer is connected \ninput :");

        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        new workerThread(in, out, port).start();
      }
    } catch (IOException i) {
      System.out.println("ConnectionThread Exception : " + i.getMessage());
    }
  }
}

class workerThread extends Thread {

  private DataInputStream in = null;
  private DataOutputStream out = null;
  private int listenPort;
  private int id = 0;

  workerThread(DataInputStream in, DataOutputStream out, int listenPort) {
    this.in = in;
    this.out = out;
    this.listenPort = listenPort;
  }

  protected HashMap<Integer, String> convertToHashMap(String text) {
    HashMap<Integer, String> data = new HashMap<>();
    Pattern p = Pattern.compile("[\\{\\}\\=\\, ]++");
    String[] split = p.split(text);
    for (int i = 1; i + 2 <= split.length; i += 2) {
      data.put(Integer.parseInt(split[i]), split[i + 1]);
    }
    return data;
  }

  @Override
  public void run() {
    try {
      String input = "";
      while (!input.equals("quit")) {
        input = in.readUTF();
        System.out.println("nameServer input:" + input.split("#")[0]);
        String[] nameServerInput = input.split("#");
        String command = nameServerInput[0];
        switch (command) {
          case "enter":
            handleEnterCommand(nameServerInput);
            break;
          case "exit":
            handleExitCommand(nameServerInput);
            break;
          case "keyHash":
            handleKeyHashCommand(nameServerInput);
            break;
        }
      }
    } catch (IOException e) {
      System.out.println("WorkerThread Exception : " + e.getMessage());
    }
  }

  private void handleEnterCommand(String[] input) throws IOException {
    Integer key = Integer.parseInt(input[1]);
    String nodeIp = input[2];
    Integer nodePort = Integer.parseInt(input[3]);
    String serverSequence = id + "";
    if (key > Globals.predecessorID) {
      updatePredecessor(key, nodeIp, nodePort);
      updateSuccessor(key, nodeIp, nodePort);
    } else if (!Globals.successorID.equals(0)) {
      Socket socket = Globals.successor.get(Globals.successorID);
      DataInputStream inSuccessor = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      DataOutputStream outSuccessor = new DataOutputStream(socket.getOutputStream());
      outSuccessor.writeUTF("enter#" + key + "#" + nodeIp + "#" + nodePort + "#" + serverSequence);
      out.writeUTF(inSuccessor.readUTF());
    } else {
      out.writeUTF("Key out of Range#null#null#null#" + serverSequence);
    }
  }

  private void updatePredecessor(Integer key, String nodeIp, Integer nodePort) throws IOException {
    InetAddress localhost = InetAddress.getLocalHost();
    String ipAddress = (localhost.getHostAddress()).trim();
    String pred_ip = ipAddress;
    int pred_port = listenPort;
    int old_pred = Globals.predecessorID;
    Globals.predecessorID = key;
    Socket socket1 = new Socket(nodeIp, nodePort);
    if (old_pred == 0) {
      Globals.successorID = key;
      Globals.successor.put(key, socket1);
    } else {
      Socket socket2 = Globals.predecessor.get(old_pred);
      pred_ip = socket2.getLocalAddress().getHostAddress().trim();
      pred_port = socket2.getPort();
      DataOutputStream outPred = new DataOutputStream(socket2.getOutputStream());
      outPred.writeUTF("exit#suc#" + key + "#" + nodeIp + "#" + nodePort);
    }
    Globals.predecessor.put(key, socket1);
    HashMap<Integer, String> newHashMap = new HashMap<>();
    newHashMap.putAll(Globals.keyvalueMap);
    for (Integer oldKey : Globals.keyvalueMap.keySet()) {
      if (oldKey > key) {
        newHashMap.remove(oldKey);
      }
    }
    for (Integer newKey : newHashMap.keySet()) {
      if (Globals.keyvalueMap.containsKey(newKey)) {
        Globals.keyvalueMap.remove(newKey);
      }
    }
    DataOutputStream outPred = new DataOutputStream(socket1.getOutputStream());
    outPred.writeUTF("keyHash#" + newHashMap.toString());
    out.writeUTF("successful entry#Key Range: [" + (old_pred + 1) + " " + key + "]" + "#predecessor: " + old_pred + " " + pred_ip + " " + pred_port + "#successor: " + id + " " + ipAddress + " " + listenPort + "#" + serverSequence);
  }

  private void updateSuccessor(Integer key, String nodeIp, Integer nodePort) throws IOException {
    sysout("Updating Successor");
    sysout("Key: " + key + " Node IP: " + nodeIp + " Node Port: " + nodePort);
    InetAddress localhost = InetAddress.getLocalHost();
    String ipAddress = (localhost.getHostAddress()).trim();
    String suc_ip = ipAddress;
    int suc_port = listenPort;
    int old_suc = Globals.successorID;
    Globals.successorID = key;
    Socket socket1 = new Socket(nodeIp, nodePort);
  }

  private void handleExitCommand(String[] input) throws IOException {
    String hint = input[1];
    Integer newID = Integer.parseInt(input[2]);
    String newIp = input[3];
    Integer newPort = Integer.parseInt(input[4]);
    if (hint.equals("pred")) {
      if (newID != 0) {
        Socket socketpred = new Socket(newIp, newPort);
        Globals.predecessor.put(newID, socketpred);
      }
      Globals.predecessorID = newID;
    } else if (hint.equals("suc")) {
      Socket socketSuc = new Socket(newIp, newPort);
      Globals.successorID = newID;
      Globals.successor.put(newID, socketSuc);
    }
  }

  private void handleKeyHashCommand(String[] input) {
    HashMap<Integer, String> newHashMap = convertToHashMap(input[1]);
    for (Integer newkey : newHashMap.keySet()) {
      Globals.keyvalueMap.put(newkey, newHashMap.get(newkey));
    }
  }

}

class userThread extends Thread {

  private int uniqueID;
  private BufferedReader userinput = null;

  userThread(int id) {
    uniqueID = id;

    try {
      userinput = new BufferedReader(new InputStreamReader(System.in));
    } catch (Exception e) {
      System.out.println("Exception in userThread : " + e.getMessage());
    }
  }

  @Override
  @Override
  public void run() {
    try {
      String input = "";
      while (!input.equals("quit")) {
        System.out.print("Input: ");
        input = userinput.readLine();
        if (input.contains("lookup")) {
          handleLookup(input);
        } else if (input.contains("insert")) {
          handleInsert(input);
        } else if (input.contains("delete")) {
          handleDelete(input);
        } else {
          System.out.println("Invalid Command");
        }
      }
    } catch (IOException e) {
      System.out.println("Exception in userThread: " + e.getMessage());
    }
  }

  private void handleLookup(String input) throws IOException {
    String[] parts = input.split(" ");
    if (parts.length != 2) {
      System.out.println("Lookup operand missing");
      return;
    }
    int key = Integer.parseInt(parts[1]);
    if (key > Globals.predecessorID) {
      handleLookupLocal(key);
    } else {
      handleLookupRemote(key);
    }
  }

  private void handleLookupLocal(int key) {
    if (Globals.keyvalueMap.containsKey(key)) {
      System.out.println("Value: " + Globals.keyvalueMap.get(key) + "\tServerID: " + uniqueID + "\tServersSequence: " + uniqueID);
    } else {
      System.out.println("Key Not Found");
    }
  }

  private void handleLookupRemote(int key) throws IOException {
    Socket socket = Globals.successor.get(Globals.successorID);
    try (DataInputStream inSuccessor = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
         DataOutputStream outSuccessor = new DataOutputStream(socket.getOutputStream())) {
      outSuccessor.writeUTF("lookup#" + key + "#" + uniqueID);
      String[] result = inSuccessor.readUTF().split("#");
      System.out.println("Value: " + result[0] + "\tServerID: " + result[1] + "\tServersSequence: " + result[2]);
    }
  }

  private void handleInsert(String input) throws IOException {
    String[] parts = input.split(" ");
    if (parts.length != 3) {
      System.out.println("Insert operand missing");
      return;
    }
    int key = Integer.parseInt(parts[1]);
    String value = parts[2];
    if (key > Globals.predecessorID) {
      Globals.keyvalueMap.put(key, value);
      System.out.println("Insertion Successful!\tServerID: " + uniqueID + "\tServersSequence: " + uniqueID);
    } else {
      handleInsertRemote(key, value);
    }
  }

  private void handleInsertRemote(int key, String value) throws IOException {
    Socket socket = Globals.successor.get(Globals.successorID);
    try (DataInputStream inSuccessor = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
         DataOutputStream outSuccessor = new DataOutputStream(socket.getOutputStream())) {
      outSuccessor.writeUTF("insert#" + key + "#" + value + "#" + uniqueID);
      String[] result = inSuccessor.readUTF().split("#");
      System.out.println(result[0] + "\tServerID: " + result[1] + "\tServersSequence: " + result[2]);
    }
  }

  private void handleDelete(String input) throws IOException {
    String[] parts = input.split(" ");
    if (parts.length != 2) {
      System.out.println("Delete operand missing");
      return;
    }
    int key = Integer.parseInt(parts[1]);
    if (key > Globals.predecessorID) {
      handleDeleteLocal(key);
    } else {
      handleDeleteRemote(key);
    }
  }

  private void handleDeleteLocal(int key) {
    if (Globals.keyvalueMap.containsKey(key)) {
      Globals.keyvalueMap.remove(key);
      System.out.println("Deletion Successful!\tServerID: " + uniqueID + "\tServersSequence: " + uniqueID);
    } else {
      System.out.println("Key Not Found");
    }
  }

  private void handleDeleteRemote(int key) throws IOException {
    Socket socket = Globals.successor.get(Globals.successorID);
    try (DataInputStream inSuccessor = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
         DataOutputStream outSuccessor = new DataOutputStream(socket.getOutputStream())) {
      outSuccessor.writeUTF("delete#" + key + "#" + uniqueID);
      String[] result = inSuccessor.readUTF().split("#");
      System.out.println(result[0] + "\tServerID: " + result[1] + "\tServersSequence: " + result[2]);
    }
  }

}

  public static void main(String[] args) {
    if (args.length == 1) {
      String configFile = args[0];
      processConfigurationFile(configFile);
    } else {
      System.out.println("Missing arguments: Expected configuration file name");
    }
  }

  private static void processConfigurationFile(String configFile) {
    File file = new File(configFile);
    if (file.exists() && file.isFile()) {
      try (Scanner scanner = new Scanner(file)) {
        int id = Integer.parseInt(scanner.nextLine());
        int listenPort = Integer.parseInt(scanner.nextLine());
        while (scanner.hasNextLine()) {
          String keyValueSet = scanner.nextLine();
          String[] parts = keyValueSet.split(" ");
          if (parts.length == 2) {
            int key = Integer.parseInt(parts[0]);
            String value = parts[1];
            Globals.keyvalueMap.put(key, value);
          }
        }
        new BootStrapServer(id, listenPort);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("Configuration file: " + configFile + " not found ");
    }
  }
