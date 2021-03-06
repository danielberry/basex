package org.basex.test.query.func;

import static org.basex.query.func.Function.*;

import org.basex.query.util.*;
import org.basex.test.query.*;
import org.junit.*;

/**
 * This class tests the XQuery hashing functions prefixed with "hash".
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class FNHashTest extends AdvancedQueryTest {
  /** Test method for the md5() function. */
  @Test
  public void utilMd5() {
    check(_HASH_MD5);
    query(_HASH_MD5.args(""), "1B2M2Y8AsgTpgAmY7PhCfg==");
    query(_HASH_MD5.args("BaseX"), "DWUYXJ4pYxHAoiABeeR5og==");
  }

  /** Test method for the sha1() function. */
  @Test
  public void utilSha1() {
    check(_HASH_SHA1);
    query(_HASH_SHA1.args(""), "2jmj7l5rSw0yVb/vlWAYkK/YBwk=");
    query(_HASH_SHA1.args("BaseX"), "OtWVjw8n1a/9yilXVg8SHQWXpO0=");
  }

  /** Test method for the sha256() function. */
  @Test
  public void utilSha256() {
    check(_HASH_SHA256);
    query(_HASH_SHA256.args(""), "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=");
    query(_HASH_SHA256.args("BaseX"), "FdVwdj3rddcou2lkM5KHO4NczMlKLx6IGQnaR2YoIaM=");
  }

  /** Test method for the hash() function. */
  @Test
  public void utilHash() {
    check(_HASH_HASH);
    query(_HASH_HASH.args("", "MD5"), "1B2M2Y8AsgTpgAmY7PhCfg==");
    query(_HASH_HASH.args("", "md5"), "1B2M2Y8AsgTpgAmY7PhCfg==");
    query(_HASH_HASH.args("", "SHA"), "2jmj7l5rSw0yVb/vlWAYkK/YBwk=");
    query(_HASH_HASH.args("", "SHA1"), "2jmj7l5rSw0yVb/vlWAYkK/YBwk=");
    query(_HASH_HASH.args("", "SHA-1"), "2jmj7l5rSw0yVb/vlWAYkK/YBwk=");
    query(_HASH_HASH.args("", "SHA-256"), "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=");
    query(_HASH_HASH.args("xs:base64Binary('')", "md5"), "1B2M2Y8AsgTpgAmY7PhCfg==");
    error(_HASH_HASH.args("xs:hexBinary('')", "md5"), Err.STRB64TYPE);
    error(_HASH_HASH.args("", ""), Err.HASH_ALG);
  }
}
