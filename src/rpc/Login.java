package rpc;

import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//import org.json.JSONArray;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;

/**
 * Servlet implementation class Login
 */
@WebServlet("/login")
public class Login extends HttpServlet {
	private static final long serialVersionUID = 1L;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Login() {
        super();
        // TODO Auto-generated constructor stub
    }
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException { //检查session是否有效
		DBConnection connection = DBConnectionFactory.getConnection();
		try { 
			HttpSession session = request.getSession(false); //避免在session不存在时新创建一个session
			JSONObject obj = new JSONObject();
			
			if (session != null) {
				String userId = session.getAttribute("user_id").toString(); //getAttribute()
				obj.put("status", "OK").put("user_id", userId).put("name", connection.getFullname(userId));
			} else {
				response.setStatus(403);
				obj.put("status", "Invalid Session");
			}
	   		RpcHelper.writeJsonObject(response, obj);
		} catch (Exception e) {
			
		} finally {
			connection.close();
		} 
	}
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		DBConnection connection = DBConnectionFactory.getConnection();
		try { 
			 JSONObject input = RpcHelper.readJSONObject(request); 
	   		 String userId = input.getString("user_id");
	   		 String password = input.getString("password");
	   		 
	   		 JSONObject obj = new JSONObject();
	   		 if (connection.verifyLogin(userId, password)) {
	   			 HttpSession session = request.getSession(); //通过request创建：返回或新创建一个
	   			 session.setAttribute("user_id", userId);
	   			 session.setMaxInactiveInterval(600); //有效期，多少秒
	   			 obj.put("status", "OK").put("user_id", userId).put("name", connection.getFullname(userId)); //方便前端debug
	   		 } else {
	   			 response.setStatus(401); //用户不存在
	   			 obj.put("status", "User doesn't exist!");
	   		 }
	   		 RpcHelper.writeJsonObject(response, obj);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connection.close();
		} 
	}
}
