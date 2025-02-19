package org.algorithm.client;

import javafx.application.Platform;
import org.algorithm.components.Node;
import org.algorithm.maze_builder.Maze;
import org.algorithm.visualizer.MazeVisualizer;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private final String host;
    private final int port;
    private Maze maze;
    private  AtomicBoolean running = new AtomicBoolean(true);
    private Thread heartbeatThread;

    private final BlockingQueue<Node> nodeQueue = new LinkedBlockingQueue<>();

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        this.maze = new Maze();

    }

    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (out != null) {
                        out.println("heartbeat");
                        out.flush();
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Heartbeat failed: " + e.getMessage());
                    reconnect();
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void reconnect() {
        while (running.get()) {
            try {
                System.out.println("Attempting to reconnect...");
                stop();
                Thread.sleep(5000);
                setupConnection();
                System.out.println("Reconnected successfully!");
                return;
            } catch (Exception e) {
                System.err.println("Reconnection failed: " + e.getMessage());
            }
        }
    }

    private void setupConnection() throws IOException {
        clientSocket = new Socket(host, port);
        clientSocket.setKeepAlive(true);
        clientSocket.setSoTimeout(30000);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }




    public void sendNodeToServer(Node node) {
        try {
            nodeQueue.put(node);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Failed to queue node: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            setupConnection();
            startHeartbeat();
            initializeMazeData();
            startMessageListener();
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Node node = nodeQueue.take();
                    sendMessage("node");
                    sendNode(node);
                    System.out.println("Sent node: " + node);
                } catch (IOException e) {
                    System.err.println("Communication error: " + e.getMessage());
                    reconnect();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void startMessageListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (running.get()) {
                    String message = in.readLine();
                    if (message == null) {
                        System.out.println("Server disconnected - null message received");
                        reconnect();
                        break;
                    }


                    switch (message) {
                        case "node":
                            Node receivedNode = receiveNode();
                            if (receivedNode != null) {
                                System.out.println("Received node: " + receivedNode.getRow() + " " + receivedNode.getColumn());
                                MazeVisualizer.updateOtherPlayerPosition(receivedNode.getRow(), receivedNode.getColumn());
                            } else {
                                System.err.println("Received node is null!");
                            }
                            break;
                        case "turn":
                            Platform.runLater(() -> {
                                MazeVisualizer.handleTurnChange(true);
                            });
                            break;
                        case "not":
                            Platform.runLater(() -> {
                                MazeVisualizer.handleTurnChange(false);
                            });
                            break;
                        case "score":
                            try {
                                int score = Integer.parseInt(in.readLine());
                                MazeVisualizer.updateScore(score);
                            } catch (IOException | NumberFormatException e) {
                                System.err.println("Error reading score: " + e.getMessage());
                            }
                            break;
                        case "otherScore":
                            try {
                                int opponentScore = Integer.parseInt(in.readLine());
                                MazeVisualizer.updateOpponentScore(opponentScore);
                            } catch (IOException | NumberFormatException e) {
                                System.err.println("Error reading opponent score: " + e.getMessage());
                            }
                            break;
                        case "gameOver":
                           MazeVisualizer.handleGameOver();
                            break;
                        default:
                            System.out.println("Received unknown message: " + message);
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Message listener error: " + e.getMessage());
                    e.printStackTrace();
                    reconnect();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    private void initializeMazeData() throws IOException {
        sendMessage("row");
        String rowResponse = in.readLine();
        maze.setNbRow(Integer.parseInt(rowResponse));
        sendMessage("column");
        String colResponse = in.readLine();
        maze.setNbCol(Integer.parseInt(colResponse));
        sendMessage("start");
        maze.setStart(receiveNode());
        sendMessage("end");
        maze.setEnd(receiveNode());
        sendMessage("theme");
        maze.setTheme(in.readLine());
        sendMessage("maze");
        Node[][] mazeRec = receiveMaze();
        if (mazeRec != null) {
            maze.setMaze(mazeRec);
            MazeVisualizer.MAZE_HEIGHT = maze.getNbRow();
            MazeVisualizer.MAZE_WIDTH = maze.getNbCol();
            MazeVisualizer.maze = mazeRec;
            MazeVisualizer.startNode = maze.getStart();
            MazeVisualizer.endNode = maze.getEnd();
            MazeVisualizer.themeProperty.set(maze.getTheme());
        }
    }

    private void cleanup() {
        running.set(false);
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        try {
            stop();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    private void sendMessage(String msg) throws IOException {
        out.println(msg);
    }

    private void stop() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (clientSocket != null) clientSocket.close();
    }

    public Node[][] receiveMaze() {
        try {
            String base64Data = in.readLine();
            if (base64Data == null) return null;
            byte[] mazeData = Base64.getDecoder().decode(base64Data);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(mazeData);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                return (Node[][]) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Node receiveNode() {
        try {
            String nodeResponse = in.readLine();
            if (nodeResponse != null) {
                String[] parts = nodeResponse.substring(1, nodeResponse.length() - 1).split(",");
                int row = Integer.parseInt(parts[0]);
                int column = Integer.parseInt(parts[1]);
                return new Node(row, column);
            }
        } catch (IOException e) {
            System.out.println("problem in receive node ");
        }
        return null;
    }



    public void sendNode(Node node) {
        out.println("(" + node.getRow() + "," + node.getColumn() + ")");
        out.flush();
    }

}