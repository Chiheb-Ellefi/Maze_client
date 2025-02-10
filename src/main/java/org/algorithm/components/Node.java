package org.algorithm.components;

import java.io.*;
import java.net.Socket;

public class Node implements Serializable {
    private int row;
    private int column;
    private boolean[] borders;
    private char value;
    private boolean partOfMaze; // New field to track if the node is part of the maze

    public Node(int row, int column) {
        this.row = row;
        this.column = column;
        this.borders = new boolean[]{true, true, true, true}; // Borders: top, right, bottom, left
        this.partOfMaze = false; // Initially, the node is not part of the maze

    }




    public void setValue(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public boolean[] getBorders() {
        return borders;
    }

    public void setBorders(boolean[] borders) {
        this.borders = borders;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void removeBorder(int direction) {
        borders[direction] = false;
    }

    public int incrementRow(int increment) {
        return row + increment;
    }

    public int incrementColumn(int increment) {
        return column + increment;
    }

    // New method to check if the node is part of the maze
    public boolean isPartOfMaze() {
        return partOfMaze;
    }


    public void setPartOfMaze(boolean partOfMaze) {
        this.partOfMaze = partOfMaze;
    }

}
