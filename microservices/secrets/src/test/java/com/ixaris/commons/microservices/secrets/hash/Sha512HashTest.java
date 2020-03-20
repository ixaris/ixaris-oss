package com.ixaris.commons.microservices.secrets.hash;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class Sha512HashTest {
    
    @Test
    public void generateSha512Hash_success() {
        final String data = "solongandthanksforallthefish";
        final String hashedData = Sha512Hash.generate(data);
        
        Assertions.assertThat(hashedData).isEqualTo("s35wOOf0F1OwU1wbm99uk1XgspeECFK75fxQ7FVR60gGNr10L6p/buQNZ+tUd4VahpR1SpohnHm3JSPk4jVDSQ==");
    }
    
    @Test
    public void generateSha512Hash_generateHashForOtherData_hashShouldNotMatch() {
        final String data = "solongandthanksforallthefish";
        final String hashedData = Sha512Hash.generate(data);
        
        final String otherData = "supercalifragilisticexpialidocious";
        final String hashedOtherData = Sha512Hash.generate(otherData);
        
        Assertions.assertThat(hashedData).isNotEqualTo(hashedOtherData);
    }
}
