
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


package org.pentaho.di.core.util.serialization;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.step.StepMetaInterface;

import static java.util.Objects.requireNonNull;
import static org.pentaho.di.core.util.serialization.StepMetaProps.from;

/**
 * Writes/Reads StepMetaInterface to and from a {@link Repository}
 * <p>
 * Usage:
 * <p>
 * RepoSerializer
 * .builder()
 * .repo( repo )
 * .stepMeta( stepMeta )
 * .stepId( stepId )
 * .transId( transId )
 * .serialize();
 * <p>
 * Future enhancement could cover inclusion of Metastore and Databases for steps which need that info.
 */
public class RepoSerializer {

  private final StepMetaInterface stepMetaInterface;
  private final Repository rep;
  private final ObjectId idTrans;
  private final ObjectId idStep;
  private static final String REPO_TAG = "step-xml";

  private RepoSerializer( StepMetaInterface stepMetaInterface, Repository rep, ObjectId idTrans, ObjectId idStep ) {
    this.stepMetaInterface = stepMetaInterface;
    this.rep = rep;
    this.idTrans = idTrans;
    this.idStep = idStep;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void serialize()
    throws KettleException {
    String xml = MetaXmlSerializer.serialize(
      from( stepMetaInterface ) ); //.encryptedFields( encryptedFields ) );

    rep.saveStepAttribute( idTrans, idStep, REPO_TAG, xml );
  }

  public void deserialize()
    throws KettleException {
    String xml = rep.getStepAttributeString( idStep, REPO_TAG );
    requireNonNull( MetaXmlSerializer.deserialize( xml ) )
      .to( stepMetaInterface );
  }

  public static class Builder {

    private Repository repo;
    private StepMetaInterface stepMetaInterface;
    private ObjectId idTrans;
    private ObjectId idStep;

    public Builder repo( Repository repo ) {
      this.repo = repo;
      return this;
    }

    public Builder stepMeta( StepMetaInterface stepMetaInterface ) {
      this.stepMetaInterface = stepMetaInterface;
      return this;
    }

    public Builder transId( ObjectId idTrans ) {
      this.idTrans = idTrans;
      return this;
    }

    public Builder stepId( ObjectId idStep ) {
      this.idStep = idStep;
      return this;
    }

    public void serialize() throws KettleException {
      new RepoSerializer(
        requireNonNull( stepMetaInterface ),
        requireNonNull( repo ),
        idTrans,
        idStep )
        .serialize();
    }

    public void deserialize() throws KettleException {
      new RepoSerializer(
        requireNonNull( stepMetaInterface ),
        requireNonNull( repo ),
        null,
        idStep )
        .deserialize();
    }


  }

}
