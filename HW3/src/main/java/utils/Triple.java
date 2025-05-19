package utils;

public class Triple{
    public String first;
    public String second;
    public Boolean third;
    public String originalClass; 
    public int offset;
    // Constructor
    public Triple(String first, String second, Boolean third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
    public Triple(String first, String second, Boolean third, String originalClass, int offset) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.originalClass = originalClass;
        this.offset = offset;
    }
    
    // Optionally, you can override toString for better output
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ")";
    }
}