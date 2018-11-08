/*
 * Copyright (c) 2010 Nutanix Inc. All rights reserved.
 *
 * Author: prakash@nutanix.com
 */

package com.github.krr.springmvc.support.impl;

import com.github.krr.springmvc.support.api.ClusterAdministration;
import com.github.krr.springmvc.support.beans.PrimitiveDto;
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
