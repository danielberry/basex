package org.basex.gui.dialog;

import static org.basex.Text.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import org.basex.BaseX;
import org.basex.core.proc.DropDB;
import org.basex.core.proc.InfoDB;
import org.basex.core.proc.List;
import org.basex.data.MetaData;
import org.basex.gui.GUI;
import org.basex.gui.GUIProp;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXListChooser;
import org.basex.gui.layout.BaseXText;
import org.basex.io.IO;
import org.basex.util.StringList;
import org.basex.util.Token;

/**
 * Open Database Dialog.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class DialogOpen extends Dialog {
  /** List of currently available databases. */
  private BaseXListChooser choice;
  /** Information panel. */
  private BaseXLabel doc;
  /** Information panel. */
  private BaseXText detail;
  /** Buttons. */
  private BaseXBack buttons;

  /**
   * Default Constructor.
   * @param parent parent frame
   * @param drop show drop dialog
   */
  public DialogOpen(final JFrame parent, final boolean drop) {
    super(parent, drop ? DROPTITLE : OPENTITLE);

    // create database chooser
    final StringList db = List.list();
    if(db.size == 0) return;
    
    choice = new BaseXListChooser(this, db.finish(), HELPOPEN);
    set(choice, BorderLayout.CENTER);
    choice.setSize(130, 356);

    final BaseXBack info = new BaseXBack();
    info.setLayout(new BorderLayout());
    info.setBorder(new CompoundBorder(new EtchedBorder(),
        new EmptyBorder(10, 10, 10, 10)));

    doc = new BaseXLabel(DIALOGINFO);
    doc.setFont(new Font(GUIProp.font, 0, 18));
    doc.setBorder(0, 0, 5, 0);
    info.add(doc, BorderLayout.NORTH);

    detail = new BaseXText(HELPOPENINFO, false, this);
    detail.setBorder(new EmptyBorder(5, 5, 5, 5));
    detail.setFocusable(false);

    BaseXLayout.setWidth(detail, 480);
    info.add(detail, BorderLayout.CENTER);

    final BaseXBack pp = new BaseXBack();
    pp.setBorder(new EmptyBorder(0, 12, 0, 0));
    pp.setLayout(new BorderLayout());
    pp.add(info, BorderLayout.CENTER);

    // create buttons
    final BaseXBack p = new BaseXBack();
    p.setLayout(new BorderLayout());

    if(drop) {
      buttons = BaseXLayout.newButtons(this, true,
          new String[] { BUTTONDROP, BUTTONCANCEL },
          new byte[][] { HELPDROP, HELPCANCEL });
    } else {
      buttons = BaseXLayout.newButtons(this, true,
          new String[] { BUTTONRENAME, BUTTONOPEN, BUTTONCANCEL },
          new byte[][] { HELPRENAMEDB, HELPOPENDB, HELPCANCEL });
    }
    p.add(buttons, BorderLayout.EAST);
    pp.add(p, BorderLayout.SOUTH);

    set(pp, BorderLayout.EAST);
    setInfo();
    finish(parent);
  }

  /**
   * Returns the database name.
   * @return database name
   */
  public String db() {
    final String db = choice.getValue();
    return ok && db.length() > 0 ? db : null;
  }

  /**
   * Returns if no databases have been found.
   * @return result of check
   */
  public boolean nodb() {
    return choice == null;
  }

  @Override
  public void action(final String cmd) {
    if(BUTTONRENAME.equals(cmd)) {
      new DialogRename(GUI.get(), choice.getValue());
      choice.setData(List.list().finish());
    } else if(BUTTONOPEN.equals(cmd)) {
      close();
    } else if(BUTTONDROP.equals(cmd)) {
      final String db = choice.getValue();
      if(db.length() == 0) return;
      if(JOptionPane.showConfirmDialog(this, BaseX.info(DROPCONF, db),
          DIALOGINFO, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        DropDB.drop(db);
        choice.setData(List.list().finish());
        choice.requestFocusInWindow();
      }
    } else {
      ok = setInfo();
      BaseXLayout.enableOK(buttons, ok);
    }
  }

  @Override
  public void close() {
    if(ok) dispose();
  }

  /**
   * Refreshes the database information panel.
   * @return true if the current choice is valid. 
   */
  boolean setInfo() {
    final String db = choice.getValue().trim();
    if(db.length() == 0) return false;

    // read the database version
    final File dir = IO.dbpath(db);
    if(!dir.exists()) return false;
    doc.setText(db);

    try {
      ok = true;
      final MetaData meta = new MetaData(db);
      final int size = meta.read();
      detail.setText(InfoDB.db(meta, size, false, true));
    } catch(final IOException ex) {
      detail.setText(Token.token(ex.getMessage()));
      ok = false;
    }
    BaseXLayout.enableOK(buttons, ok);
    return ok;
  }
}
