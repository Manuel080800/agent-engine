package io.agentengine.core.rule.service;

import io.agentengine.core.common.record.ResourceResponseRecord;

import java.util.*;

public interface RuleResolver {
    List<ResourceResponseRecord> resolve(List<ResourceResponseRecord> rules);
}
