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

package io.dingodb.client.common;

import io.dingodb.common.partition.PartitionDefinition;
import io.dingodb.common.type.converter.DataConverter;
import io.dingodb.common.type.scalar.LongType;
import io.dingodb.common.util.Optional;
import io.dingodb.sdk.common.index.Index;
import io.dingodb.sdk.common.index.IndexParameter;
import io.dingodb.sdk.common.partition.Partition;
import io.dingodb.sdk.common.partition.PartitionDetailDefinition;
import io.dingodb.sdk.common.partition.PartitionRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexDefinition implements Index {

    private static final LongType LONG_TYPE = new LongType(false);

    private String name;
    private Integer version;
    private PartitionDefinition indexPartition;
    private Integer replica;
    private IndexParameter indexParameter;
    private Boolean isAutoIncrement;
    private Long autoIncrement;

    public Partition getIndexPartition() {
        return new PartitionRule(
            indexPartition.getFuncName(),
            indexPartition.getCols(),
            Optional.mapOrGet(indexPartition.getDetails(), __ -> indexPartition.getDetails().stream().map(d ->
                    new PartitionDetailDefinition(
                        d.getPartName(),
                        d.getOperator(),
                        Arrays.stream(d.getOperand()).map(o -> LONG_TYPE.convertFrom(o, DataConverter.DEFAULT)).toArray()))
                .collect(Collectors.toList()), ArrayList::new)
        );
    }
}
