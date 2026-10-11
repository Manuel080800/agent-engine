package io.agentengine.core.execution.record;

import java.util.*;

public record TestEvidenceRecord(String commandHash, String outputHash, int exitCode, boolean timedOut) {
}
