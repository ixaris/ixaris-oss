package com.ixaris.commons.protobuf.lib;

import java.util.List;
import java.util.function.Function;

import com.ixaris.commons.protobuf.lib.SensitiveMessageHelper.SensitiveDataContext;

public interface SensitiveDataFunction extends Function<List<SensitiveDataContext>, List<SensitiveDataContext>> {}
