/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package russbot.plugins;

import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage.Builder;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import russbot.Session;
import org.json.JSONObject;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author keisenb
 */
public class CSWeekly implements Plugin {

    List<List<SlackAttachment>> attachments = null;

    private final String WEBSITE_URL = "https://testing.atodd.io/newsletter-generator/public/";
    public enum Days {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
    }

    @Override
    public String getRegexPattern() {
        return "![Nn]ews .*|![Nn]ews|![Nn]ews help";
    }

    @Override
    public String[] getChannels() {
        String[] channels = {
            "test"
        };
        return channels;
    }

    @Override
    public String getInfo() {
        return "CS Weekly Newsletter";
    }

    @Override
    public String[] getCommands() {
        String[] commands = {
            "!news - Returns a list of Computer Science events for the week.",
            "!news help - Returns information on how to use the plugin.",
            "!news <day of week> - Returns the news for a specific day of the upcoming week."
        };
        return commands;
    }

    @Override
    public void messagePosted(String message, String channel) {

        if (message.toLowerCase().startsWith("!news help")) {
            String msg = "This is the help message";
            Session.getInstance().sendMessage(msg, channel);
        } else if (message.toLowerCase().startsWith("!news ")) {
            String msg = message.substring(6);
            //todo
            Session.getInstance().sendMessage(msg, channel);
        } else if(message.toLowerCase().startsWith("!news")) {
            JSONArray json = allNewsRequest(WEBSITE_URL);
            SlackAttachment[] attachments = BuildMessage(json, channel);
            SlackPreparedMessage msg = BuildSlackMessage(attachments);
            Session.getInstance().sendPreparedMessage(channel, msg);
        }
    }

    public SlackPreparedMessage BuildSlackMessage(SlackAttachment[] attaches) {

        Builder builder = new Builder();
        builder.withMessage("*CS Weekly Newsletter*");
        for (SlackAttachment attachment : attaches) {
            builder.addAttachment(attachment);
        }
        return builder.build();
    }



    public SlackAttachment[] BuildMessage(JSONArray articles, String channel) {

        attachments = new ArrayList<List<SlackAttachment>>(8);
        for(int x = 0; x < 8; x++) {
            attachments.add(new ArrayList<SlackAttachment>());
        }
        for(int x = 0; x < articles.length(); x ++) {

            JSONObject article = articles.getJSONObject(x);
            String category = article.getJSONArray("categories").getJSONObject(0).getString("category");
            Article art = new Article(article.getString("title"), article.getString("text"), category);

            String date = "", location = "", link = "", title = article.getString("title"), text = article.getString("text");

            if(!article.isNull("location")) {
                art.setLocation(article.getString("location"));
            }
            if(!article.isNull("link")) {
                art.setLink(article.getString("link"));
            }
            if(!article.isNull("date")) {
                Date d = CreateDate(article.getString(("date")));
                art.setDate(d);
            }

            SlackAttachment attachment = art.createAttachment();
            int day = art.GetDay();
            attachments.get(day).add(attachment);

        }

        List<SlackAttachment> result = new ArrayList<SlackAttachment>();
        for(List<SlackAttachment> array : attachments) {
            result.addAll(array);
        }

        SlackAttachment[] arrayResult = new SlackAttachment[articles.length()];
        arrayResult = result.toArray(arrayResult);
        return arrayResult;
    }


    public Date CreateDate(String date) {
        try
        {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = format.parse(date);
            return d;
        }
        catch (Exception ex)
        {
            System.out.println("Exception "+ex.toString());
            return null;
        }
    }

    public JSONArray allNewsRequest(String url) {
        try {
            HttpResponse<String> response = Unirest.get("https://testing.atodd.io/newsletter-generator/public/api/articles").asString();
            String body = response.getBody();
            JSONObject object = new JSONObject(body);
            JSONArray articles = object.getJSONArray("articles");
            return articles;
        }
        catch (Exception ex) {
            System.out.println(ex.toString());
            return null;
        }
    }

    private static class Article implements Comparable<Article> {

        public enum Category { GENERAL, CLUB, OTHER, JOB }
        private String title;
        private Date date = null;
        private String location = null;
        private String link = null;
        private String text;
        private Category category;
        private int day = 7;

        public Article(String title, String text, String category) {
            this.title = title;
            this.text = text;
            this.category = parseCategory(category);
        }

        public Category parseCategory(String category) {
            HashMap<String, Category> map = new HashMap<String, Category>(){{
            put("General Announcements", Category.GENERAL);
            put("Club Announcements", Category.CLUB);
            put("Other Announcements", Category.OTHER);
            put("Job Opportunities", Category.JOB);
            }};

            return map.get(category);
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public void setLink(String link) {
            this.link = link;
        }

        @Override
        public int compareTo(Article a)
        {
            return this.date.compareTo(a.Date());
        }

        //todo override toString method

        public SlackAttachment createAttachment() {
            String time = null;
            if(date != null) {
                time = ParseTime(CreateCalendar(date));
            }
            String contents = title + (time != null ? " - "+time : "") + (location != null ? " - "+location : "");

            SlackAttachment attachment = new SlackAttachment(contents, "", text, "");
            attachment.addMiscField("color", "#512888");
            attachment.setTitleLink(link);
            return attachment;
        }

        public Date Date() {
            return date;
        }

        public Calendar CreateCalendar(Date date) {
            try
            {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                day = cal.get(Calendar.DAY_OF_WEEK) -1;
                return cal;
            }
            catch (Exception ex)
            {
                System.out.println("Exception "+ex.toString());
                return null;
            }
        }

        public int GetDay() {
            return day;
        }

        public String ParseTime(Calendar cal) {

            SimpleDateFormat format = new SimpleDateFormat("EEEE, M/dd @ h:mm a");
            return format.format(cal.getTime());
        }

    }
}
