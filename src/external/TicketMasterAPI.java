package external;

import java.io.BufferedReader;
//import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI { // 作为Client向TicketMaster发请求，进行交互
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = "event"; // "", no restriction
	private static final String API_KEY = "27VGgvHACA6AISXSVFGU3KDUGK7U8L0G";

	public List<Item> search(double lat, double lon, String keyword) {
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			keyword = URLEncoder.encode(keyword, "UTF-8"); //可能有特殊字符，静态方法enconde(): "Rick Sun" => "Rick%20Sun"
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		// using geoHash instead of latlong
		String geoHash = GeoHash.encodeGeohash(lat, lon, 8);
		String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);
		//String query = String.format("apikey=%s&latlong=%s,%s&keyword=%s&radius=%s", API_KEY, lat, lon, keyword, 50); // latlong=%s,%s
		String url = URL + "?" + query;
		
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(); // default return: URLConnection
			connection.setRequestMethod("GET");
			
			int responseCode = connection.getResponseCode(); // 发请求+得到code
			System.out.println("Sending request to url: " + url);
			System.out.println("Response code: " + responseCode);
			
			if (responseCode != 200) {
				return new ArrayList<>(); //JSONArray() 
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();
			while ((line = reader.readLine()) != null) { //readLine()一行一行读
				 response.append(line);
			}
			reader.close();  // don't forget !
			JSONObject obj = new JSONObject(response.toString()); // toString() is important
			
			if (!obj.isNull("_embedded")) {
				JSONObject embedded = obj.getJSONObject("_embedded"); // "_embedded" is Key
				return getItemList(embedded.getJSONArray("events")); // "events" is Key
			}
			//connection.getInputStream(); // 通过client 读数据  
			//connection.getOutputStream(); // 往request body里面写东西	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
		}
		return new ArrayList<>(); 
	}
	
	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		// JSONArray 不能用for each 遍历, ArrayList可以
		for (int i = 0; i < events.length(); ++i) {
			JSONObject event = events.getJSONObject(i);
			// convert JSONObject to item class
			ItemBuilder builder = new ItemBuilder();
			if (!event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (!event.isNull("name")) {
				builder.setName(event.getString("name"));
			}
			if (!event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (!event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			if (!event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			// 下面三个不用写if判断，因为里面调用的helper函数判断了
			builder.setAddress(getAddress(event));
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			itemList.add(builder.build()); // don't forget
		}
		return itemList;
	}
	// 3 helper functions
	private String getAddress(JSONObject event) throws JSONException {
		if (!event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			if (!embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				for (int i = 0; i < venues.length(); ++i) { // 第一个不为空就返回第一个
					JSONObject venue = venues.getJSONObject(i);
					StringBuilder addressBuilder = new StringBuilder(); // StringBuilder 拼接字符串
					if (!venue.isNull("address")) {
						JSONObject address = venue.getJSONObject("address");
						if (!address.isNull("line1")) {
							addressBuilder.append(address.getString("line1"));
						}
						if (!address.isNull("line2")) {
							addressBuilder.append(",");
							addressBuilder.append(address.getString("line2"));
						}
						if (!address.isNull("line3")) {
							addressBuilder.append(",");
							addressBuilder.append(address.getString("line3"));
						}
					}
					
					if (!venue.isNull("city")) {
						JSONObject city = venue.getJSONObject("city");
						if (!city.isNull("name")) {
							addressBuilder.append(",");
							addressBuilder.append(city.getString("name"));
						}
					}
					
					String addressStr = addressBuilder.toString(); // toString is necessary !
					if (!addressStr.equals("")) {
						return addressStr;
					}
				}
			}
		}

		return "";
	}
  	
	private String getImageUrl(JSONObject event) throws JSONException {
		if (!event.isNull("images")) {
			JSONArray array = event.getJSONArray("images");
			for (int i = 0; i < array.length(); ++i) {
				JSONObject image = array.getJSONObject(i);
				if (!image.isNull("url")) {
					return image.getString("url"); // 第一个不为空就返回第一个
				}
			}

		}
		return "";
	}

	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>(); // 不止返回一个，我们希望返回所有该item属于的类型
		if (!event.isNull("classifications")) {
			JSONArray classifications = event.getJSONArray("classifications");
			for (int i = 0; i < classifications.length(); ++i) {
				JSONObject classification = classifications.getJSONObject(i);
				if (!classification.isNull("segment")) {
					JSONObject segment = classification.getJSONObject("segment");
					if (!segment.isNull("name")) {
						categories.add(segment.getString("name"));
					}
				}
			}
		}

		return categories;
	}
 	
	// test
	private void queryAPI(double lat, double lon) {  
		List<Item> events = search(lat, lon, null);
		for (Item event : events) {
			System.out.println(event.toJSONObject());
		}
	}
	
	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) { // add main to test
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
//		tmApi.queryAPI(29.682684, -95.295410);
	}
}
