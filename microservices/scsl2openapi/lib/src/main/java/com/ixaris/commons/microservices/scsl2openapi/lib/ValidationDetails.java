package com.ixaris.commons.microservices.scsl2openapi.lib;

import java.math.BigDecimal;

/**
 * POJO to represent validation details (extracted from Protobuf to be used to describe the constraints in Swagger)
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
final class ValidationDetails {
    
    private boolean required = false;
    
    private int minLength = -1;
    private int maxLength = -1;
    
    private BigDecimal maxValue = null;
    private boolean maxExclusive = false;
    private BigDecimal minValue = null;
    private boolean minInclusive = false;
    
    private String regex = null;
    
    boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    int getMinLength() {
        return minLength;
    }
    
    void setMinLength(final int minLength) {
        this.minLength = minLength;
    }
    
    int getMaxLength() {
        return maxLength;
    }
    
    void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }
    
    BigDecimal getMaxValue() {
        return maxValue;
    }
    
    void setMaxValue(final BigDecimal maxValue) {
        this.maxValue = maxValue;
    }
    
    public boolean isMaxExclusive() {
        return maxExclusive;
    }
    
    public void setMaxExclusive(boolean maxExclusive) {
        this.maxExclusive = maxExclusive;
    }
    
    BigDecimal getMinValue() {
        return minValue;
    }
    
    void setMinValue(final BigDecimal minValue) {
        this.minValue = minValue;
    }
    
    public boolean isMinExclusive() {
        return minInclusive;
    }
    
    public void setMinInclusive(boolean minInclusive) {
        this.minInclusive = minInclusive;
    }
    
    String getRegex() {
        return regex;
    }
    
    void setRegex(final String regex) {
        this.regex = regex;
    }
}
