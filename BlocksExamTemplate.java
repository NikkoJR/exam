import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Scanner;

public class BlocksExamTemplate extends Application {
    int[][] grid;
    int selectedId = 0;
    int currentLevel = 1;
    int moves = 0;
    long elapsedSeconds = 0;
    boolean solved = false;

    GridPane board = new GridPane();
    Label info = new Label();
    ArrayDeque<GameState> undo = new ArrayDeque<>();
    Timeline timer;

    String[] colors = {
            "#ef5350", "#42a5f5", "#66bb6a", "#ffa726", "#ab47bc", "#ffee58",
            "#26c6da", "#8d6e63", "#ec407a", "#7e57c2", "#78909c", "#d4e157"
    };

    @Override
    public void start(Stage stage) throws Exception {
        loadLevelFile(levelFileName());
        BorderPane root = new BorderPane();
        HBox controls = new HBox(8);
        Button prev = new Button("Prev");
        Button next = new Button("Next");
        Button save = new Button("Save");
        Button load = new Button("Load");
        Button undoButton = new Button("Undo");
        controls.getChildren().addAll(prev, next, save, load, undoButton, info);
        root.setCenter(board);
        root.setBottom(controls);

        prev.setOnAction(e -> changeLevel(-1));
        next.setOnAction(e -> changeLevel(1));
        save.setOnAction(e -> saveGame("blocks-save.txt"));
        load.setOnAction(e -> loadGame("blocks-save.txt"));
        undoButton.setOnAction(e -> undoMove());

        Scene scene = new Scene(root, 850, 850);
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP:
                    tryMove(selectedId, -1, 0);
                    break;
                case DOWN:
                    tryMove(selectedId, 1, 0);
                    break;
                case LEFT:
                    tryMove(selectedId, 0, -1);
                    break;
                case RIGHT:
                    tryMove(selectedId, 0, 1);
                    break;
            }
        });

        stage.setScene(scene);
        stage.setTitle("Blocks Exam Template");
        stage.show();
        drawGrid();
        startTimer();
        root.requestFocus();
    }

    String levelFileName() {
        return "blocks" + currentLevel + ".txt";
    }

    void loadLevelFile(String filename) throws Exception {
        ArrayList<int[]> rows = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            int[] row = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                row[i] = Integer.parseInt(parts[i]);
            }
            rows.add(row);
        }
        grid = new int[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            grid[i] = rows.get(i);
        }
        selectedId = 0;
        moves = 0;
        elapsedSeconds = 0;
        solved = false;
        undo.clear();
        System.out.println("Rectangular blocks: " + countRectangles());
    }

    void drawGrid() {
        board.getChildren().clear();
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                int value = grid[row][col];
                StackPane cell = new StackPane();
                Rectangle rect = new Rectangle(cellWidth(row, col), cellHeight(row, col));
                rect.setFill(colorFor(value));
                rect.setStroke(value == selectedId && value > 0 && !isBorder(row, col) ? Color.BLACK : Color.GRAY);
                rect.setStrokeWidth(value == selectedId && value > 0 && !isBorder(row, col) ? 4 : 1);
                cell.getChildren().add(rect);

                if (value > 0) {
                    Label text = new Label(String.valueOf(value));
                    cell.getChildren().add(text);
                }

                int id = value;
                int r = row;
                int c = col;
                cell.setOnMouseClicked(e -> {
                    if (id > 0 && !isBorder(r, c)) {
                        selectedId = id;
                        drawGrid();
                    }
                });
                board.add(cell, col, row);
            }
        }
        updateInfo();
    }

    double cellWidth(int row, int col) {
        if (isBorder(row, col)) {
            return 25;
        }
        return 70;
    }

    double cellHeight(int row, int col) {
        if (isBorder(row, col)) {
            return 25;
        }
        return 70;
    }

    boolean isBorder(int row, int col) {
        return row == 0 || row == grid.length - 1 || col == 0 || col == grid[row].length - 1;
    }

    Color colorFor(int value) {
        if (value == -1) {
            return Color.BLACK;
        }
        if (value == 0) {
            return Color.WHITE;
        }
        return Color.web(colors[(value - 1) % colors.length]);
    }

    void tryMove(int id, int dr, int dc) {
        if (id <= 0 || solved) {
            return;
        }
        int mode = moveMode(id, dr, dc);
        if (mode == 0) {
            return;
        }
        undo.push(new GameState(copyGrid(), moves, elapsedSeconds));
        if (mode == 2) {
            removeBlock(id);
        } else {
            int[][] old = copyGrid();
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[row].length; col++) {
                    if (grid[row][col] == id && !isBorder(row, col)) {
                        grid[row][col] = 0;
                    }
                }
            }
            for (int row = 0; row < old.length; row++) {
                for (int col = 0; col < old[row].length; col++) {
                    if (old[row][col] == id && !isBorder(row, col)) {
                        grid[row + dr][col + dc] = id;
                    }
                }
            }
        }
        moves++;
        if (!containsBlockInside()) {
            solved = true;
            info.setText("Solved. Next level in 10 seconds.");
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(10), e -> changeLevel(1)));
            t.setCycleCount(1);
            t.play();
        }
        drawGrid();
    }

    int moveMode(int id, int dr, int dc) {
        boolean exits = false;
        for (int row = 1; row < grid.length - 1; row++) {
            for (int col = 1; col < grid[row].length - 1; col++) {
                if (grid[row][col] == id) {
                    int nr = row + dr;
                    int nc = col + dc;
                    if (nr < 0 || nr >= grid.length || nc < 0 || nc >= grid[nr].length) {
                        return 0;
                    }
                    if (isBorder(nr, nc)) {
                        if (grid[nr][nc] == id) {
                            exits = true;
                        } else {
                            return 0;
                        }
                    } else if (grid[nr][nc] != 0 && grid[nr][nc] != id) {
                        return 0;
                    }
                }
            }
        }
        if (exits) {
            return 2;
        }
        return 1;
    }

    void removeBlock(int id) {
        for (int row = 1; row < grid.length - 1; row++) {
            for (int col = 1; col < grid[row].length - 1; col++) {
                if (grid[row][col] == id) {
                    grid[row][col] = 0;
                }
            }
        }
        if (selectedId == id) {
            selectedId = 0;
        }
    }

    boolean containsBlockInside() {
        for (int row = 1; row < grid.length - 1; row++) {
            for (int col = 1; col < grid[row].length - 1; col++) {
                if (grid[row][col] > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    int findMaxId() {
        int max = 0;
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] > max) {
                    max = grid[row][col];
                }
            }
        }
        return max;
    }

    int countRectangles() {
        int count = 0;
        int max = findMaxId();
        for (int id = 1; id <= max; id++) {
            if (isRectangle(id)) {
                count++;
            }
        }
        return count;
    }

    boolean isRectangle(int id) {
        Bounds b = getBounds(id);
        if (b.cells == 0) {
            return false;
        }
        return b.cells == b.width() * b.height();
    }

    Bounds getBounds(int id) {
        Bounds b = new Bounds();
        b.minRow = grid.length;
        b.maxRow = -1;
        b.minCol = grid[0].length;
        b.maxCol = -1;
        b.cells = 0;
        for (int row = 1; row < grid.length - 1; row++) {
            for (int col = 1; col < grid[row].length - 1; col++) {
                if (grid[row][col] == id) {
                    b.cells++;
                    if (row < b.minRow) b.minRow = row;
                    if (row > b.maxRow) b.maxRow = row;
                    if (col < b.minCol) b.minCol = col;
                    if (col > b.maxCol) b.maxCol = col;
                }
            }
        }
        return b;
    }

    int[][] copyGrid() {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = new int[grid[row].length];
            for (int col = 0; col < grid[row].length; col++) {
                copy[row][col] = grid[row][col];
            }
        }
        return copy;
    }

    void changeLevel(int delta) {
        int next = currentLevel + delta;
        if (next < 1) {
            next = 1;
        }
        if (next > 6) {
            next = 6;
        }
        currentLevel = next;
        try {
            loadLevelFile(levelFileName());
            drawGrid();
        } catch (Exception ex) {
            info.setText("Cannot load " + levelFileName());
        }
    }

    void undoMove() {
        if (undo.isEmpty()) {
            return;
        }
        GameState state = undo.pop();
        grid = state.grid;
        moves = state.moves;
        elapsedSeconds = state.elapsedSeconds;
        solved = false;
        drawGrid();
    }

    void saveGame(String filename) {
        try {
            PrintWriter out = new PrintWriter(filename);
            out.println(currentLevel);
            out.println(moves);
            out.println(elapsedSeconds);
            out.println(grid.length + " " + grid[0].length);
            for (int row = 0; row < grid.length; row++) {
                for (int col = 0; col < grid[row].length; col++) {
                    out.print(grid[row][col]);
                    if (col + 1 < grid[row].length) out.print(" ");
                }
                out.println();
            }
            out.close();
            updateInfo();
        } catch (Exception ex) {
            info.setText("Save failed");
        }
    }

    void loadGame(String filename) {
        try {
            Scanner sc = new Scanner(new File(filename));
            currentLevel = sc.nextInt();
            moves = sc.nextInt();
            elapsedSeconds = sc.nextLong();
            int rows = sc.nextInt();
            int cols = sc.nextInt();
            grid = new int[rows][cols];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    grid[row][col] = sc.nextInt();
                }
            }
            selectedId = 0;
            solved = false;
            undo.clear();
            drawGrid();
        } catch (Exception ex) {
            info.setText("Load failed");
        }
    }

    void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            updateInfo();
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    void updateInfo() {
        info.setText("Level: " + currentLevel + "  Selected: " + selectedId + "  Moves: " + moves + "  Time: " + elapsedSeconds + "s");
    }

    static class Bounds {
        int minRow;
        int maxRow;
        int minCol;
        int maxCol;
        int cells;

        int width() {
            return maxCol - minCol + 1;
        }

        int height() {
            return maxRow - minRow + 1;
        }
    }

    static class GameState {
        int[][] grid;
        int moves;
        long elapsedSeconds;

        GameState(int[][] grid, int moves, long elapsedSeconds) {
            this.grid = grid;
            this.moves = moves;
            this.elapsedSeconds = elapsedSeconds;
        }
    }

    @Override
    public void stop() {
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
