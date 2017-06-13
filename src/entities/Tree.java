package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jonty on 27/05/2017.
 */

public class Tree<T> {
    private Node<T> root;

    public Tree(T rootData) {
        root = new Node<T>();
        root.data = rootData;
        root.children = new ArrayList<Node<T>>();
    }

    public Tree() {
        root = null;
        root.data = null;
        root.children = null;
    }

    public static class Node<T> {
        private T data;
        private Node<T> parent;
        private List<Node<T>> children;
    }

    public static void addChild(Node Parent, Node child) {
        Parent.children.add(child);
    }

    public static List returnChild(Node Parent) {
        return Parent.children;
    }


}

