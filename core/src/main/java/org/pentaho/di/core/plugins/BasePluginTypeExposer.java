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


package org.pentaho.di.core.plugins;

import java.lang.annotation.Annotation;

/**
 * This Class serves only one purpose, defeat the package protection modifiers on the BasePluginType. We normally would
 * simply move classes needing access to the same package, split between jars. However, in OSGI, split packages are
 * not encouraged and not possible unless the packages are being supplied by bundles with special notation denoting the
 * package split. Kettle is currently imported into OSGI as part of the System Bundle [0], which we cannot modify in 
 * such a way.
 *
 * Unless you're running within OSGI, you should never use this class.
 *
 * Created by nbaker on 2/11/15.
 */
public class BasePluginTypeExposer {
  private BasePluginType pluginType;
  private Object target;
  private String packageName;
  private final String altPackageName;

  public BasePluginTypeExposer( BasePluginType pluginType, Object target ) {
    this.target = target;
    this.pluginType = pluginType;

    altPackageName = target.getClass().getPackage().getName();
  }

  public String extractID( Annotation annotation ) {
    return pluginType.extractID( annotation );
  }

  public String extractName( Annotation annotation ) {
    String name = pluginType.extractName( annotation );
    packageName = extractI18nPackageName( annotation );
    return BasePluginType.getTranslation( name, packageName, altPackageName, target.getClass() );

  }

  public String extractDesc( Annotation annotation ) {
    String desc = pluginType.extractDesc( annotation );
    packageName = extractI18nPackageName( annotation );
    return BasePluginType.getTranslation( desc, packageName, altPackageName, target.getClass() );
  }

  public String extractCategory( Annotation annotation ) {
    String category = pluginType.extractCategory( annotation );
    packageName = extractI18nPackageName( annotation );
    return BasePluginType.getTranslation( category, packageName, altPackageName, target.getClass() );
  }

  public String extractImageFile( Annotation annotation ) {
    return pluginType.extractImageFile( annotation );
  }

  public boolean extractSeparateClassLoader( Annotation annotation ) {
    return pluginType.extractSeparateClassLoader( annotation );
  }

  public String extractI18nPackageName( Annotation annotation ) {
    return pluginType.extractI18nPackageName( annotation );
  }

  public String extractDocumentationUrl( Annotation annotation ) {
    return pluginType.extractDocumentationUrl( annotation );
  }

  public String extractCasesUrl( Annotation annotation ) {
    return pluginType.extractCasesUrl( annotation );
  }

  public String extractForumUrl( Annotation annotation ) {
    return pluginType.extractForumUrl( annotation );
  }

}
