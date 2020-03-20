package com.ixaris.commons.clustering.lib.service;

import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;

public interface ClusterBroadcastFilter extends AsyncFilter<ClusterBroadcastEnvelope, Boolean> {}
