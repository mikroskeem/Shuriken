package eu.mikroskeem.shuriken.instrumentation.methodreflector;

/**
 * Magical values of witchcraft & other weird shit
 *
 * @author Mark Vainomaa
 */
final class Magic {
    /* REFLECTOR CLASS FLAGS */
    final static int REFLECTOR_CLASS_USE_METHODHANDLE = 1;    // Indicates that reflector class must have method handle array field
    final static int REFLECTOR_CLASS_USE_INSTANCE = 1 << 1;   // Indicates that reflector class must have target class instance reference

    /* REFLECTOR CLASS METHOD FLAGS */
    final static int REFLECTOR_METHOD_USE_INVOKEINTERFACE = 1 << 2; // Indicates that reflector method should use INVOKEINTERFACE instruction
    final static int REFLECTOR_METHOD_USE_METHODHANDLE = 1 << 3; // Indicates that reflector method should use MethodHandle for invocation
    final static int REFLECTOR_METHOD_USE_INSTANCE = 1 << 4; // Indicates that reflector method should use target instance

    /* CLASS */
    final static int TARGET_CLASS_VISIBILITY_PUBLIC = 1 << 5; // Indicates that target class is public
    //final static int TARGET_CLASS_VISIBILITY_PACKAGE = 1 << 6; // Indicates that target class is package-private or protected
    final static int TARGET_CLASS_VISIBILITY_PRIVATE = 1 << 7; // Indicates that target class is private

    /* METHOD & FIELD */
    final static int RETURN_TYPE_PUBLIC = 1 << 8; // Indicates that method return/field/constructor type is public

    /* FIELD */
    final static int FIELD_GETTER = 1 << 9; // Indicates that reflector method is field getter
    final static int FIELD_SETTER = 1 << 10; // Indicates that reflector method is field setter

    /* CTOR */
    final static int CTOR_INVOKER = 1 << 11; // Indicates that reflector method is constructor invoker
}
