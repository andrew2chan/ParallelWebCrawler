package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Pattern;

public final class CrawlingTaskImpl extends CrawlingTask {
    private final String url;
    private final int maxDepth;
    private final Instant deadline;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final ForkJoinPool pool;
    private final CrawlingTaskFactory crawlingTaskFactory;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;

    public CrawlingTaskImpl(String url, int maxDepth, Instant deadline, Map<String, Integer> counts, Set<String> visitedUrls, ForkJoinPool pool, CrawlingTaskFactory crawlingTaskFactory, Clock clock, List<Pattern> ignoredUrls, PageParserFactory parserFactory) {
        this.url = url;
        this.maxDepth = maxDepth;
        this.deadline = deadline;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.pool = pool;
        this.crawlingTaskFactory = crawlingTaskFactory;
        this.clock = clock;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;
    }

    /**
     * The main computation performed by this task.
     */
    @Override
    protected void compute() {

        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }

        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        synchronized(visitedUrls) { //In case multiple threads access the same link at the same time and visitedurls has not updated yet
            if (visitedUrls.contains(url)) {
                return;
            }
            visitedUrls.add(url);
        }

        PageParser.Result result = parserFactory.get(url).parse();

        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : v + e.getValue());
        }

        List<ForkJoinTask<Void>> tasks = new ArrayList<>();
        for (String link : result.getLinks()) {
            tasks.add(pool.submit(crawlingTaskFactory.createCrawlingTask(link, maxDepth - 1, pool, crawlingTaskFactory)));
        }

        try {
            for(ForkJoinTask<Void> v : tasks) {
                v.get(); //wait for recursive threads to finish
            }
        }
        catch(Exception ex) {
            //ex.printStackTrace();
        }

    }
}
