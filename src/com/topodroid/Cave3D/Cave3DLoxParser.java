/** @file Cave3DLoxParser.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D loch file parser 
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 */
package com.topodroid.Cave3D;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.util.Log;

public class Cave3DLoxParser extends Cave3DParser
{

  public Cave3DLoxParser( Cave3D cave3d, String filename ) throws Cave3DParserException
  {
    super( cave3d );

    readfile( filename );

  }

  private void readfile( String filename ) throws Cave3DParserException
  {
    LoxFile lox = new LoxFile( filename );


  }

}

