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

package cherry.mastermeister5.connectionschema.repository;

import cherry.mastermeister5.connectionschema.entity.DbTable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbTableJpaRepository extends JpaRepository<DbTable, Long> {

    List<DbTable> findAllBySchemaId(Long schemaId);

    /** Unit 4's AccessControlServiceImpl: primary-key lookup for record create/delete rules. */
    Optional<DbTable> findBySchemaIdAndTableName(Long schemaId, String tableName);

    void deleteAllBySchemaId(Long schemaId);
}
