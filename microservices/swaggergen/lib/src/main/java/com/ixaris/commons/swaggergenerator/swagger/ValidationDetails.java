package com.ixaris.commons.swaggergenerator.swagger;

import java.math.BigDecimal;

/**
 * POJO to represent validation details (extracted from Protobuf to be used to describe the constraints in Swagger)
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
final class ValidationDetails {
    
    private boolean required = false;
    private boolean requireNonEmpty = false;
    
    private int minLength = -1;
    private int maxLength = -1;
    
    private BigDecimal maxValue = null;
    private BigDecimal minValue = null;
    
    private String regex = null;
    
    boolean isRequired() {
        return required;
    }
    
    void setRequired() {
        this.required = true;
    }
    
    boolean isRequireNonEmpty() {
        return requireNonEmpty;
    }
    
    void setRequireNonEmpty() {
        this.requireNonEmpty = true;
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
    
    BigDecimal getMinValue() {
        return minValue;
    }
    
    void setMinValue(final BigDecimal minValue) {
        this.minValue = minValue;
    }
    
    String getRegex() {
        return regex;
    }
    
    void setRegex(final String regex) {
        this.regex = regex;
    }
}
