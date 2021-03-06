package org.basex.query.expr;

import static org.basex.query.util.Err.*;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Root node.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class Root extends Simple {
  /**
   * Constructor.
   * @param ii input info
   */
  public Root(final InputInfo ii) {
    super(ii);
    type = SeqType.DOC_ZM;
  }

  @Override
  public Expr compile(final QueryContext ctx) {
    return ctx.value != null && ctx.value.type == NodeType.DOC ? ctx.value : this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    final Iter iter = checkCtx(ctx).iter();
    final NodeSeqBuilder nc = new NodeSeqBuilder().check();
    for(Item i; (i = iter.next()) != null;) {
      final ANode n = root(i);
      if(n == null || n.type != NodeType.DOC) CTXNODE.thrw(info);
      nc.add(n);
    }
    return nc;
  }

  /**
   * Returns the root node of the specified item.
   * @param v input node
   * @return root node
   */
  public static ANode root(final Value v) {
    if(!v.type.isNode()) return null;
    ANode n = (ANode) v;
    while(true) {
      final ANode p = n.parent();
      if(p == null) return n;
      n = p;
    }
  }

  @Override
  public boolean uses(final Use u) {
    return u == Use.CTX;
  }

  @Override
  public boolean databases(final StringList db) {
    // [JE] XQuery: should only be added if placed outside a predicate
    db.add("");
    return true;
  }

  @Override
  public boolean iterable() {
    return true;
  }

  @Override
  public boolean sameAs(final Expr cmp) {
    return cmp instanceof Root;
  }

  @Override
  public String toString() {
    return "root()";
  }
}
