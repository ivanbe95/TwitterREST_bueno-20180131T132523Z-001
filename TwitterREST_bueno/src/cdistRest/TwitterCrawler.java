package cdistRest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.mashape.unirest.request.body.RequestBodyEntity;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import com.mongodb.MongoClient;

public class TwitterCrawler implements Callback<String>, Runnable {

	private static final String oauth_consumer_key = "ZryvOAmB32wb87Gtp7htGAS2I";
	private static final String oauth_consumer_secret = "rIyYKlBz8l5r3oyesAA0sm8xSDgferFmRE9j5UWQWHD0ngUFu5";

	private static String nombre[] = { "Pablo_Iglesias_", "Albert_Rivera", "sanchezcastejon", "marianorajoy" };

	public class BearerToken {
		private String access_token;
		private String token_type;

	}

	public static String deserializeJson(String input_json) {
		Gson gson = new Gson();
		BearerToken new_bearerToken = gson.fromJson(input_json, BearerToken.class);
		return new_bearerToken.access_token;
	}

	private long max_id;
	private String bt;
	public Object mySyncObject = new Object();
	private List<Tweet> tweet_list = new ArrayList<Tweet>();
	private String screen_name;
	public static final Logger logger = Logger.getLogger(TwitterCrawler.class.getName());

	static int retuits_hechos = 0;

	public TwitterCrawler(String screen_name) {
		this.screen_name = screen_name;
		/*
		 * get bearer token according to https://dev.twitter.com/oauth/application-only
		 */
		String URLEncoderConsumerKey;
		try {
			URLEncoderConsumerKey = URLEncoder.encode(oauth_consumer_key, "UTF-8");

			String URLEncoderConsumerSecret = URLEncoder.encode(oauth_consumer_secret, "UTF-8");
			String AuthorizationHeader = URLEncoderConsumerKey + ":" + URLEncoderConsumerSecret;
			String AuthorizationHeaderB64 = Base64.encodeBase64String(AuthorizationHeader.getBytes("UTF8"));

			System.out.println(AuthorizationHeaderB64);
			RequestBodyEntity postReq = Unirest.post("https://api.twitter.com/oauth2/token")
					.header("User-Agent", "TwitterApp").header("host", "api.twitter.com")
					.header("Authorization", "Basic " + AuthorizationHeaderB64)
					.header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
					.body("grant_type=client_credentials");
			HttpResponse<String> res = postReq.asString();
			bt = TwitterCrawler.deserializeJson(res.getBody());

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			System.out.println("Primer catch");
			e.printStackTrace();

		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			System.out.println("Segundo catch");
			e.printStackTrace();

		}
	}

	@Override
	public void cancelled() {
		logger.info("The request has been canceled");

	}

	@Override
	public void completed(HttpResponse<String> json_str_Response) {
		synchronized (mySyncObject) {
			List<Tweet> tweet_list_aux = Tweet.deserializeJsonArray(json_str_Response.getBody());
			// add current results to the global list of the twitterCrawler
			this.tweet_list.addAll(tweet_list_aux);
			System.out.println("Request completed to " + screen_name + ". " + tweet_list_aux.size() + " tweets added!");
			System.out.println("Json : " + json_str_Response);
			mySyncObject.notify();

		}

	}

	@Override
	public void failed(UnirestException arg0) {
		logger.severe("The request has been canceled \n" + arg0.getMessage());
	}

	public long getMax_id() {
		return max_id;
	}

	public void setMax_id(long max_id) {
		this.max_id = max_id;
	}

	public void updateLowestId(List<Tweet> tweet_list) {
		/* finding the lowest ID */
		long max_id = Long.MAX_VALUE;
		for (Tweet tw : tweet_list) {
			if (tw.id < max_id)
				max_id = tw.id;
		}
	}

	public String getBearerToken() {
		return bt;
	}

	public void getTweets() {
		// GET statuses/home_timeline
		synchronized (mySyncObject) {
			Future<HttpResponse<String>> json_str_Response = null;
			GetRequest getReq = null;
			getReq = Unirest.get(
					"https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name={screen_name}&count={count}")
					.routeParam("screen_name", this.screen_name).routeParam("count", "100")
					.header("Authorization", "Bearer " + getBearerToken());
			System.out.println("Request to: " + getReq.getUrl());
			System.out.println("Authorization header Bearer " + getBearerToken());
			// now the interface is implemented by TwitterCrawler
			json_str_Response = getReq.asStringAsync(this);

		}

	}

	public void waitForCompletion() throws InterruptedException {
		synchronized (mySyncObject) {
			mySyncObject.wait();

		}

	}

	@SuppressWarnings({ "deprecation", "unused", "unused" })
	public static void main(String args[]) throws IOException {
		/* instantiate twitterCrawler */

		List<Thread> lThread = new ArrayList<>();
		// TwitterCrawler crawlers[] = { crawler_podemos, crawler_cs, crawler_psoe,
		// crawler_pp };

		for (String screen_name : nombre) {
			TwitterCrawler tcCrawler = new TwitterCrawler(screen_name);
			Thread t = new Thread(tcCrawler);
			t.start();
			lThread.add(t);
		}

		for (Thread t : lThread) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Unirest.shutdown();

		/////////////////////////////////////////////////////////////
		// Tweets guardados; Empieza la recuperación de datos
		/////////////////////////////////////////////////////////////

		MongoClient mongo = new MongoClient("localhost", 27017);
		Morphia morphia = new Morphia();
		morphia.map(Tweet.class);

		// Crear datastore para cada cuenta
		Datastore[] ds = new Datastore[nombre.length];
		for (int i = 0; i < nombre.length; i++) {
			ds[i] = morphia.createDatastore(mongo, nombre[i]);
		}

		List<Integer> retuits_propios = new ArrayList<>();
		List<Integer> retuits_otros = new ArrayList<>();
		List<Integer> fav = new ArrayList<>();
		List<String> date = new ArrayList<>();

		/////////////////////////////////////////////////////////////
		// Print en output.txt
		/////////////////////////////////////////////////////////////

		PrintWriter writer = new PrintWriter("output.txt", "UTF-8");
		// writer.println("The first line2");
		// ds[0].createQuery(Tweet.class).asList().forEach(p -> {
		// retuits_obtenidos.add(p.retweet_count);
		// int acumulado = retuits_obtenidos.stream().mapToInt(Integer::intValue).sum();
		// writer.println("Acumulado: "+acumulado+"\t\tFecha: "+p.created_at);
		// System.out.println(p.user.name + " " + sum(total_RT));
		// });

		// for (int k = 0; k<ds.length; k++) {
		// retuits_hechos = 0;
		// ds[k].createQuery(Tweet.class).asList().forEach(p -> {
		// if (p.text.charAt(0) == 'R' && p.text.charAt(1) == 'T' && p.text.charAt(2) ==
		// ' '
		// && p.text.charAt(3) == '@') {
		//// System.out.println(p.user.name + " RT");
		// retuits_hechos++;
		// } else {
		// date.add(p.created_at);
		// rtc.add(p.retweet_count);
		// fav.add(p.favorite_count);
		//
		//// System.out.println(p.user.name + " " + p.retweet_count);
		// }
		// });
		// writer.println(nombre[k]+" ha hecho "+retuits_hechos+" retuits.");
		// }
		//
		// int sum = retuits_obtenidos.stream().mapToInt(Integer::intValue).sum();
		// writer.println("Total retweets de "+nombre[0]+": " + sum);
		// writer.close();

		int total_retuits_propios, total_retuits_otros, total_favs;

		PrintWriter salida = new PrintWriter("salida.csv", "UTF-8");

		StringBuilder sb = new StringBuilder();
		sb.append("Nombre");
		sb.append(',');
		sb.append("Retuits propios");
		sb.append(',');
		sb.append("Retuits de otros");
		sb.append(',');
		sb.append("Favs");
		sb.append('\n');

		PrintWriter fechas = new PrintWriter("fechas.csv", "UTF-8");

		StringBuilder sb_fecha = new StringBuilder();
		sb_fecha.append("Nombre");
		sb_fecha.append(',');
		sb_fecha.append("Retuits");
		sb_fecha.append(',');
		sb_fecha.append("Fecha");
		sb_fecha.append(',');
		sb_fecha.append("Dia del año");
		sb_fecha.append(',');
		sb_fecha.append("Dia de la semana");
		sb_fecha.append('\n');

		for (int j = 0; j < ds.length; j++) {
			total_retuits_propios = 0;
			total_retuits_otros = 0;
			total_favs = 0;
			retuits_hechos = 0;

			retuits_propios.clear();
			retuits_otros.clear();
			fav.clear();
			date.clear();

			ds[j].createQuery(Tweet.class).asList().forEach(p -> {
				if (p.text.charAt(0) == 'R' && p.text.charAt(1) == 'T' && p.text.charAt(2) == ' '
						&& p.text.charAt(3) == '@') {
					// System.out.println(p.user.name + " RT");
					retuits_otros.add(p.retweet_count);
					retuits_hechos++;
				} else {
					retuits_propios.add(p.retweet_count);
					date.add(p.created_at);
					fav.add(p.favorite_count);

					int acumulado = retuits_propios.stream().mapToInt(Integer::intValue).sum();
					// writer.println("Acumulado: " + acumulado + "\t\tFecha: " + p.created_at);

					// ArrayList<Date> dateList = new ArrayList<Date>();

					// SimpleDateFormat sdf = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy",
					// Locale.ENGLISH);
					//
					// ArrayList<Date> dateList = new ArrayList<Date>();
					// try {
					// dateList.add((Date) sdf.parse(p.created_at));
					// } catch (ParseException e) {
					// e.printStackTrace();
					// }
					// int i = 0;
					// int count = 0;
					// // System.out.println(dateList.get(0).getDate());
					// Calendar cal = new GregorianCalendar();
					// cal.setTime(dateList.get(0)); // Give your own date
					// System.out.println(cal.get(Calendar.DAY_OF_YEAR));
					// System.out.println(cal.get(Calendar.DAY_OF_WEEK));
					//
					// sb_fecha.append(p.user.name);
					// sb_fecha.append(',');
					// sb_fecha.append(p.retweet_count);
					// sb_fecha.append(',');
					// try {
					// sb_fecha.append((Date) sdf.parse(p.created_at));
					// } catch (ParseException e) {
					// e.printStackTrace();
					// }
					// sb_fecha.append(p.retweet_count);
					// sb_fecha.append(',');
					// sb_fecha.append(p.retweet_count);
					// sb_fecha.append(',');
					// sb_fecha.append('\n');

				}
			});

			SimpleDateFormat sdf = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
			ArrayList<Date> dateList = new ArrayList<Date>();
			Calendar cal = new GregorianCalendar();
			String[] dias_semana = {"Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado" };

			for (int i = 0; i < date.size(); i++) {

				try {
					dateList.add((Date) sdf.parse(date.get(i)));
				} catch (ParseException e) {
					e.printStackTrace();
				}

				cal.setTime(dateList.get(i));

				sb_fecha.append(nombre[j]);
				sb_fecha.append(',');
				sb_fecha.append(retuits_propios.get(i));
				sb_fecha.append(',');
				try {
					sb_fecha.append((Date) sdf.parse(date.get(i)));
				} catch (ParseException e) {
					e.printStackTrace();
				}
				sb_fecha.append(',');
				sb_fecha.append(cal.get(Calendar.DAY_OF_YEAR));
				sb_fecha.append(',');
				sb_fecha.append(dias_semana[cal.get(Calendar.DAY_OF_WEEK)-1]);
				sb_fecha.append(',');
				sb_fecha.append('\n');

			}

			total_retuits_propios = retuits_propios.stream().mapToInt(Integer::intValue).sum();
			total_retuits_otros = retuits_otros.stream().mapToInt(Integer::intValue).sum();
			total_favs = fav.stream().mapToInt(Integer::intValue).sum();

			sb.append(nombre[j]);
			sb.append(',');
			sb.append(total_retuits_propios);
			sb.append(',');
			sb.append(total_retuits_otros);
			sb.append(',');
			sb.append(total_favs);
			sb.append('\n');

			long total = ds[j].getCount(Tweet.class);

			writer.println("Total retweets de " + nombre[j] + ": " + (total_retuits_propios + total_retuits_otros)
					+ " [" + total + "]");
			;
			writer.println("De los cuales " + total_retuits_propios + " son de sus propios tuits ["
					+ (total - retuits_hechos) + "].");
			writer.println(total_retuits_otros + " son de otras cuentas");
			// writer.println(date.toString());

			// ArrayList<Date> dateList = new ArrayList<Date>();
			// SimpleDateFormat sdf = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy",
			// Locale.ENGLISH);
			// for (String dateString : date) {
			// try {
			// dateList.add((Date) sdf.parse(dateString));
			// } catch (ParseException e) {
			// e.printStackTrace();
			// }
			// }
			// int i = 0;
			// int count = 0;
			// // System.out.println(dateList.get(0).getDate());
			// Calendar cal = new GregorianCalendar();
			// cal.setTime(dateList.get(0)); // Give your own date
			// System.out.println(cal.get(Calendar.DAY_OF_YEAR));
			// System.out.println(cal.get(Calendar.DAY_OF_WEEK));

			// while (dateList.size() > i) {
			//
			// Calendar cal = new GregorianCalendar();
			// cal.setTime(dateList.get(i)); // Give your own date
			// System.out.println(cal.get(Calendar.DAY_OF_YEAR));
			//
			// }

			// while (dateList.size() > i+1) {
			// long diff = dateList.get(i).getTime() - dateList.get(i+1).getTime();
			// long diffSeconds = diff / 1000 % 60;
			// long diffMinutes = diff / (60 * 1000) % 60;
			// long diffHours = diff / (60 * 60 * 1000) % 24;
			// long diffDays = diff / (24 * 60 * 60 * 1000);
			//
			// writer.println(dateList.get(i) + "..." + dateList.get(i+1));
			// writer.println(diffDays + ":" + diffHours + ":" + diffMinutes + ":" +
			// diffSeconds);
			// i++;
			// }
			// System.out.println(dateList.get(0) + "..." + dateList.get(1));
			// System.out.println(diffDays + ":" + diffHours + ":" + diffMinutes + ":" +
			// diffSeconds);

			writer.println();
		}
		salida.write(sb.toString());
		fechas.write(sb_fecha.toString());
		writer.close();
		salida.close();
		fechas.close();
		System.out.println("done!");

		/*
		 * List<Tweet> tl2 = ds.createQuery(Tweet.class).asList(); tl2.forEach( a -> {
		 * Field[] f = Tweet.class.getDeclaredFields(); Arrays.asList(f).forEach(j -> {
		 * Field val; try { val = a.getClass().getDeclaredField(j.getName());
		 * System.out.println((String)val.get(a)); } catch (NoSuchFieldException |
		 * SecurityException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (IllegalArgumentException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); } catch
		 * (IllegalAccessException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 * 
		 * 
		 * });
		 * 
		 * } );
		 */

		// forEach(x -> {
		// rtc.add(new Integer(x.retweet_count));
		// });

	}

	@Override
	public void run() {
		logger.info(screen_name + " :: " + "get tweets...");
		getTweets();
		logger.info(screen_name + " :: " + "done ");
		MongoClient mongo = new MongoClient("localhost", 27017);
		Morphia morphia = new Morphia();
		morphia.map(Tweet.class);
		List<Tweet> proccessedTweets1 = tweet_list;

		Datastore ds;
		for (int i = 0; i < nombre.length; i++) {
			ds = morphia.createDatastore(mongo, nombre[i]);
			ds.delete(ds.createQuery(Tweet.class));
		}

		ds = morphia.createDatastore(mongo, screen_name);
		try {
			waitForCompletion();
			logger.info(screen_name + " :: " + "committ ");
			ds.save(proccessedTweets1);
			logger.info(screen_name + " :: " + "done ");
			System.out.println("Shutting down unirest for crawler " + screen_name);

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}