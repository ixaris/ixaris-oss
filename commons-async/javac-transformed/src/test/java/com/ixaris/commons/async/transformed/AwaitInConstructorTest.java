package com.ixaris.commons.async.transformed;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class AwaitInConstructorTest {
    
    @Test
    public void testAwaitInConstructorCall() throws InterruptedException {
        block(awaitInConstructorCall(new TestRequest("A", "AA")));
        block(convolutedAwaitInConstructorCall(new TestRequest("A", "AA")));
    }
    
    public static Async<TestRequest> awaitInConstructorCall(final TestRequest in) {
        final TestRequest req = new TestRequest(in.getS(), await(tokenize(in.getS())));
        final String s2 = "test";
        final String s = await(result(""));
        return result(req);
    }
    
    public static Async<TestRequest> convolutedAwaitInConstructorCall(final TestRequest in) {
        final TestRequest req = new TestRequest(
            new TestRequest(in.getS(), await(tokenize(in.getS()))), await(tokenize(in.getS()))
        );
        final String s2 = "test";
        final String s = await(result(""));
        return result(req);
    }
    
    static final class TestRequest {
        
        private final String s;
        
        TestRequest(final String s, final String ts) {
            this.s = s;
        }
        
        TestRequest(final TestRequest s, final String ts) {
            this(s.s, ts);
        }
        
        public String getS() {
            return s;
        }
        
    }
    
    private static Async<String> tokenize(final String s) {
        await(AsyncExecutor.sleep(10L, TimeUnit.MILLISECONDS));
        return result(s);
    }
    
}
