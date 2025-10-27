/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public interface CustomerAssistant {
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
