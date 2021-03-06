package org.basex.query.func;

import static org.basex.query.util.Err.*;
import static org.basex.util.Token.*;

import java.io.*;
import java.math.*;
import java.nio.*;
import java.nio.charset.*;

import org.basex.io.in.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Functions for converting data to other formats.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class FNConvert extends StandardFunc {
  /**
   * Constructor.
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  public FNConvert(final InputInfo ii, final Function f, final Expr... e) {
    super(ii, f, e);
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    switch(sig) {
      case _CONVERT_BINARY_TO_BYTES: return binaryToBytes(ctx).iter();
      default:                       return super.iter(ctx);
    }
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    switch(sig) {
      case _CONVERT_BINARY_TO_BYTES: return binaryToBytes(ctx);
      default:                       return super.value(ctx);
    }
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    switch(sig) {
      case _CONVERT_INTEGER_FROM_BASE: return integerFromBase(ctx, ii);
      case _CONVERT_INTEGER_TO_BASE:   return integerToBase(ctx, ii);
      case _CONVERT_BINARY_TO_STRING:  return toString(ctx);
      case _CONVERT_STRING_TO_BASE64:  return new B64(stringToBinary(ctx));
      case _CONVERT_BYTES_TO_BASE64:   return new B64(bytesToBinary(ctx));
      case _CONVERT_STRING_TO_HEX:     return new Hex(stringToBinary(ctx));
      case _CONVERT_BYTES_TO_HEX:      return new Hex(bytesToBinary(ctx));
      default:                         return super.item(ctx, ii);
    }
  }

  /** Digits used in base conversion. */
  private static final byte[] DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b',
    'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
  };

  /**
   * Converts the given number to a string, using base
   * 2<sup>shift</sup>.
   * @param num number item
   * @param shift number of bits to use for one digit
   * @return string representation of the given number
   */
  private static Str toBaseFast(final long num, final int shift) {
    final byte[] bytes = new byte[(64 + shift - 1) / shift];
    final int mask = (1 << shift) - 1;
    long n = num;
    int pos = bytes.length;
    do {
      bytes[--pos] = DIGITS[(int) (n & mask)];
      n >>>= shift;
    } while(n != 0);
    return Str.get(substring(bytes, pos));
  }

  /** BigInteger representing 2 * ({@link Long#MAX_VALUE} + 1). */
  private static final BigInteger MAX_ULONG = BigInteger.ONE.shiftLeft(64);

  /**
   * Converts the given number to a string, using the given base.
   * @param ctx query context
   * @param ii input info
   * @return string representation of the given number
   * @throws QueryException query exception
   */
  private Str integerToBase(final QueryContext ctx, final InputInfo ii)
      throws QueryException {

    final long num = checkItr(expr[0], ctx), base = checkItr(expr[1], ctx);
    if(base < 2 || base > 36) INVBASE.thrw(ii, base);

    // use fast variant for powers of two
    for(int i = 1, p = 2; i < 6; i++, p <<= 1)
      if(base == p) return toBaseFast(num, i);

    final ByteList tb = new ByteList();
    long n = num;
    if(n < 0) {
      // unsigned value doesn't fit in any native type...
      final BigInteger[] dr = BigInteger.valueOf(n).add(
          MAX_ULONG).divideAndRemainder(BigInteger.valueOf(base));
      n = dr[0].longValue();
      tb.add(DIGITS[dr[1].intValue()]);
    } else {
      tb.add(DIGITS[(int) (n % base)]);
      n /= base;
    }
    while (n != 0) {
      tb.add(DIGITS[(int) (n % base)]);
      n /= base;
    }

    final byte[] res = tb.toArray();
    Array.reverse(res);
    return Str.get(res);
  }

  /**
   * Converts the given string to a number, interpreting it as an xs:integer
   * encoded in the given base.
   * @param ctx query context
   * @param ii input info
   * @return read integer
   * @throws QueryException exception
   */
  private Int integerFromBase(final QueryContext ctx, final InputInfo ii)
      throws QueryException {

    final byte[] str = checkStr(expr[0], ctx);
    final long base = checkItr(expr[1], ctx);
    if(base < 2 || base > 36) INVBASE.thrw(ii, base);

    long res = 0;
    for(final byte b : str) {
      final int num = b <= '9' ? b - 0x30 : (b & 0xDF) - 0x37;
      if(!(b >= '0' && b <= '9' || b >= 'a' && b <= 'z' ||
          b >= 'A' && b <= 'Z') || num >= base)
        INVDIG.thrw(ii, base, (char) (b & 0xff));

      res = res * base + num;
    }

    return Int.get(res);
  }

  /**
   * Extracts the bytes from a given item.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private Value binaryToBytes(final QueryContext ctx) throws QueryException {
    try {
      return new ByteSeq(checkItem(expr[0], ctx).input(info).content());
    } catch(final IOException ex) {
      throw BXCO_STRING.thrw(info, ex);
    }
  }

  /**
   * Converts the specified data to a string.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private Str toString(final QueryContext ctx) throws QueryException {
    final Bin bin = checkBinary(expr[0], ctx);
    final String enc = encoding(1, BXCO_ENCODING, ctx);

    try {
      return Str.get(toString(bin.input(info), enc));
    } catch(final IOException ex) {
      throw BXCO_STRING.thrw(info, ex);
    }
  }

  /**
   * Converts the specified input to a string in the specified encoding.
   * @param is input stream
   * @param enc encoding
   * @return resulting value
   * @throws IOException I/O exception
   */
  public static byte[] toString(final InputStream is, final String enc)
      throws IOException {

    try {
      return new NewlineInput(is).encoding(enc).valid(true).content();
    } catch(final IOException ex) {
      Util.debug(ex);
      throw ex;
    } finally {
      is.close();
    }
  }

  /**
   * Converts the first argument from a string to a byte array.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private byte[] stringToBinary(final QueryContext ctx) throws QueryException {
    final byte[] in = checkStr(expr[0], ctx);
    final String enc = encoding(1, BXCO_ENCODING, ctx);
    if(enc == null || enc == Token.UTF8) return in;
    try {
      final CharsetEncoder ce = Charset.forName(enc).newEncoder();
      final CharBuffer cb = CharBuffer.wrap(Token.string(in));
      return ce.encode(cb).array();
    } catch(final CharacterCodingException ex) {
      throw BXCO_BASE64.thrw(info);
    }
  }

  /**
   * Converts the first argument from a byte sequence to a byte array.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private byte[] bytesToBinary(final QueryContext ctx) throws QueryException {
    final Value v = expr[0].value(ctx);
    // directly pass on byte array
    if(v instanceof ByteSeq) return ((ByteSeq) v).bytes();

    // check if all arguments are bytes
    final Iter ir = v.iter();
    final ByteList bl = new ByteList(Math.max(ElementList.CAP, (int) v.size()));
    for(Item it; (it = ir.next()) != null;) {
      bl.add((int) ((Int) checkType(it, AtomType.BYT)).itr());
    }
    return bl.toArray();
  }
}
