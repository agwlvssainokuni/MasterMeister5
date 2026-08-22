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

package cherry.mastermeister5.mastermaintenance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * domain-entities.md ColumnCustomization. {@code hidden}/{@code readOnly}
 * never override Unit 4's access control (business-rules.md BR-14) — that
 * merge happens in {@code MasterMaintenanceServiceImpl}, not here.
 */
@Entity
@Table(name = "column_customization")
public class ColumnCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tableCustomizationId;

    @Column(nullable = false)
    private String columnName;

    private String displayLabel;

    private Integer displayOrder;

    @Column(nullable = false)
    private boolean hidden;

    @Column(nullable = false)
    private boolean readOnly;

    @Enumerated(EnumType.STRING)
    private InputWidget inputWidget;

    /** JSON-serialized {@code [{value,label}, ...]}, only meaningful when inputWidget=SELECT. */
    @Column(length = 2000)
    private String selectOptionsJson;

    protected ColumnCustomization() {
    }

    public ColumnCustomization(Long tableCustomizationId, String columnName) {
        this.tableCustomizationId = tableCustomizationId;
        this.columnName = columnName;
        this.hidden = false;
        this.readOnly = false;
    }

    public Long getId() {
        return id;
    }

    public Long getTableCustomizationId() {
        return tableCustomizationId;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public InputWidget getInputWidget() {
        return inputWidget;
    }

    public String getSelectOptionsJson() {
        return selectOptionsJson;
    }

    public void applySettings(
            String displayLabel,
            Integer displayOrder,
            boolean hidden,
            boolean readOnly,
            InputWidget inputWidget,
            String selectOptionsJson) {
        this.displayLabel = displayLabel;
        this.displayOrder = displayOrder;
        this.hidden = hidden;
        this.readOnly = readOnly;
        this.inputWidget = inputWidget;
        this.selectOptionsJson = selectOptionsJson;
    }
}
