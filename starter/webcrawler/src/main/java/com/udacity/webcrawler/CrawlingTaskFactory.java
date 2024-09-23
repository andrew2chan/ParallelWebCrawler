package com.udacity.webcrawler;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public interface CrawlingTaskFactory {
    public RecursiveAction createCrawlingTask(String url, int maxDepth, ForkJoinPool pool, CrawlingTaskFactory crawlingTaskFactory);
}
