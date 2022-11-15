package com.backbase.accelerators.payment.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DataModelAdditions {

    ORIGINAL_NEXT_EXECUTION_DATE("originalExecutionDate"),
    EXECUTION_COUNT("executionCount");

    private final String value;
}
