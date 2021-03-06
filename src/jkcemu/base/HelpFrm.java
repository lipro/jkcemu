/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige der Hilfetexte
 */

package jkcemu.base;

import java.awt.Event;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.lang.*;
import java.io.IOException;
import java.net.URL;
import java.util.EmptyStackException;
import java.util.EventObject;
import java.util.Stack;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import jkcemu.Main;


public class HelpFrm extends HTMLViewFrm implements HyperlinkListener
{
  private static class URLStackEntry
  {
    public URL    url;
    public Double viewPos;	// relative Angabe von 0.0 bis 1.0

    public URLStackEntry( URL url, Double viewPos )
    {
      this.url     = url;
      this.viewPos = viewPos;
    }
  };


  private static HelpFrm instance = null;

  private javax.swing.Timer    timer;
  private Double               posToScroll;
  private URL                  urlHome;
  private Stack<URLStackEntry> urlStack;
  private JMenuItem            mnuNavBack;
  private JMenuItem            mnuNavHome;
  private JButton              btnBack;
  private JButton              btnHome;
  private JButton              btnPrint;


  /*
   * Das Oeffnen bzw. Anzeigen des Fensters erfolgt erst im naechsten
   * Event-Verarbeitungszyklus, damit es keine Probleme gibt,
   * wenn die Methode aus einem modalen Dialog heraus aufgerufen werden.
   */
  public static void open( final String page )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    openInternal( page );
		  }
		} );
  }


	/* --- HyperlinkListener --- */

  @Override
  public void hyperlinkUpdate( HyperlinkEvent e )
  {
    if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
      setUrl( true, e.getURL(), null );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnPrint ) {
	rv = true;
	doPrint();
      }
      else if( (src == this.mnuNavBack) || (src == this.btnBack) ) {
	rv = true;
	doBack();
      }
      else if( (src == this.mnuNavHome) || (src == this.btnHome) ) {
	rv = true;
	setUrl( true, null, null );
      }
      else if( src == this.timer ) {
	rv = true;
	this.timer.stop();
	doScrollTo( this.posToScroll );
	this.posToScroll = null;
      }
    }
    if( !rv ) {
      super.doAction( e );
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private HelpFrm()
  {
    setTitle( "JKCEMU Hilfe" );
    Main.updIcon( this );
    this.timer    = new javax.swing.Timer( 500, this );
    this.urlStack = new Stack<>();
    this.urlHome  = getClass().getResource( "/help/home.htm" );


    // Menu
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    JMenu mnuNav = new JMenu( "Navigation" );
    mnuNav.setMnemonic( KeyEvent.VK_N );

    this.mnuNavBack = createJMenuItem(
		"Zur\u00FCck",
		KeyStroke.getKeyStroke( KeyEvent.VK_B, Event.CTRL_MASK ) );
    mnuNav.add( this.mnuNavBack );

    this.mnuNavHome = createJMenuItem(
		"Startseite",
		KeyStroke.getKeyStroke( KeyEvent.VK_H, Event.CTRL_MASK ) );
    mnuNav.add( this.mnuNavHome );

    createMenuBar( mnuFile, mnuNav, null );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE,
					new Insets( 5, 0, 0, 0 ),
					0, 0 );

    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );

    this.btnBack = createImageButton( "/images/nav/back.png", "Zur\u00FCck" );
    toolBar.add( this.btnBack );

    this.btnHome = createImageButton( "/images/nav/home.png", "Startseite" );
    toolBar.add( this.btnHome );
    toolBar.addSeparator();

    this.btnPrint = createImageButton( "/images/file/print.png", "Drucken" );
    toolBar.add( this.btnPrint );

    add( toolBar, gbc );


    // Anzeigebereich
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridy++;
    createEditorPane( gbc );
    this.editorPane.addCaretListener( this );
    this.editorPane.addHyperlinkListener( this );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      setBoundsToDefaults();
    }
    setResizable( true );
  }


	/* --- private Methoden --- */

  private void doBack()
  {
    if( this.urlStack.size() > 1 ) {
      try {
	this.urlStack.pop();	// aktuelle Seite vom Stack entfernen
	URLStackEntry entry = this.urlStack.pop();
	setUrl( false, entry.url, entry.viewPos );
      }
      catch( EmptyStackException ex ) {}
    }
  }


  private void doScrollTo( Double viewPos )
  {
    if( viewPos != null ) {
      double d = viewPos.doubleValue();
      int    h = this.editorPane.getHeight();
      if( (d > 0.0) && (d <= 1.0) && (h > 0) ) {
	JViewport vp = this.scrollPane.getViewport();
	if( vp != null ) {
	  vp.setViewPosition(
		new Point( 0, (int) Math.round( d * (double) h ) ) );
	}
      }
    }
  }


  private static void openInternal( String page )
  {
    if( instance == null ) {
      instance = new HelpFrm();
    }
    EmuUtil.showFrame( instance );
    instance.setPage( page );
  }


  private void setPage( String page )
  {
    setUrl(
	true,
	page != null ? getClass().getResource( page ) : null,
	null );
  }


  private void setUrl( boolean saveCurViewPos, URL url, Double viewPos )
  {
    if( url == null ) {
      this.urlStack.clear();
      url = this.urlHome;
    }
    if( url != null ) {

      /*
       * Seite nur anzeigen, wenn sie sich von der vorhergehenden
       * unterscheidet
       */
      boolean       alreadyVisible = false;
      URLStackEntry topEntry       = null;
      if( this.urlStack.size() > 0 ) {
	topEntry = this.urlStack.peek();
	if( topEntry.url.equals( url ) ) {
	  alreadyVisible = true;
	}
      }
      if( !alreadyVisible ) {
	try {

	  // aktuelle Position ermitteln und merken
	  if( saveCurViewPos && (topEntry != null) ) {
	    topEntry.viewPos = null;
	    int h = this.editorPane.getHeight();
	    if( h > 0 ) {
	      JViewport vp = this.scrollPane.getViewport();
	      if( vp != null ) {
		Point pt = vp.getViewPosition();
		if( pt != null ) {
		  double d = (double) pt.y / (double) h;
		  if( (d > 0.0) && (d <= 1.0) ) {
		    topEntry.viewPos = d;
		  }
		}
	      }
	    }
	  }

	  // neue Seite anzeigen
	  this.editorPane.setPage( url );

	  // wenn Seite angezeigt werden konnte, neuen Stack-Eintrag erzeugen
	  this.urlStack.push( new URLStackEntry( url, null ) );

	  // auf Position scrollen
	  if( viewPos != null ) {
	    this.posToScroll = viewPos;
	    this.timer.restart();
	  }

	  // Aktionsknoepfe aktualisieren
	  boolean stateBack = (this.urlStack.size() > 1);
	  this.mnuNavBack.setEnabled( stateBack );
	  this.btnBack.setEnabled( stateBack );

	  boolean stateHome = (stateBack || !url.equals( this.urlHome));
	  this.mnuNavHome.setEnabled( stateHome );
	  this.btnHome.setEnabled( stateHome );
	}
	catch( Exception ex ) {
	  BaseDlg.showErrorDlg(
		this,
		"Die Hilfeseite kann nicht angezeigt werden.\n\n"
			+ ex.getMessage() );
	}
      }
    }
  }
}
