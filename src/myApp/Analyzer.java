package myApp;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import weibo4j.model.Comment;
import weibo4j.model.Status;

/**
 * 分析微博转发和评论中的对话关系
 * @author locoyou
 *
 */
public class Analyzer {
	int count;
	
	public Analyzer() {
		count = 1;
	}
	
	/**
	 * 将抓取的微博信息写入文件并分析对话矩阵
	 * @param status
	 * @param repostStatus
	 * @param comments
	 */
	public void analyse(Status status, ArrayList<Status> repostStatus, ArrayList<Comment> comments) {
		try{
			String outputFileName = "output/status"+count+".txt";
			PrintWriter bw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)));
			bw.println("status|||"+status.getUser().getName()+"|||"+status.getText());
			for(Status rep:repostStatus) {
				bw.println("repost|||"+rep.getUser().getName()+"|||"+rep.getText());
			}
			for(Comment com:comments) {
				bw.println("comment|||"+com.getUser().getName()+"|||"+com.getText());
			}
			bw.close();
			matrix(outputFileName);
		}
		catch(Exception e) {
			
		}
	}
	
	/**
	 * 读取指定文件，完成对话矩阵
	 * @param fileName
	 */
	public void matrix(String fileName) {
		
	}
}