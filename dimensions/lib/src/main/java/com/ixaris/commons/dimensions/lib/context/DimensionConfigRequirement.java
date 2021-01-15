package com.ixaris.commons.dimensions.lib.context;

public enum DimensionConfigRequirement {
    
    UNDEFINED_OR_MATCH_ANY, // only undefined or match any allowed, not specific value
    OPTIONAL,
    REQUIRED // value is required (or match any if supported)
    
}
