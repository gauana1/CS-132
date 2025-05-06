package utils;

public class Pair {
    public String first;
    public String second;
    // Constructor
    public Pair(String first, String second) {
        this.first = first;
        this.second = second;
    }
    // Optionally, you can override toString for better output
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}