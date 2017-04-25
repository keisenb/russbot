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

    private final String WEBSITE_URL = "https://testing.atodd.io/newsletter-generator/public/api";

    HashMap<String,Integer> day_map = new HashMap<String,Integer>(){{
        put( "SUNDAY", 0 );
        put( "MONDAY", 1 );
        put( "TUESDAY", 2 );
        put( "WEDNESDAY", 3 );
        put( "THURSDAY", 4 );
        put( "FRIDAY", 5 );
        put( "SATURDAY", 6 );
    }};

    @Override
    public String getRegexPattern() {
        return "![Nn]ews .*|![Nn]ews";
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
            "!news <day of week> - Returns the news for a specific day of the upcoming week."
        };
        return commands;
    }

    @Override
    public void messagePosted(String message, String channel) {

        JSONArray json;
        SlackAttachment[] attachments;
        SlackPreparedMessage response;

        if (message.toLowerCase().startsWith("!news clubs")) {

            json = clubsNewsRequest(WEBSITE_URL);
            attachments = BuildMessage(json, channel);
            response = BuildSlackMessage(attachments);
            Session.getInstance().sendPreparedMessage(channel, response);

        } else if (message.toLowerCase().startsWith("!news ")) {

            int withoutNews = 6;
            Calendar cal = Calendar.getInstance();
            int day = cal.get(Calendar.DAY_OF_WEEK) - 1;
            String msg = message.substring(withoutNews).toUpperCase();
            Integer value = day_map.get(msg);

            if ( value != null ) {
                json = dayNewsRequest(WEBSITE_URL, value);
                attachments = BuildMessage(json, channel);
                response = BuildSlackMessage(attachments);
                Session.getInstance().sendPreparedMessage(channel, response);
            } else if(msg.startsWith("TODAY")) {
                json = dayNewsRequest(WEBSITE_URL, day);
                attachments = BuildMessage(json, channel);
                response = BuildSlackMessage(attachments);
                Session.getInstance().sendPreparedMessage(channel, response);
            } else if(msg.startsWith("TOMORROW")) {
                json = dayNewsRequest(WEBSITE_URL, day+ 1);
                attachments = BuildMessage(json, channel);
                response = BuildSlackMessage(attachments);
                Session.getInstance().sendPreparedMessage(channel, response);
            } else {
                Session.getInstance().sendMessage("Invalid day. Try !news tuesday", channel);
            }

        } else if(message.toLowerCase().startsWith("!news")) {

            json = allNewsRequest(WEBSITE_URL);
            attachments = BuildMessage(json, channel);
            response = BuildSlackMessage(attachments);
            Session.getInstance().sendPreparedMessage(channel, response);
        }
    }

    public SlackPreparedMessage BuildSlackMessage(SlackAttachment[] attachments) {

        Builder builder = new Builder();
        if(attachments.length != 0) {
            builder.withMessage("*CS Weekly Newsletter*");
        }
        else {
            builder.withMessage("No events!");
        }
        for (SlackAttachment attachment : attachments) {
            builder.addAttachment(attachment);
        }
        return builder.build();
    }



    public SlackAttachment[] BuildMessage(JSONArray articles, String channel) {

        int listOfDaysLength = 8;
        attachments = new ArrayList<List<SlackAttachment>>(listOfDaysLength);
        for(int x = 0; x < listOfDaysLength; x++) {
            attachments.add(new ArrayList<SlackAttachment>());
        }
        for(int x = 0; x < articles.length(); x ++) {

            JSONObject article = articles.getJSONObject(x);
            String title = article.getString("title");
            String text = article.getString("text");
            Article art = new Article(title, text);
            String date = "", location = "", link = "";

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
            HttpResponse<String> response = Unirest.get(url + "/articles").asString();
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

    public JSONArray dayNewsRequest(String url, int day) {
        try {
            HttpResponse<String> response = Unirest.get(url + "/articles/daily/" + day).asString();
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

    public JSONArray clubsNewsRequest(String url) {
        try {
            HttpResponse<String> response = Unirest.get(url + "/articles/clubs").asString();
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
        private int day = 7;

        public Article(String title, String text) {
            this.title = title;
            this.text = text;
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
