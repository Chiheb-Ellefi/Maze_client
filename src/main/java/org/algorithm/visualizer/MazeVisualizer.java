package org.algorithm.visualizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.algorithm.client.Client;
import org.algorithm.components.Node;

public class MazeVisualizer extends Application {
    private static volatile MazeVisualizer instance;
    private static final Object LOCK = new Object();

    private static final int CELL_SIZE = 30;
    private static final int TURN_TIME = 20; // 15 seconds per turn

    public static int MAZE_WIDTH;
    public static int MAZE_HEIGHT;
    public static Node[][] maze;

    private static Pane mazePane;
    private Circle player;
    private static Circle otherPlayer;
    private int playerRow;
    private int playerCol;
    public static Node startNode;
    public static Node endNode;
    private Rectangle[][] cellRectangles;
    private static Button[] controlButtons;
    private Client client;
    static public volatile Boolean turn;
    private Label themeLabel; // Add new field for theme display
    public static String currentTheme;
    private Label timeLabel;
    private Label scoreLabel;
    private Label opponentScoreLabel;
    private int timeRemaining;
    private int currentScore = 0;
    private int opponentScore = 0;
    private javax.swing.Timer timer;
    private boolean isGameActive = true;
    // Singleton getter
    public static MazeVisualizer getInstance() {
        MazeVisualizer result = instance;
        if (result == null) {
            synchronized (LOCK) {
                result = instance;
                if (result == null) {
                    result = new MazeVisualizer();
                    instance = result;
                }
            }
        }
        return result;
    }

    @Override
    public void init() throws Exception {
        synchronized (LOCK) {
            if (instance == null) {
                instance = this;
            }
        }

        client = new Client("localhost", 5000);
        turn = false;
        timeRemaining = TURN_TIME;
        currentTheme = "";
        initializeTimer();

        new Thread(() -> {
            client.run();
            monitorTurnChanges();
        }).start();

        while (maze == null) {
            Thread.sleep(1000);
            System.out.println("Waiting for other player!");
        }
    }
    private void initializeTimer() {
        timer = new javax.swing.Timer(1000, e -> {
            if (timeRemaining > 0 ) {
                timeRemaining--;
                Platform.runLater(() -> updateTimeLabel());
            }
        });
        timer.start();
    }
    public static void updateTheme(String theme) {
        Platform.runLater(() -> {
            MazeVisualizer viz = getInstance();
            if (viz != null && viz.themeLabel != null) {
                viz.currentTheme = theme;
                viz.themeLabel.setText("Theme: " + theme);
            }
        });
    }
    private void updateTimeLabel() {
        if (timeLabel != null) {
            timeLabel.setText("Time: " + timeRemaining);
            if (timeRemaining <= 5) {
                timeLabel.setTextFill(Color.RED);
            } else {
                timeLabel.setTextFill(Color.WHITE);
            }
        }
    }

    // Method to handle turn changes
    public static void handleTurnChange(boolean isPlayerTurn) {
        Platform.runLater(() -> {
            MazeVisualizer viz = getInstance();
            if (viz != null && viz.isGameActive) {
                turn = isPlayerTurn;
                viz.timeRemaining = TURN_TIME;
                viz.updateTimeLabel();
                updateButtonStates(isPlayerTurn);
            }
        });
    }

    public static void updateScore(int score) {
        Platform.runLater(() -> {
            MazeVisualizer viz = getInstance();
            if (viz != null && viz.isGameActive) {
                viz.currentScore = score;
                viz.updateScoreLabels();
            }
        });
    }

    public static void updateOpponentScore(int score) {
        Platform.runLater(() -> {
            MazeVisualizer viz = getInstance();
            if (viz != null && viz.isGameActive) {
                viz.opponentScore = score;
                viz.updateScoreLabels();
            }
        });
    }

    private void updateScoreLabels() {
        scoreLabel.setText("Score: " + currentScore);
        opponentScoreLabel.setText("Opponent: " + opponentScore);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        synchronized (LOCK) {
            if (instance == this) {
                instance = null;
            }
        }
        if (timer != null) {
            timer.stop();
        }
    }
    private void showGameOverAlert(String message, boolean won) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);

        String style = won ?
                "-fx-background-color: #4CAF50;" :
                "-fx-background-color: #FF5722;";

        alert.getDialogPane().setStyle(style);
        alert.setContentText(String.format("""
            %s
            Your Score: %d
            Opponent Score: %d
            """, message, currentScore, opponentScore));

        alert.showAndWait();
    }


    private void monitorTurnChanges() {
        Thread turnMonitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100); // Check every 100ms
                    if (client != null && controlButtons != null) {
                        boolean currentTurn = turn;
                        Platform.runLater(() -> updateButtonStates(currentTurn));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        turnMonitor.setDaemon(true);
        turnMonitor.start();
    }

    public static void updateButtonStates(boolean enabled) {
        Platform.runLater(() -> {
            if (controlButtons != null) {
                for (int i = 0; i < controlButtons.length ; i++) {
                    controlButtons[i].setDisable(!enabled);
                }
            }
        });
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2b2b2b;");

        mazePane = new Pane();
        mazePane.setBackground(new Background(new BackgroundFill(Color.web("#3c3f41"), CornerRadii.EMPTY, Insets.EMPTY)));

        // Create top status bar
        HBox statusBar = new HBox(20);
        statusBar.setAlignment(Pos.CENTER);
        statusBar.setPadding(new Insets(10));
        statusBar.setStyle("-fx-background-color: #3c3f41;");

        timeLabel = new Label("Time: 20");
        scoreLabel = new Label("Score: 0");
        opponentScoreLabel = new Label("Opponent: 0");
        themeLabel = new Label("Theme: " + currentTheme.toUpperCase()); // Initialize themeLabel here

        // Style the labels
        Font statusFont = Font.font("Arial", FontWeight.BOLD, 14);
        Color textColor = Color.WHITE;

        timeLabel.setFont(statusFont);
        scoreLabel.setFont(statusFont);
        opponentScoreLabel.setFont(statusFont);
        themeLabel.setFont(statusFont);
        timeLabel.setTextFill(textColor);
        scoreLabel.setTextFill(textColor);
        opponentScoreLabel.setTextFill(textColor);
        themeLabel.setTextFill(textColor);

        statusBar.getChildren().addAll(timeLabel, themeLabel, scoreLabel, opponentScoreLabel);
        root.setTop(statusBar);

        // Control buttons setup
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setPadding(new Insets(15));
        controlBox.setStyle("-fx-background-color: #3c3f41;");

        controlButtons = new Button[] {
                createStyledButton("↑", "-fx-base: #4CAF50;", -1, 0),
                createStyledButton("↓", "-fx-base: #4CAF50;", 1, 0),
                createStyledButton("←", "-fx-base: #2196F3;", 0, -1),
                createStyledButton("→", "-fx-base: #2196F3;", 0, 1),
                createStyledButton("↖", "-fx-base: #9C27B0;", -1, -1),
                createStyledButton("↗", "-fx-base: #9C27B0;", -1, 1),
                createStyledButton("↙", "-fx-base: #9C27B0;", 1, -1),
                createStyledButton("↘", "-fx-base: #9C27B0;", 1, 1)
        };

        controlBox.getChildren().addAll(controlButtons);
        root.setBottom(controlBox);
        root.setCenter(mazePane);

        updateButtonStates(turn);
        regenerateMaze();

        Scene scene = new Scene(root, MAZE_WIDTH * CELL_SIZE + 40, MAZE_HEIGHT * CELL_SIZE + 120);
        primaryStage.setTitle("Multiplayer Maze Game");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the timer when the game begins
        timer.start();
    }


    public static void handleGameOver() {
        Platform.runLater(() -> {
            MazeVisualizer viz = getInstance();
            if (viz != null) {
                viz.isGameActive = false;
                viz.timer.stop();
                viz.updateScoreLabels();
                boolean won =  viz.currentScore > viz.opponentScore ;
                viz.showGameOverAlert(won ? "Congratulations! You won!" : "Game Over! You lost!", won);
            }
        });
    }




    private Button createStyledButton(String text, String style, int deltaRow, int deltaCol) {
        Button button = new Button(text);
        button.setStyle(style + " -fx-text-fill: white; -fx-font-weight: bold;");
        button.setFont(Font.font("Arial", 16));
        button.setMinSize(50, 50);
        if (!text.equals("New Game")) {
            button.setOnAction(e -> movePlayer(deltaRow, deltaCol));
        }
        return button;
    }

    private void movePlayer(int deltaRow, int deltaCol) {
        int newRow = playerRow + deltaRow;
        int newCol = playerCol + deltaCol;

        if (isValidMove(playerRow, playerCol, newRow, newCol)) {
            updatePlayerPosition(newRow, newCol);

            Node currentNode = maze[newRow][newCol];
            client.sendNodeToServer(currentNode);
        }
    }

    public void updatePlayerPosition(int newRow, int newCol) {
        // Validate newRow and newCol
        if (newRow < 0 || newRow >= MAZE_HEIGHT || newCol < 0 || newCol >= MAZE_WIDTH) {
            throw new IllegalArgumentException("Invalid player position: (" + newRow + ", " + newCol + ")");
        }

        // Update the player's position
        cellRectangles[playerRow][playerCol].setFill(Color.TRANSPARENT);
        playerRow = newRow;
        playerCol = newCol;
        player.setCenterX(newCol * CELL_SIZE + CELL_SIZE / 2);
        player.setCenterY(newRow * CELL_SIZE + CELL_SIZE / 2);
        cellRectangles[newRow][newCol].setFill(Color.LIGHTBLUE);
    }

    // Method to update the other player's position
    public static void updateOtherPlayerPosition(int newRow, int newCol) {
        Platform.runLater(() -> {
            if (otherPlayer == null) {
                // Create the other player's circle if it doesn't exist
                otherPlayer = new Circle(CELL_SIZE / 3);
                otherPlayer.setFill(Color.TRANSPARENT);
                otherPlayer.setStroke(Color.web("#FF5722")); // Different color for the other player
                otherPlayer.setStrokeWidth(2);
                mazePane.getChildren().add(otherPlayer);
            }

            // Update the other player's position
            otherPlayer.setCenterX(newCol * CELL_SIZE + CELL_SIZE / 2);
            otherPlayer.setCenterY(newRow * CELL_SIZE + CELL_SIZE / 2);
        });
    }

    private boolean isValidMove(int oldRow, int oldCol, int newRow, int newCol) {
        if (newRow < 0 || newRow >= MAZE_HEIGHT || newCol < 0 || newCol >= MAZE_WIDTH) return false;

        int rowDiff = newRow - oldRow;
        int colDiff = newCol - oldCol;

        return possibleMove(rowDiff, colDiff,
                maze[oldRow][oldCol].getBorders(),
                maze[newRow][newCol].getBorders());
    }

    private boolean possibleMove(int dRow, int dCol, boolean[] cBorders, boolean[] nBorders) {
        return switch (dRow) {
            case -1 -> {
                yield switch (dCol) {
                    case -1 -> (!cBorders[0] && !cBorders[3]) && (!nBorders[1] && !nBorders[2]);
                    case 0 -> !cBorders[0] && !nBorders[2];
                    case 1 -> (!cBorders[0] && !cBorders[1]) && (!nBorders[2] && !nBorders[3]);
                    default -> false;
                };
            }
            case 1 -> {
                yield switch (dCol) {
                    case -1 -> (!cBorders[2] && !cBorders[3]) && (!nBorders[1] && !nBorders[0]);
                    case 0 -> !cBorders[2] && !nBorders[0];
                    case 1 -> (!cBorders[1] && !cBorders[2]) && (!nBorders[0] && !nBorders[3]);
                    default -> false;
                };
            }
            case 0 -> {
                yield switch (dCol) {
                    case -1 -> !cBorders[3] && !nBorders[1];
                    case 0 -> true;
                    case 1 -> !cBorders[1] && !nBorders[3];
                    default -> false;
                };
            }
            default -> false;
        };
    }

    private void regenerateMaze() {
        mazePane.getChildren().clear();
        cellRectangles = new Rectangle[MAZE_HEIGHT][MAZE_WIDTH];

        for (int y = 0; y < MAZE_HEIGHT; y++) {
            for (int x = 0; x < MAZE_WIDTH; x++) {
                Node node = maze[y][x];
                double startX = x * CELL_SIZE;
                double startY = y * CELL_SIZE;

                Rectangle bgRect = new Rectangle(startX, startY, CELL_SIZE, CELL_SIZE);
                bgRect.setFill(Color.TRANSPARENT);
                mazePane.getChildren().add(bgRect);
                cellRectangles[y][x] = bgRect;

                boolean[] borders = node.getBorders();
                if (borders[0]) mazePane.getChildren().add(new Line(startX, startY, startX + CELL_SIZE, startY));
                if (borders[1]) mazePane.getChildren().add(new Line(startX + CELL_SIZE, startY, startX + CELL_SIZE, startY + CELL_SIZE));
                if (borders[2]) mazePane.getChildren().add(new Line(startX, startY + CELL_SIZE, startX + CELL_SIZE, startY + CELL_SIZE));
                if (borders[3]) mazePane.getChildren().add(new Line(startX, startY, startX, startY + CELL_SIZE));

                Label label = new Label(String.valueOf(node.getValue()));
                label.setFont(Font.font("Arial", FontWeight.MEDIUM, 20));
                label.setLayoutX(startX + (CELL_SIZE - label.getWidth()) / 3);
                label.setTextFill(Color.WHITE);
                label.setLayoutY(startY);
                label.setAlignment(Pos.CENTER);
                mazePane.getChildren().add(label);

                if (startNode != null && endNode != null) {
                    if (node.getRow() == startNode.getRow() && node.getColumn() == startNode.getColumn()) {
                        drawSpecialCell(startX, startY, String.valueOf(node.getValue()), "#4CAF50", "🚩");
                    } else if (node.getRow() == endNode.getRow() && node.getColumn() == endNode.getColumn()) {
                        drawSpecialCell(startX, startY, String.valueOf(node.getValue()), "#FF5722", "🏁");
                    }
                }
            }
        }

        // Ensure player position is valid
        if (startNode != null) {
            playerRow = startNode.getRow();
            playerCol = startNode.getColumn();
        } else {
            playerRow = 0;
            playerCol = 0;
        }

        createPlayer();
        cellRectangles[playerRow][playerCol].setFill(Color.LIGHTBLUE);
    }

    private void drawSpecialCell(double x, double y, String value, String color, String emoji) {
        Rectangle rect = new Rectangle(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        rect.setFill(Color.web(color + "80"));
        rect.setStroke(Color.web(color));
        rect.setStrokeWidth(3);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        Label label = new Label(emoji + " " + value);
        label.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 14; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
        label.setLayoutX(x + 5);
        label.setLayoutY(y + 8);

        mazePane.getChildren().addAll(rect, label);
    }

    private void createPlayer() {
        player = new Circle(CELL_SIZE / 3);
        player.setFill(Color.TRANSPARENT);
        player.setStroke(Color.web("#2196F3"));
        player.setStrokeWidth(2);
        updatePlayerPosition(playerRow, playerCol);
        mazePane.getChildren().add(player);
    }

    public static void main(String[] args) {
        launch(args);
    }
}