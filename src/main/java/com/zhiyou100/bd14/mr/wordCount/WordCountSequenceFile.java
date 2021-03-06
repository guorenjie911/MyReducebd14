package com.zhiyou100.bd14.mr.wordCount;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;


public class WordCountSequenceFile {
	
	
	// 定义map(map输入类型key value, map输出类型key value)
	public static class WordCountSequenceFileMap extends Mapper<LongWritable, Text, Text, IntWritable> {
		private String[] infos;
		private Text oKey = new Text();
		private final IntWritable oValue = new IntWritable(1);

		// 读取文件
		@Override
		protected void map(
					LongWritable key,
					//读文件
					Text value,
					Mapper<LongWritable, Text, Text, IntWritable>.Context context)
					throws IOException, InterruptedException {
				//解析一行数据, 转换成一个单词组成的数据
				infos = value.toString().split("\\s");
				
				for (String i : infos) {
					//把单词形成的一个kv对发送给reducer(单词,1)
					oKey.set(i);
					//向reduce中输入每一行的一个单词, 和null
					context.write(oKey, oValue);
					
				}
			}
		}
	
		
		//定义reducer
		public static class WordCountSequenceFileReducer extends Reducer< Text, IntWritable, Text, IntWritable>{
			private int sum;
			private IntWritable oValue = new IntWritable(0);
			
			private int keyValuesNumber = 0;
			
			@Override
			protected void reduce(
					Text key, 
					Iterable<IntWritable> values,
					Reducer<Text, IntWritable, Text, IntWritable>.Context content
					) throws IOException, InterruptedException {
				sum = 0;
				keyValuesNumber = 0;
				for(IntWritable value : values){
					keyValuesNumber ++;
					sum += value.get();
				}
				
				//数据倾斜
				System.out.println("key:\t"+key+"\t,kv对数量:\t"+keyValuesNumber);
				
				//输出kv
				oValue.set(sum);
				content.write(key, oValue);
				
			}
		}
		
		
		public static void main(String[] args) throws Exception{
			
			//Conguration, 从classpath读取配置文件core-site.xml
			Configuration configuration = new Configuration();
			
			//创建一个新的工作
			Job job = Job.getInstance(configuration);
			job.setJarByClass(WordCountSequenceFile.class);
			job.setJobName("第一个mr作业: wordcount");
			
			//向job中添加, map
			job.setMapperClass(WordCountSequenceFileMap.class);
			//向job中添加reduce
			job.setReducerClass(WordCountSequenceFileReducer.class);
			
			//向job中指定job的Map的output key和value 类型, 如果和最终输出的kv对, 类型不同时
			//需要特殊指定
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(IntWritable.class);
			
			
			//设置输出格式
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
			
			
			
			//设置Path
			//from data
			Path inputPath = new Path("/README.txt");
			//向map-reduce job中添加inputPath
			FileInputFormat.addInputPath(job, inputPath);
			
			
			Path outputPath = new Path("/user/output/WordCountSequernceFile");
			//通过Path可以, 得到hdfs文件管理系统, 进行递归删除, 先进行删除
			outputPath.getFileSystem(configuration).delete(outputPath,true);
			//向map-reduce job中添加outputPath
			FileOutputFormat.setOutputPath(job, outputPath);
			
			
			//begin
			boolean result = job.waitForCompletion(true);
			System.exit(result ? 0 : 1);
		}
		
}
