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

/** domain-entities.md ValidationRule (Functional Design Question 8). */
@Entity
@Table(name = "validation_rule")
public class ValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long columnCustomizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValidationRuleType type;

    private String pattern;

    private String minValue;

    private String maxValue;

    protected ValidationRule() {
    }

    public ValidationRule(Long columnCustomizationId, ValidationRuleType type, String pattern, String minValue, String maxValue) {
        this.columnCustomizationId = columnCustomizationId;
        this.type = type;
        this.pattern = pattern;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public Long getId() {
        return id;
    }

    public Long getColumnCustomizationId() {
        return columnCustomizationId;
    }

    public ValidationRuleType getType() {
        return type;
    }

    public String getPattern() {
        return pattern;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }
}
