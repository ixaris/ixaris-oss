package com.ixaris.commons.protobuf.async;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataContext;
import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface AsyncSensitiveDataFunction
extends Function<List<SensitiveDataContext>, Async<List<SensitiveDataContext>>> {}
