package org.basex.test.cs;

import static org.junit.Assert.*;

import org.basex.BaseXServer;
import org.basex.core.Context;
import org.basex.core.Process;
import org.basex.core.Session;
import org.basex.core.proc.Close;
import org.basex.core.proc.CreateDB;
import org.basex.core.proc.DropDB;
import org.basex.core.proc.Open;
import org.basex.server.ClientSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests transaction and locking cases.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Andreas Weiler
 */
public class LockingTest {
  /** Server reference. */
  static BaseXServer server;
  /** Database context. */
  protected static final Context CONTEXT = new Context();
  /** Test file. */
  private static final String FILE = "input.xml";
  /** Test name. */
  private static final String NAME = "input";
  /** Start value before each test. */
  private int start;
  /** Socket reference. */
  static Session session1;
  /** Socket reference. */
  static Session session2;

  /** Starts the server. */
  @BeforeClass
  public static void start() {
    new Thread() {
      @Override
      public void run() {
        server = new BaseXServer();
      }
    }.start();

    try {
      session1 = new ClientSession(CONTEXT);
      session2 = new ClientSession(CONTEXT);
    } catch(final Exception ex) {
      throw new AssertionError(ex.toString());
    }
  }
  
  /** Create and Drop Tests. */
  @Test
  public final void createAndDrop() {
    start = 0;
    ok(new CreateDB(FILE), session1);
    stay();
    ok(new CreateDB(FILE), session1);
    stay();
    no(new CreateDB(FILE), session2);
    stay();
    no(new CreateDB(FILE), session2);
    stay();
    no(new DropDB(NAME), session2);
    stay();
    ok(new DropDB(NAME), session1);
  }
  
  /** Close and Open Tests*/
  @Test
  public final void closeAndOpen() {
    start = 0;
    ok(new CreateDB(FILE), session2);
    stay();
    ok(new Close(), session1);
    stay();
    ok(new Close(), session2);
    stay();
    ok(new Open(NAME), session1);
    stay();
    ok(new Open(NAME), session2);
    plus();
    ok(new Close(), session1);
    neg();
  }

  /** Stops the server. */
  @AfterClass
  public static void stop() {
    try {
      session1.close();
      session2.close();
    } catch(final Exception ex) {
      throw new AssertionError(ex.toString());
    }

    // Stop server instance.
    new BaseXServer("stop");
  }
  
  /**
   * The number of references of the DB in the pool is raised by 1.
   */
  private void plus() {
    boolean flag = ((start + 1) == CONTEXT.size(NAME));
    assertTrue(flag);
    start = CONTEXT.size(NAME);
  }
  
  /**
   * The number of references of the DB in the pool is reduced by 1.
   */
  private void neg() {
    boolean flag = ((start - 1) == CONTEXT.size(NAME));
    assertTrue(flag);
    start = CONTEXT.size(NAME);
  }
  
  /**
   * The number of references of the DB in the pool remains constant.
   */
  private void stay() {
    boolean flag = (start == CONTEXT.size(NAME));
    assertTrue(flag);
  }
  
  /**
   * Assumes that this command is successful.
   * @param pr process reference
   * @param s Session
   */
  private void ok(final Process pr, final Session s) {
    final String msg = process(pr, s);
    if(msg != null) fail(msg);
  }

  /**
   * Assumes that this command fails.
   * @param pr process reference
   * @param s Session
   */
  private void no(final Process pr, final Session s) {
    ok(process(pr, s) != null);
  }
  
  /**
   * Assumes that the specified flag is successful.
   * @param flag flag
   */
  private static void ok(final boolean flag) {
    assertTrue(flag);
  }
  
  /**
   * Runs the specified process.
   * @param pr process reference
   * @param session Session
   * @return success flag
   */
  private String process(final Process pr, final Session session) {
    try {
      return session.execute(pr) ? null : session.info();
    } catch(final Exception ex) {
      return ex.toString();
    }
  }
}
