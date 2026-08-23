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

package cherry.mastermeister5.connectionschema.controller.dto;

import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** {@code password}: blank/omitted keeps the current password unchanged. */
public record UpdateConnectionRequest(
        @NotBlank String name,
        @NotNull RdbmsType rdbmsType,
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        @NotBlank String databaseName,
        String schemaNameHint,
        /** Appended verbatim to the JDBC URL; printable-ASCII allowlist only (no control chars/quotes). */
        @Pattern(regexp = "^[A-Za-z0-9?&=;:./%+,_-]*$") String extraParams,
        @NotBlank String username,
        String password) {
}
