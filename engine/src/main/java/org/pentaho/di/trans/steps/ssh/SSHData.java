/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.ssh;

import java.io.CharArrayWriter;
import java.io.InputStream;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.bowl.Bowl;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyData;

/**
 * @author Samatar
 * @since 03-Juin-2008
 *
 */
public class SSHData extends BaseStepData implements StepDataInterface {
  public int indexOfCommand;
  public Connection conn;
  public boolean wroteOneRow;
  public String commands;
  public int nrInputFields;
  public int nrOutputFields;

  // Output fields
  public String stdOutField;
  public String stdTypeField;

  public RowMetaInterface outputRowMeta;

  public SSHData() {
    super();
    this.indexOfCommand = -1;
    this.conn = null;
    this.wroteOneRow = false;
    this.commands = null;
    this.stdOutField = null;
    this.stdTypeField = null;
  }

  public static Connection OpenConnection( Bowl bowl, String serveur, int port, String username, String password,
      boolean useKey, String keyFilename, String passPhrase, int timeOut, VariableSpace space, String proxyhost,
      int proxyport, String proxyusername, String proxypassword ) throws KettleException {
    Connection conn = null;
    char[] content = null;
    boolean isAuthenticated = false;
    try {
      // perform some checks
      if ( useKey ) {
        if ( Utils.isEmpty( keyFilename ) ) {
          throw new KettleException( BaseMessages.getString( SSHMeta.PKG, "SSH.Error.PrivateKeyFileMissing" ) );
        }
        FileObject keyFileObject = KettleVFS.getInstance( bowl ).getFileObject( keyFilename );

        if ( !keyFileObject.exists() ) {
          throw new KettleException( BaseMessages.getString( SSHMeta.PKG, "SSH.Error.PrivateKeyNotExist", keyFilename ) );
        }

        FileContent keyFileContent = keyFileObject.getContent();

        CharArrayWriter charArrayWriter = new CharArrayWriter( (int) keyFileContent.getSize() );

        try ( InputStream in = keyFileContent.getInputStream() ) {
          IOUtils.copy( in, charArrayWriter );
        }

        content = charArrayWriter.toCharArray();
      }
      // Create a new connection
      conn = createConnection( serveur, port );

      /* We want to connect through a HTTP proxy */
      if ( !Utils.isEmpty( proxyhost ) ) {
        /* Now connect */
        // if the proxy requires basic authentication:
        if ( !Utils.isEmpty( proxyusername ) ) {
          conn.setProxyData( new HTTPProxyData( proxyhost, proxyport, proxyusername, proxypassword ) );
        } else {
          conn.setProxyData( new HTTPProxyData( proxyhost, proxyport ) );
        }
      }

      // and connect
      if ( timeOut == 0 ) {
        conn.connect();
      } else {
        conn.connect( null, 0, timeOut * 1000 );
      }
      // authenticate
      if ( useKey ) {
        isAuthenticated =
          conn.authenticateWithPublicKey( username, content, space.environmentSubstitute( passPhrase ) );
      } else {
        isAuthenticated = conn.authenticateWithPassword( username, password );
      }
      if ( isAuthenticated == false ) {
        throw new KettleException( BaseMessages.getString( SSHMeta.PKG, "SSH.Error.AuthenticationFailed", username ) );
      }
    } catch ( Exception e ) {
      // Something wrong happened
      // do not forget to disconnect if connected
      if ( conn != null ) {
        conn.close();
      }
      throw new KettleException( BaseMessages.getString( SSHMeta.PKG, "SSH.Error.ErrorConnecting", serveur, username ), e );
    }
    return conn;
  }

  @VisibleForTesting
   static Connection createConnection( String serveur, int port ) {
    return new Connection( serveur, port );
  }
}
