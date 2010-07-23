package org.basex.query.ft;

import static org.basex.util.Token.*;
import java.io.IOException;
import org.basex.io.IO;
import org.basex.util.TokenMap;

/**
 * Simple stemming directory for full-text requests.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class StemDir extends TokenMap {
  /**
   * Reads a stop words file.
   * @param fl file reference
   * @return true if everything went alright
   */
  public boolean read(final IO fl) {
    try {
      for(final byte[] sl : split(fl.content(), '\n')) {
        byte[] val = null;
        for(final byte[] st : split(norm(sl), ' ')) {
          if(val == null) val = st;
          else add(st, val);
        }
      }
      return true;
    } catch(final IOException ex) {
      return false;
    }
  }

  /**
   * Returns a stemmed word or the word itself.
   * @param word word to be stemmed
   * @return resulting token
   */
  public byte[] stem(final byte[] word) {
    final byte[] sn = get(word);
    return sn != null ? sn : word;
  }
}
