package myApp;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import weibo4j.model.Comment;
import weibo4j.model.Status;
import weibo4j.model.User;

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
			matrix(status.getUser(), repostStatus, comments);
			count++;
		}
		catch(Exception e) {
			
		}
	}
	
	/**
	 * 完成对话矩阵
	 * @param user
	 * @param repostStatus
	 * @param comments
	 */
	public void matrix(User user, ArrayList<Status> repostStatus, ArrayList<Comment> comments) {
		try{
			String outputFileName = "output/matrix"+count+".csv";
			PrintWriter bw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)));
			
			//遍历转发和评论，抽取对话关系放入dialog中
			String userName = user.getName();
			HashSet<String> allUser = new HashSet<String>();
			allUser.add(userName);
			HashMap<String, HashSet<String>> dialog = new HashMap<String, HashSet<String>>();
			for(Status status:repostStatus) {
				String text = status.getText();
				String repostUser = status.getUser().getName();
				allUser.add(repostUser);
				HashSet<String> beContacted;
				if(dialog.containsKey(repostUser)) {
					beContacted = dialog.get(repostUser);
				}
				else {
					beContacted = new HashSet<String>();
				}
				if(text.indexOf("@") == -1) {
					beContacted.add(userName);
				}
				else {
					String[] texts = text.split("//");
					ArrayList<String> atUsers = extractUser(texts[0]);
					beContacted.addAll(atUsers);
					if(texts[0].indexOf("回复") == -1)
					{
						if(texts.length == 1) {
							beContacted.add(userName);
						}
						else if(texts.length > 1) {
							String str;
							if((str = extractRep(texts[1])) != null)
								beContacted.add(str);
						}
					}
				}
				allUser.addAll(beContacted);
				dialog.put(repostUser, beContacted);
				
			}
			
			for(Comment comment:comments) {
				String text = comment.getText();
				String commentUser = comment.getUser().getName();
				//System.out.println("com:"+commentUser);
				allUser.add(commentUser);
				HashSet<String> beContacted;
				if(dialog.containsKey(commentUser)) {
					beContacted = dialog.get(commentUser);
				}
				else {
					beContacted = new HashSet<String>();
				}
				if(text.indexOf("@") == -1) {
					beContacted.add(userName);
				}
				else {
					String[] texts = text.split("//");
					ArrayList<String> atUsers = extractUser(texts[0]);
					//System.out.println(atUsers.get(0) + " " + atUsers.size());
					beContacted.addAll(atUsers);
					if(texts[0].indexOf("回复") == -1)
					{
						if(texts.length == 1) {
							beContacted.add(userName);
						}
						else if(texts.length > 1) {
							String str;
							if((str = extractRep(texts[1])) != null)
								beContacted.add(str);
						}
					}
				}
				allUser.addAll(beContacted);
				//System.out.println(commentUser+" "+beContacted.toArray());
				dialog.put(commentUser, beContacted);
			}
			
			//输出对话矩阵
			//System.out.println(dialog.size());
			ArrayList<String> names = new ArrayList<String>();
			bw.print("姓名");
			for(String name:allUser) {
				names.add(name);
				bw.print(","+name);
			}
			bw.println();
			bw.flush();
			for(String name:names) {
				bw.print(name);
				if(dialog.containsKey(name)) {
					HashSet<String> set = dialog.get(name);
					for(int i = 0; i < names.size(); i++) {
						if(set.contains(names.get(i)))
							bw.print(",1");
						else
							bw.print(",0");
					}
				}
				else {
					for(int i = 0; i < names.size(); i++) {
						bw.print(",0");
					}
				}
				bw.println();
				bw.flush();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String extractRep(String text) {
		text = text + " ";
		Pattern pattern = Pattern.compile("@(.*?):");
		Matcher match = pattern.matcher(text);
		while(match.find()) {
			return match.group(1);
		}
		return null;
	}
	
	public ArrayList<String> extractUser(String text) {
		text = text + " ";
		ArrayList<String> users = new ArrayList<String>();
		Pattern pattern = Pattern.compile("@(.*?)[ :，,.。！!]");
		Matcher match = pattern.matcher(text);
		while(match.find()) {
			//System.out.println(match.group(1));
			users.add(match.group(1));
		}
		return users;
	}
	
	public static void main(String[] args) {
		//System.out.println("//@西安平价艺术节:转发微博".split("//")[1]);
		Analyzer a = new Analyzer();
		a.extractUser("@一种香气 ");
	}
}