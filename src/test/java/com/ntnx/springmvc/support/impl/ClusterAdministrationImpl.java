/*
 * Copyright (c) 2010 Nutanix Inc. All rights reserved.
 *
 * Author: prakash@nutanix.com
 */

package com.ntnx.springmvc.support.impl;

import com.ntnx.springmvc.support.api.ClusterAdministration;
import com.ntnx.springmvc.support.beans.PrimitiveDto;
import lombok.extern.slf4j.Slf4j;

/**
 * Cluster administration services implementation
 */
@SuppressWarnings({"Duplicates", "SingleElementAnnotation"})
@Slf4j
public class ClusterAdministrationImpl implements ClusterAdministration {

  @Override
  public PrimitiveDto<String> getClusterInfo() {
    return null;
  }

  @Override
  public PrimitiveDto<Boolean> patchClusterParams(PrimitiveDto<String> aClusterDTO) {
    return null;
  }

  @Override
  public PrimitiveDto<Boolean> editClusterParams(PrimitiveDto<String> aClusterDTO) {
    return null;
  }

  @Override
  public PrimitiveDto<Boolean> addNfsSubnetWhitelist(PrimitiveDto<String> subnetAddress) {
    return null;
  }

  @Override
  public PrimitiveDto<Boolean> removeNfsSubnetWhitelist(String subnetAddress) {
    return null;
  }
}
