package com.ixaris.commons.protobuf.lib;

import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataContext;
import java.util.List;
import java.util.function.Function;

public interface SensitiveDataFunction extends Function<List<SensitiveDataContext>, List<SensitiveDataContext>> {}
