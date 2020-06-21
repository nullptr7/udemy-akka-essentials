package part5infra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SomeTestClassToTestMyKeyboard {


    public static void main(String[] args) {


        List<Integer> ltr = new ArrayList<Integer>() {{
            add(1);
            add(2);
            add(3);
            add(4);
            add(5);
            add(6);
            add(7);
        }};

        final long count = ltr.stream()
                              .filter(i -> i == 0)
                              .count();

        System.out.println("count = " + count);


        HashSet<Integer> stringSet = new HashSet<Integer>(){{
            add(1);
            add(2);
            add(3);
            add(4);
            add(5);
        }};
    }
}
