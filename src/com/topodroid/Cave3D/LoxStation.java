/* @file LoxStation.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D loch Station 
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;


class LoxStation
{
  int id;
  int sid; // survey
  String name;
  String comment;
  int flag;

  double x, y, z;

  LoxStation( int _id, int _sid, String n, String c, int f, double _x, double _y, double _z )
  {
    id = _id;
    sid = _sid;
    name = n;
    comment = c;
    flag = f;
    x = _x;
    y = _y;
    z = _z;
  }
    
  int Id()  { return id; }
  int Survey()  { return sid; }
  int Flag()  { return flag; }

  String NameStr() { return name; }
  String Name()    { return name; }
  String Comment() { return comment; }

}
