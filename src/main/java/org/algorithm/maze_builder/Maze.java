package org.algorithm.maze_builder;

import org.algorithm.components.Node;

public class Maze {
    private int nbRow;
    private  int nbCol;
    private Node[][] maze;
    private Node start;
    private Node end;
    private String theme;


    public void setTheme(String theme) {
        if (theme != null && !theme.isEmpty()) {
            this.theme = theme.substring(0, 1).toUpperCase() + theme.substring(1);
        } else {
            this.theme = theme;
        }
    }

    public String getTheme() {
        return theme;
    }

    public void setEnd(Node end) {
        this.end = end;
    }

    public void setStart(Node start) {
        this.start = start;
    }

    public Node getEnd() {
        return end;
    }

    public Node getStart() {
        return start;
    }

    public void setNbCol(int nbCol) {
        this.nbCol = nbCol;
    }

    public void setNbRow(int nbRow) {
        this.nbRow = nbRow;
    }

    public void setMaze(Node[][] maze) {
        this.maze = maze;
    }

    public Node[][] getMaze() {
        return maze;
    }

    public int getNbRow() {
        return nbRow;
    }

    public int getNbCol() {
        return nbCol;
    }
    public  void printMaze() {
        if (maze == null) {
            System.out.println("Maze is null.");
            return;
        }

        for (Node[] row : maze) {
            for (Node node : row) {
                System.out.print(node.isPartOfMaze() ? node.getValue() : "#");
            }
            System.out.println();
        }
    }
}
