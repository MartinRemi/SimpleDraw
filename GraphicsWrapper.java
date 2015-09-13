
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;



import java.lang.Math;
import java.util.ArrayList;
import java.awt.Color;


class GraphicsWrapper {

	AffineTransform originalTransform = null;

	private int windowWidthInPixels = 10; // must be initialized to something positive
	private int windowHeightInPixels = 10; // must be initialized to something positive

	// The client may either call frame() or resize() first,
	// and we must initialize ourself differently depending on the case.
	private boolean hasFrameOrResizeBeenCalledBefore = false;

	public int getWidth() { return windowWidthInPixels; }
	public int getHeight() { return windowHeightInPixels; }


	private Graphics g = null;
	private Graphics2D g2 = null;
	private GeneralPath generalPath = new GeneralPath();
	private Line2D line2D = new Line2D.Float();
	private Path2D path2D = new Path2D.Float();
	private Rectangle2D.Float rectangle2D = new Rectangle2D.Float();
	private Ellipse2D.Float ellipse2D = new Ellipse2D.Float();
	private Arc2D.Float arc2D = new Arc2D.Float();

	public void set( Graphics g ) { this.g = g; this.g2 = (Graphics2D)g; this.originalTransform = g2.getTransform(); }




	private int fontHeight = 14;
	private Font font = new Font( "Sans-serif", Font.BOLD, fontHeight );
	private FontMetrics fontMetrics = null;
	public void setFontHeight( int h ) {
		fontHeight = h;
		font = new Font( "Sans-serif", Font.BOLD, fontHeight );
		fontMetrics = null;
	}
	public int getFontHeight() {
		return fontHeight;
	}









	private float offsetXInPixels = 0;
	private float offsetYInPixels = 0;
	private float scaleFactorInWorldSpaceUnitsPerPixel = 1.0f; // greater if user is more zoomed out

	public float convertPixelsToWorldSpaceUnitsX( float XInPixels ) { return ( XInPixels - offsetXInPixels )*scaleFactorInWorldSpaceUnitsPerPixel; }
	public float convertPixelsToWorldSpaceUnitsY( float YInPixels ) { return ( YInPixels - offsetYInPixels )*scaleFactorInWorldSpaceUnitsPerPixel; }
	public Point2D convertPixelsToWorldSpaceUnits( Point2D p ) { return new Point2D(convertPixelsToWorldSpaceUnitsX(p.x()),convertPixelsToWorldSpaceUnitsY(p.y())); }

	public int convertWorldSpaceUnitsToPixelsX( float x ) { return Math.round( x / scaleFactorInWorldSpaceUnitsPerPixel + offsetXInPixels ); }
	public int convertWorldSpaceUnitsToPixelsY( float y ) { return Math.round( y / scaleFactorInWorldSpaceUnitsPerPixel + offsetYInPixels ); }
	public Point2D convertWorldSpaceUnitsToPixels( Point2D p ) { return new Point2D(convertWorldSpaceUnitsToPixelsX(p.x()),convertWorldSpaceUnitsToPixelsY(p.y())); }

	public float getScaleFactorInWorldSpaceUnitsPerPixel() { return scaleFactorInWorldSpaceUnitsPerPixel; }

	public void pan( float dx, float dy ) {
		offsetXInPixels += dx;
		offsetYInPixels += dy;
	}
	public void zoomIn(
		float zoomFactor, // greater than 1 to zoom in, between 0 and 1 to zoom out
		float centerXInPixels,
		float centerYInPixels
	) {
		scaleFactorInWorldSpaceUnitsPerPixel /= zoomFactor;
		offsetXInPixels = centerXInPixels - (centerXInPixels - offsetXInPixels) * zoomFactor;
		offsetYInPixels = centerYInPixels - (centerYInPixels - offsetYInPixels) * zoomFactor;
	}
	public void zoomIn(
		float zoomFactor // greater than 1 to zoom in, between 0 and 1 to zoom out
	) {
		zoomIn( zoomFactor, windowWidthInPixels * 0.5f, windowHeightInPixels * 0.5f );
	}

	// This can be used to implement bimanual (2-handed) camera control,
	// or 2-finger camera control, as in a "pinch" gesture
	public void panAndZoomBasedOnDisplacementOfTwoPoints(
		// these are assumed to be in pixel coordinates
		Point2D A_old, Point2D B_old,
		Point2D A_new, Point2D B_new
	) {
		// Compute midpoints of each pair of points
		Point2D M1 = Point2D.average( A_old, B_old );
		Point2D M2 = Point2D.average( A_new, B_new );

		// This is the translation that the world should appear to undergo.
		Vector2D translation = Point2D.diff( M2, M1 );

		// Compute a vector associated with each pair of points.
		Vector2D v1 = Point2D.diff( A_old, B_old );
		Vector2D v2 = Point2D.diff( A_new, B_new );

		float v1_length = v1.length();
		float v2_length = v2.length();
		float scaleFactor = 1;
		if ( v1_length > 0 && v2_length > 0 )
			scaleFactor = v2_length / v1_length;
		pan( translation.x(), translation.y() );
		zoomIn( scaleFactor, M2.x(), M2.y() );
	}

	public void frame(
		AlignedRectangle2D rect,
		boolean expand // true if caller wants a margin of whitespace added around the rect
	) {
		hasFrameOrResizeBeenCalledBefore = true;
		assert windowWidthInPixels > 0 && windowHeightInPixels > 0;

		if ( rect.isEmpty() || rect.getDiagonal().x() == 0 || rect.getDiagonal().y() == 0 ) {
			return;
		}
		if ( expand ) {
			float diagonal = rect.getDiagonal().length() / 20;
			Vector2D v = new Vector2D( diagonal, diagonal );
			rect = new AlignedRectangle2D( Point2D.diff(rect.getMin(),v), Point2D.sum(rect.getMax(),v) );
		}
		if ( rect.getDiagonal().x() / rect.getDiagonal().y() >= windowWidthInPixels / (float)windowHeightInPixels ) {
			offsetXInPixels = - rect.getMin().x() * windowWidthInPixels / rect.getDiagonal().x();
			scaleFactorInWorldSpaceUnitsPerPixel = rect.getDiagonal().x() / windowWidthInPixels;
			offsetYInPixels = windowHeightInPixels/2 - rect.getCenter().y() / scaleFactorInWorldSpaceUnitsPerPixel;
		}
		else {
			offsetYInPixels = - rect.getMin().y() * windowHeightInPixels / rect.getDiagonal().y();
			scaleFactorInWorldSpaceUnitsPerPixel = rect.getDiagonal().y() / windowHeightInPixels;
			offsetXInPixels = windowWidthInPixels/2 - rect.getCenter().x() / scaleFactorInWorldSpaceUnitsPerPixel;
		}
	}


	public void resize( int w, int h ) {
		if ( ! hasFrameOrResizeBeenCalledBefore ) {
			windowWidthInPixels = w;
			windowHeightInPixels = h;
			hasFrameOrResizeBeenCalledBefore = true;
			return;
		}

		Point2D oldCenter = convertPixelsToWorldSpaceUnits( new Point2D(
			windowWidthInPixels * 0.5f, windowHeightInPixels * 0.5f
		) );
		float radius = Math.min( windowWidthInPixels, windowHeightInPixels ) * 0.5f * scaleFactorInWorldSpaceUnitsPerPixel;


		windowWidthInPixels = w;
		windowHeightInPixels = h;

		if ( radius > 0 ) {
			frame(
				new AlignedRectangle2D(
					new Point2D( oldCenter.x() - radius, oldCenter.y() - radius ),
					new Point2D( oldCenter.x() + radius, oldCenter.y() + radius )
				),
				false
			);
		}
	}

	public void setCoordinateSystemToPixels() {
		AffineTransform transform = new AffineTransform();
		g2.setTransform(originalTransform);
		g2.transform(transform);
	}

	public void setCoordinateSystemToWorldSpaceUnits() {
		AffineTransform transform = new AffineTransform();
		transform.translate( offsetXInPixels, offsetYInPixels );
		float s = 1.0f/scaleFactorInWorldSpaceUnitsPerPixel;
		transform.scale( s, s );
		g2.setTransform(originalTransform);
		g2.transform(transform);
	}

	public void clear( float r, float g, float b ) {
		setColor(r,g,b);
		setCoordinateSystemToPixels();
		this.g.fillRect( 0, 0, windowWidthInPixels, windowHeightInPixels );
	}

	public void setupForDrawing() {
	}

	public void enableAlphaBlending() {
	}

	public void disableAlphaBlending() {
	}

	public void setColor( float r, float g, float b ) {
		g2.setColor( new Color( r, g, b ) );
	}

	public void setColor( float r, float g, float b, float alpha ) {
		g2.setColor( new Color( r, g, b, alpha ) );
	}

	public void setColor( Color c ) {
		setColor( c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, c.getAlpha()/255.0f );
	}

	public void setColor( Color c, float alpha ) {
		setColor( c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, alpha );
	}

	public void setLineWidth( float width ) {
		g2.setStroke( new BasicStroke( width ) );
	}

	public void drawLine( float x1, float y1, float x2, float y2 ) {
		//generalPath.reset();
		//generalPath.moveTo( x1, y1 );
		//generalPath.lineTo( x2, y2 );
		//g2.draw( generalPath );

		line2D.setLine( x1, y1, x2, y2 );
		g2.draw( line2D );
	}

	public void drawPolyline( ArrayList< Point2D > points, boolean isClosed, boolean isFilled ) {
		if ( points.size() <= 1 )
			return;
		path2D.reset();
		Point2D p = points.get(0);
		path2D.moveTo( p.x(), p.y() );
		for ( int i = 1; i < points.size(); ++i ) {
			p = points.get(i);
			path2D.lineTo( p.x(), p.y() );
		}
		if ( isClosed )
			path2D.closePath();
		if ( isFilled ) g2.fill( path2D );
		else g2.draw( path2D );
	}

	public void drawPolyline( ArrayList< Point2D > points ) {
		drawPolyline( points, false, false );
	}
	public void drawPolygon( ArrayList< Point2D > points ) {
		drawPolyline( points, true, false );
	}
	public void fillPolygon( ArrayList< Point2D > points ) {
		drawPolyline( points, true, true );
	}

	public void drawRect( float x, float y, float w, float h, boolean isFilled ) {
		if ( isFilled ) fillRect( x, y, w, h );
		else drawRect( x, y, w, h );
	}

	public void drawRect( float x, float y, float w, float h ) {
		if ( w < 0 ) {
			w = -w;
			x -= w;
		}
		if ( h < 0 ) {
			h = -h;
			y -= h;
		}
		rectangle2D.setRect( x, y, w, h );
		g2.draw( rectangle2D );
	}

	public void fillRect( float x, float y, float w, float h ) {
		if ( w < 0 ) {
			w = -w;
			x -= w;
		}
		if ( h < 0 ) {
			h = -h;
			y -= h;
		}
		rectangle2D.setRect( x, y, w, h );
		g2.fill( rectangle2D );
	}

	public void drawCircle( float x, float y, float radius, boolean isFilled ) {
		ellipse2D.setFrame( x, y, 2*radius, 2*radius );
		if ( isFilled ) g2.fill( ellipse2D );
		else g2.draw( ellipse2D );
	}

	public void drawCircle( float x, float y, float radius ) {
		drawCircle( x, y, radius, false );
	}

	public void fillCircle( float x, float y, float radius ) {
		drawCircle( x, y, radius, true );
	}

	public void drawCenteredCircle( float x, float y, float radius, boolean isFilled ) {
		x -= radius;
		y -= radius;
		drawCircle( x, y, radius, isFilled );
	}

	public void drawArc(
		float center_x, // increases right
		float center_y, // increases down
		float radius,
		float startAngle, // in radians; zero for right, increasing counterclockwise
		float angleExtent, // in radians; positive for counterclockwise
		boolean isFilled
	) {
		if ( isFilled ) {
			arc2D.setArcByCenter( center_x, center_y, radius, startAngle/Math.PI*180.0f, angleExtent/Math.PI*180.0f, Arc2D.PIE );
			g2.fill( arc2D );
		}
		else {
			arc2D.setArcByCenter( center_x, center_y, radius, startAngle/Math.PI*180.0f, angleExtent/Math.PI*180.0f, Arc2D.OPEN );
			g2.draw( arc2D );
		}
	}

	public void drawArc(
		float center_x, float center_y, float radius,
		float startAngle, // in radians
		float angleExtent // in radians
	) {
		drawArc( center_x, center_y, radius, startAngle, angleExtent, false );
	}

	public void fillArc(
		float center_x, float center_y, float radius,
		float startAngle, // in radians
		float angleExtent // in radians
	) {
		drawArc( center_x, center_y, radius, startAngle, angleExtent, true );
	}



	// returns the width of a string
	public float stringWidth( String s ) {
		if ( s == null || s.length() == 0 ) return 0;
		if ( fontMetrics == null ) {
			assert g2 != null;
			if ( g2 == null ) return 0;
			fontMetrics = g2.getFontMetrics( font );
		}
		return fontMetrics.stringWidth( s );
	}


	public void drawString(
		float x, float y,      // lower left corner of the string
		String s           // the string
	) {
		if ( s == null || s.length() == 0 ) return;

		g2.setFont( font );
		g2.drawString(
			s, x, y
		);
	}


}


