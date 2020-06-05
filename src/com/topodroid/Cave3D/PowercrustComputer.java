/** @file PowercrustComputer.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D convex hull model computer
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.util.List;
import java.util.ArrayList;

class PowercrustComputer
{
  TglParser mParser;
  List<Cave3DStation> mStations;
  List<Cave3DShot>    mShots;
  ArrayList<Cave3DPolygon> mPlanview    = null;
  ArrayList<Cave3DSegment> mProfilearcs = null;
  ArrayList<Triangle3D> mTriangles;
  Cave3DSite[] mVertices;

  private Cave3DPowercrust powercrust = null;

  PowercrustComputer( TglParser parser, List<Cave3DStation> stations, List<Cave3DShot> shots )
  {
    mParser   = parser;
    mStations = stations;
    mShots    = shots;
  }

  boolean hasTriangles() { return mTriangles != null; }
  ArrayList<Triangle3D> getTriangles() { return mTriangles; }
  Cave3DSite[] getVertices() { return mVertices; }
 
  boolean hasPlanview() { return mPlanview != null; }
  boolean hasProfilearcs() { return mProfilearcs != null; }
  ArrayList<Cave3DPolygon> getPlanview() { return mPlanview; }
  ArrayList<Cave3DSegment> getProfilearcs() { return mProfilearcs; } 

  boolean computePowercrust( )
  {
    double delta = GlModel.mPowercrustDelta;
    try {
      // mCave3D.toast( "computing the powercrust" );
      powercrust = new Cave3DPowercrust( );
      powercrust.resetSites( 3 );
      double x, y, z, v;
      int ntot = mStations.size();
      // Log.v( "TopoGL", "... add sites (stations " + ntot + ")" );

      /* average angular distance
      double da = 0;
      int na = 0;
      for ( int n0 = 0; n0 < ntot; ++n0 ) {
        Cave3DStation st = mStations.get( n0 );
        ArrayList< Cave3DShot > station_splays = mParser.getSplayAt( st, false );
        int ns = station_splays.size();
        Cave3DShot sh = station_splays.get( 0 );
        double h = sh.len * Math.cos( sh.cln );
        double x0 = h * Math.sin(sh.ber);
        double y0 = h * Math.cos(sh.ber);
        double z0 = sh.len * Math.sin(sh.cln);
        double v0 = sh.len;
        for ( int n=0; n<ns; ++n ) {
          sh = station_splays.get( n );
          h = sh.len * Math.cos( sh.cln );
          x = h * Math.sin(sh.ber);
          y = h * Math.cos(sh.ber);
          z = sh.len * Math.sin(sh.cln);
          v = sh.len;
          da += ( x*x0 + y*y0 + z*z0 )/(v*v0);
          na ++;
          x0 = x;
          y0 = y;
          z0 = z;
          v0 = v;
        } 
      }
      da = (1.0 - da/na);
      // Log.v( "TopoGL", "average splay angle " + da );
      */

      for ( int n0 = 0; n0 < ntot; ++n0 ) {
        Cave3DStation st = mStations.get( n0 );
        x = st.x;
        y = st.y;
        z = st.z;
        powercrust.addSite( x, y, z );
        ArrayList< Cave3DShot > station_splays = mParser.getSplayAt( st, false );
        int ns = station_splays.size();
        if ( ns > 1 ) {
          // Log.v( "TopoGL", "station " + n0 + ": splays " + ns ); 
          double len_prev = station_splays.get( ns-1 ).len;
    
          for ( int n=0; n<ns; ++n ) {
            Cave3DShot sh = station_splays.get( n );
            double len = sh.len;
            double len_next = station_splays.get( (n+1)%ns ).len;
            if ( ( len+delta < len_prev && len+delta < len_next ) || ( len-delta > len_prev && len-delta > len_next ) ) {
              /* nothing */
            } else {
              double h = sh.len * Math.cos( sh.cln );
              x = h * Math.sin(sh.ber);
              y = h * Math.cos(sh.ber);
              z = sh.len * Math.sin(sh.cln);
              /* filtering with average angular distance
              double r2 = sh.len * da;
              r2 = r2*r2;
              for ( int n1=0; n1<ns; ++n1 ) {
                if ( n1 == n ) continue;
                Cave3DShot sh1 = station_splays.get( n );
                h = sh.len * Math.cos( sh.cln );
                double x1 = h * Math.sin(sh.ber) - x;
                double y1 = h * Math.cos(sh.ber) - y;
                double z1 = sh.len * Math.sin(sh.cln) - z;
                if ( (x1*x1 + y1*y1 + z1*z1) < r2 ) {
                  powercrust.addSite( st.e+x, st.n+y, st.z+z );
                  break;
                }
              }
              */
              powercrust.addSite( st.x+x, st.y+y, st.z+z );
            }
            len_prev = len;
          }
        }
        long nsites = powercrust.nrSites();
        // Log.v( "TopoGL", "after station " + n0 + "/" + ns + " sites " + nsites );
      }
      // long nsites = powercrust.nrSites();
      // Log.v( "TopoGL", "total sites " + powercrust.nrSites() + " ... compute" );
      int ok = powercrust.compute( );
      if ( ok == 1 ) {
        // Log.v( "TopoGL", "... insert triangles" );
        mTriangles = new ArrayList<Triangle3D>();
        mVertices = powercrust.insertTrianglesIn( mTriangles );
      }
      // Log.v( "TopoGL", "... release powercrust NP " + powercrust.np + " NF " + powercrust.nf );
      powercrust.release();
      // Log.v( "TopoGL", "powercrust done" );
      if ( ok != 1 ) return false;
      if ( mTriangles != null && mVertices != null ) {
        computePowercrustPlanView( );
        computePowercrustProfileView( );
      }
    } catch ( Exception e ) {
      Log.e( "TopoGL-POWERCRUST", "Error: " + e.getMessage() );
      return false;
    }
    // Log.v( "TopoGL", "Powercrust V " + mVertices.length + " F " + mTriangles.size() );
    return true;
  }

  private void computePowercrustPlanView( )
  {
    mPlanview = null;
    double eps = 0.01f;
    int nup = 0;
    for ( Triangle3D t : mTriangles ) {
      if ( t.normal.z < 0 ) {
        int nn = t.size;
        if ( nn > 2 ) { 
          nup ++;
          Cave3DSite s1 = (Cave3DSite)t.vertex[nn-2];
          Cave3DSite s0 = (Cave3DSite)t.vertex[nn-1];
          for ( int k=0; k<nn; ++k ) {
            Cave3DSite s2 = (Cave3DSite)t.vertex[k];
            s0.insertAngle( s1, s2 );
            s1 = s0;
            s0 = s2;
          }
        }
        t.direction = 1;
      } else {
        t.direction = -1;
      }
    }
    mPlanview = makePolygons( mVertices );
  }

  // polygons are built chaining the sites thru their angle' V1 vertex
  private ArrayList<Cave3DPolygon> makePolygons( Cave3DSite[] vertices )
  {
    ArrayList<Cave3DPolygon> polygons = new ArrayList< Cave3DPolygon >();
    // Log.v( "TopoGL", "up triangles " + nup );
    int nsite = 0;
    for ( int k = 0; k<vertices.length; ++k ) {
      Cave3DSite s0 = vertices[k];
      if ( s0.poly != null ) continue;
      if ( s0.isOpen() ) {
        Cave3DPolygon polygon = new Cave3DPolygon();
        polygon.addPoint( s0 );  // add S0 to the polygon
        s0.poly = polygon;       // and set the poligon to S0
        for ( Cave3DSite s1 = s0.angle.v1; s1 != s0; s1=s1.angle.v1 ) {
          if ( s1 == null || ! s1.isOpen() ) break; // 20200512 added test ! isOpen()
          if ( polygon.addPoint( s1 ) ) break; // break if S1 is already in the polygon 
          s1.poly = polygon;                   // otherwise S1 is added to the polygon and the polygon set to S1
        }
        polygons.add( polygon );
        nsite += polygon.size();
      }
    }
    // Log.v( "TopoGL", "polygon sites " + nsite );
    // Log.v( "TopoGL", "plan polygons " + polygons.size() );
    return polygons;
  }

  private void computePowercrustProfileView( )
  {
    // profileview = null;
    mProfilearcs = null;
    int nst = mStations.size();
    int nsh = mShots.size();
    Point2D F[] = new Point2D[ nsh ]; // P - from point of shot k
    Point2D T[] = new Point2D[ nsh ]; // P - to point of shot k
    Point2D P[] = new Point2D[ nsh ]; // point on intersection of bisecants
    Point2D B[] = new Point2D[ nst ]; // bisecant at station j
    Point2D M[] = new Point2D[ nsh ]; // midpoint of shot k

    // find bisecant of shots at st:
    //      ... -- sh1 ----(st)--- sh2 -- ...
    for ( int k=0; k < nst; ++k ) {
      Cave3DStation st = mStations.get(k);
      Cave3DShot sh1 = null;
      Cave3DShot sh2 = null;
      // find shots at st
      for ( Cave3DShot sh : mShots ) {
        if ( sh.from_station == st || sh.to_station == st ) {
          if ( sh1 == null ) {
            sh1 = sh;
          } else {
            sh2 = sh;
            break;
          }
        }
      }
      if ( sh2 != null ) { // ... (st1)--- sh1 ---(st)--- sh2 ---(st2) ...
        Cave3DStation st1 = ( sh1.from_station == st )? sh1.to_station : sh1.from_station;
        Cave3DStation st2 = ( sh2.from_station == st )? sh2.to_station : sh2.from_station;
        double dx1 = st1.x - st.x;
        double dy1 = st1.y - st.y;
        double d1  = Math.sqrt( dx1*dx1 + dy1*dy1 );
        dx1 /= d1; // unit vector along sh1 (in the horizontal plane)
        dy1 /= d1;
        double dx2 = st2.x - st.x;
        double dy2 = st2.y - st.y;
        double d2  = Math.sqrt( dx2*dx2 + dy2*dy2 );
        dx2 /= d2; // unit vector along sh2 (in the horizontal plane)
        dy2 /= d2;
        double dx = dx1 + dx2;
        double dy = dy1 + dy2;
        // double d   = Math.sqrt( dx*dx + dy*dy );
        // B[k] = new Point2D( dx/d, dy/d );
        B[k] = new Point2D( dx, dy ); // bisecant (no need to normalize)
      } else if ( sh1 != null ) { // end-station: ... (st1)--- sh1 ---(st)
        Cave3DStation st1 = ( sh1.from_station == st )? sh1.to_station : sh1.from_station;
        double dx1 = st1.x - st.x;
        double dy1 = st1.y - st.y;
        // double d1  = Math.sqrt( dx1*dx1 + dy1*dy1 );
        // B[k] = new Point2D( dy1/d1, -dx1/d1 );
        B[k] = new Point2D( dy1, -dx1 ); // orthogonal: no need to normalize
      } else { // ERROR unattached station
        Log.e( "TopoGL-POWERCRUST", "Error: missing station shots at " + st.name );
        B[k] = new Point2D( 0, 0 ); // ERROR
      }
    }

    // find midpoints
    for ( int k = 0; k < nsh; ++k ) {
      Cave3DShot sh = mShots.get(k);
      Cave3DStation fr = sh.from_station;
      Cave3DStation to = sh.to_station;
      F[k] = new Point2D( fr.x, fr.y ); // CRASH here - but there is no reason a shot doesnot have stations
      T[k] = new Point2D( to.x, to.y );
      M[k] = new Point2D( (fr.x+to.x)/2, (fr.y+to.y)/2 );
      // intersection of bisecants
      Point2D b1 = null; // bisecant at from point
      Point2D b2 = null; // bisecant at to point
      for (int kk=0; kk<nst; ++kk ) {
        Cave3DStation st = mStations.get(kk);
        if ( st == fr ) { b1 = B[kk]; if ( b2 != null ) break; }
        else if ( st == to ) { b2 = B[kk]; if ( b1 != null ) break; }
      }
      // intersection point of the lines
      //   fr + b1 * t
      //   to + b2 * s
      // ie  b1.x t - b2.x s = to.x - fr.x
      //     b1.y t - b2.y s = to.y - fr.y
      double a11 = b1.x;  double a12 = -b2.x;  double c1 = to.x - fr.x;
      double a21 = b1.y;  double a22 = -b2.y;  double c2 = to.y - fr.y;
      double det = a11 * a22 - a12 * a21;
      double t = ( a22 * c1 - a12 * c2 ) / det;
      // double s = ( a11 * c2 - a21 * c1 ) / det;
      P[k] = new Point2D( fr.x + a11 * t, fr.y + a21 * t );
    }

    // clear sites angles
    int nvp = mVertices.length;
    for ( int k=0; k<nvp; ++k ) mVertices[k].angle = null;

    int nup = 0;
    mProfilearcs = new ArrayList< Cave3DSegment >();
    // intersection triangles is ok for vertical caves
    for ( int k = 0; k < nsh; ++k ) {
      Cave3DShot sh = mShots.get(k);
      Vector3D p1 = sh.from_station; // .toVector();
      Vector3D p2 = sh.to_station; // .toVector();
      ArrayList< Cave3DSegment > tmp = new ArrayList<>();
      for ( Triangle3D t : mTriangles ) {
        int nn = t.size;
        if ( nn <= 2 ) continue;
        Cave3DIntersection q1 = null;
        Cave3DIntersection q2 = null;
        Cave3DSite s1 = (Cave3DSite)t.vertex[nn-1];
        double z1=1;
        for ( int kk=0; kk<nn; ++kk ) {
          Cave3DSite s2 = (Cave3DSite)t.vertex[kk];
          Cave3DIntersection qq = intersect2D( p1, p2, s1, s2 );
          if ( qq != null ) {
            if ( q1 == null ) {
              q1 = qq;
            } else if ( q2 == null ) {
              if ( qq.s < q1.s ) { q2 = q1; q1 = qq; } else { q2 = qq; }
            } else {
              if ( qq.s < q1.s ) { q1 = qq; }
              else if ( qq.s > q2.s ) { q2 = qq; }
            }
          }
        }
        if ( q2 != null ) {
          Cave3DSegment sgm = getSegment( p1, p2, q1, q2 );
          if ( sgm != null ) tmp.add( sgm );
        }
      }
      // split in up and down
      Cave3DSegmentList lup = new Cave3DSegmentList();
      Cave3DSegmentList ldw = new Cave3DSegmentList();
      Vector3D dp = p2.difference( p1 );
      Vector3D pp = new Vector3D( dp.y, -dp.x, 0 );
      Vector3D pz = pp.crossProduct( dp ); 
      for ( Cave3DSegment s1 : tmp ) {
        Vector3D ds = s1.v1.difference( p1 );
        if ( pz.dotProduct( ds ) > 0 ) {
          lup.insert( s1 );
        } else {
          ldw.insert( s1 );
        }
      }
      
      // Log.v("TopoGL", "segments " + tmp.size() + " up " + lup.size + " down " + ldw.size );
      if ( lup.size > 0 ) {
        Cave3DSegment s1 = lup.head;
        mProfilearcs.add( s1 );
        for ( Cave3DSegment s2 = s1.next; s2 != null; s2 = s2.next ) {
          if ( s1.v2.s < s2.v1.s ) {
            mProfilearcs.add( new Cave3DSegment( s1.v2, s2.v1 ) );
          }  
          mProfilearcs.add( s2 );
          s1 = s2;
        }
      }
      if ( ldw.size > 0 ) {
        Cave3DSegment s1 = ldw.head;
        mProfilearcs.add( s1 );
        for ( Cave3DSegment s2 = s1.next; s2 != null; s2 = s2.next ) {
          if ( s1.v2.s < s2.v1.s ) {
            mProfilearcs.add( new Cave3DSegment( s1.v2, s2.v1 ) );
          }  
          mProfilearcs.add( s2 );
          s1 = s2;
        }
      }
    }
    // now make polygons from segments ???
  }

  double getVolume()
  {
    if ( mTriangles == null || mVertices == null ) return 0;
    Vector3D cm = new Vector3D();
    int nv = mVertices.length;
    for ( int k = 0; k < nv; ++k ) cm.add( mVertices[k] );
    cm.scaleBy( 1.0f / nv );
    double vol = 0;
    for ( Triangle3D t : mTriangles ) vol += t.volume( cm );
    return vol / 6;
  }


  // return the intersection abscissa if [p1,p2) intersect [q1,q2] in the X-Y plane
  // p1, p2 shot endpoints
  // q1, q2 triangle side
  //
  //   p1x * (1-s) + p2x * s = q1x * (1-t) + q2x * t
  //     (p2x-p1x) * s + (q1x-q2x) * t == q1x - p1x
  //     (p2y-p1y) * s + (q1y-q2y) * t == q1y - p1y
  //   det = (p2x-p1x)*(q1y-q2y) - (q1x-q2x)*(p2y-p1y)
  //
  // private double intersectZ = 0;

  private Cave3DIntersection intersect2D( Vector3D p1, Vector3D p2, Vector3D q1, Vector3D q2 )
  {
    double det = (p2.x-p1.x)*(q1.y-q2.y) - (q1.x-q2.x)*(p2.y-p1.y);
    if ( det == 0f ) return null;

    double s = ( (q1.y-q2.y) * (q1.x - p1.x) - (q1.x-q2.x) * (q1.y - p1.y) )/ det;
    double t = (-(p2.y-p1.y) * (q1.x - p1.x) + (p2.x-p1.x) * (q1.y - p1.y) )/ det;
    if ( t >= 0 && t < 1 ) {
      // intersectZ = s;
      return new Cave3DIntersection( Vector3D.sum( q1.scaledBy(1-t), q2.scaledBy(t) ), s );
    }
    return null;
  }

  // z = z2 + t ( z1 - z2 )
  // s = s2 + t ( s1 - s2 )
  // t(s=0) = ( 0 - s2 ) / ( s1 - s2 )
  // t(s=1) = ( 1 - s2 ) / ( s1 - s2 )
  private Cave3DSegment getSegment( Vector3D p1, Vector3D p2, Cave3DIntersection q1, Cave3DIntersection q2 )
  {
    if ( q1.s == q2.s ) return null;
    double t1 = ( 0 - q2.s ) / ( q1.s - q2.s );
    double t2 = ( 1 - q2.s ) / ( q1.s - q2.s );

    if ( q1.s <= 0 ) {
      if ( q2.s <= 0 ) return null;
      double z1 = q2.z + t1 * ( q1.z - q2.z );
      if ( q2.s <= 1 ) { // p1--q2
        return new Cave3DSegment( new Cave3DIntersection( p1.x, p1.y, z1, 0 ), q2 );
      }
      // q2.s > 1
      double z2 = q2.z + t2 * ( q1.z - q2.z );
      return new Cave3DSegment( new Cave3DIntersection( p1.x, p1.y, z1, 0 ), new Cave3DIntersection( p2.x, p2.y, z2, 1 ) );
    }  
    if ( q1.s >= 1 ) {
      if ( q2.s >= 1 ) return null;
      double z2 = q2.z + t2 * ( q1.z - q2.z );
      if ( q2.s >= 0 ) { // q2-p2
        return new Cave3DSegment( q2, new Cave3DIntersection( p2.x, p2.y, z2, 1 ) );
      }
      // q2.s < 0
      double z1 = q2.z + t1 * ( q1.z - q2.z );
      return new Cave3DSegment( new Cave3DIntersection( p1.x, p1.y, z1, 0 ), new Cave3DIntersection( p2.x, p2.y, z2, 1 ) );
    }  
    // 0 < q1.s < 1
    if ( q2.s < 0 ) { // p1-q1
      double z1 = q2.z + t1 * ( q1.z - q2.z );
      return new Cave3DSegment( new Cave3DIntersection( p1.x, p1.y, z1, 0 ), q1 );
    }
    if ( q2.s > 1 ) { // q1-p2
      double z2 = q2.z + t2 * ( q1.z - q2.z );
      return new Cave3DSegment( q1, new Cave3DIntersection( p2.x, p2.y, z2, 1 ) );
    }
    // 0 <= q2.s <= 1
    return new Cave3DSegment( q1, q2 );
  }

}
