package com.example.onlinejava.problem;

/**
 * Problem-solving technique associated with a coding problem.
 *
 * <p>Modeled as an enum rather than a plain {@code String} so that
 * values like {@code "sorting"}, {@code "Sorting"}, or a typo like
 * {@code "sortign"} can't slip in as distinct, inconsistent values.
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