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

package io.dingodb.web.utils;

import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import io.dingodb.client.common.IndexDefinition;
import io.dingodb.client.common.VectorSearchParameter;
import io.dingodb.client.common.VectorWithId;
import io.dingodb.common.partition.PartitionDefinition;
import io.dingodb.common.partition.PartitionDetailDefinition;
import io.dingodb.proxy.common.ProxyCommon;
import io.dingodb.proxy.meta.ProxyMeta;
import io.dingodb.sdk.common.index.DiskAnnParam;
import io.dingodb.sdk.common.index.FlatParam;
import io.dingodb.sdk.common.index.HnswParam;
import io.dingodb.sdk.common.index.IndexParameter;
import io.dingodb.sdk.common.index.IvfFlatParam;
import io.dingodb.sdk.common.index.IvfPqParam;
import io.dingodb.sdk.common.index.VectorIndexParameter;
import io.dingodb.sdk.common.vector.ScalarField;
import io.dingodb.sdk.common.vector.ScalarValue;
import io.dingodb.sdk.common.vector.Search;
import io.dingodb.sdk.common.vector.SearchDiskAnnParam;
import io.dingodb.sdk.common.vector.SearchFlatParam;
import io.dingodb.sdk.common.vector.SearchHnswParam;
import io.dingodb.sdk.common.vector.SearchIvfFlatParam;
import io.dingodb.sdk.common.vector.SearchIvfPqParam;
import io.dingodb.sdk.common.vector.Vector;

import java.util.Map;
import java.util.stream.Collectors;

public class Conversion {

    public static IndexDefinition mapping(ProxyMeta.IndexDefinition definition) {
        return new IndexDefinition(
            definition.getName(),
            definition.getVersion(),
            mapping(definition.getIndexPartition()),
            definition.getReplica(),
            mapping(definition.getIndexParameter()),
            definition.getWithAutoIncrement(),
            definition.getAutoIncrement()
            );
    }

    public static PartitionDefinition mapping(ProxyMeta.PartitionRule partitionRule) {
        return new PartitionDefinition(
            partitionRule.getFuncName(),
            partitionRule.getColumnsList(),
            partitionRule.getDetailsList().stream()
                .map(d -> new PartitionDetailDefinition(d.getPartName(), d.getOperator(), d.getOperandList().toArray()))
                .collect(Collectors.toList())
            );
    }

    public static IndexParameter mapping(ProxyCommon.IndexParameter parameter) {
        ProxyCommon.VectorIndexParameter vectorIndexParameter = parameter.getVectorIndexParameter();
        VectorIndexParameter.VectorIndexType vectorIndexType = VectorIndexParameter.VectorIndexType.valueOf(vectorIndexParameter.getVectorIndexType().name());
        VectorIndexParameter vectorParameter;
        switch (vectorIndexParameter.getVectorIndexType()) {
            case VECTOR_INDEX_TYPE_FLAT:
                ProxyCommon.CreateFlatParam flatParam = vectorIndexParameter.getFlatParameter();
                vectorParameter = new VectorIndexParameter(
                    vectorIndexType, new FlatParam(
                        flatParam.getDimension(),
                    VectorIndexParameter.MetricType.valueOf(flatParam.getMetricType().name()))
                );
                break;
            case VECTOR_INDEX_TYPE_IVF_FLAT:
                ProxyCommon.CreateIvfFlatParam ivfFlatParam = vectorIndexParameter.getIvfFlatParameter();
                vectorParameter = new VectorIndexParameter(
                    vectorIndexType,
                    new IvfFlatParam(
                        ivfFlatParam.getDimension(),
                        VectorIndexParameter.MetricType.valueOf(ivfFlatParam.getMetricType().name()),
                        ivfFlatParam.getNcentroids()
                    )
                );
                break;
            case VECTOR_INDEX_TYPE_IVF_PQ:
                ProxyCommon.CreateIvfPqParam pqParam = vectorIndexParameter.getIvfPqParameter();
                vectorParameter = new VectorIndexParameter(
                    vectorIndexType,
                    new IvfPqParam(
                        pqParam.getDimension(),
                        VectorIndexParameter.MetricType.valueOf(pqParam.getMetricType().name()),
                        pqParam.getNcentroids(),
                        pqParam.getNsubvector(),
                        pqParam.getBucketInitSize(),
                        pqParam.getBucketInitSize() // TODO fix
                    )
                );
                break;
            case VECTOR_INDEX_TYPE_HNSW:
                ProxyCommon.CreateHnswParam hnswParam = vectorIndexParameter.getHnswParameter();
                vectorParameter = new VectorIndexParameter(
                    vectorIndexType,
                    new HnswParam(
                        hnswParam.getDimension(),
                        VectorIndexParameter.MetricType.valueOf(hnswParam.getMetricType().name()),
                        hnswParam.getEfConstruction(),
                        hnswParam.getMaxElements(),
                        hnswParam.getNlinks()
                    )
                );
                break;
            case VECTOR_INDEX_TYPE_DISKANN:
                ProxyCommon.CreateDiskAnnParam diskAnnParam = vectorIndexParameter.getDiskannParameter();
                vectorParameter = new VectorIndexParameter(
                    vectorIndexType,
                    new DiskAnnParam(
                        diskAnnParam.getDimension(),
                        VectorIndexParameter.MetricType.valueOf(diskAnnParam.getMetricType().name())
                    )
                );
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + vectorIndexParameter.getVectorIndexType());
        }
        return new IndexParameter(
            IndexParameter.IndexType.valueOf(parameter.getIndexType().name()), vectorParameter);
    }

    public static VectorSearchParameter mapping(ProxyCommon.VectorSearchParameter parameter) {
        Search search = null;
        if (parameter.getIvfPq() != null) {
            ProxyCommon.SearchIvfPqParam ivfPq = parameter.getIvfPq();
            search = new Search(new SearchIvfPqParam(ivfPq.getNprobe(), ivfPq.getParallelOnQueries(), ivfPq.getRecallNum()));
        }
        if (parameter.getIvfFlat() != null) {
            ProxyCommon.SearchIvfFlatParam ivfFlat = parameter.getIvfFlat();
            search = new Search(new SearchIvfFlatParam(ivfFlat.getNprobe(), ivfFlat.getParallelOnQueries()));
        }
        if (parameter.getHnsw() != null) {
            search = new Search(new SearchHnswParam(parameter.getHnsw().getEfSearch()));
        }
        if (parameter.getFlat() != null) {
            search = new Search(new SearchFlatParam(parameter.getFlat().getParallelOnQueries()));
        }
        if (parameter.getDiskann() != null) {
            search = new Search(new SearchDiskAnnParam());
        }
        return new VectorSearchParameter(
            parameter.getTopN(),
            parameter.getWithOutVectorData(),
            parameter.getWithScalarData(),
            parameter.getSelectedKeysList(),
            search,
            parameter.getUseScalarFilter());
    }

    public static VectorWithId mapping(ProxyCommon.VectorWithId withId) {
        ProxyCommon.Vector vector = withId.getVector();
        return new VectorWithId(
            withId.getId(),
            new Vector(
                vector.getDimension(),
                Vector.ValueType.valueOf(vector.getValueType().name()),
                vector.getFloatValuesList(),
                vector.getBinaryValuesList().stream().map(ByteString::toByteArray).collect(Collectors.toList())),
            withId.getScalarData().getScalarDataMap().entrySet().stream().collect(
                Maps::newHashMap,
                (map, entry) -> map.put(entry.getKey(), mapping(entry.getValue())),
                Map::putAll
            ));
    }

    public static ScalarValue mapping(ProxyCommon.ScalarValue value) {
        return new ScalarValue(ScalarValue.ScalarFieldType.valueOf(value.getFieldType().name()),
            value.getFieldsList().stream()
                .map(f -> mapping(f, value.getFieldType()))
                .collect(Collectors.toList()));
    }

    public static ScalarField mapping(ProxyCommon.ScalarField field, ProxyCommon.ScalarFieldType type) {
        switch (type) {
            case BOOL:
                return new ScalarField(field.getBoolData());
            case INT8:
            case INT16:
            case INT32:
                return new ScalarField(field.getIntData());
            case INT64:
                return new ScalarField(field.getLongData());
            case FLOAT32:
                return new ScalarField(field.getFloatData());
            case DOUBLE:
                return new ScalarField(field.getDoubleData());
            case STRING:
                return new ScalarField(field.getStringData());
            case BYTES:
                return new ScalarField(field.getBytesData().toByteArray());
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    public static ProxyCommon.VectorWithId mapping(VectorWithId withId) {
        ProxyCommon.VectorWithId.Builder builder = ProxyCommon.VectorWithId.newBuilder()
            .setId(withId.getId());
        if (withId.getVector() != null) {
            builder.setVector(mapping(withId.getVector()));
        }
        if (withId.getScalarData() != null) {
            builder.setScalarData(mapping(withId.getScalarData()));
        }
        return builder.build();
    }

    public static ProxyCommon.VectorScalarData mapping(Map<String, ScalarValue> scalarData) {
        return ProxyCommon.VectorScalarData.newBuilder().putAllScalarData(scalarData.entrySet().stream()
                .collect(
                    Maps::newHashMap,
                    (map, entry) -> map.put(entry.getKey(), mapping(entry.getValue())),
                    Map::putAll))
            .build();
    }

    public static ProxyCommon.Vector mapping(Vector vector) {
        return ProxyCommon.Vector.newBuilder()
            .setDimension(vector.getDimension())
            .setValueType(ProxyCommon.ValueType.valueOf(vector.getValueType().name()))
            .addAllFloatValues(vector.getFloatValues())
            .addAllBinaryValues(vector.getBinaryValues()
                .stream()
                .map(ByteString::copyFrom)
                .collect(Collectors.toList()))
            .build();
    }

    public static ProxyCommon.ScalarValue mapping(ScalarValue value) {
        return ProxyCommon.ScalarValue.newBuilder()
            .setFieldType(ProxyCommon.ScalarFieldType.valueOf(value.getFieldType().name()))
            .addAllFields(value.getFields().stream().map(f -> mapping(f, value.getFieldType()))
                .collect(Collectors.toList()))
            .build();
    }

    public static ProxyCommon.ScalarField mapping(ScalarField field, ScalarValue.ScalarFieldType type) {
        switch (type) {
            case BOOL:
                return ProxyCommon.ScalarField.newBuilder().setBoolData((Boolean) field.getData()).build();
            case INTEGER:
                return ProxyCommon.ScalarField.newBuilder().setIntData((Integer) field.getData()).build();
            case LONG:
                return ProxyCommon.ScalarField.newBuilder().setLongData((Long) field.getData()).build();
            case FLOAT:
                return ProxyCommon.ScalarField.newBuilder().setFloatData((Float) field.getData()).build();
            case DOUBLE:
                return ProxyCommon.ScalarField.newBuilder().setDoubleData((Double) field.getData()).build();
            case STRING:
                return ProxyCommon.ScalarField.newBuilder().setStringData(field.getData().toString()).build();
            case BYTES:
                return ProxyCommon.ScalarField.newBuilder().setBytesData(ByteString.copyFromUtf8(field.getData().toString())).build();
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}
