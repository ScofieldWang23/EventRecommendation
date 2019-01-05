package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;


public class MySQLConnection implements DBConnection{
	private Connection connection;
	
	public MySQLConnection() {
	   	 try {
	   		 Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance(); // Ensure the driver is registered
	   		 connection  = DriverManager.getConnection(MySQLDBUtil.URL);
	   		 
	   	 } catch (Exception e) {
	   		 e.printStackTrace();
	   	 }
	}

	@Override
	public void close() {
		 if (connection != null) {
	   		 try {
	   			 connection.close();
	   		 } catch (Exception e) {
	   			 e.printStackTrace();
	   		 }
	   	 }
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if (connection == null) {
			System.err.println("DB connection failed");
	   		return;
		}
		try {
			 String sql = "INSERT IGNORE INTO history(user_id, item_id) VALUES (?, ?)";
	   		 PreparedStatement ps = connection.prepareStatement(sql);
	   		 ps.setString(1, userId);
	   		 for (String itemId : itemIds) { // all items liked by user
	   			 ps.setString(2, itemId);
	   			 ps.execute();
	   		 }
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		// TODO Auto-generated method stub
		if (connection == null) {
			System.err.println("DB connection failed");
	   		return;
		}
		try {
			 String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
	   		 PreparedStatement ps = connection.prepareStatement(sql);
	   		 ps.setString(1, userId);
	   		 for (String itemId : itemIds) {
	   			 ps.setString(2, itemId);
	   			 ps.execute();
	   		 }
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (connection == null) {
			return new HashSet<>();
		}
		
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, itemId);
				ResultSet rs = stmt.executeQuery();
				ItemBuilder builder = new ItemBuilder();
				
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rs.getDouble("distance"));
					builder.setRating(rs.getDouble("rating"));
					favoriteItems.add(builder.build()); // don't forget!
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (connection == null) {
			return new HashSet<>();
		}
		Set<String> favoriteItemId = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, userId);
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				favoriteItemId.add(itemId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return favoriteItemId; //id

	}

	@Override
	public Set<String> getCategories(String itemId) {
		if (connection == null) {
			return null;
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? ";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				String category = rs.getString("category");
				categories.add(category);
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) { // term is category
		TicketMasterAPI ticketMasterAPI = new TicketMasterAPI();
	    List<Item> items = ticketMasterAPI.search(lat, lon, term);
	    // save data to database
	    for(Item item : items) {
	    	saveItem(item);
	    }
	    return items;
	}

	@Override
	public void saveItem(Item item) { // save data to database
		if (connection == null) {
			System.err.println("DB connection failed");
			return;
		}
		
		// sql injection
	   	// select * from users where username = '' AND password = '';
	   	// username: fakeuser ' OR 1 = 1; DROP  --
	   	// select * from users where username = 'fakeuser ' OR 1 = 1 --' AND password = '';
	   	 
	   	 try {
	   		 String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
	   		 PreparedStatement ps = connection.prepareStatement(sql);
	   		 ps.setString(1, item.getItemId());
	   		 ps.setString(2, item.getName());
	   		 ps.setDouble(3, item.getRating());
	   		 ps.setString(4, item.getAddress());
	   		 ps.setString(5, item.getImageUrl());
	   		 ps.setString(6, item.getUrl());
	   		 ps.setDouble(7, item.getDistance());
	   		 ps.execute();

	   		 sql = "INSERT IGNORE INTO categories VALUES(?, ?)";
	   		 ps = connection.prepareStatement(sql);
	   		 ps.setString(1, item.getItemId());
	   		 
	   		 for(String category : item.getCategories()) { // all categories
	   			 ps.setString(2, category);
	   			 ps.execute();
	   		 }
	   	 } catch (Exception e) {
	   		 e.printStackTrace();
	   	 }
	}

	@Override
	public String getFullname(String userId) {
		if (connection == null) {
			return "";
		}
		
		String name = "";
		try {
			String sql = "SELECT first_name, last_name from users WHERE user_id = ? "; // ?的好处：防止sql injection, 告诉mysql user_id作为普通字符串处理
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery(); //有一行或者没有：1个或0个
			while (rs.next()) { // if也行
				name = rs.getString("first_name") + " " + rs.getString("last_name");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return name;
	}
	
	@Override
	public boolean verifyLogin(String userId, String password) {
		if (connection == null) {
			return false;
		}
		
		try {
			String sql = "SELECT user_id from users WHERE user_id = ? AND password = ?"; // user_id 可以改为 count(*)
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery(); //有一行或者没有：1个或0个
			if (rs.next()) { // 有没有返回数据
				return true;
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
		return false;
	}

}
