package net.kroia.modutilities;

/**
 * Mutable pair with the same API as {@code net.minecraft.util.Tuple}, which Minecraft 26.2 removed.
 * Use this instead of the vanilla class so the code compiles on every supported version.
 */
public class Tuple<A, B> {
    private A a;
    private B b;

    public Tuple(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() { return a; }
    public void setA(A a) { this.a = a; }
    public B getB() { return b; }
    public void setB(B b) { this.b = b; }
}
