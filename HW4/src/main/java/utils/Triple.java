package utils;

public class Triple {
    public String methodName;
    public String labelName;
    public int line;

    public Triple(String methodname, String labelname, int line) {
        this.methodName = methodname;
        this.labelName = labelname;
        this.line = line;
    }
}
