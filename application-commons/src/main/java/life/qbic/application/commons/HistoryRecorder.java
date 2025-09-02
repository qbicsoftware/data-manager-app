package life.qbic.application.commons;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import life.qbic.application.commons.CanSnapshot.SnapshotRestorationException;

public class HistoryRecorder {

  private final Deque<Snapshot> history;
  private final CanSnapshot originator;
  private int historySize = 5;

  private HistoryRecorder(CanSnapshot originator) {
    this.originator = Objects.requireNonNull(originator);
    this.history = new ArrayDeque<>(historySize);
  }

  public synchronized void record() {
    if (history.size() == historySize) {
      history.removeFirst();
    }
    history.addLast(originator.snapshot());
  }

  public synchronized void reset() {
    history.clear();
    record();
  }

  public synchronized boolean undo() {
    if (history.isEmpty()) {
      return false;
    }
    Snapshot lastState = history.getLast();
    try {
      originator.restore(lastState);
      history.removeLast();
      return true;
    } catch (SnapshotRestorationException e) {
      return false;
    }
  }

  public static <S extends CanSnapshot> HistoryRecorder recorderFor(S component) {
    return new HistoryRecorder(component);
  }

}
