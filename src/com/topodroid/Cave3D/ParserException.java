/* @file ParserException.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D perser exception
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.Cave3D;

public class ParserException extends Exception
{
  String filename;
  int linenr;

  ParserException( String name, int nr ) 
  {
    filename = name;
    linenr   = nr;
  }

  String msg() { return filename + ":" + linenr; }

}
