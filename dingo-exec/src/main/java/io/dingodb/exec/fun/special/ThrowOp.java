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

package io.dingodb.exec.fun.special;

import io.dingodb.common.exception.DingoSqlException;
import io.dingodb.expr.core.TypeCode;
import io.dingodb.expr.parser.op.Op;
import io.dingodb.expr.runtime.EvalContext;
import io.dingodb.expr.runtime.EvalEnv;
import io.dingodb.expr.runtime.RtExpr;
import io.dingodb.expr.runtime.op.RtOp;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ThrowOp extends Op {
    public ThrowOp(String name) {
        super(name);
    }

    @Override
    protected boolean evalNull(RtExpr @NonNull [] rtExprArray) {
        return false;
    }

    @Override
    protected @NonNull RtExpr evalConst(RtExpr @NonNull [] rtExprArray, @Nullable EvalEnv env) {
        return new Runtime(rtExprArray);
    }

    @Override
    protected @NonNull RtOp createRtOp(RtExpr[] rtExprArray) {
        return new Runtime(rtExprArray);
    }

    public static final class Runtime extends RtOp {
        private static final long serialVersionUID = 2611671886580403590L;

        private Runtime(RtExpr @NonNull [] paras) {
            super(paras);
        }

        @Override
        public @Nullable Object eval(@Nullable EvalContext etx) {
            Object v = paras[0].eval(etx);
            throw new DingoSqlException(
                v != null ? v.toString() : null,
                DingoSqlException.TEST_ERROR_CODE,
                DingoSqlException.TEST_ERROR_STATE
            );
        }

        @Override
        public int typeCode() {
            return TypeCode.STRING;
        }
    }
}