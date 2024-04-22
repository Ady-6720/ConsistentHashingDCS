import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

class Globals {

  public static final Map<Integer, String> dataMap = new HashMap<>();
  public static Integer successorID;
  public static Integer predecessorID = 0;
  public static Integer bootStrapID = 0;
  public static final Map<Integer, Socket> successor = new HashMap<>();
  public static final Map<Integer, Socket> predecessor = new HashMap<>();
}

class connectThread implements Runnable {

  private int port;
  private int id;
  private ServerSocket server;
  private Socket socket;
  DataInputStream in;
  DataOutputStream out;

  connectThread(int port, int id) {
    this.port = port;
    this.id = id;
    new Thread(this).start();
  }

  @Override
  public void run() {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Server initialized on port " + port + ". Waiting for client connections...");

      while (!Thread.currentThread().isInterrupted()) {
        System.out.println("Ready to accept a new connection...");
        Socket clientSocket = serverSocket.accept();
        System.out.println("Connection established with client: " + clientSocket.getInetAddress().getHostAddress());

        // Setting up data streams for communication
        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

        // Start a new thread for handling client communication
        startClientHandler(dataInputStream, dataOutputStream, id, port);
      }
    } catch (IOException e) {
      System.err.println("Server encountered an IOException: " + e.getMessage());
    } finally {
      closeServerSocket(serverSocket);
    }
  }

  private void startClientHandler(DataInputStream inputStream, DataOutputStream outputStream, int serverId, int serverPort) {
    WorkerThread worker = new WorkerThread(inputStream, outputStream, serverId, serverPort);
    worker.start();
    System.out.println("Started a new worker thread for client communication.");
  }

  private void closeServerSocket(ServerSocket serverSocket) {
    if (serverSocket != null && !serverSocket.isClosed()) {
      try {
        serverSocket.close();
        System.out.println("Server socket closed successfully.");
      } catch (IOException e) {
        System.err.println("Failed to close server socket: " + e.getMessage());
      }
    }
  }



  public void stop() {
    try {
      System.out.println("closing everything");
      server.close();
      socket.close();
      in.close();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class connectToBootstrap extends Thread {

  private String ip;
  private Integer port;
  private Integer listenPort;
  private Integer id;

  connectToBootstrap(String ip, Integer port, Integer listenPort, Integer id) {
    this.ip = ip;
    this.port = port;
    this.listenPort = listenPort;
    this.id = id;
  }

  @Override
  public void run() {
    try {
      Socket socket = new Socket(ip, port);
      System.out.println("nameServer is connected");

      DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
      new userThread(input, in, out, listenPort, id).start();
    } catch (IOException i) {
      System.out.println("Exception in connectToBootstrap" + i.getMessage());
    }
  }
}

class userThread extends Thread {

  private DataInputStream in = null;
  private BufferedReader input = null;
  private DataOutputStream out = null;
  private int listenPort;
  private int id;
  private connectThread connection = null;

  userThread(BufferedReader input, DataInputStream in, DataOutputStream out, int listenPort, int id) {
    this.in = in;
    this.input = input;
    this.out = out;
    this.listenPort = listenPort;
    this.id = id;
  }

  private static String serialize(Serializable o) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    return Base64.getEncoder().encodeToString(baos.toByteArray());
  }

  private static Object deserialize(String s) throws IOException, ClassNotFoundException {
    byte[] data = Base64.getDecoder().decode(s);
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    Object o = ois.readObject();
    ois.close();
    return o;
  }

  @Override
  public void run() {
    try {
      String line = "";
      while (!line.equals("quit")) {
        System.out.print("input : ");
        line = input.readLine();
        switch (line) {
          case "enter":
            connection = new connectThread(listenPort, id);
            InetAddress localhost = InetAddress.getLocalHost();
            String ipAddress = (localhost.getHostAddress()).trim();
            String command = "enter#" + id + "#" + ipAddress + "#" + listenPort;
            out.writeUTF(command);
            String serverInput = in.readUTF(); //set Pred and Succ
            String inputSplitted[] = serverInput.split("#");
                        /*for (int i = 0; i < inputSplitted.length; i++) {
							System.out.println("inputSplitted     " + inputSplitted[i]);
						}*/
            String keyRange = inputSplitted[1];
            String predecessor = inputSplitted[2];
            Integer predId = Integer.parseInt(predecessor.split(" ")[1]);
            //System.out.println("predId: " + predId + "predecessor  " + predecessor);
            String predIp = predecessor.split(" ")[2];
            Integer predPort = Integer.parseInt(predecessor.split(" ")[3]);
            String successor = inputSplitted[3];
            Integer sucId = Integer.parseInt(successor.split(" ")[1]);
            String sucIp = successor.split(" ")[2];
            Integer sucPort = Integer.parseInt(successor.split(" ")[3]);
            String sequence = inputSplitted[4];
            Socket socketpred = new Socket(predIp, predPort);
            Globals.predecessorID = predId;
            //System.out.println("Globals.predecessorID: " + Globals.predecessorID + " predId: " + predId + " predIp: " + predIp + " predPort: " + predPort);
            Globals.predecessor.put(predId, socketpred);
            Socket socketSuc = new Socket(sucIp, sucPort);
            Globals.successorID = sucId;
            Globals.successor.put(sucId, socketSuc);
            System.out.println(inputSplitted[0] +"\t" +keyRange +"\tPredecessor: " +predId +"\tSuccessor: " +sucId +"\t Retreived Nodes: " +sequence);
            break;
          case "exit":
            String direction = nameServerInput[1];
            int updatedNodeId = Integer.parseInt(nameServerInput[2]);
            String updatedNodeIp = nameServerInput[3];
            int updatedNodePort = Integer.parseInt(nameServerInput[4]);

            if ("pred".equals(direction)) {
              Socket newPredecessorSocket = establishConnection(updatedNodeIp, updatedNodePort);
              ServerGlobals.previousNodeId = updatedNodeId;
              ServerGlobals.previousNodes.put(updatedNodeId, newPredecessorSocket);
            } else if ("suc".equals(direction)) {
              Socket newSuccessorSocket = establishConnection(updatedNodeIp, updatedNodePort);
              ServerGlobals.nextNodeId = updatedNodeId;
              ServerGlobals.nextNodes.put(updatedNodeId, newSuccessorSocket);
            }
            break;

          private Socket establishConnection(String ip, int port) throws IOException {
            return new Socket(ip, port);
          }

        }
      }
    } catch (IOException i) {
      //System.out.println("userThread: Exception is caught" + i.getMessage());
    }
  }
}

class workerThread extends Thread {

  private DataInputStream in = null;
  private DataOutputStream out = null;
  private int id;
  private int listenPort;

  workerThread(DataInputStream in, DataOutputStream out, int id, int listenPort) {
    this.in = in;
    this.out = out;
    this.id = id;
    this.listenPort = listenPort;
  }

  protected HashMap<Integer, String> convertToHashMap(String text) {
    HashMap<Integer, String> data = new HashMap<Integer, String>();
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
          case "lookup":
            Integer key = Integer.parseInt(nameServerInput[1]);
            String serverSequence = nameServerInput[2] + "-" + id;
            if (key <= id && key > Globals.predecessorID) {
              if (Globals.dataMap.containsKey(key)) {
                out.writeUTF(Globals.dataMap.get(key) + "#" + id + "#" + serverSequence);
              } else out.writeUTF("Key not found#null#" + serverSequence);
            } else if (!Globals.successorID.equals(Globals.bootStrapID)) {
              Socket socket = Globals.successor.get(Globals.successorID);
              DataInputStream inSuccessor = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
              DataOutputStream outSuccessor = new DataOutputStream(socket.getOutputStream());
              outSuccessor.writeUTF(command + "#" + key + "#" + serverSequence);
              out.writeUTF(inSuccessor.readUTF());
            } else out.writeUTF("Key not found#null#" + serverSequence);
            break;
          case "insert":
            key = Integer.parseInt(nameServerInput[1]);
            String value = nameServerInput[2];
            serverSequence = nameServerInput[3] + "-" + id;
            if (key <= id && key > Globals.predecessorID) {
              Globals.dataMap.put(key, value);
              out.writeUTF("Insertion Successful#" + id + "#" + serverSequence);
            } else if (!Globals.successorID.equals(Globals.bootStrapID)) {
              Socket socket = Globals.successor.get(Globals.successorID);
              DataInputStream inSuccessor = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
              DataOutputStream outSuccessor = new DataOutputStream(socket.getOutputStream());
              outSuccessor.writeUTF(command + "#" + key + "#" + value + "#" + serverSequence);
              out.writeUTF(inSuccessor.readUTF());
            } else out.writeUTF("Key not found#null#" + serverSequence);
            break;
          case "delete":
            int deletionKey = Integer.parseInt(nameServerInput[1]);
            String transactionId = nameServerInput[2] + "-" + nodeId;
            if (deletionKey <= nodeId && deletionKey > ServerGlobals.previousNodeId) {
              if (ServerGlobals.keyValueStore.containsKey(deletionKey)) {
                ServerGlobals.keyValueStore.remove(deletionKey);
                dataOutput.writeUTF("Deletion Successful#" + nodeId + "#" + transactionId);
              } else {
                dataOutput.writeUTF("Key not found#null#" + transactionId);
              }
            } else if (!ServerGlobals.nextNodeId.equals(ServerGlobals.bootstrapNodeId)) {
              Socket nextNodeSocket = ServerGlobals.nextNodes.get(ServerGlobals.nextNodeId);
              DataInputStream nextNodeInput = new DataInputStream(new BufferedInputStream(nextNodeSocket.getInputStream()));
              DataOutputStream nextNodeOutput = new DataOutputStream(nextNodeSocket.getOutputStream());
              nextNodeOutput.writeUTF("delete#" + deletionKey + "#" + transactionId);
              dataOutput.writeUTF(nextNodeInput.readUTF());
            } else {
              dataOutput.writeUTF("Key not found#null#" + transactionId);
            }
            break;

          case "enter":
            int entryKey = Integer.parseInt(nameServerInput[1]);
            String newNodeIp = nameServerInput[2];
            int newNodePort = Integer.parseInt(nameServerInput[3]);
            String transactionId = nameServerInput[4] + "-" + nodeId;

            if (entryKey < nodeId && entryKey > ServerGlobals.previousNodeId) {
              Socket previousNodeSocket = ServerGlobals.previousNodes.get(ServerGlobals.previousNodeId);
              String previousNodeIp = previousNodeSocket.getLocalAddress().getHostAddress().trim();
              int previousNodePort = previousNodeSocket.getPort();
              InetAddress localhost = InetAddress.getLocalHost();
              String localIp = localhost.getHostAddress().trim();

              DataOutputStream outputToPrevious = new DataOutputStream(previousNodeSocket.getOutputStream());
              outputToPrevious.writeUTF("exit#suc#" + entryKey + "#" + newNodeIp + "#" + newNodePort);

              ServerGlobals.previousNodes.get(ServerGlobals.previousNodeId).close();
              int oldPreviousNodeId = ServerGlobals.previousNodeId;
              ServerGlobals.previousNodeId = entryKey;

              Socket newNodeSocket = new Socket(newNodeIp, newNodePort);
              ServerGlobals.previousNodes.put(entryKey, newNodeSocket);

              HashMap<Integer, String> updatedDataMap = new HashMap<>();
              updatedDataMap.putAll(ServerGlobals.dataStore);
              updatedDataMap.keySet().removeIf(key -> key > entryKey);

              DataOutputStream outputToNewPrevious = new DataOutputStream(newNodeSocket.getOutputStream());
              outputToNewPrevious.writeUTF("keyHash#" + updatedDataMap.toString());

              dataOutput.writeUTF("successful entry#Key Range: [" + (oldPreviousNodeId + 1) + " " + entryKey + "]" +
                      "#predecessor: " + oldPreviousNodeId + " " + previousNodeIp + " " + previousNodePort +
                      "#successor: " + nodeId + " " + localIp + " " + nodePort + "#" + transactionId);
              System.out.println("successful entry#Key Range: [" + (oldPreviousNodeId + 1) + " " + entryKey + "]" +
                      "#predecessor: " + oldPreviousNodeId + " " + previousNodeIp + " " + previousNodePort +
                      "#successor: " + nodeId + " " + localIp + " " + nodePort + "#" + transactionId);
            } else if (!ServerGlobals.nextNodeId.equals(ServerGlobals.bootstrapNodeId)) {
              Socket nextNodeSocket = ServerGlobals.nextNodes.get(ServerGlobals.nextNodeId);
              DataInputStream inputFromSuccessor = new DataInputStream(new BufferedInputStream(nextNodeSocket.getInputStream()));
              DataOutputStream outputToSuccessor = new DataOutputStream(nextNodeSocket.getOutputStream());

              outputToSuccessor.writeUTF("enter#" + entryKey + "#" + newNodeIp + "#" + newNodePort + "#" + transactionId);
              dataOutput.writeUTF(inputFromSuccessor.readUTF());
            } else {
              dataOutput.writeUTF("Key out of Range#null#null#null#" + transactionId);
            }
            break;

          case "exit":
            String hint = nameServerInput[1];
            Integer newID = Integer.parseInt(nameServerInput[2]);
            String newIp = nameServerInput[3];
            Integer newPort = Integer.parseInt(nameServerInput[4]);
            if (hint.equals("pred")) {
              Socket socketpred = new Socket(newIp, newPort);
              Globals.predecessorID = newID;
              Globals.predecessor.put(newID, socketpred);
            } else if (hint.equals("suc")) {
              Socket socketSuc = new Socket(newIp, newPort);
              Globals.successorID = newID;
              Globals.successor.put(newID, socketSuc);
            }
            break;
          case "keyHash":
            HashMap<Integer, String> newHashMap = convertToHashMap(nameServerInput[1]);
            for (Integer newkey : newHashMap.keySet()) {
              Globals.dataMap.put(newkey, newHashMap.get(newkey));
            }
            break;
        }
      }
    } catch (IOException i) {
      //System.out.println("workerThread: Exception is caught" + i.getMessage());
    }
  }
}

public class nameServer {

  // constructor with port
  public nameServer(int id, int listenPort, String serverIP, int serverPort) {
    // starts server and waits for a connection
    new connectToBootstrap(serverIP, serverPort, listenPort, id).start();
  }

  public static void main(String[] args) {
    if (args.length == 1) {
      File file = new File(args[0]);
      if (file.exists() && file.isFile()) {
        try {
          Scanner sc2 = new Scanner(file);
          int id = Integer.parseInt(sc2.nextLine());

          int listenPort = Integer.parseInt(sc2.nextLine());
          String serverSocket = sc2.nextLine();
          String serverIP = serverSocket.split(" ")[0];
          int serverPort = Integer.parseInt(serverSocket.split(" ")[1]);

          new nameServer(id, listenPort, serverIP, serverPort);
          sc2.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else System.out.println("Configuration file: " + args[1] + " not found " );
    } else System.out.println(
            "Missing arguments: Expected configuration file name!"
    );
  }
}