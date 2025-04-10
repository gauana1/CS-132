import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set; 

public class Parse {
    ArrayList<String> tokens = new ArrayList<>(); 
    Set<String> tokenSet = new HashSet<>();
    public Parse(ArrayList<String> tokens){
        this.tokens = tokens;
        String[] arr = {"{", "}", ";", "true", "false", "System.out.println", "(", ")", "while", "if", "else", "!", "$"};
        for(String s : arr){
            this.tokenSet.add(s);
        }
    }
    public static void main(String[] args){
        Scanner obj = new Scanner(System.in);
        String input = "";
        while(obj.hasNextLine()){
            input += obj.nextLine();
        }
        // String input = "{ System.out.println(true) ;}";
        // System.out.println(input);
        Tokenizer tokenizer = new Tokenizer(input);
        Parse parser = new Parse(tokenizer.tokens);
        // System.out.println(parser.tokens);
        if(parser.preprocess() && parser.parse()){
            System.out.println("Program parsed successfully");
        } else{
            System.out.println("Parse error");
        }
        obj.close();
    }
    public boolean preprocess(){
        for(String s: tokens){
            if(!this.tokenSet.contains(s)){
                // System.out.println(s + "TOKEN");
                return false;
            }
        }
        return true;
    }
    public boolean parse(){
        ArrayList<String> stack = new ArrayList<>(); 
        stack.add("$");
        stack.add("S");
        int ptr = 0;
        String currString = tokens.get(ptr);
        while(true){            
            while (currString.equals(stack.get(stack.size() -1))){
                //pop top off the stack
                stack.remove(stack.size() -1);
                ptr += 1; 
                if(ptr < tokens.size()){
                    currString = tokens.get(ptr);
                } else{
                    return true;
                }
            }
            if(stack.get(stack.size() -1).equals("S")){
                stack.remove(stack.size() -1);
                if(currString.equals("{")){
                    stack.add("}");
                    stack.add("L");
                    stack.add("{");
                } else if(currString.equals("if")){
                    stack.add("S");
                    stack.add("else");
                    stack.add("S");
                    stack.add(")");
                    stack.add("E");
                    stack.add("(");
                    stack.add("if");
                } else if(currString.equals("while")){
                    stack.add("S");
                    stack.add(")");
                    stack.add("E");
                    stack.add("(");
                    stack.add("while");
                } else if(currString.equals("System.out.println")){
                    stack.add(";");
                    stack.add(")");
                    stack.add("E");
                    stack.add("(");
                    stack.add("System.out.println");
                } else{
                    return false; 
                }
            } else if(stack.get(stack.size() -1).equals("L")){
                stack.remove(stack.size()-1);
                if(currString.equals("{") || currString.equals("if") || currString.equals("while") || currString.equals("System.out.println")){
                    stack.add("L");
                    stack.add("S");
                } else if(currString.equals("}")){
                    //then just pop off the stack 
                    //can do nothing
                }else{
                    return false; 
                } 
            } else if(stack.get(stack.size() -1).equals("E")){
                stack.remove(stack.size()-1);
                if(currString.equals("true")){
                    stack.add("true");
                } else if(currString.equals("false")){
                    stack.add("false");
                } else if(currString.equals("!")){
                    stack.add("E");
                    stack.add("!");
                } else {
                    return false; 
                }
            } else{
                return false;
            }
        }

    }
}
class Tokenizer {
    String code; 
    ArrayList<String> tokens = new ArrayList<>(); 
    public Tokenizer(String code){
        this.code = code; 
        Tokenize(); 
    }
    private String Strip(String s){
        int left = 0; 
        int right = s.length() - 1; 
        while(left < s.length() && (String.valueOf(s.charAt(left)).equals(" ") || String.valueOf(s.charAt(left)).equals("\n"))){
            left += 1;
        }
        while(right >= 0 && (String.valueOf(s.charAt(right)).equals(" ") || String.valueOf(s.charAt(right)).equals("\n"))){
            right -= 1;
        }
        if(left < right){
            return s.substring(left, right+1);
        }
        return "";
    }
    public void Tokenize(){
        int ptr = 0; 
        String currString = ""; 
        while (ptr < code.length()){
            String currChar = String.valueOf(code.charAt(ptr));
            if(currChar.equals(" ") || currChar.equals("\n")){
                ptr += 1; 
                currString += currChar; 
                continue;
            }else if (currChar.equals("(") || currChar.equals(")") || currChar.equals("{") || currChar.equals("}") || currChar.equals("!") || currChar.equals(";")) {
                //in this case add directly to the tokens list, since all one char terminals
                tokens.add(currChar);
                ptr +=1; 
                continue;
            } 
            currString += currChar; 
            String strippedString = Strip(currString);
            if(strippedString.equals("true") || strippedString.equals("false") || strippedString.equals("if") || strippedString.equals("else") ||  
            strippedString.equals("while") ||  strippedString.equals("System.out.println")){
                //in this case then we can just add it to to the tokens and wipe the current string
                tokens.add(strippedString); 
                currString = "";
            } 
            ptr += 1; 
        }
        if(currString.length() > 0 && Strip(currString).length() > 0){
            tokens.add(Strip(currString));
        }
        tokens.add("$");
    }

}