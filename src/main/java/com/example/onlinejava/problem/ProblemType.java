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
    STRING,
    HASH_TABLE,
    TWO_POINTERS,
    SLIDING_WINDOW,
    STACK,
    BINARY_SEARCH,
    LINKED_LIST
}