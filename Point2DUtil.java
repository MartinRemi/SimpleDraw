
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;



// This is used to sort points by some "score",
// which could be an angle or other metric associated with each point.
class Point2DAndScore {
	public Point2D point;
	public float score;
	public boolean isScorePositiveInfinity; // if true, ``score'' is ignored
	public Point2DAndScore(Point2D p,float s,boolean isPosInf) {
		point = p; score = s; isScorePositiveInfinity = isPosInf;
	}
}
class Point2DAndScoreComparator implements Comparator<Point2DAndScore> {
	public int compare( Point2DAndScore a, Point2DAndScore b ) {
		if ( a.isScorePositiveInfinity ) {
			if ( b.isScorePositiveInfinity ) return 0; // equal
			else return 1; // a is greater
		}
		else if ( b.isScorePositiveInfinity ) {
			return -1; // b is greater
		}
		else return (a.score<b.score) ? -1 : ( (a.score>b.score) ? 1 : 0 );
	}
}



public class Point2DUtil {

	static public Point2D computeCentroidOfPoints( ArrayList<Point2D> points ) {
		float x = 0, y = 0;
		for ( Point2D p : points ) {
			x += p.x();
			y += p.y();
		}
		if ( points.size() > 1 ) {
			x /= points.size();
			y /= points.size();
		}
		return new Point2D( x, y );
	}

	static public boolean isPointInsidePolygon( ArrayList< Point2D > polygonPoints, Point2D q ) {
		// This code was copied, with minor changes, from
		//    http://local.wasp.uwa.edu.au/~pbourke/geometry/insidepoly/
		// where it (the code, not the algorithm) is attributed to Randolph Franklin.
		// The idea behind the algorithm is to imagine a ray projecting
		// from the point toward the right, and then count how many times
		// that ray intersects an edge of the polygon.
		// If the number is odd, the point is inside the polygon.

		boolean returnValue = false;
		int i, j;

		for (i = 0, j = polygonPoints.size()-1; i < polygonPoints.size(); j = i++) {

			Point2D pi = polygonPoints.get(i);
			float xi = pi.x();
			float yi = pi.y();
			Point2D pj = polygonPoints.get(j);
			float xj = pj.x();
			float yj = pj.y();

			if (
				(((yi <= q.y()) && (q.y() < yj)) || ((yj <= q.y()) && (q.y() < yi)))
				&& (q.x() < (xj - xi) * (q.y() - yi) / (yj - yi) + xi)
			) {
				returnValue = ! returnValue;
			}
		}
		return returnValue;
	}

	// Returns the points on the convex hull in counter-clockwise order
	// (assuming a coordinate system with x+ right and y+ up).
	// Uses the well known algorithm "Graham's scan" for computing the convex hull in 2D,
	// an algorithm nicely explained at
	//    http://www.personal.kent.edu/~rmuhamma/Compgeometry/MyCG/ConvexHull/GrahamScan/grahamScan.htm
	static public ArrayList< Point2D > computeConvexHull(
		// input
		ArrayList< Point2D > points
	) {
		if ( points == null ) return null;
		if ( points.size() < 3 ) {
			ArrayList< Point2D > returnValue = new ArrayList< Point2D >();
			for ( Point2D p : points ) {
				returnValue.add( p );
			}
			return returnValue;
		}

		// There could be one or more points with minimal y coordinate.
		// We'll call these the "bottom" points.
		// Of these, we find the one with minimal x coordinate (the "bottom left" point)
		// and maximal x coordinate (the "bottom right" point).
		int indexOfBottomLeftPoint = 0;
		Point2D bottomLeftPoint = points.get( 0 );
		int indexOfBottomRightPoint = 0;
		Point2D bottomRightPoint = points.get( 0 );
		for ( int i = 1; i < points.size(); ++i ) {
			Point2D candidatePoint = points.get( i );
			if ( candidatePoint.y() < bottomLeftPoint.y() ) {
				indexOfBottomLeftPoint = indexOfBottomRightPoint = i;
				bottomLeftPoint = bottomRightPoint = candidatePoint;
			}
			else if ( candidatePoint.y() == bottomLeftPoint.y() ) {
				if ( candidatePoint.x() < bottomLeftPoint.x() ) {
					indexOfBottomLeftPoint = i;
					bottomLeftPoint = candidatePoint;
				}
				else if ( candidatePoint.x() > bottomRightPoint.x() ) {
					indexOfBottomRightPoint = i;
					bottomRightPoint = candidatePoint;
				}
			}
		}

		// Imagine that for each point, we compute the point's angle with respect to bottomLeftPoint,
		// and then sort the points by this angle.
		// This is equivalent to sorting the points by their cotangent, which is faster to compute.
		// Points with minimal y coordinate (i.e., "bottom" points)
		// will be given a cotangent of +infinity and dealt with later.
		Point2DAndScore [] pointsWithCotangents = new Point2DAndScore[ points.size() ];
		for ( int i = 0; i < points.size(); ++i ) {
			Point2D p = points.get( i );
			float delta_y = p.y() - bottomLeftPoint.y();
			assert delta_y >= 0;
			if ( delta_y == 0 ) {
				pointsWithCotangents[i] = new Point2DAndScore( p, 0, true );
			}
			else {
				float delta_x = p.x() - bottomLeftPoint.x();
				pointsWithCotangents[i] = new Point2DAndScore( p, delta_x/delta_y /* the cotangent */, false );
			}
		}
		// sort the points by their cotangent
		Arrays.sort(pointsWithCotangents, new Point2DAndScoreComparator());

		// We'll need to be able to efficiently remove points from consideration,
		// so we copy them into a linked list.
		// In doing this, we also reverse the order of points
		// (so they are in descending order of cotangent, i.e., in counter-clockwise order).
		// The points with +infinity cotangent (i.e. the "bottom" points)
		// can also be removed from consideration here,
		// so long as we keep the "bottom left" and "bottom right" points.
		LinkedList< Point2D > orderedPoints = new LinkedList< Point2D >();
		orderedPoints.add( bottomLeftPoint );
		// check if the "bottom left" and "bottom right" points are distinct
		if ( indexOfBottomLeftPoint != indexOfBottomRightPoint )
			orderedPoints.add( bottomRightPoint );
		for ( int i = pointsWithCotangents.length - 1; i >= 0; --i ) {
			if ( ! pointsWithCotangents[i].isScorePositiveInfinity ) {
				orderedPoints.add( pointsWithCotangents[i].point );
			}
		}

		if ( orderedPoints.size() > 2 ) {
			// We will loop through the ordered points, processing 3 consecutive points at a time.
			// Two iterators are used to backup and move forward.
			Point2D p0 = orderedPoints.get(0);
			Point2D p1 = orderedPoints.get(1);
			Point2D p2 = orderedPoints.get(2);
			ListIterator< Point2D > it3 = orderedPoints.listIterator(3);
			assert it3.nextIndex() == 3;
			while ( true ) {
				assert orderedPoints.size() > 2;
				Vector2D v01 = new Vector2D( p1.x()-p0.x(), p1.y()-p0.y() );
				Vector2D v12 = new Vector2D( p2.x()-p1.x(), p2.y()-p1.y() );

				// Compute the z component of the cross product of v1 and v2
				// (Note that the x and y components of the cross product are zero,
				// because the z components of a and b are both zero)
				float crossProduct_z = v01.x()*v12.y() - v01.y()*v12.x();

				if ( crossProduct_z > 0 ) {
					// we have a left turn; try to step forward
					if ( it3.hasNext() ) {
						p0 = p1;
						p1 = p2;
						p2 = it3.next();
					}
					else {
						// we can't step forward
						break;
					}
				}
				else {
					// Either we have a right-hand turn,
					// or the points are collinear (with the 3rd point either in front, or behind, the 2nd)
					// In any case, we remove the 2nd point from consideration and (try to) backup.
					assert it3.hasPrevious();
					it3.previous();
					assert it3.hasPrevious();
					it3.previous();
					it3.remove(); // deletes the 2nd point
					assert it3.hasNext();
					it3.next(); // now the iterator is back to where it used to be

					// now we try to backup
					assert it3.hasPrevious();
					it3.previous();
					assert it3.hasPrevious();
					it3.previous();
					if ( it3.hasPrevious() ) {
						p1 = p0;
						p0 = it3.previous();
						it3.next();
						it3.next();
						it3.next();
					}
					else {
						it3.next();
						it3.next();
						// we step forward instead
						if ( it3.hasNext() ) {
							p1 = p2;
							p2 = it3.next();
						}
						else {
							// we can't move in either direction
							break;
						}
					}
				}
			} // while
		}

		// copy the results to the appropriate output format
		ArrayList< Point2D > returnValue = new ArrayList< Point2D >();

		for ( Point2D p : orderedPoints ) {
			returnValue.add( p );
		}
		return returnValue;
	}

	static public ArrayList< Point2D > computeExpandedPolygon(
		ArrayList< Point2D > points, // input
		float marginThickness
	) {
		ArrayList< Point2D > newPoints = new ArrayList< Point2D >();
		if ( points.size() == 0 ) {
			// do nothing
		}
		else if ( points.size() == 1 ) {
			Point2D p = points.get(0);
			newPoints.add( new Point2D( p.x()-marginThickness, p.y() ) );
			newPoints.add( new Point2D( p.x(), p.y()-marginThickness ) );
			newPoints.add( new Point2D( p.x()+marginThickness, p.y() ) );
			newPoints.add( new Point2D( p.x(), p.y()+marginThickness ) );
		}
		else if ( points.size() == 2 ) {
			Point2D p0 = points.get(0);
			Point2D p1 = points.get(1);
			Vector2D v0 = Vector2D.mult( Point2D.diff(p1,p0).normalized(), marginThickness );
			Vector2D v1 = new Vector2D( -v0.y(), v0.x() );
			newPoints.add( Point2D.sum( p0, v1 ) );
			newPoints.add( Point2D.sum( p0, v0.negated() ) );
			newPoints.add( Point2D.sum( p0, v1.negated() ) );
			newPoints.add( Point2D.sum( p1, v1.negated() ) );
			newPoints.add( Point2D.sum( p1, v0 ) );
			newPoints.add( Point2D.sum( p1, v1 ) );
		}
		else {
			for ( int i = 0; i < points.size(); ++i ) {
				Point2D p = points.get(i);
				Point2D p_previous = points.get( i==0 ? points.size()-1 : i-1 );
				Point2D p_next = points.get( (i+1) % points.size() );
				Vector2D v_previous = Point2D.diff( p, p_previous ).normalized();
				Vector2D v_next = Point2D.diff( p, p_next ).normalized();

				newPoints.add( Point2D.sum( p, Vector2D.mult(new Vector2D(v_previous.y(),-v_previous.x()),marginThickness) ) );
				newPoints.add( Point2D.sum( p, Vector2D.mult( Vector2D.sum(v_next,v_previous).normalized(), marginThickness ) ) );
				newPoints.add( Point2D.sum( p, Vector2D.mult(new Vector2D(-v_next.y(),v_next.x()),marginThickness) ) );
			}
		}
		return newPoints;
	}

}

