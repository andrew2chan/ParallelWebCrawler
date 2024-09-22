package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth; //added
  private final ForkJoinPool pool;
  private final List<Pattern> ignoredUrls; //added
  private PageParserFactory parserFactory; //added

  @Inject
  ParallelWebCrawler(
      Clock clock,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @MaxDepth int maxDepth, //added
      @TargetParallelism int threadCount,
      @IgnoredUrls List<Pattern> ignoredUrls,
      PageParserFactory parserFactory) { //added
    this.clock = clock;
    this.timeout = timeout;
    this.maxDepth = maxDepth; //added
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.ignoredUrls = ignoredUrls; //added
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    final Instant deadline = clock.instant().plus(timeout);
    final Map<String, Integer> counts = new ConcurrentHashMap<>();
    final Set<String> visitedUrls = new ConcurrentSkipListSet<>();
    CrawlingTaskFactory crawlingTaskFactory = new CrawlingTaskFactory(deadline, counts, visitedUrls, clock, ignoredUrls, parserFactory);
    List<ForkJoinTask<Void>> startingTasks = new ArrayList<>();

    for (String url : startingUrls) {
      startingTasks.add(pool.submit(crawlingTaskFactory.createCrawlingTask(url, maxDepth, pool, crawlingTaskFactory)));
    }

    try {
      for(ForkJoinTask<Void> v : startingTasks) {
        v.get(); //wait for all threads to finish
      }
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    finally {
      pool.shutdown();
    }

    if(!counts.isEmpty()) { //done after threads are finished so no need for concurrent map
      return new CrawlResult.Builder()
              .setWordCounts(WordCounts.sort(counts, popularWordCount))
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder().build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
