    package org.algorithm.visualizer;

    import javafx.animation.Animation;
    import javafx.animation.KeyFrame;
    import javafx.animation.Timeline;
    import javafx.application.Application;
    import javafx.application.Platform;
    import javafx.beans.binding.Bindings;
    import javafx.beans.property.SimpleStringProperty;
    import javafx.beans.property.StringProperty;
    import javafx.geometry.Insets;
    import javafx.geometry.Pos;
    import javafx.scene.Scene;
    import javafx.scene.media.AudioClip;
    import javafx.scene.control.Button;
    import javafx.scene.control.Label;
    import javafx.scene.image.Image;
    import javafx.scene.input.KeyEvent;
    import javafx.scene.layout.*;
    import javafx.scene.paint.Color;
    import javafx.scene.paint.ImagePattern;
    import javafx.scene.shape.*;
    import javafx.scene.text.Font;
    import javafx.scene.text.FontWeight;
    import javafx.stage.Stage;
    import javafx.stage.StageStyle;
    import javafx.util.Duration;
    import org.algorithm.client.Client;
    import org.algorithm.components.Node;

    public class MazeVisualizer extends Application {
        private static volatile MazeVisualizer instance;
        private static final Object LOCK = new Object();

        private static final int CELL_SIZE = 50;
        private static final int TURN_TIME = 20; // 15 seconds per turn

        public static int MAZE_WIDTH;
        public static int MAZE_HEIGHT;
        public static Node[][] maze;

        private static Pane mazePane;
        private Arc player;
        private static Circle otherPlayer;
        private int playerRow;
        private int playerCol;
        public static Node startNode;
        public static Node endNode;
        private Rectangle[][] cellRectangles;
        private static Button[] controlButtons;
        private Client client;
        static public volatile Boolean turn;
        private Label themeLabel;

        public static final StringProperty themeProperty = new SimpleStringProperty("");
        private Label timeLabel;
        private Label scoreLabel;
        private Label opponentScoreLabel;
        private int timeRemaining;
        private int currentScore = 0;
        private int opponentScore = 0;
        private javax.swing.Timer timer;
        private boolean isGameActive = true;
        private static final Color BACKGROUND_COLOR = Color.web("#000000");
        private static final Color WALL_COLOR = Color.web("#2121DE");
        private static final Color PLAYER_COLOR = Color.web("#FFFF00");
        private static final Color OPPONENT_COLOR = Color.web("#FF0000");
        private static final Color PLAYER_PATH_COLOR = Color.web("#FFFF0040");
        private static final Color OPPONENT_PATH_COLOR = Color.web("#FF000040");
        private static final Color TEXT_COLOR = Color.web("#FFFFFF");
        private static final int PLAYER_SIZE = CELL_SIZE*2 / 5;
        private Rectangle[][] playerPathCells;
        private Rectangle[][] opponentPathCells;
        private Arc pacmanArc;
        private Timeline pacmanAnimation;
        private AudioClip wakawaka;
        private AudioClip death;
        private AudioClip gameStart;
        private AudioClip victory;
        private static AudioClip bonus ;
        private boolean mouthOpen = true;

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
            initializeTimer();

            new Thread(() -> {
                client.run();
                monitorTurnChanges();
            }).start();

            while (maze == null) {
                Thread.sleep(1000);
                System.out.println("Waiting for other player!");
            }
            wakawaka = new AudioClip(getClass().getResource("/sounds/wakawaka.wav").toExternalForm());
            death = new AudioClip(getClass().getResource("/sounds/death.wav").toExternalForm());
            gameStart = new AudioClip(getClass().getResource("/sounds/game_start.wav").toExternalForm());
            victory = new AudioClip(getClass().getResource("/sounds/victory.wav").toExternalForm());
            bonus=new AudioClip(getClass().getResource("/sounds/bonus.wav").toExternalForm());
            initializePacmanAnimation();
        }
        private void initializePacmanAnimation() {
            pacmanArc = new Arc(0, 0, PLAYER_SIZE, PLAYER_SIZE, 45, 270);
            pacmanArc.setFill(PLAYER_COLOR);
            pacmanArc.setType(ArcType.ROUND);

            pacmanAnimation = new Timeline(
                    new KeyFrame(Duration.millis(100), e -> {
                        mouthOpen = !mouthOpen;
                        pacmanArc.setStartAngle(mouthOpen ? 5 : 45);
                        pacmanArc.setLength(mouthOpen ? 350 : 270);
                    })
            );
            pacmanAnimation.setCycleCount(Timeline.INDEFINITE);
            pacmanAnimation.play();

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

                    if (viz.currentScore != score) {
                        bonus.play();
                        viz.currentScore = score;
                        viz.updateScoreLabels();
                    }
                }
            });
        }


        public static void updateOpponentScore(int score) {
            Platform.runLater(() -> {
                MazeVisualizer viz = getInstance();
                if (viz != null && viz.isGameActive) {

                    if (viz.opponentScore != score) {
                        bonus.play();
                        viz.opponentScore = score;
                        viz.updateScoreLabels();
                    }
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
        public void showGameOverAlert(String message, boolean won) {

            pacmanAnimation.stop();



            if (won) {
                victory.play();
            } else {
                death.play();
            }


            Stage dialogStage = new Stage(StageStyle.TRANSPARENT);
            VBox dialogVbox = new VBox(20);
            dialogVbox.setAlignment(Pos.CENTER);
            dialogVbox.setPadding(new Insets(30));
            dialogVbox.setStyle("""
                -fx-background-color: #000000;
                -fx-border-color: #2121DE;
                -fx-border-width: 3;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-effect: dropshadow(gaussian, #0000FF, 20, 0.5, 0, 0);
                """);


            Label titleLabel = new Label("GAME OVER");
            titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
            titleLabel.setTextFill(won ? Color.web("#FFD700") : Color.web("#FF0000"));
            titleLabel.setStyle("-fx-effect: dropshadow(gaussian, " + (won ? "#FFD700" : "#FF0000") + ", 10, 0.7, 0, 0);");

            // Result message
            Label messageLabel = new Label(message);
            messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            messageLabel.setTextFill(Color.WHITE);

            // Scores
            Label scoreLabel = new Label(String.format("Your Score: %d", currentScore));
            Label opponentScoreLabel = new Label(String.format("Opponent Score: %d", opponentScore));
            scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            opponentScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
            scoreLabel.setTextFill(Color.WHITE);
            opponentScoreLabel.setTextFill(Color.WHITE);

            // Close button
            Button closeButton = new Button("Close");
            closeButton.setStyle("""
                -fx-background-color: #2121DE;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-font-size: 16;
                -fx-padding: 10 20;
                -fx-background-radius: 5;
                -fx-effect: dropshadow(gaussian, #0000FF, 5, 0.5, 0, 0);
                """);
            closeButton.setOnAction(e -> dialogStage.close());

            dialogVbox.getChildren().addAll(titleLabel, messageLabel, scoreLabel, opponentScoreLabel, closeButton);

            Scene dialogScene = new Scene(dialogVbox);
            dialogScene.setFill(null);
            dialogStage.setScene(dialogScene);

            // Center the dialog on the screen
            dialogStage.setOnShown(e -> {
                Stage mainStage = (Stage) mazePane.getScene().getWindow();
                dialogStage.setX(mainStage.getX() + (mainStage.getWidth() - dialogStage.getWidth()) / 2);
                dialogStage.setY(mainStage.getY() + (mainStage.getHeight() - dialogStage.getHeight()) / 2);
            });

            dialogStage.show();
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
            root.setStyle("-fx-background-color: #000000;");

            // Create centered maze container
            StackPane mazeContainer = new StackPane();
            mazeContainer.setPadding(new Insets(20));
            mazeContainer.setAlignment(Pos.CENTER); // Ensure StackPane centers its content

            mazePane = new Pane();
            try {
                Image backgroundImage = new Image(getClass().getResourceAsStream("/images/c.jpg"));//"/images/"+currentTheme.toLowerCase()+".jpg"
                ImagePattern backgroundPattern = new ImagePattern(backgroundImage);
                mazePane.setBackground(new Background(new BackgroundFill(backgroundPattern, CornerRadii.EMPTY, Insets.EMPTY)));
            } catch (Exception e) {
                System.err.println("Failed to load background image: " + e.getMessage());
                mazePane.setBackground(new Background(new BackgroundFill(BACKGROUND_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
            }

            // Calculate the exact size needed for the maze
            double mazeWidth = MAZE_WIDTH * CELL_SIZE;
            double mazeHeight = MAZE_HEIGHT * CELL_SIZE;

            // Set the preferred size for mazePane
            mazePane.setPrefSize(mazeWidth, mazeHeight);
            mazePane.setMaxSize(mazeWidth, mazeHeight);
            mazePane.setMinSize(mazeWidth, mazeHeight);

            // Wrap mazePane in a centering container
            VBox centeringBox = new VBox(mazePane);
            centeringBox.setAlignment(Pos.CENTER);

            // Add the centering container to the mazeContainer
            mazeContainer.getChildren().add(centeringBox);

            // Set the mazeContainer to grow and center in the available space
            VBox.setVgrow(mazeContainer, Priority.ALWAYS);
            HBox.setHgrow(mazeContainer, Priority.ALWAYS);

            // Initialize path tracking arrays
            playerPathCells = new Rectangle[MAZE_HEIGHT][MAZE_WIDTH];
            opponentPathCells = new Rectangle[MAZE_HEIGHT][MAZE_WIDTH];

            // Create Pac-Man styled status bar
            HBox statusBar = new HBox(20);
            statusBar.setAlignment(Pos.CENTER);
            statusBar.setPadding(new Insets(10));
            statusBar.setStyle("-fx-background-color: #000000; -fx-border-color: #2121DE; -fx-border-width: 0 0 2 0;");

            timeLabel = new Label("Time: 20");
            scoreLabel = new Label("Score: 0");
            opponentScoreLabel = new Label("Opponent: 0");
            Label themeLabel = new Label();
            themeLabel.textProperty().bind(
                    Bindings.createStringBinding(
                            () -> "Theme: " + themeProperty.get().toUpperCase(),
                            themeProperty
                    )
            );


            // Style the labels with Pac-Man theme
            Font arcadeFont = Font.font("Arial", FontWeight.BOLD, 16);
            styleLabel(timeLabel, arcadeFont);
            styleLabel(scoreLabel, arcadeFont);
            styleLabel(opponentScoreLabel, arcadeFont);
            styleLabel(themeLabel, arcadeFont);

            statusBar.getChildren().addAll(timeLabel, themeLabel, scoreLabel, opponentScoreLabel);
            root.setTop(statusBar);

            // Create Pac-Man styled control buttons (keeping them as backup)
            HBox controlBox = new HBox(15);
            controlBox.setAlignment(Pos.CENTER);
            controlBox.setPadding(new Insets(15));
            controlBox.setStyle("-fx-background-color: #000000; -fx-border-color: #2121DE; -fx-border-width: 2 0 0 0;");

            controlButtons = new Button[] {
                    createPacManButton("↑", -1, 0),
                    createPacManButton("↓", 1, 0),
                    createPacManButton("←", 0, -1),
                    createPacManButton("→", 0, 1),
                    createPacManButton("↖", -1, -1),
                    createPacManButton("↗", -1, 1),
                    createPacManButton("↙", 1, -1),
                    createPacManButton("↘", 1, 1)
            };

            controlBox.getChildren().addAll(controlButtons);
            root.setBottom(controlBox);
            root.setCenter(mazeContainer);

            updateButtonStates(turn);
            regenerateMaze();

            // Calculate the window size based on the maze size
            double windowWidth = Math.max(MAZE_WIDTH * CELL_SIZE + 100, 800); // Minimum width of 800
            double windowHeight = MAZE_HEIGHT * CELL_SIZE + 200; // Extra space for controls

            Scene scene = new Scene(root, windowWidth, windowHeight);

            // Add keyboard controls
            scene.setOnKeyPressed(this::handleKeyPress);

            primaryStage.setTitle("Pac-Man Maze Game");
            primaryStage.setScene(scene);
            primaryStage.show();
            gameStart.play();
            timer.start();
        }
        private void handleKeyPress(KeyEvent event) {
            if (!turn) return; // Only process keyboard input during player's turn

            switch (event.getText().toLowerCase()) {
                case "z" -> movePlayer(-1, 0);  // Up
                case "s" -> movePlayer(1, 0);   // Down
                case "q" -> movePlayer(0, -1);  // Left
                case "d" -> movePlayer(0, 1);   // Right
                case "e" -> movePlayer(-1, 1);  // Top-right
                case "a" -> movePlayer(-1, -1); // Top-left
                case "w" -> movePlayer(1, -1);  // Bottom-left
                case "c" -> movePlayer(1, 1);   // Bottom-right
            }
        }

        private void styleLabel(Label label, Font font) {
            label.setFont(font);
            label.setTextFill(TEXT_COLOR);
            label.setStyle("-fx-effect: dropshadow(gaussian, #2121DE, 2, 0.5, 0, 0);");
        }
        private Button createPacManButton(String text, int deltaRow, int deltaCol) {
            Button button = new Button(text);
            button.setStyle("""
                -fx-background-color: #2121DE;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(gaussian, #0000FF, 5, 0.5, 0, 0);
                -fx-border-color: #4242FF;
                -fx-border-width: 2;
                """);
            button.setFont(Font.font("Arial", 16));
            button.setMinSize(50, 50);
            button.setOnAction(e -> movePlayer(deltaRow, deltaCol));

            // Add hover effect
            button.setOnMouseEntered(e -> button.setStyle("""
                -fx-background-color: #4242FF;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(gaussian, #0000FF, 8, 0.8, 0, 0);
                -fx-border-color: #6363FF;
                -fx-border-width: 2;
                """));
            button.setOnMouseExited(e -> button.setStyle("""
                -fx-background-color: #2121DE;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-effect: dropshadow(gaussian, #0000FF, 5, 0.5, 0, 0);
                -fx-border-color: #4242FF;
                -fx-border-width: 2;
                """));

            return button;
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






        private void movePlayer(int deltaRow, int deltaCol) {
            int newRow = playerRow + deltaRow;
            int newCol = playerCol + deltaCol;

            if (isValidMove(playerRow, playerCol, newRow, newCol)) {
                wakawaka.play();

                // Just update rotation without stopping the animation
                double angle = Math.toDegrees(Math.atan2(deltaRow, deltaCol));
                pacmanArc.setRotate(angle);

                updatePlayerPosition(newRow, newCol);
                Node currentNode = maze[newRow][newCol];
                client.sendNodeToServer(currentNode);
            }
        }

        public void updatePlayerPosition(int newRow, int newCol) {
            if (newRow < 0 || newRow >= MAZE_HEIGHT || newCol < 0 || newCol >= MAZE_WIDTH) {
                throw new IllegalArgumentException("Invalid player position: (" + newRow + ", " + newCol + ")");
            }

            // Add path cell for previous position
            if (playerPathCells[playerRow][playerCol] == null) {
                Rectangle pathCell = new Rectangle(
                        playerCol * CELL_SIZE,
                        playerRow * CELL_SIZE,
                        CELL_SIZE,
                        CELL_SIZE
                );
                pathCell.setFill(PLAYER_PATH_COLOR);
                mazePane.getChildren().add(pathCell);
                playerPathCells[playerRow][playerCol] = pathCell;
            }

            playerRow = newRow;
            playerCol = newCol;
            player.setCenterX(newCol * CELL_SIZE + CELL_SIZE / 2);
            player.setCenterY(newRow * CELL_SIZE + CELL_SIZE / 2);
        }


        // Method to update the other player's position
        public static void updateOtherPlayerPosition(int newRow, int newCol) {
            Platform.runLater(() -> {
                MazeVisualizer viz = getInstance();
                if (viz != null) {
                    // Add path cell for opponent
                    if (viz.opponentPathCells[newRow][newCol] == null) {
                        Rectangle pathCell = new Rectangle(
                                newCol * CELL_SIZE,
                                newRow * CELL_SIZE,
                                CELL_SIZE,
                                CELL_SIZE
                        );
                        pathCell.setFill(OPPONENT_PATH_COLOR);
                        mazePane.getChildren().add(pathCell);
                        viz.opponentPathCells[newRow][newCol] = pathCell;
                    }

                    if (otherPlayer == null) {
                        otherPlayer = new Circle(PLAYER_SIZE);
                        otherPlayer.setFill(OPPONENT_COLOR);
                        otherPlayer.setStroke(Color.WHITE);
                        otherPlayer.setStrokeWidth(2);
                        mazePane.getChildren().add(otherPlayer);
                    }

                    otherPlayer.setCenterX(newCol * CELL_SIZE + CELL_SIZE / 2);
                    otherPlayer.setCenterY(newRow * CELL_SIZE + CELL_SIZE / 2);
                }
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
            playerPathCells = new Rectangle[MAZE_HEIGHT][MAZE_WIDTH];
            opponentPathCells = new Rectangle[MAZE_HEIGHT][MAZE_WIDTH];

            for (int y = 0; y < MAZE_HEIGHT; y++) {
                for (int x = 0; x < MAZE_WIDTH; x++) {
                    Node node = maze[y][x];
                    double startX = x * CELL_SIZE;
                    double startY = y * CELL_SIZE;

                    Rectangle bgRect = new Rectangle(startX, startY, CELL_SIZE, CELL_SIZE);
                    bgRect.setFill(Color.TRANSPARENT);
                    mazePane.getChildren().add(bgRect);
                    cellRectangles[y][x] = bgRect;

                    // Draw walls with Pac-Man style
                    boolean[] borders = node.getBorders();
                    if (borders[0]) drawPacManWall(startX, startY, startX + CELL_SIZE, startY);
                    if (borders[1]) drawPacManWall(startX + CELL_SIZE, startY, startX + CELL_SIZE, startY + CELL_SIZE);
                    if (borders[2]) drawPacManWall(startX, startY + CELL_SIZE, startX + CELL_SIZE, startY + CELL_SIZE);
                    if (borders[3]) drawPacManWall(startX, startY, startX, startY + CELL_SIZE);

                    // Draw node value
                    Label label = new Label(String.valueOf(node.getValue()));
                    label.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 20));
                    label.setTextFill(TEXT_COLOR);
                    label.setLayoutX(startX + (CELL_SIZE - label.getWidth()) / 3);
                    label.setLayoutY(startY + (CELL_SIZE - label.getWidth()) / 4);
                    label.setAlignment(Pos.CENTER);
                    mazePane.getChildren().add(label);

                    // Draw special cells (start/end)
                    if (startNode != null && endNode != null) {
                        if (node.getRow() == startNode.getRow() && node.getColumn() == startNode.getColumn()) {
                            drawSpecialPacManCell(startX, startY, String.valueOf(node.getValue()), "#FFD700");
                        } else if (node.getRow() == endNode.getRow() && node.getColumn() == endNode.getColumn()) {
                            drawSpecialPacManCell(startX, startY, String.valueOf(node.getValue()), "#FF0000");
                        }
                    }
                }
            }

            if (startNode != null) {
                playerRow = startNode.getRow();
                playerCol = startNode.getColumn();
            } else {
                playerRow = 0;
                playerCol = 0;
            }

            createPacManPlayer();
        }
        private void drawPacManWall(double startX, double startY, double endX, double endY) {
            Line wall = new Line(startX, startY, endX, endY);
            wall.setStroke(WALL_COLOR);
            wall.setStrokeWidth(3);
            wall.setStyle("-fx-effect: dropshadow(gaussian, #0000FF, 5, 0.5, 0, 0);");
            mazePane.getChildren().add(wall);
        }

        private void drawSpecialPacManCell(double x, double y, String value, String color) {
            Rectangle rect = new Rectangle(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
            rect.setFill(Color.web(color + "40"));
            rect.setStroke(Color.web(color));
            rect.setStrokeWidth(3);
            rect.setArcWidth(10);
            rect.setArcHeight(10);
            rect.setStyle("-fx-effect: dropshadow(gaussian, " + color + ", 10, 0.5, 0, 0);");



            mazePane.getChildren().addAll(rect);
        }

        private void createPacManPlayer() {
            player = pacmanArc;
            player.setFill(PLAYER_COLOR);
            player.setStroke(Color.TRANSPARENT);
            player.setStrokeWidth(2);
            updatePlayerPosition(playerRow, playerCol);
            mazePane.getChildren().add(player);

            // Make sure animation is running
            if (!pacmanAnimation.getStatus().equals(Animation.Status.RUNNING)) {
                pacmanAnimation.play();
            }
        }

        public static void main(String[] args) {
            launch(args);
        }
    }