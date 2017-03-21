package eu.mikroskeem.shuriken.instrumentation;

public class ClassTools {
    public static String unqualifyName(String className){
        return className.replaceAll("\\.", "/");
    }
}