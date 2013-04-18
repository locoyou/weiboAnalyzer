package myApp;

import weibo4j.Timeline;
import weibo4j.model.Status;
import weibo4j.model.StatusWapper;

public class MyWeibo {
	public static void main(String[] args) {
		String token = "2.00wt5KGCI61mQC0b2b8917405nhplB";
		Timeline timeline = new Timeline();
		timeline.client.setToken(token);
		try {
			StatusWapper statusList = timeline.getUserTimelineByName("李开复");
			for (Status status:statusList.getStatuses()) {
				if(status.getCommentsCount() > 10) {
					System.out.println(status.getCommentsCount()+" "+status.getText());
					System.out.println(status.getCreatedAt());
					//StatusWapper repostStatusList = timeline.getRepostTimeline(status.getId());
					//for(Status repostStatus:repostStatusList.getStatuses()) {
						//System.out.println(repostStatus.getText());
					//}
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}