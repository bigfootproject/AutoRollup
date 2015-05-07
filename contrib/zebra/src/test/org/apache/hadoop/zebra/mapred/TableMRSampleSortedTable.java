/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.zebra.mapred;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.zebra.mapred.BasicTableOutputFormat;
import org.apache.hadoop.zebra.mapred.TestBasicTableIOFormatLocalFS.InvIndex;
import org.apache.hadoop.zebra.parser.ParseException;
import org.apache.hadoop.zebra.types.SortInfo;
import org.apache.hadoop.zebra.schema.*;
import org.apache.hadoop.zebra.types.TypesUtils;
import org.apache.hadoop.zebra.types.*;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DefaultTuple;
import org.apache.pig.data.Tuple;

/**
 * This is a sample a complete MR sample code for Table. It doens't contain
 * 'read' part. But, it should be similar and easier to write. Refer to test
 * cases in the same directory.
 * 
 * Assume the input files contain rows of word and count, separated by a space:
 * 
 * <pre>
 * this 2
 * is 1
 * a 4 
 * test 2 
 * hello 1 
 * world 3
 * </pre>
 * 
 */
public class TableMRSampleSortedTable {
    static class MapClass implements
      Mapper<LongWritable, Text, BytesWritable, Tuple> {
    private BytesWritable bytesKey;
    private Tuple tupleRow;
    private Object javaObj;
    private byte[] types;
    private ColumnType[] sortColTypes;

    @Override
    public void map(LongWritable key, Text value,
        OutputCollector<BytesWritable, Tuple> output, Reporter reporter)
        throws IOException {
        // value should contain "word count"
        String[] wdct = value.toString().split(" ");
        if (wdct.length != 2) {
          // LOG the error
          return;
        }

        byte[] word = wdct[0].getBytes();

        // This key has to be created by user
        bytesKey.set(word, 0, word.length);
        
        // This tuple has to be created by user
        tupleRow.set(0, new String(word));
        tupleRow.set(1, Integer.parseInt(wdct[1]));

        output.collect(bytesKey, tupleRow);
    }

    @Override
    public void configure(JobConf job) {
      bytesKey = new BytesWritable();
      try {
          
        /* New M/R Interface to get sort information */  
        SortInfo sortInfo = BasicTableOutputFormat.getSortInfo(job);
        
        /* New M/R Interface SortInfo is exposed to user 
         * To get types of sort columns.
         * Similar interface to get names and indices
         */
           sortColTypes  = sortInfo.getSortColumnTypes();
           types = new byte[sortColTypes.length];
           for(int i =0 ; i < sortColTypes.length; ++i){
             types[i] = sortColTypes[i].pigDataType();
           }
        Schema outSchema = BasicTableOutputFormat.getSchema(job);
        tupleRow = TypesUtils.createTuple(outSchema);
                
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void close() throws IOException {
      // no-op
    }
  }

  static class ReduceClass implements
    Reducer<BytesWritable, Tuple, BytesWritable, Tuple> {
      Tuple outRow;
     

    @Override
    public void configure(JobConf job) {
    }

    @Override
    public void close() throws IOException {
    }    
    public void reduce(BytesWritable key, Iterator<Tuple> values,
      OutputCollector<BytesWritable, Tuple> output, Reporter reporter)
           throws IOException {
          try {
            for(; values.hasNext();)  {
              output.collect(key, values.next());
            }  
          } catch (ExecException e) {
            e.printStackTrace();
          }
    }
  
  }  
  
  public static void main(String[] args) throws ParseException, IOException, Exception {
    JobConf jobConf = new JobConf();
    jobConf.setJobName("tableMRSample");
    jobConf.set("table.output.tfile.compression", "gz");

    // input settings
    jobConf.setInputFormat(TextInputFormat.class);
    jobConf.setMapperClass(TableMRSampleSortedTable.MapClass.class);
    jobConf.setReducerClass(TableMRSampleSortedTable.ReduceClass.class);
    jobConf.setMapOutputKeyClass(BytesWritable.class);
    jobConf.setMapOutputValueClass(DefaultTuple.class);
    FileInputFormat.setInputPaths(jobConf, new Path(
        "/home/gauravj/work/grid/myTesting/input.txt"));
    jobConf.setNumMapTasks(1);

    // output settings
    Path outPath = new Path("/home/gauravj/work/grid/myTesting/tableOuts");
    jobConf.setOutputFormat(BasicTableOutputFormat.class);
    BasicTableOutputFormat.setOutputPath(jobConf, outPath);
    // set the logical schema with 2 columns
    BasicTableOutputFormat.setSchema(jobConf, "word:string, count:int");
    // for demo purposes, create 2 physical column groups
    BasicTableOutputFormat.setStorageHint(jobConf, "[word];[count]");
    
    /* New M/R Interface */
    /* Set sort columns in a comma separated string */
    /* Each sort column should belong to schema columns */
    BasicTableOutputFormat.setSortInfo(jobConf, "word, count");

    // set map-only job.
    jobConf.setNumReduceTasks(1);
    JobClient.runJob(jobConf);
  }
}
