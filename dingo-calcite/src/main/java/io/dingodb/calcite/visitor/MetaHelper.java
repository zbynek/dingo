/*
 * Copyright 2021 DataCanvas
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

package io.dingodb.calcite.visitor;

import io.dingodb.common.table.TableDefinition;
import io.dingodb.common.table.TableId;
import io.dingodb.exec.Services;
import io.dingodb.meta.Location;
import io.dingodb.meta.MetaService;
import lombok.Getter;
import org.apache.calcite.plan.RelOptTable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

class MetaHelper {
    @Getter
    private final MetaService metaService;
    @Getter
    private final String tableName;

    MetaHelper(@Nonnull RelOptTable table) {
        List<String> fullName = table.getQualifiedName();
        // `fullName.get(0)` is the root schema.
        metaService = Services.metaServices.get(fullName.get(1));
        tableName = fullName.stream().skip(2).collect(Collectors.joining("."));
    }

    TableDefinition getTableDefinition() {
        return metaService.getTableDefinition(tableName);
    }

    Map<String, Location> getPartLocations() {
        return metaService.getPartLocations(tableName);
    }

    TableId getTableId() {
        return new TableId(metaService.getTableKey(tableName));
    }
}