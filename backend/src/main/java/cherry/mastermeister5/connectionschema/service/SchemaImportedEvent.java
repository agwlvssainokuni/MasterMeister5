/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister5.connectionschema.service;

import java.util.List;
import org.springframework.context.ApplicationEvent;

/**
 * nfr-design/logical-components.md (Unit 5) Question 1: published by
 * {@link ConnectionSchemaServiceImpl#importSchema} after a schema
 * re-import so Unit 5's {@code MasterMaintenanceServiceImpl} can prune
 * stale customization definitions, without {@code connectionschema}
 * importing anything from {@code mastermaintenance} (avoids a package
 * cycle, unlike a direct method call). Spring's default synchronous
 * {@code @EventListener} dispatch means the listener runs — and can call
 * {@link #setPrunedCustomizationCount} — before {@code publishEvent}
 * returns.
 */
public class SchemaImportedEvent extends ApplicationEvent {

    private final Long connectionId;
    private final List<String> removedTableRefs;
    private final List<String> removedColumnRefs;
    private int prunedCustomizationCount;

    public SchemaImportedEvent(
            Object source, Long connectionId, List<String> removedTableRefs, List<String> removedColumnRefs) {
        super(source);
        this.connectionId = connectionId;
        this.removedTableRefs = removedTableRefs;
        this.removedColumnRefs = removedColumnRefs;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public List<String> getRemovedTableRefs() {
        return removedTableRefs;
    }

    public List<String> getRemovedColumnRefs() {
        return removedColumnRefs;
    }

    public int getPrunedCustomizationCount() {
        return prunedCustomizationCount;
    }

    public void setPrunedCustomizationCount(int prunedCustomizationCount) {
        this.prunedCustomizationCount = prunedCustomizationCount;
    }
}
