/*
 * Copyright (c) 2010 Nutanix Inc. All rights reserved.
 *
 * Author: prakash@nutanix.com
 */

package com.github.krr.springmvc.support.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.krr.springmvc.support.beans.PrimitiveDto;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Cluster administration services
 */
@Path(ClusterAdministration.CLUSTER_URI_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ClusterAdministration {

  String CLUSTER_URI_BASE_PATH = "/cluster";

  @GET
  @Path("/")
  @JsonView(VersionViews.v2_0.class)
  PrimitiveDto<String> getClusterInfo();

  @PATCH
  @Path("/")
  PrimitiveDto<Boolean> patchClusterParams(PrimitiveDto<String> aClusterDTO);

  @PUT
  @Path("/")
  PrimitiveDto<Boolean> editClusterParams(PrimitiveDto<String> aClusterDTO);

  @POST
  @Path("/nfs_whitelist")
  PrimitiveDto<Boolean> addNfsSubnetWhitelist(PrimitiveDto<String> subnetAddress);

  @DELETE
  @Path("/nfs_whitelist/{name:.+}")
  PrimitiveDto<Boolean> removeNfsSubnetWhitelist(@PathParam("name")String subnetAddress);

}
