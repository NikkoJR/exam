File → Project Structure
Слева Project
В поле SDK выбери:
если есть Java 21 — выбери её
если нет — нажми Download JDK
Выбери:
Version: 21
Vendor: Eclipse Temurin или Oracle
Нажми Download
Потом зайди в Modules → demo → Dependencies
В Module SDK поставь Project SDK / JDK 21
Apply → OK
Перезапусти билд / Run










package com.example.demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class UniversalMatrixConsoleTemplate {
    static int[][] grid;

    public static void loadSquareWithHeader(String filename) throws IOException {
        Scanner sc = new Scanner(new java.io.File(filename));
        int n = sc.nextInt();
        int firstHeaderNumber = sc.nextInt();
        int secondHeaderNumber = sc.nextInt();
        grid = new int[n][n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                grid[row][col] = sc.nextInt();
            }
        }
        System.out.println(firstHeaderNumber);
        System.out.println(secondHeaderNumber);
    }

    public static void loadAnyMatrix(String filename) throws IOException {
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
    }

    public static void printGrid() {
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                System.out.print(grid[row][col] + " ");
            }
            System.out.println();
        }
    }

    public static int findMaxId() {
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

    public static Bounds getBounds(int id) {
        Bounds b = new Bounds();
        b.minRow = grid.length;
        b.maxRow = -1;
        b.minCol = grid[0].length;
        b.maxCol = -1;
        b.cells = 0;
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
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

    public static boolean isHorizontal(int id) {
        Bounds b = getBounds(id);
        return b.cells > 0 && b.height() == 1 && b.width() > 1;
    }

    public static boolean isVertical(int id) {
        Bounds b = getBounds(id);
        return b.cells > 0 && b.width() == 1 && b.height() > 1;
    }

    public static boolean isRectangle(int id) {
        Bounds b = getBounds(id);
        return b.cells > 0 && b.cells == b.width() * b.height();
    }

    public static int countHorizontal() {
        int count = 0;
        int max = findMaxId();
        for (int id = 1; id <= max; id++) {
            if (isHorizontal(id)) {
                count++;
            }
        }
        return count;
    }

    public static int countRectangles() {
        int count = 0;
        int max = findMaxId();
        for (int id = 1; id <= max; id++) {
            if (isRectangle(id)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isInside(int row, int col) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[row].length;
    }

    public static int[][] copyGrid() {
        int[][] copy = new int[grid.length][];
        for (int row = 0; row < grid.length; row++) {
            copy[row] = new int[grid[row].length];
            for (int col = 0; col < grid[row].length; col++) {
                copy[row][col] = grid[row][col];
            }
        }
        return copy;
    }

    public static class Bounds {
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

    public static void main(String[] args) throws Exception {
        loadAnyMatrix("blocks1.txt");
        printGrid();
        System.out.println("Max id: " + findMaxId());
        System.out.println("Rectangles: " + countRectangles());
    }
}
