/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package russbot.plugins;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import russbot.Session;
import org.json.JSONObject;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;


/**
 *
 * @author keisenb
 */
public class CSWeekly implements Plugin {

    private final String WEBSITE_URL = "https://testing.atodd.io/newsletter-generator/public/";

    String[] days = {
        "*Sunday*\n",
        "*Monday*\n",
        "*Tuesday*\n",
        "*Wednesday*\n",
        "*Thursday*\n",
        "*Friday*\n",
        "*Saturday*\n"
    };

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
            String msg = BuildMessage(json);
            Session.getInstance().sendMessage(msg, channel);
        }
    }

    public void AddArticle(int day, String article) {
        days[day -1] += article;
    }

    

    public String BuildMessage(JSONArray articles) {
        String message = "", other = "";

        for(int x = 0; x < articles.length(); x ++) {

            JSONObject article = articles.getJSONObject(x);
            String date = "", location = "", link = "", title = article.getString("title");

            if(!article.isNull("location")) {
                location = article.getString("location");
            }
            if(!article.isNull("link")) {

                link = article.getString("link");
            }
            if(article.isNull("date")) {
                if(other == "") {
                    other += "*Other Announcements*\n";
                }
                other += ">\u2022 " + title + "\n";
            } else {

                date = article.getString(("date"));
                Calendar cal = CreateCalendar(date);
                String time = ParseTime(cal);
                String web = "";
                if(link != "") {
                    web = " - <" + link + "| read more>";
                }
                String entry = ">\u2022 " + title+  " @ "  + time + " - " + location + web + "\n";
                AddArticle(cal.get(Calendar.DAY_OF_WEEK), entry);
            }
        }
        for(int x = 0; x < days.length; x ++) {
            message += days[x];
        }
        message += other;
        message += "\n" + "To learn more about these events check out the online newsletter here! " + WEBSITE_URL;
        return message;
}

    public Calendar CreateCalendar(String date) {
        try
        {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d = simpleDateFormat.parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            return cal;
        }
        catch (Exception ex)
        {
            System.out.println("Exception "+ex.toString());
            return null;
        }
    }

    public String ParseTime(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min =  cal.get(Calendar.MINUTE);
        return hour%12 + ":" + min + ((min==0) ? "0" : "") + " " + ((hour>=12) ? "PM" : "AM");
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
}
