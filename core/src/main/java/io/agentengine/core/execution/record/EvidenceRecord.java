package io.agentengine.core.execution.record;

import java.util.*;

public record EvidenceRecord(String kind, String snapshotHash, boolean backupReady, List<TestEvidenceRecord> tests, boolean passed) {
}
