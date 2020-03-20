package com.ixaris.commons.clustering.lib.service;

import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterRequestEnvelope;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterResponseEnvelope;

public interface ClusterRouteFilter extends AsyncFilter<ClusterRequestEnvelope, ClusterResponseEnvelope> {}
