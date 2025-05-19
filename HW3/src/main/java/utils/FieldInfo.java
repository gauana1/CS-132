package utils;
import java.util.ArrayList;
public class FieldInfo {
    public ArrayList<Pair> fieldTypes = new ArrayList<>();
    public int offset;

    public void addField(String fieldType, String fieldName, String c) {
        fieldTypes.add(new Pair(fieldType, fieldName, c));
    }
}
