package com.ixaris.commons.microservices.test.mocks;

import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import com.ixaris.commons.collections.lib.ExtendedCollections;

public final class SkeletonResourceMockVerifier {
    
    SkeletonResourceMockVerifier() {
        SkeletonResourceMock.setVerifying(true);
    }
    
    public <T> T called(final T mock) {
        try {
            return Mockito.verify(SkeletonResourceMockAnswer.unwrap(mock));
        } finally {
            SkeletonResourceMock.setVerifying(false);
        }
    }
    
    public <T> T called(final T mock, final long timeout) {
        try {
            return Mockito.verify(SkeletonResourceMockAnswer.unwrap(mock), Mockito.timeout(timeout));
        } finally {
            SkeletonResourceMock.setVerifying(false);
        }
    }
    
    public <T> T called(final T mock, final VerificationMode mode) {
        try {
            return Mockito.verify(SkeletonResourceMockAnswer.unwrap(mock), mode);
        } finally {
            SkeletonResourceMock.setVerifying(false);
        }
    }
    
    public void noMoreInteractions(final Object... mocks) {
        try {
            Mockito.verifyNoMoreInteractions(ExtendedCollections.mapArray(mocks, SkeletonResourceMockAnswer::unwrap));
        } finally {
            SkeletonResourceMock.setVerifying(false);
        }
    }
    
    public void zeroInteractions(final Object... mocks) {
        try {
            Mockito.verifyZeroInteractions(ExtendedCollections.mapArray(mocks, SkeletonResourceMockAnswer::unwrap));
        } finally {
            SkeletonResourceMock.setVerifying(false);
        }
    }
    
}
