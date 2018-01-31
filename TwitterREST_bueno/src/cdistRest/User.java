package cdistRest;

import com.google.gson.annotations.SerializedName;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("user")
public class User {

	   @Id ObjectId id_mongo;
	
       @SerializedName("friends_count")
       public int friends_count;

       @SerializedName("profile_background_color")
       public String profile_background_color;

       @SerializedName("profile_background_image_url")
       public String profile_background_image_url;

       @SerializedName("created_at")
       public String created_at;

       @SerializedName("description")
       public String description;

       @SerializedName("favourites_count")
       public int favourites_count;

       @SerializedName("lang")
       public String lang;

       @SerializedName("notifications")
       public boolean notifications;

       @SerializedName("id_str")
       public String id_str;

       @SerializedName("default_profile_image")
       public boolean default_profile_image;

       @SerializedName("profile_text_color")
       public String profile_text_color;

       @SerializedName("default_profile")
       public boolean default_profile;

       @SerializedName("show_all_inline_media")
       public boolean show_all_inline_media;

       @SerializedName("contributors_enabled")
       public boolean contributors_enabled;

       @SerializedName("geo_enabled")
       public boolean geo_enabled;

       @SerializedName("screen_name")
       public String screen_name;

       @SerializedName("profile_sidebar_fill_color")
       public String profile_sidebar_fill_color;

       @SerializedName("profile_image_url")
       public String profile_image_url;

       @SerializedName("profile_background_tile")
       public boolean profile_background_tile;

       @SerializedName("follow_request_sent")
       public boolean follow_request_sent;

       @SerializedName("url")
       public String url;

       @SerializedName("statuses_count")
       public int statuses_count;

       @SerializedName("following")
       public boolean following;

       @SerializedName("time_zone")
       public String time_zone;

       @SerializedName("profile_link_color")
       public String profile_link_color;

       @SerializedName("protected")
       public boolean protectedd;

       @SerializedName("verified")
       public boolean verified;

       @SerializedName("profile_sidebar_border_color")
       public String profile_sidebar_border_color;

       @SerializedName("followers_count")
       public int followers_count;

       @SerializedName("location")
       public String location;

       @SerializedName("name")
       public String name;

       @SerializedName("is_translator")
       public boolean is_translator;

       @SerializedName("id")
       public long id;

       @SerializedName("listed_count")
       public int listed_count;

       @SerializedName("profile_use_background_image")
       public boolean profile_use_background_image;

       @SerializedName("utc_offset")
       public int utc_offset;

}