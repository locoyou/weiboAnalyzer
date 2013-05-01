package myApp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import weibo4j.Comments;
import weibo4j.Timeline;
import weibo4j.model.Comment;
import weibo4j.model.Paging;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;

/**
 * 抓取微博并分析对华关系
 * @author locoyou
 *
 */
public class MyWeibo {
	
	String access_token;
	boolean byID;
	Date startDate;
	Date endDate;
	Timeline timeline;
	int repostNum;
	int commentNum;
	Analyzer analyzer;
	
	public static void main(String[] args) {
		MyWeibo myWeibo = new MyWeibo();
		myWeibo.start();
		myWeibo.crawlAndAnalyse();
	}
	
	MyWeibo() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("access_token.txt")));
			String line = br.readLine();
			access_token = line;
			br.close();
			timeline = new Timeline();
			timeline.client.setToken(access_token);
			analyzer = new Analyzer();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 初始化参数
	 */
	public void start() {
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line;
			
			//程序相关信息
			System.out.println("本程序是为王晓珊同学开发，其他人使用需获得王晓珊或开发者授权");
			System.out.println("发现Bug或token过期请联系开发者柳春洋 liuchunyang2012@gmail.com");
			System.out.println("------------------------------------------");
			
			//输入类型
			System.out.print("输入文件为UserInfo.txt，使用用户ID/用户Name作为输入(I/N):");
			line = br.readLine();
			if(line.equalsIgnoreCase("I"))
				byID = true;
			else 
				byID = false;
			
			//解析起始截止日期
			System.out.print("输入起始日期(yyyy/mm/dd)：");
			line = br.readLine();
			String[] nums = line.split("/");
			startDate = new Date(Integer.valueOf(nums[0])-1900, Integer.valueOf(nums[1])-1, Integer.valueOf(nums[2]));
			
			System.out.print("输入截止日期(yyyy/mm/dd)：");
			line = br.readLine();
			nums = line.split("/");
			endDate = new Date(Integer.valueOf(nums[0])-1900, Integer.valueOf(nums[1])-1, Integer.valueOf(nums[2]), 23, 59);
			
			//设定转发、评论下限
			System.out.print("输入转发、评论数下限，以空格隔开：");
			line = br.readLine();
			nums = line.split(" ");
			repostNum = Integer.valueOf(nums[0]);
			commentNum = Integer.valueOf(nums[1]);
			
			//程序运行信息
			System.out.println("起始日期:"+startDate);
			System.out.println("截止日期:"+endDate);
			System.out.println("转发、评论数:"+repostNum+commentNum);
			System.out.println("是否使用Uid:"+byID);
			System.out.println("token:"+access_token);
			System.out.println("程序开始运行，输出结果放置在output文件夹中");
			System.out.println("由于新浪微博API限制，程序设置了运行延时，与程序运行速度无关，请耐心等待");
			System.out.println("------------------------------------------");
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 延时10秒左右，以防止频繁发送请求超出新浪微博API限制
	 * @throws Exception
	 */
	public void delay() throws Exception{
		System.out.println("delay");
		Thread.sleep(25000);
	}
	
	/**
	 * 抓取并调用Analyzer分析指定微博
	 */
	public void crawlAndAnalyse() {
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("UserInfo.txt")));
			String userInfo;
			PrintWriter bw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("output/AllWeibo.csv")));
			bw.println("微博,用户,加V,@他人,包含链接");
			//读取每个用户信息并抓取指定微博
			while((userInfo = br.readLine())!= null) {
				System.out.println(userInfo+" "+userInfo.length());
				ArrayList<Status> statusList = getStatusList(userInfo);
				//遍历合格微博，获取评论、转发并解析
				for(Status status:statusList) {
					ArrayList<Status> repostStatus = new ArrayList<Status>();
					ArrayList<Comment> comments = new ArrayList<Comment>();
					Comments cs = new Comments();
					cs.client.setToken(access_token);
					delay();
					comments = (ArrayList<Comment>)cs.getCommentById(status.getId(), new Paging(1, 200), 0).getComments();
					delay();
					repostStatus = (ArrayList<Status>)timeline.getRepostTimeline(status.getId(), new Paging(1,200)).getStatuses();
					analyzer.analyse(status, repostStatus, comments);
					boolean atOthers,con;
					if(analyzer.extractUser(status.getText()).size() > 0)
						atOthers = true;
					else
						atOthers = false;
					if(status.getText().contains("http://t.cn"))
						con = true;
					else
						con = false;
					bw.println(status.getText().replaceAll(","," ")+","+status.getUser().getName()+","+status.getUser().isVerified()+","+atOthers+","+con);
					bw.flush();
				}
				
			}
			br.close();
			bw.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 筛选出指定用户符合条件（起始终止日期、转发评论数量）的微博
	 * @param userInfo
	 * @return
	 */
	public ArrayList<Status> getStatusList(String userInfo) {
		ArrayList<Status> statusList = new ArrayList<Status>();
		try{
			StatusWapper statusWapper;
			for(int pageNo = 1; pageNo < 5;pageNo++) {
				delay();
				if(byID) {
					statusWapper = timeline.getUserTimelineByUid(userInfo, new Paging(pageNo, 200), 0, 1);
				}	
				else {
					statusWapper = timeline.getUserTimelineByName(userInfo, new Paging(pageNo, 200), 0, 1);
				}
				//判断是否符合条件。时间早于开始时间则结束当前用户微博抓取
				boolean early = false;
				for(Status status:statusWapper.getStatuses()) {
					
					Date createdAt = status.getCreatedAt();
					System.out.println("created:"+createdAt);
					System.out.println(status.getCommentsCount() + " " + status.getRepostsCount());
					if(createdAt.before(startDate)) {
						System.out.println("early");
						early = true;
						break;
					}
					else if(createdAt.after(endDate)) {
						continue;
					}
					else if((status.getCommentsCount() >= commentNum) && (status.getRepostsCount() >= repostNum) && (status.getCommentsCount() <= 200) && (status.getCommentsCount() <= 200)) {
						statusList.add(status);
						if(statusList.size() >= 30)
							break;
					}
				}
				if(statusList.size() >= 30)
					break;
				if(early) break;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println(statusList.size());
		return statusList;
	}
}