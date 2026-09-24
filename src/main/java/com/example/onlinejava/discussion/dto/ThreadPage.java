package com.example.onlinejava.discussion.dto;

import java.util.List;

/**
 * One page of a problem's threads.
 *
 * @param threads threads, newest first
 * @param page zero-based page number
 * @param hasMore whether a later page exists
 * @param totalThreads total number of threads
 */
public record ThreadPage(List<ThreadView> threads, int page, boolean hasMore, long totalThreads) {
}
