package org.basex.query.path;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.util.*;

/**
 * Map ("bang") operator. Only occurs as argument of the {@link MixedPath} expression.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class Bang extends Single {
  /**
   * Constructor.
   * @param ii input info
   * @param e expression
   */
  public Bang(final InputInfo ii, final Expr e) {
    super(ii, e);
  }

  @Override
  public Expr compile(final QueryContext ctx) throws QueryException {
    super.compile(ctx);
    type = expr.type();
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    return ctx.iter(expr);
  }

  @Override
  public Value value(final QueryContext ctx) throws QueryException {
    return ctx.value(expr);
  }

  @Override
  public String toString() {
    return expr.toString();
  }
}
