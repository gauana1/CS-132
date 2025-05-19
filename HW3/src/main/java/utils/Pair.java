package utils;

public class Pair {
    public String first;
    public String second;
    public String originalClass;
    public int offset;
    // Constructor
    public Pair(String first, String second) {
        this.first = first;
        this.second = second;
    }
    public Pair(String first, String second, String t) {
        this.first = first;
        this.second = second;
        this.originalClass = t;
    }
    public Pair(Pair other) {
        this.first = other.first;
        this.second = other.second;
        this.originalClass = other.originalClass;
        this.offset = other.offset;
    }
    // Optionally, you can override toString for better output
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}