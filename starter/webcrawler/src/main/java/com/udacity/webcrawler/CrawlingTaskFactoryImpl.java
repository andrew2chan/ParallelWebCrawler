package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public final class CrawlingTaskFactoryImpl implements CrawlingTaskFactory {
    private Clock clock;
    private Instant deadline;
    private Map<String, Integer> counts;
    private Set<String> visitedUrls;
    private List<Pattern> ignoredUrls; //added
    private PageParserFactory parserFactory;

    public CrawlingTaskFactoryImpl(Instant deadline, Map<String, Integer> counts, Set<String> visitedUrls, Clock clock, List<Pattern> ignoredUrls, PageParserFactory parserFactory) {
        this.deadline = deadline;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    public RecursiveAction createCrawlingTask(String url, int maxDepth, ForkJoinPool pool, CrawlingTaskFactory crawlingTaskFactory) {
        return new CrawlingTaskImpl(url, maxDepth, deadline, counts, visitedUrls, pool, crawlingTaskFactory, clock, ignoredUrls, parserFactory);
    }
}
