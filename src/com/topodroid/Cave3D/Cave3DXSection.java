/** @file Cave3DXSection.java
 *
 * @author marco corvi
 * @date mar 2021
 *
 * @brief x-section, roughly in a plane
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

import android.util.Log;

import java.io.StringWriter;
import java.io.PrintWriter;

import java.util.List;
import java.util.ArrayList;

public class Cave3DXSection
{
  ArrayList< Vector3D > points;
  Vector3D center;
  Vector3D normal;

  public Cave3DXSection( double x, double y, double z, List< Vector3D > shots )
  {
    center = new Vector3D( x, y, z );
    points = new ArrayList< Vector3D >();
    for ( Vector3D s : shots ) addPoint( s );
    computeNormal();
    orderPoints();
  }

  public int size() { return points.size(); }

  // return true if the site is already in the x-section
  private void addPoint( Vector3D s )
  {
    points.add( new Vector3D( s ) );
  }

  private void computeNormal()
  {
    double[] A = new double[9];
    for ( int k=0; k<9; ++k ) A[k]=0;
    for ( Vector3D v : points ) {
      A[0] += v.x * v.x;   A[1] += v.x * v.y;   A[2] += v.x * v.z;
      A[3] += v.y * v.x;   A[4] += v.y * v.y;   A[5] += v.y * v.z;
      A[6] += v.z * v.x;   A[7] += v.z * v.y;   A[8] += v.z * v.z;
    }
    // compute the smallest eigenvalue of A (A is pos. semidef. therefore eigenval >= 0)
    // 
    double b2 = A[0] + A[4] + A[8]; // trace
    double b1 = -( A[0]*A[4] + A[0]*A[8] + A[4]*A[8] + A[5]*A[7] + A[1]*A[3] + A[2]*A[6] );
    double b0 = A[0]*( A[4]*A[8] - A[5]*A[7] ) - A[1]*( A[3]*A[8] - A[6]*A[5] ) + A[2]*( A[3]*A[7] - A[6]*A[4] ); // determinant
    // find first positive zero of   f(L) = L^3 + b2 L^2 + b1 L + b0 = 0;
    double L = 0;
    double f0 = L * L * L + b2 * L * L + b1 * L + b0;
    int cnt = 0;
    double delta = 0.1 * Math.sqrt(A[0] + A[4] + A[8] ) / points.size();
    do {
      double L1 = L + delta;
      double f1 = L1 * L1 * L1 + b2 * L1 * L1 + b1 * L1 + b0;
      if ( f1 >= f0 || f1*f0 < 0 ) {
        delta = delta/2;
      } else {
        L = L1;
        f0 = f1;
      }

      // System.out.println("L " + L + " f0 " + f0 + " f1 " + f1 + " delta " + delta );
      // if ( ++cnt > 20 ) break;
      // f0 = L * L * L + b2 * L * L + b1 * L + b0;
    } while ( Math.abs( f0 ) > 0.00001 );

    double a0 = A[0] - L;
    double a4 = A[4] - L;
    double a8 = A[8] - L;
    double nx = 1.0;
    //  a8   -a5  * ny = -a3 nx / det
    // -a7    a4    nz   -a6 nx / det
    double det = a4 * a8 - A[5] * A[7];
    double ny = - ( a8 * A[3] - A[5] * A[6] ) / det;
    double nz = - ( a4 * A[6] - A[7] * A[3] ) / det;
    double nlen = Math.sqrt( nx*nx + ny*ny + nz*nz );
    nx /= nlen;
    ny /= nlen;
    nz /= nlen; 
    // check eigenvector and eigenvalue
    double x = A[0] * nx + A[1] * ny + A[2] * nz - L * nx;
    double y = A[3] * nx + A[4] * ny + A[5] * nz - L * ny;
    double z = A[6] * nx + A[7] * ny + A[8] * nz - L * nz;
    // Log.v("TopoGL", "check eigenvalue " + L + ": " + nx + " " + ny + " " + nz );
    System.out.println("check eigenvalue " + L + " N: " + nx + " " + ny + " " + nz );
    normal = new Vector3D( nx, ny, nz );
    normal.normalized();
    System.out.println(" normal: " + normal.x + " " + normal.y + " " + normal.z );
  }

  private Vector3D projection( Vector3D p )
  {
    Vector3D ret = p.difference( normal.scaledBy( normal.dotProduct(p) ) );
    ret.normalized();
    return ret;
  }

  private double angle( Vector3D v1, Vector3D v2 ) 
  {
    double c = v1.dotProduct( v2 );
    double s = v1.crossProduct( v2 ).length();
    double a = Math.atan2( s, c );
    if ( a < 0 ) a += 2 * Math.PI;
    return a;
  }

  private void orderPoints()
  {
    // arbitrary vector perpendicular to the normal
    Vector3D v = new Vector3D( normal );
    double t = v.x; v.x = v.y; v.y = v.z; v.z = t;
    Vector3D w = normal.crossProduct( v );
    w.normalized();
    ArrayList< Vector3D > pts0 = new ArrayList<>();
    for ( Vector3D p : points ) pts0.add( projection( p ) );

    ArrayList< Vector3D > pts1 = new ArrayList<>();
    Vector3D p0 = pts0.get(0);
    double d0 = w.dotProduct( p0 );
    int i0 = 0;
    for ( int k = 1; k<points.size(); ++k ) {
      Vector3D p2 = pts0.get( k );
      double d2 = w.dotProduct( p2 );
      if ( d2 > d0 ) { p0 = p2; d0 = d2; i0 = k; }
    }
    pts1.add( points.get( i0 ) );
    pts0.remove( i0 );
    points.remove( i0 );
    while ( pts0.size() > 0 ) {
      Vector3D p1 = pts0.get(0);
      double a1 = angle( p0, p1 );
      int i1 = 0;
      for ( int k = 1; k<points.size(); ++k ) {
        Vector3D p2 = pts0.get( k );
        double a2 = angle( p0, p2 );
        if ( a2 > a1 ) { p1 = p2; a1 = a2; i1 = k; }
      }
      pts1.add( points.get( i1 ) );
      pts0.remove( i1 );
      points.remove( i1 );
    }
    points = pts1;
    for ( Vector3D p : points ) System.out.println("v " + p.x + " " + p.y + " " + p.z );
  }  

  /*
  public static void main( String[] args )
  {
    ArrayList<Vector3D> data = new ArrayList<>();

    for ( int k=0; k<10; ++k ) {
      double a = Math.PI * 2 * Math.random();
      data.add( new Vector3D( Math.cos(a) + 0.1*(Math.random() -0.5),
                              Math.sin(a) + 0.1*(Math.random() -0.5),
                                            0.1*(Math.random() -0.5) ) );
    }
    Cave3DXSection xsection = new Cave3DXSection( 0, 0, 0, data );
  }
  */

}
