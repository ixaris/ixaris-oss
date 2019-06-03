import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import java.util.concurrent.CompletableFuture;

public class NoPackageAsync {
    
    static String concat(int i, long j, float f, double d, Object obj, boolean b) {
        return i + ":" + j + ":" + f + ":" + d + ":" + obj + ":" + b;
    }
    
    public Async<Object> noPackageMethod(CompletableFuture<String> blocker, int var) {
        return result(concat(var, 10_000_000_000L, 1.5f, 3.5d, await(blocker), true));
    }
    
}
