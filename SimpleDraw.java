
import java.util.ArrayList;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
// import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;


// This stores a polygonal line, creating by a stroke of the pointing device.
class Stroke {
	// the points that make up the stroke, in world space coordinates
	private ArrayList< Point2D > points = new ArrayList< Point2D >();

	private AlignedRectangle2D boundingRectangle = new AlignedRectangle2D();
	private boolean isBoundingRectangleDirty = false;
	
	private Color color = Color.BLACK;

	public void addPoint( Point2D p ) {
		points.add( p );
		isBoundingRectangleDirty = true;
	}
	public ArrayList< Point2D > getPoints() {
		return points;
	}

	public AlignedRectangle2D getBoundingRectangle() {
		if ( isBoundingRectangleDirty ) {
			boundingRectangle.clear();
			for ( Point2D p : points ) {
				boundingRectangle.bound( p );
			}
			isBoundingRectangleDirty = false;
		}
		return boundingRectangle;
	}
	public void markBoundingRectangleDirty() {
		isBoundingRectangleDirty = true;
	}

	public boolean isContainedInRectangle( AlignedRectangle2D r ) {
		return r.contains( getBoundingRectangle() );
	}
	public boolean isContainedInLassoPolygon( ArrayList< Point2D > polygonPoints ) {
		for ( Point2D p : points ) {
			if ( ! Point2DUtil.isPointInsidePolygon( polygonPoints, p ) )
				return false;
		}
		return true;
	}

	public void translate( Vector2D v ) {
		for ( Point2D p : points ) {
			p.copy( Point2D.sum(p,v) );
		}
		markBoundingRectangleDirty();
	}

	public void rotate( float angle /* in radians */, Point2D center /* center of rotation */ ) {
		float sine = (float)Math.sin(angle);
		float cosine = (float)Math.cos(angle);
		for ( Point2D p : points ) {
			float delta_x = p.x() - center.x();
			float delta_y = p.y() - center.y();
			float new_x = center.x() + delta_x*cosine - delta_y*sine;
			float new_y = center.y() + delta_x*sine + delta_y*cosine;
			p.copy( new_x, new_y );
		}
		markBoundingRectangleDirty();
	}

	public void draw( GraphicsWrapper gw ) {
		gw.setColor(color);
		gw.drawPolyline( points );
	}

	// Flip the stroke in the 'rect' area
	public void flip( AlignedRectangle2D rect ) {
		for( Point2D p : points ) {
			p.copy( rect.getMin().x() + rect.getMax().x() - p.x(), p.y() );
		}
		markBoundingRectangleDirty();
	}
	
	//Sets a new color for the stroke
	public void setColor( Color color) {
		this.color = color;
	}
	
	//return the minimum distance between a given point and a stroke
	public double getDistance( Point2D m ) {
		double minDist = Double.MAX_VALUE;
		for ( Point2D p : points ) {
			double res = Math.sqrt( Math.pow( m.x() - p.x(), 2 ) + Math.pow( m.y() - p.y(), 2 ) );
			if( res < minDist ) {
				minDist = res;
			}
		}
		return minDist;
	}
}


// This stores a set of strokes.
class Drawing {

	public ArrayList< Stroke > strokes = new ArrayList< Stroke >();

	private AlignedRectangle2D boundingRectangle = new AlignedRectangle2D();
	private boolean isBoundingRectangleDirty = false;

	public void addStroke( Stroke s ) {
		strokes.add( s );
		isBoundingRectangleDirty = true;
	}

	public void clear() {
		strokes.clear();
		boundingRectangle.clear();
	}

	public AlignedRectangle2D getBoundingRectangle() {
		if ( isBoundingRectangleDirty ) {
			boundingRectangle.clear();
			for ( Stroke s : strokes ) {
				boundingRectangle.bound( s.getBoundingRectangle() );
			}
			isBoundingRectangleDirty = false;
		}
		return boundingRectangle;
	}
	public void markBoundingRectangleDirty() {
		isBoundingRectangleDirty = true;
	}

	public void draw( GraphicsWrapper gw ) {
		gw.setLineWidth( 5 );
		for ( Stroke s : strokes ) {
			s.draw( gw );
		}
		gw.setLineWidth( 1 );
	}

}



class MyCanvas extends JPanel implements MouseListener, MouseMotionListener {

	SimpleDraw simpleDraw;
	GraphicsWrapper gw = new GraphicsWrapper();

	RadialMenuWidget radialMenu = new RadialMenuWidget();

	// Stores all the strokes on the canvas,
	// except for any stroke currently being created.
	Drawing drawing = new Drawing();

	// stores a subset of the strokes
	ArrayList< Stroke > selectedStrokes = new ArrayList< Stroke >();

	int mouse_x, mouse_y, previous_mouse_x, previous_mouse_y, drag_start_x, drag_start_y;
	ArrayList< Point2D > pointerHistory = new ArrayList< Point2D >();

	private static final int DRAG_MODE_NONE = 0;
	private static final int DRAG_MODE_PAN = 1;
	private static final int DRAG_MODE_ZOOM = 2;
	private static final int DRAG_MODE_TOOL = 3;
	private int currentDragMode = DRAG_MODE_NONE;

	public MyCanvas( SimpleDraw sd ) {
		simpleDraw = sd;
		setBorder( BorderFactory.createLineBorder( Color.black ) );
		setBackground( Color.white );
		addMouseListener( this );
		addMouseMotionListener( this );

		radialMenu.setItemLabelAndID( RadialMenuWidget.CENTRAL_ITEM, "", -1 );
		radialMenu.setItemLabelAndID( RadialMenuWidget.NORTH,      sd.modeNames[ sd.MODE_PENCIL ],           sd.MODE_PENCIL );
		radialMenu.setItemLabelAndID( RadialMenuWidget.EAST,       sd.modeNames[ sd.MODE_RECT_SELECT ],      sd.MODE_RECT_SELECT );
		radialMenu.setItemLabelAndID( RadialMenuWidget.WEST,       sd.modeNames[ sd.MODE_MOVE_SELECTION ],   sd.MODE_MOVE_SELECTION );
	}
	public Dimension getPreferredSize() {
		return new Dimension( Constant.INITIAL_WINDOW_WIDTH, Constant.INITIAL_WINDOW_HEIGHT );
	}
	public void clear() {
		selectedStrokes.clear();
		drawing.clear();
		repaint();
	}
	public void deleteSelection() {
		for ( Stroke s : selectedStrokes ) {
			drawing.strokes.remove( s );
		}
		drawing.markBoundingRectangleDirty();
		selectedStrokes.clear();
	}
	public void frameDrawing() {
		gw.frame( drawing.getBoundingRectangle(), true );
	}
	public void flipSelection() {
		if(selectedStrokes.size() > 0) {
			// We try to find a global bounding box
			AlignedRectangle2D globalBoundingBox = new AlignedRectangle2D();
			for( Stroke s : selectedStrokes ) {
				globalBoundingBox.bound( s.getBoundingRectangle() );
			}

			// Then, we flip each stroke of the selection
			for( Stroke s : selectedStrokes ) {
				s.flip( globalBoundingBox );
			}
		}
	}
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		gw.set( g );
		if ( getWidth() != gw.getWidth() || getHeight() != gw.getHeight() )
			gw.resize( getWidth(), getHeight() );
		gw.clear(1,1,1);
		gw.setupForDrawing();
		gw.setCoordinateSystemToWorldSpaceUnits();
		gw.enableAlphaBlending();

		gw.setColor(1.0f,0.5f,0,0.3f); // transparent orange
		// draw filled rectangles over the selected strokes
		for ( Stroke s : selectedStrokes ) {
			AlignedRectangle2D r = s.getBoundingRectangle();
			Vector2D diagonal = r.getDiagonal();
			gw.fillRect( r.getMin().x(), r.getMin().y(), diagonal.x(), diagonal.y() );
		}

		gw.setColor( 0, 0, 0 );
		drawing.draw( gw );

		gw.setCoordinateSystemToPixels();

		if ( currentDragMode == DRAG_MODE_TOOL ) {
			switch ( simpleDraw.currentMode ) {
			case SimpleDraw.MODE_PENCIL :
				gw.setColor( 0, 0, 0 );
				gw.drawPolyline( pointerHistory );
				break;
			case SimpleDraw.MODE_RECT_SELECT :
				gw.setColor( 1.0f, 0.5f, 0, 0.3f ); // transparent orange
				gw.fillRect( drag_start_x, drag_start_y, mouse_x - drag_start_x, mouse_y - drag_start_y );
				gw.setColor( 1.0f, 0, 0 ); // red
				gw.drawRect( drag_start_x, drag_start_y, mouse_x - drag_start_x, mouse_y - drag_start_y );
				break;
			case SimpleDraw.MODE_MOVE_SELECTION :
				break;
			}
		}

		if ( radialMenu.isVisible() )
			radialMenu.draw( gw );
	}

	public void mouseClicked( MouseEvent e ) { }
	public void mouseEntered( MouseEvent e ) { }
	public void mouseExited( MouseEvent e ) { }

	public void mousePressed( MouseEvent e ) {

		if ( currentDragMode != DRAG_MODE_NONE )
			// The user is already dragging with a previously pressed button,
			// so ignore the press event from this new button.
			return;

		drag_start_x = previous_mouse_x = mouse_x = e.getX();
		drag_start_y = previous_mouse_y = mouse_y = e.getY();

		if ( radialMenu.isVisible() || (SwingUtilities.isRightMouseButton(e) && !e.isShiftDown()) ) {
			int returnValue = radialMenu.pressEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		else if ( SwingUtilities.isRightMouseButton(e) && e.isShiftDown() ) {
			currentDragMode = DRAG_MODE_ZOOM;
		}
		else if ( SwingUtilities.isLeftMouseButton(e) && e.isShiftDown() ) {
			currentDragMode = DRAG_MODE_PAN;
		}
		else if ( SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown() ) {
			pointerHistory.clear();
			pointerHistory.add( new Point2D( mouse_x, mouse_y ) );
			currentDragMode = DRAG_MODE_TOOL;
		}
	}

	public void mouseReleased( MouseEvent e ) {
		if ( radialMenu.isVisible() && SwingUtilities.isRightMouseButton(e) ) {
			int returnValue = radialMenu.releaseEvent( mouse_x, mouse_y );

			int itemID = radialMenu.getIDOfSelection();
			if ( 0 <= itemID && itemID < SimpleDraw.NUM_MODES ) {
				simpleDraw.setCurrentMode(itemID);
			}

			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		else if ( currentDragMode == DRAG_MODE_ZOOM && SwingUtilities.isRightMouseButton(e) ) {
			currentDragMode = DRAG_MODE_NONE;
		}
		else if ( currentDragMode == DRAG_MODE_PAN && SwingUtilities.isLeftMouseButton(e) ) {
			currentDragMode = DRAG_MODE_NONE;
		}
		else if ( currentDragMode == DRAG_MODE_TOOL && SwingUtilities.isLeftMouseButton(e) ) {
			Stroke newStroke;
			switch ( simpleDraw.currentMode ) {
			case SimpleDraw.MODE_PENCIL :
				newStroke = new Stroke();
				newStroke.setColor( simpleDraw.convertIntToColor( simpleDraw.currentColor ));
				for ( Point2D p : pointerHistory ) {
					newStroke.addPoint( gw.convertPixelsToWorldSpaceUnits( p ) );
				}
				drawing.addStroke( newStroke );
				break;
			case SimpleDraw.MODE_RECT_SELECT :
				// complete a rectangle selection
				if (drag_start_x != mouse_x && drag_start_y != mouse_y) {
					AlignedRectangle2D selectedRectangle = new AlignedRectangle2D(
						gw.convertPixelsToWorldSpaceUnits( new Point2D( drag_start_x, drag_start_y ) ),
						gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) )
					);
					selectedStrokes.clear();
					for ( Stroke s : drawing.strokes ) {
						if ( s.isContainedInRectangle( selectedRectangle ) )
							selectedStrokes.add( s );
					}
				}
				// closer stroke
				else {
					selectedStrokes.clear();
					Stroke closerStroke = null;
					double minDistance = Double.MAX_VALUE;
					for ( Stroke s : drawing.strokes ) {
						double dist = s.getDistance(gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) ));
						if ( dist < minDistance ) {
							minDistance = dist;
							closerStroke = s;
						}
					}
					if( closerStroke != null) {
						selectedStrokes.add(closerStroke);
					}
				}
				break;
			case SimpleDraw.MODE_MOVE_SELECTION :
				break;
			}
			currentDragMode = DRAG_MODE_NONE;
			repaint();
		}
	}

	public void mouseMoved( MouseEvent e ) {
		mouse_x = e.getX();
		mouse_y = e.getY();
		if ( e.isShiftDown() ) {
			// For debugging only:
			Point2D pointInWorldSpace = gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) );
			System.out.println( "Pixel coordinates: " + mouse_x + "," + mouse_y + "   World coordinates: " + pointInWorldSpace.x() + "," + pointInWorldSpace.y() );
		}
	}

	public void mouseDragged( MouseEvent e ) {
		previous_mouse_x = mouse_x;
		previous_mouse_y = mouse_y;
		mouse_x = e.getX();
		mouse_y = e.getY();
		int delta_x = mouse_x - previous_mouse_x;
		int delta_y = mouse_y - previous_mouse_y;



		if ( radialMenu.isVisible() ) {
			int returnValue = radialMenu.dragEvent( mouse_x, mouse_y );
			if ( returnValue == CustomWidget.S_REDRAW )
				repaint();
			if ( returnValue != CustomWidget.S_EVENT_NOT_CONSUMED )
				return;
		}
		else if ( currentDragMode == DRAG_MODE_PAN ) {
			gw.pan( delta_x, delta_y );
			repaint();
		}
		else if ( currentDragMode == DRAG_MODE_ZOOM ) {
			gw.zoomIn( (float)Math.pow( Constant.zoomFactorPerPixelDragged, delta_x-delta_y ) );
			repaint();
		}
		else if ( currentDragMode == DRAG_MODE_TOOL ) {
			switch ( simpleDraw.currentMode ) {
			case SimpleDraw.MODE_PENCIL :
				pointerHistory.add( new Point2D( mouse_x, mouse_y ) );
				break;
			case SimpleDraw.MODE_RECT_SELECT :
				break;
			case SimpleDraw.MODE_MOVE_SELECTION :
				Vector2D v = Point2D.diff(
					gw.convertPixelsToWorldSpaceUnits( new Point2D( mouse_x, mouse_y ) ),
					gw.convertPixelsToWorldSpaceUnits( new Point2D( previous_mouse_x, previous_mouse_y ) )
				);
				for ( Stroke s : selectedStrokes ) {
					s.translate( v );
				}
				drawing.markBoundingRectangleDirty();
				break;
			}
			repaint();
		}
	}

}

public class SimpleDraw implements ActionListener {

	static final String applicationName = "Simple Draw";

	JFrame frame;
	Container toolPanel;
	MyCanvas canvas;

	JMenuItem clearMenuItem, quitMenuItem, aboutMenuItem;
	JCheckBoxMenuItem toolsMenuItem;

	public static final int MODE_PENCIL = 0;
	public static final int MODE_RECT_SELECT = 1;
	public static final int MODE_MOVE_SELECTION = 2;
	public static final int NUM_MODES = 3;
	
	JRadioButton [] modeButtons = new JRadioButton[ NUM_MODES ];
	public String [] modeNames = new String[ NUM_MODES ];
	public int currentMode = MODE_PENCIL;
	
	public static final int COLOR_BLACK = 0;
	public static final int COLOR_RED = 1;
	public static final int COLOR_GREEN = 2;
	public static final int NUM_COLORS = 3;
	
	JRadioButton [] colorButtons = new JRadioButton[ NUM_COLORS ];
	public String [] colorNames = new String[ NUM_COLORS ];
	public int currentColor = COLOR_BLACK;


	JButton deleteButton;
	JButton frameButton;
	JButton flipButton;

	public void setCurrentMode( int mode ) {
		currentMode = mode;
		modeButtons[mode].setSelected(true);
	}
	
	public Color convertIntToColor( int color ) {
		switch( color ) {
		case COLOR_BLACK:
			return Color.BLACK;
		case COLOR_RED:
			return Color.RED;
		case COLOR_GREEN:
			return Color.GREEN;
		default:
			return Color.BLACK;
		}
	}
	
	public void setCurrentColor( int color ) {
		currentColor = color;
		colorButtons[color].setSelected(true);
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if ( source == clearMenuItem ) {
			canvas.clear();
		}
		else if ( source == quitMenuItem ) {
			int response = JOptionPane.showConfirmDialog(
				frame,
				"Really quit?",
				"Confirm Quit",
				JOptionPane.YES_NO_OPTION
			);

			if (response == JOptionPane.YES_OPTION) {
				System.exit(0);
			}
		}
		else if ( source == toolsMenuItem ) {
			Container pane = frame.getContentPane();
			if ( toolsMenuItem.isSelected() ) {
				pane.removeAll();
				pane.add( toolPanel );
				pane.add( canvas );
			}
			else {
				pane.removeAll();
				pane.add( canvas );
			}
			frame.invalidate();
			frame.validate();
		}
		else if ( source == aboutMenuItem ) {
			JOptionPane.showMessageDialog(
				frame,
				"'" + applicationName + "' sample program\n"
					+ "written July 2012\n",
				"About",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		else if ( source == deleteButton ) {
			canvas.deleteSelection();
			canvas.repaint();
		}
		else if ( source == frameButton ) {
			canvas.frameDrawing();
			canvas.repaint();
		}
		else if ( source == flipButton ) {
			canvas.flipSelection();
			canvas.repaint();
		}
		else {
			for ( int i = 0; i < NUM_MODES; ++i ) {
				if ( source == modeButtons[i] ) {
					currentMode = i;
					return;
				}
			}
			
			for ( int i = 0; i < NUM_COLORS; ++i ) {
				if ( source == colorButtons[i] ) {
					currentColor = i;
					return;
				}
			}
		}
	}


	// For thread safety, this should be invoked
	// from the event-dispatching thread.
	//
	private void createUI() {
		if ( ! SwingUtilities.isEventDispatchThread() ) {
			System.out.println(
				"Warning: UI is not being created in the Event Dispatch Thread!");
			assert false;
		}

		modeNames[ MODE_PENCIL ] = "Pencil";
		modeNames[ MODE_RECT_SELECT ] = "Rectangle Select";
		modeNames[ MODE_MOVE_SELECTION ] = "Move Selection";
		
		colorNames[ COLOR_BLACK ] = "Black";
		colorNames[ COLOR_RED ] = "Red";
		colorNames[ COLOR_GREEN ] = "Green";
		
		frame = new JFrame( applicationName );
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		JMenuBar menuBar = new JMenuBar();
			JMenu menu = new JMenu("File");
				clearMenuItem = new JMenuItem("Clear");
				clearMenuItem.addActionListener(this);
				menu.add(clearMenuItem);

				menu.addSeparator();

				quitMenuItem = new JMenuItem("Quit");
				quitMenuItem.addActionListener(this);
				menu.add(quitMenuItem);
			menuBar.add(menu);
			menu = new JMenu("View");
				toolsMenuItem = new JCheckBoxMenuItem("Show Tools");
				toolsMenuItem.setSelected( true );
				toolsMenuItem.addActionListener(this);
				menu.add(toolsMenuItem);
			menuBar.add(menu);
			menu = new JMenu("Help");
				aboutMenuItem = new JMenuItem("About");
				aboutMenuItem.addActionListener(this);
				menu.add(aboutMenuItem);
			menuBar.add(menu);
		frame.setJMenuBar(menuBar);

		toolPanel = new JPanel();
		toolPanel.setLayout( new BoxLayout( toolPanel, BoxLayout.Y_AXIS ) );

		canvas = new MyCanvas(this);

		Container pane = frame.getContentPane();
		pane.setLayout( new BoxLayout( pane, BoxLayout.X_AXIS ) );
		pane.add( toolPanel );
		pane.add( canvas );

		ButtonGroup group = new ButtonGroup();
		for ( int i = 0; i < NUM_MODES; ++i ) {
			modeButtons[i] = new JRadioButton( modeNames[i] );
			modeButtons[i].setAlignmentX( Component.LEFT_ALIGNMENT );
			modeButtons[i].addActionListener(this);
			if ( i == currentMode )
				modeButtons[i].setSelected(true);
			toolPanel.add( modeButtons[i] );
			group.add( modeButtons[i] );
		}

		deleteButton = new JButton( "Delete Selection" );
		deleteButton.setAlignmentX( Component.LEFT_ALIGNMENT );
		deleteButton.addActionListener(this);
		toolPanel.add( deleteButton );

		frameButton = new JButton( "Frame Drawing" );
		frameButton.setAlignmentX( Component.LEFT_ALIGNMENT );
		frameButton.addActionListener(this);
		toolPanel.add( frameButton );

		flipButton = new JButton( "Flip Selection" );
		flipButton.setAlignmentX( Component.LEFT_ALIGNMENT );
		flipButton.addActionListener(this);
		toolPanel.add( flipButton );
		
		ButtonGroup colorGroup = new ButtonGroup();
		for ( int i = 0; i < NUM_COLORS; ++i ) {
			colorButtons[i] = new JRadioButton( colorNames[i] );
			colorButtons[i].setAlignmentX( Component.LEFT_ALIGNMENT );
			colorButtons[i].addActionListener(this);
			if ( i == currentColor )
				colorButtons[i].setSelected(true);
			toolPanel.add( colorButtons[i] );
			colorGroup.add( colorButtons[i] );
		}

		frame.pack();
		frame.setVisible( true );
	}

	public static void main( String[] args ) {
		// Schedule the creation of the UI for the event-dispatching thread.
		javax.swing.SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					SimpleDraw sp = new SimpleDraw();
					sp.createUI();
				}
			}
		);
	}
}

