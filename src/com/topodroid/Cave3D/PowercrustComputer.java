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
    float delta = GlModel.mPowercrustDelta;
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
    float eps = 0.01f;
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

  private ArrayList<Cave3DPolygon> makePolygons( Cave3DSite[] vertices )
  {
    ArrayList<Cave3DPolygon> polygons = new ArrayList< Cave3DPolygon >();
    // Log.v( "TopoGL", "up triangles " + nup );
    int nsite = 0;
    for ( int k = 0; k<vertices.length; ++k ) {
      Cave3DSite s0 = vertices[k];
      if ( s0.poly != null ) continue;
      if ( s0.isOpen() ) {
        // Log.v( "TopoGL", "found at " + k + " initial polygon vertex " + s0.x + " " + s0.y );
        Cave3DPolygon polygon = new Cave3DPolygon();
        polygon.addPoint( s0 );
        s0.poly = polygon;  
        int ns = 0;
        for ( Cave3DSite s1 = s0.angle.v1; s1 != s0; s1=s1.angle.v1 ) {
          // if ( s1.poly != null ) {
          //   Log.v( "TopoGL", "site on two polygons " + s1.x + "  " + s1.y );
          // } else {
          //   // Log.v( "TopoGL", "add site to polygon  " + s1.x + "  " + s1.y );
          // }
          if ( s1 == null ) break;
          if ( polygon.addPoint( s1 ) ) break;
          s1.poly = polygon;
          // if ( ns++ > 1024 ) {
          //   Log.v( "TopoGL", "exceeded max nr polygon sites" );
          //   break;
          // }
        }
        // Log.v( "TopoGL", "polygon size " + polygon.size() );
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
    int S[] = new int[ nsh ];
    Point2D F[] = new Point2D[ nsh ]; // P - from point of shot k
    Point2D T[] = new Point2D[ nsh ]; // P - to point of shot k
    Point2D P[] = new Point2D[ nsh ]; // point on intersection of bisecants
    Point2D B[] = new Point2D[ nst ]; // bisecant at station j
    Point2D M[] = new Point2D[ nsh ]; // midpoint of shot k

    // find bisecant of shots at st
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
      if ( sh2 != null ) {
        Cave3DStation st1 = ( sh1.from_station == st )? sh1.to_station : sh1.from_station;
        Cave3DStation st2 = ( sh2.from_station == st )? sh2.to_station : sh2.from_station;
        float dx1 = st1.x - st.x;
        float dy1 = st1.y - st.y;
        float d1  = (float)Math.sqrt( dx1*dx1 + dy1*dy1 );
        dx1 /= d1;
        dy1 /= d1;
        float dx2 = st2.x - st.x;
        float dy2 = st2.y - st.y;
        float d2  = (float)Math.sqrt( dx2*dx2 + dy2*dy2 );
        dx2 /= d2;
        dy2 /= d2;
        float dx = dx1 + dx2;
        float dy = dy1 + dy2;
        // float d   = (float)Math.sqrt( dx*dx + dy*dy );
        // B[k] = new Point2D( dx/d, dy/d );
        B[k] = new Point2D( dx, dy );
      } else if ( sh1 != null ) {
        Cave3DStation st1 = ( sh1.from_station == st )? sh1.to_station : sh1.from_station;
        float dx1 = st1.x - st.x;
        float dy1 = st1.y - st.y;
        // float d1  = (float)Math.sqrt( dx1*dx1 + dy1*dy1 );
        // B[k] = new Point2D( dy1/d1, -dx1/d1 );
        B[k] = new Point2D( dy1, -dx1 ); // no need to normalize
      } else {
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
      // lines: fr + b1 * t
      //        to + b2 * s
      // ie  b1.x t - b2.x s = to.x - fr.x
      //     b1.y t - b2.y s = to.y - fr.y
      float a11 = b1.x;  float a12 = -b2.x;  float c1 = to.x - fr.x;
      float a21 = b1.y;  float a22 = -b2.y;  float c2 = to.y - fr.y;
      float det = a11 * a22 - a12 * a21;
      float t = ( a22 * c1 - a12 * c2 ) / det;
      // float s = ( a11 * c2 - a21 * c1 ) / det;
      P[k] = new Point2D( fr.x + a11 * t, fr.y + a21 * t );
      if ( k == 0 ) {
        S[k] = 1;
      } else {
        // check ( P[k] - fr ) * (P[k1] - fr )
        float z = (P[k].x - fr.x)*(P[k-1].x - fr.x) + (P[k].y - fr.y)*(P[k-1].y - fr.y);
        S[k] = (z>0)? S[k-1] : -S[k-1];
      }
    }

    // clear sites angles
    int nvp = mVertices.length;
    for ( int k=0; k<nvp; ++k ) mVertices[k].angle = null;

    int nup = 0;
    // if ( true ) {
      mProfilearcs = new ArrayList< Cave3DSegment >();
      // intersection triangles is ok for vertical caves
      for ( int k = 0; k < nsh; ++k ) {
        Cave3DShot sh = mShots.get(k);
        Vector3D p1 = sh.from_station; // .toVector();
        Vector3D p2 = sh.to_station; // .toVector();
        ArrayList< Cave3DSegment > tmp = new ArrayList< Cave3DSegment >();
        // ArrayList< Cave3DSegment > tmp = new ArrayList< Cave3DSegment >();
        for ( Triangle3D t : mTriangles ) {
          int nn = t.size;
          if ( nn <= 2 ) continue;
          Vector3D q1 = null; // intersection points
          Cave3DSite s1 = (Cave3DSite)t.vertex[nn-1];
          float z1=1;
          for ( int kk=0; kk<nn; ++kk ) {
            Cave3DSite s2 = (Cave3DSite)t.vertex[kk];
            Vector3D qq = intersect2D( p1, p2, s1, s2 );
            if ( qq != null ) {
              if ( q1 == null ) {
                q1 = qq;
                z1 = intersectZ;
              } else {
                float z2 = intersectZ;
                if ( z1 > z2 ) { float zz = z1; z1 = z2; z2 = zz; }
                if ( z1 < 1 && z2 > 0 ) {
                  tmp.add( new Cave3DSegment( q1, qq ) );
                }
                break;
              }
            }
            s1 = s2;
          }
        }
        // make lists of connected paths
        ArrayList< Cave3DSegmentList > list = new ArrayList< Cave3DSegmentList >();
        int nt = tmp.size();
        for ( Cave3DSegment s1 : tmp ) {
          Cave3DSegmentList ll = null;
          for ( Cave3DSegmentList l1 : list ) {
            if ( ll == null ) {
              for ( Cave3DSegment s2 = l1.head; s2 != null; s2 = s2.next ) {
                if ( s1.touches( s2, 0.01f ) ) {
                  l1.add( s1 );
                  ll = l1;
                  break;
                }
              }
            } else {
              for ( Cave3DSegment s2 = l1.head; s2 != null; s2 = s2.next ) {
                if ( s1.touches( s2, 0.01f ) ) {
                  ll.mergeIn( l1 );
                  break;
                }
              }
            }
          }
          if ( ll == null ) {
            list.add( new Cave3DSegmentList( s1 ) );
          } 
          // else could remove empty lists
        }
        // get the closest path-list (does not work properly)
        // float zmin = ( p1.z < p2.z )? p1.z : p2.z;
        // float zmax = ( p1.z < p2.z )? p2.z : p1.z;
        float zmed = ( p1.z + p2.z ) / 2;
        Cave3DSegmentList lup = null;
        Cave3DSegmentList ldw = null;
        float zup =   Float.MAX_VALUE;
        float zdw = - Float.MAX_VALUE;
        for ( Cave3DSegmentList ll : list ) {
          if ( ll.size == 0 ) continue;
          float z0 = ll.centerZ();
          // float z1 = ll.maxZ();
          // float z2 = ll.minZ();
          if ( z0 > zmed && z0 < zup ) { 
            lup = ll;
            zup = z0;
          } 
          if ( z0 < zmed && z0 > zdw ) {
            ldw = ll;
            zdw = z0;
          }
        }
        if ( lup != null ) {
          for ( Cave3DSegment s = lup.head; s != null; s = s.next ) {
            mProfilearcs.add( s );
          }
        }
        if ( ldw != null ) {
          for ( Cave3DSegment s = ldw.head; s != null; s = s.next ) {
            mProfilearcs.add( s );
          }
        }
      }
      // now make polygons from segments ???

    // } else {
    //   // project triangles is ok for horizontal caves
    //   for ( Triangle3D t : mTriangles_powercrust ) {
    //     int nn = t.size;
    //     if ( nn <= 2 ) continue;
    //     Point2D c = new Point2D( t.center.x, t.center.y );
    //     for ( int k=0; k<nsh; ++k ) {
    //       float dx = P[k].x - c.x;
    //       float dy = P[k].y - c.y;
    //       float zf = (P[k].x - F[k].x)*dy - (P[k].y-F[k].y)*dx;
    //       float zt = (P[k].x - T[k].x)*dy - (P[k].y-T[k].y)*dx;
    //       if ( zf * zt <= 0 ) {
    //         Point2D n = new Point2D( t.normal.x, t.normal.y );
    //         if ( (n.x*dx + n.y*dy)*S[k] > 0 ) {
    //           nup ++;
    //           Cave3DSite s1 = (Cave3DSite)t.vertex[nn-2];
    //           Cave3DSite s0 = (Cave3DSite)t.vertex[nn-1];
    //           for ( int kk=0; kk<nn; ++kk ) {
    //             Cave3DSite s2 = (Cave3DSite)t.vertex[kk];
    //             s0.insertAngle( s1, s2 );
    //             s1 = s0;
    //             s0 = s2;
    //           }
    //         }
    //         break;
    //       }
    //     }
    //   }
    //   profileview = new ArrayList< Cave3DPolygon >();
    //   ArrayList< Cave3DPolygon > tmp = new ArrayList< Cave3DPolygon >();
    //   makePolygons( tmp );
    //   // Log.v( "TopoGL", "profile polygons " + tmp.size() );
    //   for ( Cave3DPolygon poly : tmp ) {
    //     Cave3DPolygon poly2 = new Cave3DPolygon();
    //     for ( Cave3DSite site : poly.points ) {
    //       float x = site.x;
    //       float y = site.y;
    //       // find the station the site lies close to
    //       Cave3DStation st = null;
    //       float dmin = 0;
    //       for ( Cave3DStation st1 : mStations ) {
    //         float d = (x - st1.x)*(x - st1.x) + (y - st1.y)*(y - st1.y);
    //         if ( st == null || d < dmin ) { dmin = d; st = st1; }
    //       }
    //       for ( int k = 0; k < nsh; ++ k ) {
    //         Cave3DShot sh = mShots.get(k);
    //         if ( sh.from_station != st && sh.to_station != st ) continue;
    //         float dx = P[k].x - x;
    //         float dy = P[k].y - y;
    //         float zf = (P[k].x - F[k].x)*dy - (P[k].y-F[k].y)*dx;
    //         float zt = (P[k].x - T[k].x)*dy - (P[k].y-T[k].y)*dx;
    //         if ( zf * zt <= 0 ) {
    //           // project from P[k] onto the line F[k]-T[k]:
    //           // intersection of F.x + (T.x-F.x) t = P.x + (site.x-P.x) s
    //           // ie,   (Tx-Fx) t + (Px-site.x) s = Px - Fx
    //           float a11 = T[k].x - F[k].x;  float a12 = dx;   float c1 = P[k].x - F[k].x;
    //           float a21 = T[k].y - F[k].y;  float a22 = dy;   float c2 = P[k].y - F[k].y;
    //           float det = a11 * a22 - a12 * a21;
    //           float t = ( a22 * c1 - a12 * c2 ) / det;
    //           Cave3DSite s1 = new Cave3DSite( F[k].x + a11*t, F[k].y + a21*t, site.z );
    //           poly2.addPoint( s1 );
    //           s1.poly = poly2;
    //           break;
    //         }
    //       }
    //     }
    //     profileview.add( poly2 );
    //   }
    // }
  }

  float getVolume()
  {
    if ( mTriangles == null || mVertices == null ) return 0;
    Vector3D cm = new Vector3D();
    int nv = mVertices.length;
    for ( int k = 0; k < nv; ++k ) cm.add( mVertices[k] );
    cm.scaleBy( 1.0f / nv );
    float vol = 0;
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
  private float intersectZ = 0;

  private Vector3D intersect2D( Vector3D p1, Vector3D p2, Vector3D q1, Vector3D q2 )
  {
    float det = (p2.x-p1.x)*(q1.y-q2.y) - (q1.x-q2.x)*(p2.y-p1.y);
    if ( det == 0f ) return null;

    float s = ( (q1.y-q2.y) * (q1.x - p1.x) - (q1.x-q2.x) * (q1.y - p1.y) )/ det;
    float t = (-(p2.y-p1.y) * (q1.x - p1.x) + (p2.x-p1.x) * (q1.y - p1.y) )/ det;
    if ( t >= 0 && t < 1 ) {
      intersectZ = s;
      return Vector3D.sum( q1.scaledBy(1-t), q2.scaledBy(t) );
    }
    return null;
  }

}
