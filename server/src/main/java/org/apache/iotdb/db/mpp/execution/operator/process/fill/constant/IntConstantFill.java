/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.mpp.execution.operator.process.fill.constant;

import org.apache.iotdb.db.mpp.execution.operator.process.fill.IFill;
import org.apache.iotdb.tsfile.read.common.block.column.Column;
import org.apache.iotdb.tsfile.read.common.block.column.IntColumn;
import org.apache.iotdb.tsfile.read.common.block.column.RunLengthEncodedColumn;

import java.util.Optional;

public class IntConstantFill implements IFill {

  // fill value
  private final int value;
  // used for constructing RunLengthEncodedColumn
  private final int[] valueArray;

  public IntConstantFill(int value) {
    this.value = value;
    this.valueArray = new int[] {value};
  }

  @Override
  public Column fill(Column valueColumn) {
    int size = valueColumn.getPositionCount();
    // if this valueColumn doesn't have any null value, or it's empty, just return itself;
    if (!valueColumn.mayHaveNull() || size == 0) {
      return valueColumn;
    }
    // if its values are all null
    if (valueColumn instanceof RunLengthEncodedColumn) {
      return new RunLengthEncodedColumn(new IntColumn(1, Optional.empty(), valueArray), size);
    } else {
      int[] array = new int[size];
      for (int i = 0; i < size; i++) {
        if (valueColumn.isNull(i)) {
          array[i] = value;
        } else {
          array[i] = valueColumn.getInt(i);
        }
      }
      return new IntColumn(size, Optional.empty(), array);
    }
  }
}