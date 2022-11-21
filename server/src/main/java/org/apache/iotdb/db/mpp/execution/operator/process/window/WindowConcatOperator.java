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

package org.apache.iotdb.db.mpp.execution.operator.process.window;

import org.apache.iotdb.db.mpp.aggregation.timerangeiterator.ITimeRangeIterator;
import org.apache.iotdb.db.mpp.execution.operator.Operator;
import org.apache.iotdb.db.mpp.execution.operator.OperatorContext;
import org.apache.iotdb.db.mpp.execution.operator.process.ProcessOperator;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.TimeRange;
import org.apache.iotdb.tsfile.read.common.block.TsBlock;

import java.util.List;

public class WindowConcatOperator implements ProcessOperator {

  protected final OperatorContext operatorContext;

  protected final Operator child;

  private final ITimeRangeIterator sampleTimeRangeIterator;
  private TimeRange curTimeRange;

  private final WindowSliceQueue windowSliceQueue;

  public WindowConcatOperator(
      OperatorContext operatorContext,
      Operator child,
      ITimeRangeIterator sampleTimeRangeIterator,
      List<TSDataType> outputDataTypes) {
    this.operatorContext = operatorContext;
    this.child = child;
    this.sampleTimeRangeIterator = sampleTimeRangeIterator;
    this.windowSliceQueue = new WindowSliceQueue(outputDataTypes);
  }

  @Override
  public OperatorContext getOperatorContext() {
    return operatorContext;
  }

  @Override
  public TsBlock next() {
    if (!child.hasNext()) {
      curTimeRange = null;
      return windowSliceQueue.outputWindow();
    }

    TsBlock inputTsBlock = child.next();
    if (inputTsBlock == null || inputTsBlock.isEmpty()) {
      return null;
    }

    if (curTimeRange == null && sampleTimeRangeIterator.hasNextTimeRange()) {
      curTimeRange = sampleTimeRangeIterator.nextTimeRange();
      windowSliceQueue.updateTimeRange(curTimeRange);
    }

    if (inputTsBlock.getStartTime() > curTimeRange.getMax()) {
      TsBlock outputWindow = windowSliceQueue.outputWindow();
      if (sampleTimeRangeIterator.hasNextTimeRange()) {
        curTimeRange = sampleTimeRangeIterator.nextTimeRange();
        windowSliceQueue.updateTimeRange(curTimeRange);
      } else {
        curTimeRange = null;
      }
      windowSliceQueue.processTsBlock(inputTsBlock);
      return outputWindow;
    } else {
      windowSliceQueue.processTsBlock(inputTsBlock);
      return null;
    }
  }

  @Override
  public boolean hasNext() {
    return (!windowSliceQueue.isEmpty() || child.hasNext())
        && (curTimeRange != null || sampleTimeRangeIterator.hasNextTimeRange());
  }

  @Override
  public boolean isFinished() {
    return !this.hasNext();
  }

  @Override
  public long calculateMaxPeekMemory() {
    return child.calculateMaxPeekMemory();
  }

  @Override
  public long calculateMaxReturnSize() {
    return child.calculateMaxReturnSize();
  }

  @Override
  public long calculateRetainedSizeAfterCallingNext() {
    return child.calculateRetainedSizeAfterCallingNext();
  }
}