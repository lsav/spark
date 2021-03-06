/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.execution.datasources.v2.json

import org.apache.hadoop.mapreduce.{Job, TaskAttemptContext}

import org.apache.spark.sql.catalyst.json.JSONOptions
import org.apache.spark.sql.catalyst.util.CompressionCodecs
import org.apache.spark.sql.execution.datasources.{CodecStreams, OutputWriter, OutputWriterFactory}
import org.apache.spark.sql.execution.datasources.json.JsonOutputWriter
import org.apache.spark.sql.execution.datasources.v2.FileWriteBuilder
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types._
import org.apache.spark.sql.util.CaseInsensitiveStringMap

class JsonWriteBuilder(
    options: CaseInsensitiveStringMap,
    paths: Seq[String],
    formatName: String,
    supportsDataType: DataType => Boolean)
  extends FileWriteBuilder(options, paths, formatName, supportsDataType) {
  override def prepareWrite(
      sqlConf: SQLConf,
      job: Job,
      options: Map[String, String],
      dataSchema: StructType): OutputWriterFactory = {
    val conf = job.getConfiguration
    val parsedOptions = new JSONOptions(
      options,
      sqlConf.sessionLocalTimeZone,
      sqlConf.columnNameOfCorruptRecord)
    parsedOptions.compressionCodec.foreach { codec =>
      CompressionCodecs.setCodecConfiguration(conf, codec)
    }

    new OutputWriterFactory {
      override def newInstance(
          path: String,
          dataSchema: StructType,
          context: TaskAttemptContext): OutputWriter = {
        new JsonOutputWriter(path, parsedOptions, dataSchema, context)
      }

      override def getFileExtension(context: TaskAttemptContext): String = {
        ".json" + CodecStreams.getCompressionExtension(context)
      }
    }
  }
}
