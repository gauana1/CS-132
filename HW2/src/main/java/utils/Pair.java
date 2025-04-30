package utils;

public class Pair {
    public String first;
    public String second;
    public String third;
    public String fourth;
    // Constructor
    public Pair(String first, String second) {
        this.first = first;
        this.second = second;
    }
    public Pair(String first, String second, String third, String fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }
    // Getters
    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    // Optionally, you can override toString for better output
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
