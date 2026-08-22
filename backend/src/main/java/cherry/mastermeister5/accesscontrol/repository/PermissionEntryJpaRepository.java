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

package cherry.mastermeister5.accesscontrol.repository;

import cherry.mastermeister5.accesscontrol.entity.PermissionEntry;
import cherry.mastermeister5.accesscontrol.entity.ResourceLevel;
import cherry.mastermeister5.accesscontrol.entity.SubjectType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionEntryJpaRepository extends JpaRepository<PermissionEntry, Long> {

    /**
     * business-rules.md BR-7 upsert lookup. Spring Data JPA's query
     * derivation binds a null tableName/columnName as {@code IS NULL}, so
     * this also matches SCHEMA/TABLE-level rows correctly.
     */
    Optional<PermissionEntry> findBySubjectTypeAndSubjectIdAndConnectionIdAndResourceLevelAndSchemaNameAndTableNameAndColumnName(
            SubjectType subjectType,
            Long subjectId,
            Long connectionId,
            ResourceLevel resourceLevel,
            String schemaName,
            String tableName,
            String columnName);

    /** business-logic-model.md Section 3: batched fetch for effective-permission resolution. */
    @Query(
            "SELECT p FROM PermissionEntry p WHERE p.connectionId = :connectionId AND p.schemaName = :schemaName "
                    + "AND ((p.subjectType = cherry.mastermeister5.accesscontrol.entity.SubjectType.USER "
                    + "AND p.subjectId = :userId) "
                    + "OR (p.subjectType = cherry.mastermeister5.accesscontrol.entity.SubjectType.GROUP "
                    + "AND p.subjectId IN :groupIds))")
    List<PermissionEntry> findForResolution(
            @Param("userId") Long userId,
            @Param("groupIds") List<Long> groupIds,
            @Param("connectionId") Long connectionId,
            @Param("schemaName") String schemaName);

    List<PermissionEntry> findAllByConnectionIdAndSubjectTypeAndSubjectId(
            Long connectionId, SubjectType subjectType, Long subjectId);

    List<PermissionEntry> findAllByConnectionId(Long connectionId);

    void deleteAllByConnectionId(Long connectionId);

    void deleteAllBySubjectTypeAndSubjectId(SubjectType subjectType, Long subjectId);
}
