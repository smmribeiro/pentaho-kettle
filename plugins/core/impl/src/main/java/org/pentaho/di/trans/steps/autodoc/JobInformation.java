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


package org.pentaho.di.trans.steps.autodoc;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.bowl.Bowl;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.AreaOwner;
import org.pentaho.di.core.gui.GCInterface;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.ScrollBarInterface;
import org.pentaho.di.core.gui.SwingGC;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.JobPainter;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.repository.Repository;

public class JobInformation {

  private static JobInformation jobInfo;

  private Repository repository;

  public static final JobInformation getInstance() {
    if ( jobInfo == null ) {
      throw new RuntimeException( "The JobInformation singleton was not initialized!" );
    }
    return jobInfo;
  }

  public static final void init( Repository repository ) {
    jobInfo = new JobInformation();
    jobInfo.repository = repository;
  }

  private class JobInformationValues {
    public BufferedImage image;
    public JobMeta jobMeta;
    public List<AreaOwner> areaOwners;
  }

  private Map<ReportSubjectLocation, JobInformationValues> map;

  private JobInformation() {
    this.map = new HashMap<ReportSubjectLocation, JobInformationValues>();
  }

  public BufferedImage getImage( Bowl bowl, ReportSubjectLocation location ) throws KettleException {
    return getValues( bowl, location ).image;
  }

  public JobMeta getJobMeta( Bowl bowl, ReportSubjectLocation location ) throws KettleException {
    return getValues( bowl, location ).jobMeta;
  }

  public List<AreaOwner> getImageAreaList( Bowl bowl, ReportSubjectLocation location ) throws KettleException {
    return getValues( bowl, location ).areaOwners;
  }

  private JobInformationValues getValues( Bowl bowl, ReportSubjectLocation location ) throws KettleException {
    JobInformationValues values = map.get( location );
    if ( values == null ) {
      values = loadValues( bowl, location );
      map.put( location, values );
    }
    return values;
  }

  private JobMeta loadJob( Bowl bowl, ReportSubjectLocation location ) throws KettleException {
    JobMeta jobMeta;
    if ( !Utils.isEmpty( location.getFilename() ) ) {
      jobMeta = new JobMeta( bowl, location.getFilename(), repository );
    } else {
      jobMeta = repository.loadJob( location.getName(), location.getDirectory(), null, null );
    }
    return jobMeta;
  }

  private JobInformationValues loadValues( Bowl bowl, ReportSubjectLocation location ) throws KettleException {

    // Load the job
    //
    JobMeta jobMeta = loadJob( bowl, location );

    Point min = jobMeta.getMinimum();
    Point area = jobMeta.getMaximum();
    area.x += 30;
    area.y += 30;
    int iconsize = 32;

    ScrollBarInterface bar = new ScrollBarInterface() {
      public void setThumb( int thumb ) {
      }

      public int getSelection() {
        return 0;
      }
    };

    // Paint the transformation...
    //
    GCInterface gc = new SwingGC( null, area, iconsize, 50, 20 );
    List<AreaOwner> areaOwners = new ArrayList<AreaOwner>();
    JobPainter painter =
      new JobPainter(
        gc, jobMeta, area, bar, bar, null, null, null, areaOwners, new ArrayList<JobEntryCopy>(), iconsize, 1,
        0, 0, true, "FreeSans", 10 );
    painter.setMagnification( 0.25f );
    painter.drawJob();
    BufferedImage bufferedImage = (BufferedImage) gc.getImage();
    int newWidth = bufferedImage.getWidth() - min.x;
    int newHeigth = bufferedImage.getHeight() - min.y;
    BufferedImage image = new BufferedImage( newWidth, newHeigth, bufferedImage.getType() );
    image.getGraphics().drawImage(
      bufferedImage, 0, 0, newWidth, newHeigth, min.x, min.y, min.x + newWidth, min.y + newHeigth, null );

    JobInformationValues values = new JobInformationValues();
    values.jobMeta = jobMeta;
    values.image = image;
    values.areaOwners = areaOwners;

    return values;
  }

  public void drawImage( Bowl bowl, final Graphics2D g2d, final Rectangle2D rectangle2d, ReportSubjectLocation location,
    boolean pixelateImages ) throws KettleException {

    // Load the job
    //
    JobMeta jobMeta = loadJob( bowl, location );

    Point min = jobMeta.getMinimum();
    Point area = jobMeta.getMaximum();

    area.x -= min.x;
    area.y -= min.y;

    int iconsize = 32;

    ScrollBarInterface bar = new ScrollBarInterface() {
      public void setThumb( int thumb ) {
      }

      public int getSelection() {
        return 0;
      }
    };

    // Paint the transformation...
    //
    Rectangle rect = new java.awt.Rectangle( 0, 0, area.x, area.y );
    double magnificationX = rectangle2d.getWidth() / rect.getWidth();
    double magnificationY = rectangle2d.getHeight() / rect.getHeight();
    float magnification = (float) Math.min( 1, Math.min( magnificationX, magnificationY ) );

    SwingGC gc = new SwingGC( g2d, rect, iconsize, 0, 0 );
    gc.setDrawingPixelatedImages( pixelateImages );
    List<AreaOwner> areaOwners = new ArrayList<AreaOwner>();
    JobPainter painter =
        new JobPainter( gc, jobMeta, area, bar, bar, null, null, null, areaOwners, new ArrayList<JobEntryCopy>(),
            iconsize, 1, 0, 0, true, "FreeSans", 10 );

    painter.setMagnification( magnification );

    painter.setTranslationX( ( -min.x ) * magnification );
    painter.setTranslationY( ( -min.y ) * magnification );

    painter.drawJob();
  }

}
