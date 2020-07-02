package com.ixaris.commons.jooq.persistence;

import org.jooq.impl.AbstractConverter;

public final class CharacterConverter extends AbstractConverter<String, Character> {
    
    private static final long serialVersionUID = 1L;
    
    public CharacterConverter() {
        super(String.class, Character.class);
    }
    
    @Override
    public Character from(final String s) {
        if (s == null) {
            return null;
        }
        return s.charAt(0);
    }
    
    @Override
    public String to(final Character character) {
        if (character == null) {
            return null;
        }
        
        return character.toString();
    }
}
