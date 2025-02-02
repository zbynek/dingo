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

package org.apache.calcite.sql2rel;

import io.dingodb.calcite.DingoParserContext;
import io.dingodb.calcite.DingoTable;
import io.dingodb.calcite.grammar.SqlUserDefinedOperators;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.SqlTableFunction;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.InferTypes;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

public class SqlFunctionScanOperator extends SqlFunction implements SqlTableFunction {

    public static void register(DingoParserContext context) {
        StandardConvertletTable.INSTANCE.registerOp(SqlUserDefinedOperators.SCAN,
            (cx, call) -> {
                final TranslatableTable table = ((SqlFunctionScanOperator) call.getOperator()).getTable(context, call.operand(0).toString());
                DingoTable dingoTable = (DingoTable) table;
                final RelDataType rowType = dingoTable.getRowType(cx.getTypeFactory());
                RexBuilder rexBuilder = new RexBuilder(cx.getTypeFactory());
                return rexBuilder.makeCall(rowType, call.getOperator(), Arrays.asList(
                    rexBuilder.makeLiteral(call.operand(0).toString())));
            });
    }

    /**
     * Creates a SqlVectorOperator.
     *
     * @param name        Operator name
     */
    public SqlFunctionScanOperator(String name, SqlKind kind) {
        super(
            name,
            kind,
            ReturnTypes.ARG0,
            InferTypes.VARCHAR_1024,
            OperandTypes.CURSOR,
            SqlFunctionCategory.USER_DEFINED_TABLE_FUNCTION
        );
    }

    @Override public SqlReturnTypeInference getRowTypeInference() {
        return this::inferRowType;
    }

    private RelDataType inferRowType(SqlOperatorBinding callBinding) {
        final RelDataTypeFactory typeFactory = callBinding.getTypeFactory();
        final TranslatableTable table = getTable(null, null);
        return table.getRowType(typeFactory);
    }

    private TranslatableTable getTable(DingoParserContext context, String tableName) {
        return  (TranslatableTable) context.getDefaultSchema().getTable(tableName, false).getTable();
    }

    @Override
    public SqlCall createCall(@Nullable SqlLiteral functionQualifier, SqlParserPos pos, @Nullable SqlNode... operands) {
        return super.createCall(null, pos, super.createCall(functionQualifier, pos, operands));
    }

}
