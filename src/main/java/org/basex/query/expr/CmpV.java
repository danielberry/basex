package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import static org.basex.query.QueryText.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.func.FNSimple;
import org.basex.query.func.Fun;
import org.basex.query.func.FunDef;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.query.item.Seq;
import org.basex.query.util.Err;
import org.basex.util.Token;

/**
 * Value comparison.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class CmpV extends Arr {
  /** Comparators. */
  public enum Comp {
    /** Item Comparison:less or equal. */
    LE("le") {
      @Override
      public boolean e(final Item a, final Item b) throws QueryException {
        final int v = a.diff(b);
        return v != UNDEF && v <= 0;
      }
      @Override
      public Comp invert() { return GE; }
    },

    /** Item Comparison:less. */
    LT("lt") {
      @Override
      public boolean e(final Item a, final Item b) throws QueryException {
        final int v = a.diff(b);
        return v != UNDEF && v < 0;
      }
      @Override
      public Comp invert() { return GT; }
    },

    /** Item Comparison:greater of equal. */
    GE("ge") {
      @Override
      public boolean e(final Item a, final Item b) throws QueryException {
        final int v = a.diff(b);
        return v != UNDEF && v >= 0;
      }
      @Override
      public Comp invert() { return LE; }
    },

    /** Item Comparison:greater. */
    GT("gt") {
      @Override
      public boolean e(final Item a, final Item b) throws QueryException {
        final int v = a.diff(b);
        return v != UNDEF && v > 0;
      }
      @Override
      public Comp invert() { return LT; }
    },

    /** Item Comparison:equal. */
    EQ("eq") {
      @Override
      public boolean e(final Item a, final Item b) throws QueryException {
        return a.eq(b);
      }
      @Override
      public Comp invert() { return EQ; }
    },

    /** Item Comparison:not equal. */
    NE("ne") {
      @Override
      public boolean e(final Item a, final Item b) throws QueryException {
        return !a.eq(b);
      }
      @Override
      public Comp invert() { return NE; }
    };

    /** String representation. */
    public final String name;

    /**
     * Constructor.
     * @param n string representation
     */
    Comp(final String n) { name = n; }

    /**
     * Evaluates the expression.
     * @param a first item
     * @param b second item
     * @return result
     * @throws QueryException query exception
     */
    public abstract boolean e(Item a, Item b) throws QueryException;

    /**
     * Inverts the comparator.
     * @return inverted comparator
     */
    public abstract Comp invert();

    @Override
    public String toString() { return name; }
  }

  /** Comparator. */
  public Comp cmp;

  /**
   * Constructor.
   * @param e1 first expression
   * @param e2 second expression
   * @param c comparator
   */
  public CmpV(final Expr e1, final Expr e2, final Comp c) {
    super(e1, e2);
    cmp = c;
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);
    for(int e = 0; e != expr.length; e++) expr[e] = expr[e].addText(ctx);

    if(expr[0].i() && !expr[1].i()) {
      final Expr tmp = expr[0];
      expr[0] = expr[1];
      expr[1] = tmp;
      cmp = cmp.invert();
    }
    final Expr e1 = expr[0];
    final Expr e2 = expr[1];

    Expr e = this;
    if(e1.i() && e2.i()) {
      e = atomic(ctx);
    } else if(e1.e() || e2.e()) {
      e = Seq.EMPTY;
    }
    if(e != this) {
      ctx.compInfo(OPTPRE, this);
    } else if(e1 instanceof Fun) {
      final Fun fun = (Fun) expr[0];
      if(fun.func == FunDef.POS) {
        e = Pos.get(this, cmp, e2);
        if(e != this) ctx.compInfo(OPTWRITE, this);
      } else if(fun.func == FunDef.COUNT) {
        // same as for general comparisons
        if(e2.i() && ((Item) e2).n() && ((Item) e2).dbl() == 0) {
          // count(...) CMP 0
          if(cmp == Comp.LT || cmp == Comp.GE) {
            // < 0: always false, >= 0: always true
            ctx.compInfo(OPTPRE, this);
            e = Bln.get(cmp == Comp.GE);
          } else {
            // <=/= 0: empty(), >/!= 0: exist()
            final Fun f = new FNSimple();
            f.expr = fun.expr;
            f.func = cmp == Comp.EQ || cmp == Comp.LE ?
                FunDef.EMPTY : FunDef.EXISTS;
            ctx.compInfo(OPTWRITE, this);
            e = f;
          }
        }
      }
    }
    return e;
  }

  @Override
  public Bln atomic(final QueryContext ctx) throws QueryException {
    final Item a = expr[0].atomic(ctx);
    if(a == null) return null;
    final Item b = expr[1].atomic(ctx);
    if(b == null) return null;

    if(!valCheck(a, b)) Err.cmp(a, b);
    return Bln.get(cmp.e(a, b));
  }

  /**
   * Checks if the specified items can be compared.
   * @param a first item
   * @param b second item
   * @return result of check
   */
  public static boolean valCheck(final Item a, final Item b) {
    return a.type == b.type || a.n() && b.n() || (a.u() || a.s()) &&
      (b.s() || b.u()) || a.d() && b.d();
  }

  @Override
  public Return returned(final QueryContext ctx) {
    return Return.BLN;
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this, TYPE, Token.token(cmp.name));
    for(final Expr e : expr) e.plan(ser);
    ser.closeElement();
  }

  @Override
  public String color() {
    return "FF9966";
  }

  @Override
  public String info() {
    return "'" + cmp + "' expression";
  }

  @Override
  public String toString() {
    return toString(" " + cmp + " ");
  }
}