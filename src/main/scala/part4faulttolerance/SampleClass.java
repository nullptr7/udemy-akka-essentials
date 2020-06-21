package part4faulttolerance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SampleClass {

    final static int bc = 200;
    final int acc = 100;

    public static void main(String[] args) {
        final int a = 100;
        System.out.println(a);
        System.out.println(bc);

        System.out.println(new SampleClass().acc);


        List<Integer> aa = new ArrayList<Integer>() {{
            add(1);
            add(1);
            add(1);
            add(1);
        }};

        List<String> ss = new ArrayList<>();

        final List<Integer> integerList = aa.stream()
                                            .filter(aNumber -> aNumber > 0)
                                            .collect(Collectors.toList());
    }
}

