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
			bw.println("<status>");
			bw.println(" <user>"+status.getUser().getName()+"</user>");
			bw.println(" <time>"+status.getCreatedAt()+"</time>");
			bw.println(" <text>"+status.getText()+"</text>");
			bw.println("</status>");
			for(Status rep:repostStatus) {
				bw.println("<repost>");
				bw.println(" <user>"+rep.getUser().getName()+"</user>");
				bw.println(" <time>"+rep.getCreatedAt()+"</time>");
				bw.println(" <text>"+rep.getText()+"</text>");
				bw.println("</repost>");
			}
			for(Comment com:comments) {
				bw.println("<comment>");
				bw.println(" <user>"+com.getUser().getName()+"</user>");
				bw.println(" <time>"+com.getCreatedAt()+"</time>");
				bw.println(" <text>"+com.getText()+"</text>");
				bw.println("</comment>");
			}
			bw.close();
			matrix(status, repostStatus, comments);
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
	public void matrix(Status original, ArrayList<Status> repostStatus, ArrayList<Comment> comments) {
		try{
			String outputFileName = "output/matrix"+count+".csv";
			PrintWriter bw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)));
			outputFileName = "output/user"+count+".csv";
			PrintWriter bw2 = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)));
			//DONE 获取博主、转发、评论用户的信息
			//遍历转发和评论，抽取对话关系放入dialog中
			User user = original.getUser();
			String userName = user.getName();
			HashSet<String> allUser = new HashSet<String>();
			allUser.add(userName);
			HashMap<String, HashSet<String>> dialog = new HashMap<String, HashSet<String>>();
			HashMap<String, HashSet<String>> beDialog = new HashMap<String, HashSet<String>>();
			HashMap<String, User> userList = new HashMap<String, User>();
			
			
			userList.put(userName, user);
			String otext = original.getText();
			HashSet<String> ocontacted = new HashSet<String>();
			int allNum = 0;
			if(otext.indexOf("@") != -1) {
				ArrayList<String> atUsers = extractUser(otext);
				ocontacted.addAll(atUsers);		
			}

			dialog.put(userName, ocontacted);
			
			for(Status status:repostStatus) {
				String text = status.getText();
				String repostUser = status.getUser().getName();
				if(!userList.containsKey(repostUser)) {
					userList.put(repostUser, status.getUser());
				}
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
				for(String be:beContacted) {
					if(beDialog.containsKey(be)) {
						beDialog.get(be).add(repostUser);
					}
					else {
						HashSet<String> set = new HashSet<String>();
						set.add(repostUser);
						beDialog.put(be, set);
					}
				}
				
			}
			
			for(Comment comment:comments) {
				String text = comment.getText();
				String commentUser = comment.getUser().getName();
				if(!userList.containsKey(commentUser)) {
					userList.put(commentUser, comment.getUser());
				}
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

				for(String be:beContacted) {
					if(beDialog.containsKey(be)) {
						beDialog.get(be).add(commentUser);
					}
					else {
						HashSet<String> set = new HashSet<String>();
						set.add(commentUser);
						beDialog.put(be, set);
					}
				}
			}
			
			//输出对话矩阵和用户信息
			//System.out.println(dialog.size());
			ArrayList<String> names = new ArrayList<String>();
			bw.print("姓名");
			for(String name:allUser) {
				names.add(name);
				bw.print(","+name);
			}
			bw.println();
			bw.flush();
			bw2.println("姓名,ID,加V,粉丝数,关注数,微博数,等级特征,被联系,联系他人");
			for(String name:names) {
				bw.print(name);
				if(dialog.containsKey(name)) {
					HashSet<String> set = dialog.get(name);
					for(int i = 0; i < names.size(); i++) {
						if(set.contains(names.get(i))) {
							bw.print(",1");
							allNum++;
						}
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
				
				
				bw2.print(name);
				int a,b;
				if(beDialog.containsKey(name)) {
					a = beDialog.get(name).size();
				}
				else {
					a = 0;
				}
				if(dialog.containsKey(name)) {
					b = dialog.get(name).size();
				}
				else {
					b = 0;
				}

				if(userList.containsKey(name)) {
					User theUser = userList.get(name);
					
					
					bw2.print(","+theUser.getId()+","+theUser.isVerified()+","+theUser.getFollowersCount()+","+theUser.getFriendsCount()+","+theUser.getStatusesCount()+","+theUser.getWeihao()+","+a+","+b);
				}
				else {
					bw2.print(",-,-,-,-,-,-,"+a+","+b);
				}
				bw2.println();
				bw2.flush();
			}
			bw.println("密度1,"+(double)allNum/(names.size()*(names.size()-1)));
			bw.println("密度2,"+(double)(allNum-dialog.get(userName).size()-beDialog.get(userName).size())/((names.size()-1)*(names.size()-2)));
			bw.println("中心度,"+(double)beDialog.get(userName).size()/(names.size()-1));
			bw.flush();
			bw.close();
			bw2.flush();
			bw2.close();
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
		System.out.println("</status>");
		//Analyzer a = new Analyzer();
		//a.extractUser("@一种香气 ");
	}
}