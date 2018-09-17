package cf.terminator.tiquality.util;

public class Entry3<X,Y,Z> {

    private final X x;
    private final Y y;
    private final Z z;

    public Entry3(X x, Y y, Z z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public X getFirst(){
        return x;
    }

    public Y getSecond(){
        return y;
    }

    public Z getThird(){
        return z;
    }
}
