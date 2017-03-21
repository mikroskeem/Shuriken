package eu.mikroskeem.test.shuriken.reflect.classes;

public class TestClassOne {
    private String kek = "foo";
    private char a = 'a';

    public String a(){
        return "a";
    }

    public static String b(){
        return "b";
    }

    public void c(){}

    public char d() { return a; }
}
