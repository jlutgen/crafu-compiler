package decaf;

import java.io.*;
import java.util.HashMap;
import java.util.Set;

public class IrTest {
    public static void main(String[] args) {
        IrId i1 = new IrId("crapola");
        IrId i2 = new IrId("crapola");
        System.out.println("i1 == i2: " + (i1==i2));
        //System.out.println("i1 instanceof Object: " + (i1 instanceof Object));
        System.out.println("i1.equals(i2): " + i1.equals(i2));
        System.out.println("i1.hashCode(): " + i1.hashCode());
        System.out.println("i2.hashCode(): " + i2.hashCode());
        

        HashMap<IrId, String> hm = new HashMap<IrId, String>();
        hm.put(i1, "i1");
        hm.put(i2, "i2");
        System.out.println("i1 same as i2: " + (i1.hashCode() == i2.hashCode() && (i1 == i2 || i1.equals(i2))));
        Set<IrId> keys = hm.keySet();
        for (IrId k : keys)
            System.out.println(k + " --> " + hm.get(k));

        HashMap<String, String> hm2 = new HashMap<String, String>();
        String s1="crapola", s2="crapola";
        hm2.put(s1, "i1");
        hm2.put(s2, "i2");
        
        Set<String> keys2 = hm2.keySet();
        System.out.println();
        System.out.println("s1.hashCode(): " + s1.hashCode());
        System.out.println("s2.hashCode(): " + s2.hashCode());
        System.out.println("s1 == s2: " + (s1==s2));
        System.out.println("s1.equals(s2): " + s1.equals(s2));
        System.out.println("s1 same as s2: " + (s1.hashCode() == s2.hashCode() && (s1 == s2 || s1.equals(s2))));
        for (String k : keys2)
            System.out.println(k + " --> " + hm2.get(k));
    }
}
