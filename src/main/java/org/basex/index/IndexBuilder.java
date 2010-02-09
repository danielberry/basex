package org.basex.index;

import static org.basex.core.Text.*;
import java.io.IOException;
import org.basex.core.Main;
import org.basex.core.Progress;
import org.basex.core.Prop;
import org.basex.data.Data;
import org.basex.util.Performance;

/**
 * This interface defines the functions which are needed for building
 * new index structures.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public abstract class IndexBuilder extends Progress {
  /** Data reference. */
  protected final Data data;
  /** Total parsing value. */
  protected final int size;
  /** Current parsing value. */
  protected int pre;
  /** Merge flag. */
  protected boolean merge;

  /** Runtime for memory consumption. */
  private final Runtime rt = Runtime.getRuntime();
  /** Maximum memory to consume. */
  protected long maxMem = (long) (rt.maxMemory() * 0.9);
  /** Free memory threshold. */
  private int cc;

  /**
   * Builds the index structure and returns an index instance.
   * @return index instance
   * @throws IOException IO Exception
   */
  public abstract Index build() throws IOException;

  /**
   * Checks if the command was interrupted, and prints some debug output.
   */
  protected void check() {
    checkStop();
    if(Prop.debug && (pre & 0x1FFFFF) == 0) Main.err(".");
  }

  /**
   * Checks if enough memory is left to continue index building.
   * @return result of check
   * @throws IOException I/O exception
   */
  protected boolean memFull() throws IOException {
    final boolean full = rt.totalMemory() - rt.freeMemory() >= maxMem;
    if(full) {
      if(cc >= 0) throw new IOException(PROCOUTMEM);
      if(Prop.debug) Main.err("!");
      merge = true;
      cc = 30;
    } else {
      cc--;
    }
    return full;
  }

  /**
   * Constructor.
   * @param d reference
   */
  protected IndexBuilder(final Data d) {
    data = d;
    size = data.meta.size;
    if(rt.totalMemory() - rt.freeMemory() >= rt.maxMemory() / 2)
      Performance.gc(2);
  }

  @Override
  public final String tit() {
    return PROGINDEX;
  }

  @Override
  public final double prog() {
    return (double) pre / (size + (merge  ? size / 50 : 0));
  }
}