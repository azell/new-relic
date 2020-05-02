package com.github.azell.codingchallenge;

/** Track ingested data points. */
public class RecordStats {
  private long uniques;
  private long duplicates;
  private long totalUniques;

  public long uniques() {
    return uniques;
  }

  public long duplicates() {
    return duplicates;
  }

  public long totalUniques() {
    return totalUniques;
  }

  public void startTimeWindow(RecordStats stats) {
    /* copy the previous time window data */
    stats.uniques = uniques;
    stats.duplicates = duplicates;
    stats.totalUniques = totalUniques;

    /* reset the time window stats */
    uniques = 0;
    duplicates = 0;
  }

  public void updateTimeWindow(long uniques, long duplicates) {
    this.uniques += uniques;
    this.duplicates += duplicates;
    this.totalUniques += uniques;
  }
}
