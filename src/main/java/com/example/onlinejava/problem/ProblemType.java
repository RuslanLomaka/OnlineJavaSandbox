package com.example.onlinejava.problem;


/*
    Using a plain String is not ideal because someone could accidentally write:
        "easy"
        "Easy"
        "EASY"
        "very-easy"
    We want only valid values.
    So, creating enum is better option
 */
public enum ProblemType {

    ARRAY,
    SORTING,
    SEARCHING,
    HASHING,
    TWO_POINTERS,
    SLIDING_WINDOW,
    RECURSION,
    STACK,
    QUEUE,
    LINKED_LIST,
    TREE_TRAVERSAL,
    GRAPH_TRAVERSAL,
    DYNAMIC_PROGRAMMING,
    GREEDY
}