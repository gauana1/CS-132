class Main {
    public static void main(String[] args) {
        System.out.println((new A()).foo());
    }
}

class A {
   int a;
   public int foo(boolean a) {
      a = true; // TE
      return 0;
   }
}