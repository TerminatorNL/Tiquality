package cf.terminator.tiquality.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabCompletionElement {

    public static final List<String> USERNAME_INDEX = new ArrayList<>(0);
    private final TabCompletionElement[] children;
    private final String name;


    public static TabCompletionElement generateRoot(){
        return new TabCompletionElement("tiquality",
                new TabCompletionElement("info"),
                new TabCompletionElement("claim")
        );
    }











    public TabCompletionElement(String name, TabCompletionElement... children){
        this.children = children;
        this.name = name;
    }

    public TabCompletionElement(String name){
        this.children = null;
        this.name = name;
    }

    public List<String> getTabCompletions(String[] args){
        if(children == null){
            return Collections.emptyList();
        }
        ArrayList<String> list = new ArrayList<>();
        if(args.length == 1) {
            for (TabCompletionElement e : children) {
                if (e.name.startsWith(args[0].toLowerCase())) {
                    list.add(e.name);
                }
            }
        }
        return list;
    }
}
