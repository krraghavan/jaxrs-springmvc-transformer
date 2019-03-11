/*
 * Copyright (c) 2010 Nutanix Inc. All rights reserved.
 *
 * Author: prakash@nutanix.com
 */

package com.github.krr.springmvc.support.impl;

import com.github.krr.springmvc.support.api.ClusterAdministration;
import com.github.krr.springmvc.support.beans.PrimitiveDto;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;

/**
 * Cluster administration services implementation
 */
@SuppressWarnings({"Duplicates", "SingleElementAnnotation"})
@Slf4j
public class ClusterAdministrationImpl implements ClusterAdministration {

  @Override
  @GET
  public PrimitiveDto<String> getClusterInfo() {
    return null;
  }

  @Override
  @PATCH
  public PrimitiveDto<Boolean> patchClusterParams(PrimitiveDto<String> aClusterDTO) {
    return null;
  }

  @Override
  @PUT
  public PrimitiveDto<Boolean> editClusterParams(PrimitiveDto<String> aClusterDTO) {
    return null;
  }

  @Override
  @POST
  public PrimitiveDto<Boolean> addNfsSubnetWhitelist(PrimitiveDto<String> subnetAddress) {
    return null;
  }

  @Override
  @DELETE
  public PrimitiveDto<Boolean> removeNfsSubnetWhitelist(String subnetAddress) {
    return null;
  }
}
